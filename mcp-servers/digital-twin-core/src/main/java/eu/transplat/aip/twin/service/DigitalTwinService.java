package eu.transplat.aip.twin.service;

import eu.transplat.aip.mcp.common.Confidence;
import eu.transplat.aip.mcp.common.McpResponse;
import eu.transplat.aip.mcp.common.McpStatus;
import eu.transplat.aip.twin.client.DownstreamClient;
import eu.transplat.aip.twin.config.DigitalTwinProperties;
import eu.transplat.aip.twin.domain.DigitalTwinModel;
import eu.transplat.aip.twin.domain.DownstreamSlice;
import eu.transplat.aip.twin.domain.ReleaseReadiness;
import eu.transplat.aip.twin.domain.SubState;
import eu.transplat.aip.twin.domain.TechDebtReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The orchestrator brain. Fans out to the live MCP servers (Jira, GitHub,
 * Sonar) over HTTP, merges their slices into a {@link DigitalTwinModel} and
 * exposes high-level commands as MCP {@code @Tool}s.
 *
 * <p>RESILIENCE: no method ever throws out of a tool/endpoint. If a downstream
 * is unreachable its slice is marked {@code DATA_STALE} and the overall
 * confidence drops — LOW if any <em>critical</em> slice is stale, MEDIUM if only
 * non-critical slices are stale, HIGH only when every live slice is OK. A single
 * downstream being down never fails the whole call.
 */
@Service
public class DigitalTwinService {

    /** Provenance label carried in every {@link McpResponse}. */
    public static final String SOURCE = "digital-twin-core";

    private static final Logger log = LoggerFactory.getLogger(DigitalTwinService.class);

    private final DownstreamClient downstream;
    private final DigitalTwinProperties properties;

    public DigitalTwinService(DownstreamClient downstream, DigitalTwinProperties properties) {
        this.downstream = downstream;
        this.properties = properties;
    }

    // ------------------------------------------------------------------ tools

    @Tool(description = "SHOW_PROJECT_STATE: fan out to Jira (delivery), GitHub (code) and Sonar (quality+debt), "
            + "merge into the DIGITAL_TWIN_MODEL with provenance, freshness, overall confidence and recommendations.")
    public McpResponse showProjectState() {
        try {
            DigitalTwinModel model = buildModel();
            String message = model.staleSources().isEmpty()
                    ? null
                    : "Stale sources: " + model.staleSources();
            return new McpResponse(model, statusFor(model.overallConfidence()),
                    SOURCE, model.overallConfidence(), message, Instant.now());
        } catch (Exception e) {
            log.warn("showProjectState failed unexpectedly: {}", e.toString());
            return McpResponse.error(SOURCE, "showProjectState failed: " + e.getMessage());
        }
    }

    @Tool(description = "ANALYZE_TECH_DEBT: pull the Sonar QUALITY_STATE/DEBT_STATE slice and produce a "
            + "TECH_DEBT_REPORT (total debt, per-project breakdown, worst quality gate, recommendations).")
    public McpResponse analyzeTechDebt() {
        try {
            DownstreamSlice sonar = fetchSonar();
            TechDebtReport report = buildTechDebtReport(sonar);
            Confidence confidence = sonar.isOk() ? Confidence.HIGH : Confidence.LOW;
            if (sonar.isOk()) {
                return McpResponse.ok(report, SOURCE, confidence);
            }
            return McpResponse.stale(report, SOURCE, sliceMessage(sonar, "sonar-mcp"));
        } catch (Exception e) {
            log.warn("analyzeTechDebt failed unexpectedly: {}", e.toString());
            return McpResponse.error(SOURCE, "analyzeTechDebt failed: " + e.getMessage());
        }
    }

    @Tool(description = "ANALYZE_RELEASE_READINESS: combine the Sonar quality gate with Jira open epics/issues to "
            + "compute READY / READY_WITH_RISKS / NOT_READY with reasons.")
    public McpResponse analyzeReleaseReadiness() {
        try {
            DownstreamSlice sonar = fetchSonar();
            DownstreamSlice jira = fetchJira();
            ReleaseReadiness readiness = buildReleaseReadiness(sonar, jira);
            boolean anyStale = !sonar.isOk() || !jira.isOk();
            Confidence confidence = anyStale ? Confidence.LOW : Confidence.HIGH;
            if (anyStale) {
                return McpResponse.stale(readiness, SOURCE,
                        "Computed with stale inputs: " + staleLabels(List.of(sonar, jira)));
            }
            return McpResponse.ok(readiness, SOURCE, confidence);
        } catch (Exception e) {
            log.warn("analyzeReleaseReadiness failed unexpectedly: {}", e.toString());
            return McpResponse.error(SOURCE, "analyzeReleaseReadiness failed: " + e.getMessage());
        }
    }

    @Tool(description = "RUN_ARCHITECTURE_RESCAN: trigger the jQAssistant/Structurizr architecture-scan layer. "
            + "That layer is planned (MVP-2+) and not yet running, so the architecture slice is DATA_STALE; "
            + "available code/quality signals are still returned.")
    public McpResponse runArchitectureRescan() {
        try {
            DownstreamSlice jqa = fetchJqassistant();
            DownstreamSlice structurizr = fetchStructurizr();
            DownstreamSlice github = fetchGithub();
            DownstreamSlice sonar = fetchSonar();

            Map<String, Object> data = new java.util.LinkedHashMap<>();
            data.put("architectureState", "PLANNED");
            data.put("jqassistant", jqa);
            data.put("structurizr", structurizr);
            data.put("codeState", SubState.from("code", github));
            data.put("qualityState", SubState.from("quality", sonar));
            return McpResponse.stale(data, SOURCE,
                    "Architecture scan layer planned (MVP-2+): jQAssistant/Structurizr not running. "
                            + "Returned available code/quality signals.");
        } catch (Exception e) {
            log.warn("runArchitectureRescan failed unexpectedly: {}", e.toString());
            return McpResponse.error(SOURCE, "runArchitectureRescan failed: " + e.getMessage());
        }
    }

    @Tool(description = "GENERATE_REPORT: assemble a markdown report. type in {DAILY,WEEKLY,ARCHITECTURE,TECH_DEBT,RELEASE}. "
            + "The markdown is returned in the response data.")
    public McpResponse generateReport(String type) {
        try {
            String normalized = type == null ? "DAILY" : type.trim().toUpperCase();
            String markdown = switch (normalized) {
                case "DAILY", "WEEKLY" -> renderStateReport(normalized);
                case "TECH_DEBT" -> renderTechDebtReport();
                case "RELEASE" -> renderReleaseReport();
                case "ARCHITECTURE" -> renderArchitectureReport();
                default -> null;
            };
            if (markdown == null) {
                return McpResponse.error(SOURCE,
                        "Unknown report type '" + type + "'. Expected one of DAILY, WEEKLY, ARCHITECTURE, TECH_DEBT, RELEASE.");
            }
            return McpResponse.ok(markdown, SOURCE, Confidence.MEDIUM);
        } catch (Exception e) {
            log.warn("generateReport({}) failed unexpectedly: {}", type, e.toString());
            return McpResponse.error(SOURCE, "generateReport failed: " + e.getMessage());
        }
    }

    @Tool(description = "UPDATE_KNOWLEDGE_BASE: refresh the RAG knowledge index. The RAG layer is planned (MVP-3) "
            + "and not yet running, so this returns DATA_STALE.")
    public McpResponse updateKnowledgeBase() {
        return McpResponse.stale(Map.of("knowledgeState", "PLANNED"), SOURCE,
                "RAG layer planned (MVP-3): rag-mcp not running.");
    }

    // ----------------------------------------------------------- model build

    /** Fan out to the live downstreams and merge into the DIGITAL_TWIN_MODEL. */
    private DigitalTwinModel buildModel() {
        DownstreamSlice jira = fetchJira();
        DownstreamSlice github = fetchGithub();
        DownstreamSlice sonar = fetchSonar();

        SubState delivery = SubState.from("delivery", jira);
        SubState code = SubState.from("code", github);
        SubState quality = SubState.from("quality", sonar);
        SubState debt = SubState.from("debt", sonar);
        SubState architecture = new SubState("architecture", McpStatus.DATA_STALE,
                "structurizr-mcp/jqassistant-mcp", "planned (MVP-2+): server not running");
        SubState knowledge = new SubState("knowledge", McpStatus.DATA_STALE,
                "wiki-mcp/rag-mcp", "planned (MVP-2+/MVP-3): server not running");

        // Critical live slices: quality (gate/debt) and delivery. Code is non-critical.
        List<DownstreamSlice> critical = List.of(sonar, jira);
        List<DownstreamSlice> nonCritical = List.of(github);
        Confidence confidence = aggregateConfidence(critical, nonCritical);

        List<String> staleSources = new ArrayList<>();
        addIfStale(staleSources, jira, "jira-mcp");
        addIfStale(staleSources, github, "github-mcp");
        addIfStale(staleSources, sonar, "sonar-mcp");
        // Architecture/knowledge are always planned placeholders.
        staleSources.add("structurizr-mcp/jqassistant-mcp (planned MVP-2+)");
        staleSources.add("wiki-mcp/rag-mcp (planned MVP-2+/MVP-3)");

        List<String> recommendations = buildRecommendations(sonar, jira);

        return new DigitalTwinModel(delivery, code, quality, debt, architecture, knowledge,
                confidence, staleSources, recommendations, Instant.now());
    }

    /**
     * Resilience rule: HIGH only when every live slice is OK; LOW if any critical
     * live slice is stale; MEDIUM if only non-critical live slices are stale.
     * The always-planned architecture/knowledge placeholders do not by themselves
     * force LOW — they are expected to be absent in MVP-1.
     */
    private static Confidence aggregateConfidence(List<DownstreamSlice> critical,
                                                  List<DownstreamSlice> nonCritical) {
        boolean criticalStale = critical.stream().anyMatch(s -> !s.isOk());
        if (criticalStale) {
            return Confidence.LOW;
        }
        boolean nonCriticalStale = nonCritical.stream().anyMatch(s -> !s.isOk());
        return nonCriticalStale ? Confidence.MEDIUM : Confidence.HIGH;
    }

    private static McpStatus statusFor(Confidence confidence) {
        return confidence == Confidence.HIGH ? McpStatus.OK : McpStatus.DATA_STALE;
    }

    // --------------------------------------------------------- report builders

    private TechDebtReport buildTechDebtReport(DownstreamSlice sonar) {
        long totalDebt = 0L;
        String worstGate = "OK";
        List<TechDebtReport.ProjectDebt> byProject = new ArrayList<>();

        Map<String, Object> state = asMap(sonar.data());
        if (state != null) {
            // Prefer the aggregate totalDebtMinutes if present.
            totalDebt = asLong(state.get("totalDebtMinutes"), 0L);
            Object worst = state.get("worstGate");
            if (worst != null) {
                worstGate = String.valueOf(worst);
            }
            List<Map<String, Object>> projects = asListOfMaps(state.get("projects"));
            long summed = 0L;
            for (Map<String, Object> p : projects) {
                String key = String.valueOf(p.getOrDefault("projectKey", "unknown"));
                String gate = String.valueOf(p.getOrDefault("gateStatus", "UNKNOWN"));
                long debt = asLong(p.get("debtMinutes"), 0L);
                byProject.add(new TechDebtReport.ProjectDebt(key, gate, debt));
                summed += debt;
            }
            if (totalDebt == 0L) {
                totalDebt = summed;
            }
        }

        List<String> recommendations = new ArrayList<>();
        if (!sonar.isOk()) {
            recommendations.add("Sonar data is stale (" + sliceMessage(sonar, "sonar-mcp")
                    + "); debt figures may be outdated — verify manually.");
        }
        if ("ERROR".equalsIgnoreCase(worstGate)) {
            recommendations.add("At least one quality gate is FAILING (ERROR) — block release and fix gate conditions.");
        }
        byProject.stream()
                .filter(p -> p.debtMinutes() > 0)
                .max((a, b) -> Long.compare(a.debtMinutes(), b.debtMinutes()))
                .ifPresent(top -> recommendations.add(
                        "Highest-debt project: " + top.projectKey() + " (" + humanDebt(top.debtMinutes()) + ")."));
        if (recommendations.isEmpty()) {
            recommendations.add("No critical debt signals detected.");
        }

        return new TechDebtReport(totalDebt, humanDebt(totalDebt), byProject, worstGate, recommendations);
    }

    private ReleaseReadiness buildReleaseReadiness(DownstreamSlice sonar, DownstreamSlice jira) {
        List<String> reasons = new ArrayList<>();
        String worstGate = "OK";

        Map<String, Object> sonarState = asMap(sonar.data());
        if (sonarState != null && sonarState.get("worstGate") != null) {
            worstGate = String.valueOf(sonarState.get("worstGate"));
        }

        boolean gateFailing = "ERROR".equalsIgnoreCase(worstGate);
        boolean blockingIssues = hasBlockingIssues(jira);
        boolean anyStale = !sonar.isOk() || !jira.isOk();

        if (gateFailing) {
            reasons.add("Quality gate is FAILING (worstGate=ERROR).");
        }
        if (blockingIssues) {
            reasons.add("Open blocking/critical issues detected in delivery state.");
        }
        if (anyStale) {
            reasons.add("Some inputs are stale: " + staleLabels(List.of(sonar, jira)) + ".");
        }

        ReleaseReadiness.Readiness verdict;
        if (gateFailing) {
            verdict = ReleaseReadiness.Readiness.NOT_READY;
        } else if (blockingIssues || anyStale) {
            verdict = ReleaseReadiness.Readiness.READY_WITH_RISKS;
        } else {
            verdict = ReleaseReadiness.Readiness.READY;
            reasons.add("Quality gate passing and no blocking issues or stale sources.");
        }
        return new ReleaseReadiness(verdict, worstGate, reasons);
    }

    private List<String> buildRecommendations(DownstreamSlice sonar, DownstreamSlice jira) {
        List<String> recommendations = new ArrayList<>();
        Map<String, Object> sonarState = asMap(sonar.data());
        if (sonarState != null) {
            String worstGate = sonarState.get("worstGate") == null ? null : String.valueOf(sonarState.get("worstGate"));
            if ("ERROR".equalsIgnoreCase(worstGate)) {
                recommendations.add("Quality Gate failing — investigate failing conditions before release.");
            }
            long totalDebt = asLong(sonarState.get("totalDebtMinutes"), 0L);
            if (totalDebt > 0) {
                recommendations.add("Technical debt total: " + humanDebt(totalDebt) + " — schedule remediation.");
            }
        }
        if (hasBlockingIssues(jira)) {
            recommendations.add("Open critical/blocking issues present — triage before the next release.");
        }
        if (!sonar.isOk()) {
            recommendations.add("Sonar slice stale — quality/debt figures may be outdated.");
        }
        if (!jira.isOk()) {
            recommendations.add("Jira slice stale — delivery figures may be outdated.");
        }
        if (recommendations.isEmpty()) {
            recommendations.add("No action items from automated rules; project signals look healthy.");
        }
        return recommendations;
    }

    // ------------------------------------------------------------- markdown

    private String renderStateReport(String type) {
        DigitalTwinModel model = buildModel();
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(capitalize(type)).append(" Project State Report\n\n");
        sb.append("- Generated: ").append(model.generatedAt()).append("\n");
        sb.append("- Source: ").append(SOURCE).append("\n");
        sb.append("- Overall confidence: ").append(model.overallConfidence()).append("\n\n");
        sb.append("## Slices\n\n");
        appendSlice(sb, model.deliveryState());
        appendSlice(sb, model.codeState());
        appendSlice(sb, model.qualityState());
        appendSlice(sb, model.debtState());
        appendSlice(sb, model.architectureState());
        appendSlice(sb, model.knowledgeState());
        sb.append("\n## Stale sources\n\n");
        for (String s : model.staleSources()) {
            sb.append("- ").append(s).append("\n");
        }
        sb.append("\n## Recommendations\n\n");
        for (String r : model.recommendations()) {
            sb.append("- ").append(r).append("\n");
        }
        return sb.toString();
    }

    private String renderTechDebtReport() {
        DownstreamSlice sonar = fetchSonar();
        TechDebtReport report = buildTechDebtReport(sonar);
        StringBuilder sb = new StringBuilder();
        sb.append("# Technical Debt Report\n\n");
        sb.append("- Generated: ").append(Instant.now()).append("\n");
        sb.append("- Source: ").append(SOURCE).append("\n");
        sb.append("- Sonar slice status: ").append(sonar.status()).append("\n");
        sb.append("- Total debt: ").append(report.totalDebtHuman())
                .append(" (").append(report.totalDebtMinutes()).append(" min)\n");
        sb.append("- Worst quality gate: ").append(report.worstGate()).append("\n\n");
        sb.append("## By project\n\n");
        if (report.byProject().isEmpty()) {
            sb.append("_No project data available._\n");
        } else {
            sb.append("| Project | Gate | Debt |\n|---|---|---|\n");
            for (TechDebtReport.ProjectDebt p : report.byProject()) {
                sb.append("| ").append(p.projectKey()).append(" | ").append(p.gateStatus())
                        .append(" | ").append(humanDebt(p.debtMinutes())).append(" |\n");
            }
        }
        sb.append("\n## Recommendations\n\n");
        for (String r : report.recommendations()) {
            sb.append("- ").append(r).append("\n");
        }
        return sb.toString();
    }

    private String renderReleaseReport() {
        DownstreamSlice sonar = fetchSonar();
        DownstreamSlice jira = fetchJira();
        ReleaseReadiness readiness = buildReleaseReadiness(sonar, jira);
        StringBuilder sb = new StringBuilder();
        sb.append("# Release Readiness Report\n\n");
        sb.append("- Generated: ").append(Instant.now()).append("\n");
        sb.append("- Source: ").append(SOURCE).append("\n");
        sb.append("- Verdict: **").append(readiness.readiness()).append("**\n");
        sb.append("- Worst quality gate: ").append(readiness.worstGate()).append("\n\n");
        sb.append("## Reasons\n\n");
        for (String r : readiness.reasons()) {
            sb.append("- ").append(r).append("\n");
        }
        return sb.toString();
    }

    private String renderArchitectureReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Architecture Report\n\n");
        sb.append("- Generated: ").append(Instant.now()).append("\n");
        sb.append("- Source: ").append(SOURCE).append("\n\n");
        sb.append("> The architecture scan layer (jQAssistant + Structurizr) is planned (MVP-2+) ");
        sb.append("and not yet running. Architecture state is a stale placeholder.\n\n");
        DownstreamSlice sonar = fetchSonar();
        DownstreamSlice github = fetchGithub();
        sb.append("## Available signals\n\n");
        sb.append("- Code (github-mcp): ").append(github.status()).append("\n");
        sb.append("- Quality (sonar-mcp): ").append(sonar.status()).append("\n");
        return sb.toString();
    }

    // ------------------------------------------------------------- downstream

    private DownstreamSlice fetchJira() {
        return downstream.fetchState(properties.getDownstream().getJiraMcp(), "jira", "jira-mcp");
    }

    private DownstreamSlice fetchGithub() {
        return downstream.fetchState(properties.getDownstream().getGithubMcp(), "github", "github-mcp");
    }

    private DownstreamSlice fetchSonar() {
        return downstream.fetchState(properties.getDownstream().getSonarMcp(), "sonar", "sonar-mcp");
    }

    private DownstreamSlice fetchJqassistant() {
        return DownstreamSlice.planned("jqassistant-mcp", "planned (MVP-2+): server not running");
    }

    private DownstreamSlice fetchStructurizr() {
        return DownstreamSlice.planned("structurizr-mcp", "planned (MVP-2+): server not running");
    }

    // ----------------------------------------------------------------- helpers

    private static void appendSlice(StringBuilder sb, SubState slice) {
        sb.append("### ").append(capitalize(slice.name())).append("\n\n");
        sb.append("- Status: ").append(slice.status()).append("\n");
        sb.append("- Source: ").append(slice.source()).append("\n\n");
    }

    private static void addIfStale(List<String> sink, DownstreamSlice slice, String label) {
        if (!slice.isOk()) {
            sink.add(label + (slice.message() == null ? "" : " (" + slice.message() + ")"));
        }
    }

    private static String staleLabels(List<DownstreamSlice> slices) {
        List<String> labels = new ArrayList<>();
        for (DownstreamSlice s : slices) {
            if (!s.isOk()) {
                labels.add(s.source());
            }
        }
        return labels.isEmpty() ? "none" : String.join(", ", labels);
    }

    private static String sliceMessage(DownstreamSlice slice, String fallback) {
        return slice.message() != null ? slice.message() : fallback + " unavailable";
    }

    /**
     * True when the Jira delivery slice exposes any open blocking/critical issue
     * signal. Defensive: tolerates several plausible field shapes from jira-mcp.
     */
    @SuppressWarnings("unchecked")
    private static boolean hasBlockingIssues(DownstreamSlice jira) {
        Map<String, Object> state = asMap(jira.data());
        if (state == null) {
            return false;
        }
        // direct counters
        if (asLong(state.get("openBlockers"), 0L) > 0
                || asLong(state.get("openCritical"), 0L) > 0
                || asLong(state.get("blockingIssues"), 0L) > 0) {
            return true;
        }
        // per-project drill-down
        List<Map<String, Object>> projects = asListOfMaps(state.get("projects"));
        for (Map<String, Object> p : projects) {
            if (asLong(p.get("openBlockers"), 0L) > 0
                    || asLong(p.get("openCritical"), 0L) > 0
                    || asLong(p.get("blockingIssues"), 0L) > 0) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : null;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> asListOfMaps(Object o) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (o instanceof List<?> list) {
            for (Object e : list) {
                if (e instanceof Map<?, ?> m) {
                    out.add((Map<String, Object>) m);
                }
            }
        }
        return out;
    }

    private static long asLong(Object raw, long dflt) {
        if (raw == null) {
            return dflt;
        }
        try {
            return (long) Double.parseDouble(String.valueOf(raw));
        } catch (NumberFormatException e) {
            return dflt;
        }
    }

    /** Compact "Xd Yh Zmin" string (8h work-day), mirroring sonar-mcp's formatting. */
    static String humanDebt(long minutes) {
        if (minutes <= 0) {
            return "0min";
        }
        long workdayMinutes = 8 * 60;
        long days = minutes / workdayMinutes;
        long rem = minutes % workdayMinutes;
        long hours = rem / 60;
        long mins = rem % 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (mins > 0 || sb.length() == 0) {
            sb.append(mins).append("min");
        }
        return sb.toString().trim();
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}
