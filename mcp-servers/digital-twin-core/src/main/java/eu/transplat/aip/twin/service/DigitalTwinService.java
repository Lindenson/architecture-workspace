package eu.transplat.aip.twin.service;

import eu.transplat.aip.mcp.common.Confidence;
import eu.transplat.aip.mcp.common.McpResponse;
import eu.transplat.aip.mcp.common.McpStatus;
import eu.transplat.aip.twin.client.DownstreamClient;
import eu.transplat.aip.twin.config.DigitalTwinProperties;
import eu.transplat.aip.twin.domain.ArchitectureSlice;
import eu.transplat.aip.twin.domain.ArchitectureSnapshot;
import eu.transplat.aip.twin.domain.DigitalTwinModel;
import eu.transplat.aip.twin.domain.DownstreamSlice;
import eu.transplat.aip.twin.domain.KnowledgeSlice;
import eu.transplat.aip.twin.domain.ReleaseReadiness;
import eu.transplat.aip.twin.domain.SubState;
import eu.transplat.aip.twin.domain.TechDebtReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
 *
 * <p>CONFIDENCE RULE: the CRITICAL slices are <b>delivery (jira-mcp)</b>
 * and <b>quality/debt (sonar-mcp)</b> — if either is stale, overall confidence is
 * LOW. The architecture sources (<b>jqassistant-mcp</b> graph + <b>structurizr-mcp</b>
 * C4 model) are NON-CRITICAL: now that they are live they participate in the
 * snapshot and, when stale, contribute to MEDIUM and to {@code staleSources}, but
 * they NEVER force LOW. Code (github-mcp) is also non-critical.
 *
 * <p>KNOWLEDGE (rag-mcp + wiki-mcp) is an OPTIONAL layer, OFF by default
 * ({@code digital-twin.features.knowledge.enabled=false}). When disabled the
 * knowledge slice carries {@link McpStatus#DISABLED}. When enabled it is
 * NON-CRITICAL (stale ⇒ MEDIUM, never LOW). Slices whose status is
 * {@code DISABLED} are IGNORED entirely for confidence: they do not lower
 * confidence and must not appear in {@code staleSources}.
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

    @Tool(description = "RUN_ARCHITECTURE_RESCAN: pull the live architecture-scan layer and assemble an "
            + "ARCHITECTURE_SNAPSHOT — jQAssistant graph summary + cycles + layering violations (code-side truth) "
            + "and the Structurizr C4 model summary + validation (intended design), plus best-effort drift signals. "
            + "HIGH when both servers answer; MEDIUM when one is stale; DATA_STALE/LOW only when both are unreachable "
            + "(then the scan layer isn't running — start jqassistant-mcp + structurizr-mcp).")
    public McpResponse runArchitectureRescan() {
        try {
            DownstreamSlice graph = fetchJqassistant();
            DownstreamSlice cycles = fetchJqassistantCycles();
            DownstreamSlice violations = fetchJqassistantViolations();
            DownstreamSlice model = fetchStructurizr();
            DownstreamSlice validation = fetchStructurizrValidate();

            ArchitectureSnapshot snapshot = new ArchitectureSnapshot(
                    graph.data(),
                    sliceList(cycles),
                    sliceList(violations),
                    model.data(),
                    validation.data(),
                    buildDriftSignals(graph, model, cycles, violations),
                    Instant.now());

            boolean jqaOk = graph.isOk();
            boolean structurizrOk = model.isOk();
            if (jqaOk && structurizrOk) {
                return McpResponse.ok(snapshot, SOURCE, Confidence.HIGH);
            }
            if (jqaOk || structurizrOk) {
                String stale = jqaOk ? "structurizr-mcp" : "jqassistant-mcp";
                return new McpResponse(snapshot, McpStatus.OK, SOURCE, Confidence.MEDIUM,
                        "Partial architecture snapshot: " + stale + " is stale ("
                                + sliceMessage(jqaOk ? model : graph, stale) + ").",
                        Instant.now());
            }
            return McpResponse.stale(snapshot, SOURCE,
                    "Architecture scan layer not running: both jqassistant-mcp and structurizr-mcp are unreachable. "
                            + "Start jqassistant-mcp (port 8085, needs a populated Neo4j) and structurizr-mcp (port 8084).");
        } catch (Exception e) {
            log.warn("runArchitectureRescan failed unexpectedly: {}", e.toString());
            return McpResponse.error(SOURCE, "runArchitectureRescan failed: " + e.getMessage());
        }
    }

    /**
     * Best-effort, high-level drift signals between code (jQAssistant graph) and
     * the intended design (Structurizr C4 model). Full component-level drift —
     * matching individual package/class names against C4 components — is future
     * work (structurizr-mcp exposes {@code detectDrift} for that). Here we surface:
     * cycles, layering violations, C4 parse failure, and a coarse count comparison.
     */
    private List<String> buildDriftSignals(DownstreamSlice graphSlice, DownstreamSlice modelSlice,
                                           DownstreamSlice cyclesSlice, DownstreamSlice violationsSlice) {
        List<String> signals = new ArrayList<>();
        Map<String, Object> graph = asMap(graphSlice.data());
        Map<String, Object> model = asMap(modelSlice.data());

        if (graph != null) {
            long cycleCount = asLong(graph.get("cycleCount"), 0L);
            if (cycleCount > 0) {
                signals.add("DRIFT (code): " + cycleCount + " dependency cycles in the bytecode graph (jQAssistant).");
            }
            long violationCount = asLong(graph.get("layeringViolationCount"), 0L);
            if (violationCount > 0) {
                signals.add("DRIFT (code): " + violationCount
                        + " layering violations (controller -> repository) in the bytecode graph (jQAssistant).");
            }
        }
        if (model != null) {
            Object parsedOk = model.get("parsedOk");
            if (parsedOk != null && !Boolean.parseBoolean(String.valueOf(parsedOk))) {
                signals.add("DRIFT (model): C4 model (workspace.dsl) failed to parse/validate (Structurizr).");
            }
        }
        // Coarse count comparison: jQAssistant Package count vs C4 containers+components.
        if (graph != null && model != null) {
            Map<String, Object> labelCounts = asMap(graph.get("labelCounts"));
            Long packages = labelCounts == null ? null : asLong(labelCounts.get("Package"), 0L);
            long c4Elements = asLong(model.get("containers"), 0L) + asLong(model.get("components"), 0L);
            if (packages != null) {
                signals.add("INFO: graph has " + packages + " packages but the C4 model has "
                        + c4Elements + " containers/components — coarse signal only; "
                        + "component-level drift (name matching) is future work.");
            }
        }
        if (!cyclesSlice.isOk() || !violationsSlice.isOk()) {
            signals.add("INFO: cycle/violation detail unavailable (jqassistant-mcp stale) — drift is partial.");
        }
        if (signals.isEmpty()) {
            signals.add("No high-level drift signals detected from available architecture data.");
        }
        return signals;
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

    @Tool(description = "UPDATE_KNOWLEDGE_BASE: refresh the knowledge layer — trigger a rag-mcp reindex and "
            + "re-read the wiki-mcp state, returning a KNOWLEDGE_UPDATE_REPORT. The knowledge layer is OPTIONAL "
            + "and OFF by default; when disabled this returns status DISABLED (not an error).")
    public McpResponse updateKnowledgeBase() {
        try {
            if (!properties.getFeatures().getKnowledge().isEnabled()) {
                return McpResponse.disabled(SOURCE, KNOWLEDGE_DISABLED_MESSAGE);
            }
            DownstreamSlice ragReindex = reindexRag();
            DownstreamSlice wiki = fetchWiki();

            Map<String, Object> ragReport = new LinkedHashMap<>();
            ragReport.put("status", ragReindex.status());
            ragReport.put("source", ragReindex.source());
            ragReport.put("data", ragReindex.data());
            ragReport.put("message", ragReindex.message());

            Map<String, Object> wikiReport = new LinkedHashMap<>();
            wikiReport.put("status", wiki.status());
            wikiReport.put("source", wiki.source());
            wikiReport.put("data", wiki.data());
            wikiReport.put("message", wiki.message());

            Map<String, Object> report = new LinkedHashMap<>();
            report.put("ragReindex", ragReport);
            report.put("wiki", wikiReport);
            report.put("generatedAt", Instant.now());

            // Same confidence rule: DISABLED downstreams are ignored; knowledge is
            // NON-CRITICAL so any non-DISABLED stale source yields MEDIUM at worst,
            // and HIGH only when every active source is OK.
            List<DownstreamSlice> active = ignoreDisabled(List.of(ragReindex, wiki));
            boolean anyStale = active.stream().anyMatch(s -> !s.isOk());
            if (anyStale) {
                return new McpResponse(report, McpStatus.DATA_STALE, SOURCE, Confidence.MEDIUM,
                        "Knowledge update completed with stale inputs: "
                                + staleLabels(List.of(ragReindex, wiki)), Instant.now());
            }
            return McpResponse.ok(report, SOURCE, Confidence.HIGH);
        } catch (Exception e) {
            log.warn("updateKnowledgeBase failed unexpectedly: {}", e.toString());
            return McpResponse.error(SOURCE, "updateKnowledgeBase failed: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------- model build

    /** Fan out to the live downstreams and merge into the DIGITAL_TWIN_MODEL. */
    private DigitalTwinModel buildModel() {
        DownstreamSlice jira = fetchJira();
        DownstreamSlice github = fetchGithub();
        DownstreamSlice sonar = fetchSonar();
        DownstreamSlice jqa = fetchJqassistant();
        DownstreamSlice structurizr = fetchStructurizr();

        SubState delivery = SubState.from("delivery", jira);
        SubState code = SubState.from("code", github);
        SubState quality = SubState.from("quality", sonar);
        SubState debt = SubState.from("debt", sonar);

        // ARCHITECTURE_STATE: now live — combine the jQAssistant graph summary and
        // the Structurizr C4 model summary into one section, each keeping its own
        // status. The SubState is OK only when both architecture sources are OK.
        ArchitectureSlice arch = ArchitectureSlice.from(jqa, structurizr);
        McpStatus archStatus = arch.isOk() ? McpStatus.OK : McpStatus.DATA_STALE;
        SubState architecture = new SubState("architecture", archStatus,
                "jqassistant-mcp + structurizr-mcp", arch);

        // KNOWLEDGE_STATE: optional, OFF by default. When disabled, emit a single
        // DISABLED slice (not a failure) — it is excluded from confidence and from
        // staleSources. When enabled, fan out to rag-mcp + wiki-mcp lazily and
        // combine them like ArchitectureSlice, carrying any downstream DISABLED
        // status through unchanged.
        boolean knowledgeEnabled = properties.getFeatures().getKnowledge().isEnabled();
        DownstreamSlice rag = knowledgeEnabled ? fetchRag() : null;
        DownstreamSlice wiki = knowledgeEnabled ? fetchWiki() : null;
        SubState knowledge = knowledgeEnabled
                ? knowledgeSubState(rag, wiki)
                : disabledKnowledgeSubState();

        // CONFIDENCE RULE: critical = delivery (jira) + quality/debt (sonar).
        // Non-critical = code (github) + architecture (jqassistant graph +
        // structurizr C4 model) + knowledge (rag + wiki, when enabled). A stale
        // non-critical source contributes to MEDIUM and to staleSources but never
        // forces LOW.
        //
        // DISABLED slices are IGNORED ENTIRELY: they do not lower confidence and
        // must NOT appear in staleSources. We therefore filter them out before
        // scoring (aggregateConfidence treats only OK as "ok") and never add a
        // DISABLED source to staleSources.
        List<DownstreamSlice> critical = ignoreDisabled(List.of(sonar, jira));
        List<DownstreamSlice> nonCritical = new ArrayList<>(List.of(github, jqa, structurizr));
        if (knowledgeEnabled) {
            nonCritical.add(rag);
            nonCritical.add(wiki);
        }
        nonCritical = ignoreDisabled(nonCritical);
        Confidence confidence = aggregateConfidence(critical, nonCritical);

        List<String> staleSources = new ArrayList<>();
        addIfStale(staleSources, jira, "jira-mcp");
        addIfStale(staleSources, github, "github-mcp");
        addIfStale(staleSources, sonar, "sonar-mcp");
        addIfStale(staleSources, jqa, "jqassistant-mcp");
        addIfStale(staleSources, structurizr, "structurizr-mcp");
        if (knowledgeEnabled) {
            addIfStale(staleSources, rag, "rag-mcp");
            addIfStale(staleSources, wiki, "wiki-mcp");
        }

        List<String> recommendations = buildRecommendations(sonar, jira);
        recommendations.addAll(buildArchitectureRecommendations(jqa, structurizr));

        return new DigitalTwinModel(delivery, code, quality, debt, architecture, knowledge,
                confidence, staleSources, recommendations, Instant.now());
    }

    /**
     * Resilience rule: HIGH only when every live slice is OK; LOW if any critical
     * live slice is stale; MEDIUM if only non-critical live slices are stale.
     * Critical = delivery (jira) + quality/debt (sonar). Non-critical = code
     * (github) + architecture (jqassistant + structurizr) + knowledge (rag + wiki,
     * when enabled). Callers must pre-filter DISABLED slices (see
     * {@link #ignoreDisabled}) so they are excluded entirely and never force LOW.
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

    /** Message used everywhere the knowledge layer is reported as turned off. */
    static final String KNOWLEDGE_DISABLED_MESSAGE =
            "knowledge layer disabled for this project (digital-twin.features.knowledge.enabled=false)";

    /** The DISABLED knowledge sub-state emitted when the feature flag is off. */
    private static SubState disabledKnowledgeSubState() {
        return new SubState("knowledge", McpStatus.DISABLED, "rag-mcp + wiki-mcp",
                KNOWLEDGE_DISABLED_MESSAGE);
    }

    /**
     * Combine the live rag-mcp and wiki-mcp slices into one knowledge sub-state.
     * The combined status is OK only when both sources are usable (OK or their
     * own DISABLED); otherwise DATA_STALE. A downstream reporting DISABLED is
     * carried through unchanged (not an error).
     */
    private static SubState knowledgeSubState(DownstreamSlice rag, DownstreamSlice wiki) {
        KnowledgeSlice knowledge = KnowledgeSlice.from(rag, wiki);
        McpStatus status = knowledge.isOk() ? McpStatus.OK : McpStatus.DATA_STALE;
        return new SubState("knowledge", status, "rag-mcp + wiki-mcp", knowledge);
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

    /**
     * Architecture-derived recommendations from the live jQAssistant graph and
     * Structurizr C4 model slices. Defensive against the downstream data shapes:
     * jQAssistant's GraphState exposes {@code cycleCount} and
     * {@code layeringViolationCount}; Structurizr's ArchitectureState exposes
     * {@code parsedOk}.
     */
    private List<String> buildArchitectureRecommendations(DownstreamSlice jqa, DownstreamSlice structurizr) {
        List<String> recommendations = new ArrayList<>();
        Map<String, Object> graph = asMap(jqa.data());
        if (graph != null) {
            long cycles = asLong(graph.get("cycleCount"), 0L);
            if (cycles > 0) {
                recommendations.add(cycles + " dependency cycles detected (jQAssistant).");
            }
            long violations = asLong(graph.get("layeringViolationCount"), 0L);
            if (violations > 0) {
                recommendations.add(violations + " layering violations detected (jQAssistant).");
            }
        }
        Map<String, Object> model = asMap(structurizr.data());
        if (model != null) {
            Object parsedOk = model.get("parsedOk");
            if (parsedOk != null && !Boolean.parseBoolean(String.valueOf(parsedOk))) {
                recommendations.add("C4 model (workspace.dsl) failed to parse/validate (Structurizr).");
            }
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
        McpResponse rescan = runArchitectureRescan();
        StringBuilder sb = new StringBuilder();
        sb.append("# Architecture Report\n\n");
        sb.append("- Generated: ").append(Instant.now()).append("\n");
        sb.append("- Source: ").append(SOURCE).append("\n");
        sb.append("- Confidence: ").append(rescan.confidence()).append("\n");
        sb.append("- Scan status: ").append(rescan.status()).append("\n");
        if (rescan.message() != null) {
            sb.append("- Note: ").append(rescan.message()).append("\n");
        }
        sb.append("\n");

        if (!(rescan.data() instanceof ArchitectureSnapshot snapshot)) {
            sb.append("_No architecture snapshot available._\n");
            return sb.toString();
        }

        Map<String, Object> graph = asMap(snapshot.graph());
        sb.append("## Code graph (jQAssistant)\n\n");
        if (graph == null) {
            sb.append("_jqassistant-mcp slice unavailable._\n\n");
        } else {
            sb.append("- Label counts: ").append(graph.get("labelCounts")).append("\n");
            sb.append("- Dependencies: ").append(graph.get("dependencyCount")).append("\n");
            sb.append("- Cycles: ").append(graph.get("cycleCount")).append("\n");
            sb.append("- Layering violations: ").append(graph.get("layeringViolationCount")).append("\n\n");
        }

        Map<String, Object> model = asMap(snapshot.model());
        sb.append("## C4 model (Structurizr)\n\n");
        if (model == null) {
            sb.append("_structurizr-mcp slice unavailable._\n\n");
        } else {
            sb.append("- Workspace: ").append(model.get("workspaceName")).append("\n");
            sb.append("- Systems / containers / components: ")
                    .append(model.get("systems")).append(" / ")
                    .append(model.get("containers")).append(" / ")
                    .append(model.get("components")).append("\n");
            sb.append("- Relationships: ").append(model.get("relationships")).append("\n");
            sb.append("- Views: ").append(model.get("views")).append("\n");
            sb.append("- Parsed OK: ").append(model.get("parsedOk")).append("\n\n");
        }

        sb.append("## Drift signals\n\n");
        for (String s : snapshot.driftSignals()) {
            sb.append("- ").append(s).append("\n");
        }
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
        return downstream.getPath(properties.getDownstream().getJqassistantMcp(),
                "/api/jqassistant/state", "jqassistant-mcp");
    }

    private DownstreamSlice fetchJqassistantCycles() {
        return downstream.getPath(properties.getDownstream().getJqassistantMcp(),
                "/api/jqassistant/cycles", "jqassistant-mcp");
    }

    private DownstreamSlice fetchJqassistantViolations() {
        return downstream.getPath(properties.getDownstream().getJqassistantMcp(),
                "/api/jqassistant/violations", "jqassistant-mcp");
    }

    private DownstreamSlice fetchStructurizr() {
        return downstream.getPath(properties.getDownstream().getStructurizrMcp(),
                "/api/structurizr/state", "structurizr-mcp");
    }

    private DownstreamSlice fetchStructurizrValidate() {
        return downstream.getPath(properties.getDownstream().getStructurizrMcp(),
                "/api/structurizr/validate", "structurizr-mcp");
    }

    private DownstreamSlice fetchRag() {
        return downstream.getPath(properties.getDownstream().getRagMcp(),
                "/api/rag/state", "rag-mcp");
    }

    private DownstreamSlice fetchWiki() {
        return downstream.getPath(properties.getDownstream().getWikiMcp(),
                "/api/wiki/state", "wiki-mcp");
    }

    private DownstreamSlice reindexRag() {
        return downstream.postPath(properties.getDownstream().getRagMcp(),
                "/api/rag/reindex", "rag-mcp");
    }

    // ----------------------------------------------------------------- helpers

    private static void appendSlice(StringBuilder sb, SubState slice) {
        sb.append("### ").append(capitalize(slice.name())).append("\n\n");
        sb.append("- Status: ").append(slice.status()).append("\n");
        sb.append("- Source: ").append(slice.source()).append("\n\n");
    }

    private static void addIfStale(List<String> sink, DownstreamSlice slice, String label) {
        // DISABLED is intentional, not a failure: never report it as a stale source.
        if (!slice.isOk() && slice.status() != McpStatus.DISABLED) {
            sink.add(label + (slice.message() == null ? "" : " (" + slice.message() + ")"));
        }
    }

    /**
     * Drop slices whose status is {@link McpStatus#DISABLED} — they are
     * intentionally turned off and must be IGNORED ENTIRELY for confidence
     * scoring (neither lowering it nor counting as stale).
     */
    private static List<DownstreamSlice> ignoreDisabled(List<DownstreamSlice> slices) {
        List<DownstreamSlice> out = new ArrayList<>();
        for (DownstreamSlice s : slices) {
            if (s.status() != McpStatus.DISABLED) {
                out.add(s);
            }
        }
        return out;
    }

    private static String staleLabels(List<DownstreamSlice> slices) {
        List<String> labels = new ArrayList<>();
        for (DownstreamSlice s : slices) {
            // DISABLED is intentional, not a stale input.
            if (!s.isOk() && s.status() != McpStatus.DISABLED) {
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

    /**
     * Extract the {@code data} of a slice as a {@code List<Object>} (the
     * {@code /cycles} and {@code /violations} endpoints return a JSON array).
     * A stale slice or a non-list payload yields an empty list.
     */
    private static List<Object> sliceList(DownstreamSlice slice) {
        if (slice.data() instanceof List<?> list) {
            return new ArrayList<>(list);
        }
        return new ArrayList<>();
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
