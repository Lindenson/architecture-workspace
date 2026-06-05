package eu.transplat.aip.sonar.service;

import eu.transplat.aip.mcp.common.Confidence;
import eu.transplat.aip.mcp.common.McpResponse;
import eu.transplat.aip.sonar.client.SonarClient;
import eu.transplat.aip.sonar.config.SonarProperties;
import eu.transplat.aip.sonar.domain.CodeSmell;
import eu.transplat.aip.sonar.domain.Coverage;
import eu.transplat.aip.sonar.domain.QualityGate;
import eu.transplat.aip.sonar.domain.QualityState;
import eu.transplat.aip.sonar.domain.SecuritySummary;
import eu.transplat.aip.sonar.domain.SonarReport;
import eu.transplat.aip.sonar.domain.TechnicalDebt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SonarQube-backed MCP tools. Every method is resilient: it never throws out of
 * a {@code @Tool}; on a missing configuration or upstream failure it returns an
 * {@code ERROR} / {@code DATA_STALE} {@link McpResponse}. The server therefore
 * starts and stays up even with placeholder credentials.
 */
@Service
public class SonarService {

    /** Provenance label carried in every {@link McpResponse}. */
    public static final String SOURCE = "sonar-mcp:SonarQube Web API";

    private static final Logger log = LoggerFactory.getLogger(SonarService.class);

    private final SonarClient client;
    private final SonarProperties properties;

    public SonarService(SonarClient client, SonarProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    // ------------------------------------------------------------------ tools

    @Tool(description = "SonarQube quality-gate status (OK/ERROR) and any failing conditions for a project.")
    public McpResponse qualityGate(String projectKey) {
        if (notConfigured()) {
            return McpResponse.error(SOURCE, "SonarQube is not configured (base-url/token missing).");
        }
        try {
            return McpResponse.ok(parseQualityGate(projectKey, client.qualityGate(projectKey)), SOURCE);
        } catch (Exception e) {
            return failure("qualityGate", projectKey, e);
        }
    }

    @Tool(description = "SonarQube technical debt for a project: sqale_index (minutes), maintainability rating and a human 'Xd Yh' string.")
    public McpResponse technicalDebt(String projectKey) {
        if (notConfigured()) {
            return McpResponse.error(SOURCE, "SonarQube is not configured (base-url/token missing).");
        }
        try {
            Map<String, String> m = measureMap(client.measures(projectKey));
            long minutes = asLong(m.get("sqale_index"), 0L);
            return McpResponse.ok(new TechnicalDebt(
                    projectKey, minutes, humanDebt(minutes), ratingLetter(m.get("sqale_rating"))), SOURCE);
        } catch (Exception e) {
            return failure("technicalDebt", projectKey, e);
        }
    }

    @Tool(description = "Top open SonarQube code smells for a project (rule, severity, component, message, line).")
    public McpResponse codeSmells(String projectKey, Integer limit) {
        if (notConfigured()) {
            return McpResponse.error(SOURCE, "SonarQube is not configured (base-url/token missing).");
        }
        int ps = clampLimit(limit, 20);
        try {
            List<CodeSmell> smells = parseIssues(client.issues(projectKey, "CODE_SMELL", ps));
            return McpResponse.ok(smells, SOURCE);
        } catch (Exception e) {
            return failure("codeSmells", projectKey, e);
        }
    }

    @Tool(description = "SonarQube security posture for a project: open vulnerabilities plus a security-hotspot summary.")
    public McpResponse securityIssues(String projectKey) {
        if (notConfigured()) {
            return McpResponse.error(SOURCE, "SonarQube is not configured (base-url/token missing).");
        }
        try {
            Map<String, Object> vulnResp = client.issues(projectKey, "VULNERABILITY", 20);
            List<CodeSmell> vulns = parseIssues(vulnResp);
            int vulnTotal = asInt(vulnResp.get("total"), vulns.size());
            int hotspots = countHotspots(client.hotspots(projectKey));
            String secRating = ratingLetter(measureMap(client.measures(projectKey, "security_rating")).get("security_rating"));
            return McpResponse.ok(
                    new SecuritySummary(projectKey, vulnTotal, hotspots, secRating, vulns), SOURCE);
        } catch (Exception e) {
            return failure("securityIssues", projectKey, e);
        }
    }

    @Tool(description = "SonarQube coverage for a project: coverage %, ncloc and duplicated_lines_density.")
    public McpResponse coverage(String projectKey) {
        if (notConfigured()) {
            return McpResponse.error(SOURCE, "SonarQube is not configured (base-url/token missing).");
        }
        try {
            Map<String, String> m = measureMap(client.measures(projectKey));
            return McpResponse.ok(new Coverage(
                    projectKey,
                    asDouble(m.get("coverage")),
                    asLongOrNull(m.get("ncloc")),
                    asDouble(m.get("duplicated_lines_density"))), SOURCE);
        } catch (Exception e) {
            return failure("coverage", projectKey, e);
        }
    }

    @Tool(description = "Combined SonarQube report for a project: measures + quality gate + counts (bugs, vulnerabilities, smells) + ratings.")
    public McpResponse fetchReport(String projectKey) {
        if (notConfigured()) {
            return McpResponse.error(SOURCE, "SonarQube is not configured (base-url/token missing).");
        }
        try {
            return McpResponse.ok(buildReport(projectKey), SOURCE);
        } catch (Exception e) {
            return failure("fetchReport", projectKey, e);
        }
    }

    @Tool(description = "QUALITY_STATE/DEBT_STATE slice across all configured SonarQube project keys for the digital-twin orchestrator.")
    public McpResponse getState() {
        if (notConfigured()) {
            return McpResponse.error(SOURCE, "SonarQube is not configured (base-url/token missing).");
        }
        List<String> keys = properties.getProjectKeys();
        if (keys == null || keys.isEmpty()) {
            return McpResponse.ok(
                    new QualityState(List.of(), "NONE", 0L, Instant.now()),
                    SOURCE, Confidence.MEDIUM);
        }

        List<QualityState.ProjectQuality> projects = new ArrayList<>();
        long totalDebt = 0L;
        String worstGate = "OK";
        int failures = 0;

        for (String key : keys) {
            try {
                SonarReport r = buildReport(key);
                projects.add(new QualityState.ProjectQuality(
                        key, r.gateStatus(), r.bugs(), r.vulnerabilities(),
                        r.codeSmells(), r.coveragePct(), r.debtMinutes()));
                totalDebt += r.debtMinutes();
                worstGate = worseGate(worstGate, r.gateStatus());
            } catch (Exception e) {
                failures++;
                log.warn("getState: project {} failed: {}", key, e.toString());
                projects.add(new QualityState.ProjectQuality(
                        key, "UNKNOWN", 0, 0, 0, null, 0));
            }
        }

        QualityState state = new QualityState(projects, worstGate, totalDebt, Instant.now());
        if (failures == keys.size()) {
            return McpResponse.stale(state, SOURCE, "All " + failures + " project(s) failed to refresh.");
        }
        if (failures > 0) {
            return McpResponse.stale(state, SOURCE, failures + " of " + keys.size() + " project(s) failed to refresh.");
        }
        return McpResponse.ok(state, SOURCE);
    }

    // -------------------------------------------------------------- internals

    private SonarReport buildReport(String projectKey) {
        Map<String, String> m = measureMap(client.measures(projectKey));
        QualityGate gate = parseQualityGate(projectKey, client.qualityGate(projectKey));
        long debt = asLong(m.get("sqale_index"), 0L);
        return new SonarReport(
                projectKey,
                gate.status(),
                asLong(m.get("bugs"), 0L),
                asLong(m.get("vulnerabilities"), 0L),
                asLong(m.get("code_smells"), 0L),
                asDouble(m.get("coverage")),
                asDouble(m.get("duplicated_lines_density")),
                asLongOrNull(m.get("ncloc")),
                debt,
                humanDebt(debt),
                ratingLetter(m.get("reliability_rating")),
                ratingLetter(m.get("security_rating")),
                ratingLetter(m.get("sqale_rating")));
    }

    private boolean notConfigured() {
        return !properties.isConfigured();
    }

    private McpResponse failure(String tool, String projectKey, Exception e) {
        log.warn("sonar tool {} failed for project {}: {}", tool, projectKey, e.toString());
        return McpResponse.error(SOURCE, "SonarQube call failed for '" + projectKey + "': " + e.getMessage());
    }

    // ------------------------------------------------------------- parsing

    @SuppressWarnings("unchecked")
    private QualityGate parseQualityGate(String projectKey, Map<String, Object> resp) {
        Map<String, Object> ps = (Map<String, Object>) value(resp, "projectStatus");
        if (ps == null) {
            return new QualityGate(projectKey, "NONE", List.of());
        }
        String status = String.valueOf(ps.getOrDefault("status", "NONE"));
        List<QualityGate.GateCondition> conditions = new ArrayList<>();
        Object rawConditions = ps.get("conditions");
        if (rawConditions instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> c) {
                    conditions.add(new QualityGate.GateCondition(
                            str(c, "metricKey"),
                            str(c, "comparator"),
                            str(c, "errorThreshold"),
                            str(c, "actualValue"),
                            str(c, "status")));
                }
            }
        }
        return new QualityGate(projectKey, status, conditions);
    }

    /** Flatten {@code /api/measures/component} into a metric-key -> value map. */
    @SuppressWarnings("unchecked")
    private Map<String, String> measureMap(Map<String, Object> resp) {
        Map<String, String> out = new java.util.HashMap<>();
        Object component = value(resp, "component");
        if (component instanceof Map<?, ?> comp) {
            Object measures = comp.get("measures");
            if (measures instanceof List<?> list) {
                for (Object o : list) {
                    if (o instanceof Map<?, ?> measure) {
                        String metric = str(measure, "metric");
                        String val = str(measure, "value");
                        if (metric != null && val != null) {
                            out.put(metric, val);
                        }
                    }
                }
            }
        }
        return out;
    }

    private List<CodeSmell> parseIssues(Map<String, Object> resp) {
        List<CodeSmell> out = new ArrayList<>();
        Object issues = value(resp, "issues");
        if (issues instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> i) {
                    out.add(new CodeSmell(
                            str(i, "rule"),
                            str(i, "severity"),
                            str(i, "component"),
                            str(i, "message"),
                            i.get("line") == null ? null : asInt(i.get("line"), 0)));
                }
            }
        }
        return out;
    }

    private int countHotspots(Map<String, Object> resp) {
        Object paging = value(resp, "paging");
        if (paging instanceof Map<?, ?> p && p.get("total") != null) {
            return asInt(p.get("total"), 0);
        }
        Object hotspots = value(resp, "hotspots");
        return hotspots instanceof List<?> list ? list.size() : 0;
    }

    // ----------------------------------------------------------- formatting

    /** Convert SonarQube debt minutes into a compact "Xd Yh Zmin" string (8h work-day). */
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

    /** Map a numeric Sonar rating (1.0..5.0) to a letter A..E; pass through letters. */
    static String ratingLetter(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            int idx = (int) Math.round(Double.parseDouble(raw));
            return switch (idx) {
                case 1 -> "A";
                case 2 -> "B";
                case 3 -> "C";
                case 4 -> "D";
                case 5 -> "E";
                default -> raw;
            };
        } catch (NumberFormatException e) {
            return raw;
        }
    }

    /** Worse-of two gate statuses: ERROR &gt; WARN &gt; OK &gt; NONE/UNKNOWN. */
    static String worseGate(String a, String b) {
        return rank(b) > rank(a) ? b : a;
    }

    private static int rank(String status) {
        if (status == null) {
            return 0;
        }
        return switch (status.toUpperCase()) {
            case "ERROR" -> 3;
            case "WARN" -> 2;
            case "OK" -> 1;
            default -> 0;
        };
    }

    private static int clampLimit(Integer limit, int dflt) {
        if (limit == null || limit <= 0) {
            return dflt;
        }
        return Math.min(limit, 500);
    }

    // -------------------------------------------------------- map accessors

    private static Object value(Map<String, Object> map, String key) {
        return map == null ? null : map.get(key);
    }

    private static String str(Map<?, ?> map, String key) {
        Object v = map.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private static long asLong(String raw, long dflt) {
        if (raw == null || raw.isBlank()) {
            return dflt;
        }
        try {
            return (long) Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return dflt;
        }
    }

    private static Long asLongOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return (long) Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double asDouble(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int asInt(Object raw, int dflt) {
        if (raw == null) {
            return dflt;
        }
        try {
            return (int) Double.parseDouble(String.valueOf(raw));
        } catch (NumberFormatException e) {
            return dflt;
        }
    }
}
