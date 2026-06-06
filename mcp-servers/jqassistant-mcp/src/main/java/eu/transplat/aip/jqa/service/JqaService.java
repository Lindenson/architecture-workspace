package eu.transplat.aip.jqa.service;

import eu.transplat.aip.jqa.config.JqaProperties;
import eu.transplat.aip.jqa.domain.GraphState;
import eu.transplat.aip.jqa.domain.ScanResult;
import eu.transplat.aip.jqa.repository.GraphRepository;
import eu.transplat.aip.mcp.common.Confidence;
import eu.transplat.aip.mcp.common.McpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * jQAssistant-backed MCP tools over the bytecode dependency graph stored in Neo4j.
 * This is the ARCHITECTURE_GRAPH source (MVP-2): cycles, layering violations,
 * coupling, blast-radius and arbitrary read-only Cypher.
 *
 * <p>RESILIENCE: every {@code @Tool} is fully guarded — a method never throws.
 * On a Neo4j outage or misconfiguration it returns an {@code ERROR} /
 * {@code DATA_STALE} {@link McpResponse}, so the server boots and stays up even
 * when Neo4j is down or the credentials are placeholders.
 *
 * <p>SCHEMA ASSUMPTIONS (jQAssistant Java plugin):
 * <ul>
 *   <li>Nodes labelled {@code :Type}, {@code :Package}, {@code :Artifact}.</li>
 *   <li>Dependency edges labelled {@code :DEPENDS_ON}.</li>
 *   <li>Fully-qualified name is on the {@code fqn} property. Cypher uses
 *       {@code coalesce(n.fqn, n.name)} defensively in case only {@code name}
 *       is populated for packages/artifacts.</li>
 * </ul>
 * If the actual schema differs, the queries simply return empty results rather
 * than failing.
 */
@Service
public class JqaService {

    /** Provenance label carried in every {@link McpResponse}. */
    public static final String SOURCE = "jqassistant-mcp:Neo4j";

    private static final Logger log = LoggerFactory.getLogger(JqaService.class);

    /** Write/DDL keywords rejected by the read-only Cypher guard (case-insensitive). */
    private static final Pattern WRITE_KEYWORDS = Pattern.compile(
            "(?i)\\b(CREATE|MERGE|DELETE|SET|REMOVE|DROP|LOAD\\s+CSV|"
                    + "FOREACH|DETACH|CALL\\s*\\{[^}]*\\b(create|merge|delete|set|remove)\\b)");

    private static final int MAX_PROCESS_OUTPUT = 64 * 1024;

    private final GraphRepository graph;
    private final JqaProperties properties;

    public JqaService(GraphRepository graph, JqaProperties properties) {
        this.graph = graph;
        this.properties = properties;
    }

    // ------------------------------------------------------------------ tools

    @Tool(description = "Run an arbitrary READ-ONLY Cypher query against the jQAssistant bytecode graph in Neo4j. "
            + "Write/DDL statements (CREATE, MERGE, DELETE, SET, REMOVE, DROP, LOAD CSV, apoc writes) are rejected. "
            + "If 'limit' is given and the query has no LIMIT, one is appended.")
    public McpResponse queryGraph(String cypher, Integer limit) {
        if (cypher == null || cypher.isBlank()) {
            return McpResponse.error(SOURCE, "Cypher query must not be blank.");
        }
        if (WRITE_KEYWORDS.matcher(cypher).find()) {
            return McpResponse.error(SOURCE,
                    "Rejected: only read-only Cypher is allowed (no CREATE/MERGE/DELETE/SET/REMOVE/DROP/LOAD CSV/apoc writes).");
        }
        String effective = applyLimit(cypher, limit);
        try {
            List<Map<String, Object>> rows = graph.read(effective, Map.of());
            return McpResponse.ok(rows, SOURCE);
        } catch (Exception e) {
            return failure("queryGraph", e);
        }
    }

    @Tool(description = "Detect package-level dependency cycles in the bytecode graph "
            + "(packages that transitively depend on themselves). Returns the cyclic package pairs found.")
    public McpResponse findCycles() {
        try {
            return McpResponse.ok(detectCycles(), SOURCE);
        } catch (Exception e) {
            return failure("findCycles", e);
        }
    }

    @Tool(description = "Detect layering violations: repository-layer types that are depended on directly by "
            + "controller-layer types (bypassing the service layer). Layer membership is derived from the "
            + "configured package globs. Returns the violating (controller -> repository) type pairs.")
    public McpResponse findLayeringViolations() {
        try {
            return McpResponse.ok(detectLayeringViolations(), SOURCE);
        } catch (Exception e) {
            return failure("findLayeringViolations", e);
        }
    }

    @Tool(description = "Outgoing :DEPENDS_ON dependencies of the given package or type (by fully-qualified name). "
            + "Matches either a :Package or a :Type whose fqn contains the given value.")
    public McpResponse dependenciesOf(String packageOrType) {
        if (packageOrType == null || packageOrType.isBlank()) {
            return McpResponse.error(SOURCE, "packageOrType must not be blank.");
        }
        String cypher = """
                MATCH (a)-[:DEPENDS_ON]->(b)
                WHERE (a:Type OR a:Package OR a:Artifact)
                  AND coalesce(a.fqn, a.name) CONTAINS $q
                RETURN DISTINCT coalesce(a.fqn, a.name) AS from,
                                labels(a)             AS fromLabels,
                                coalesce(b.fqn, b.name) AS to,
                                labels(b)             AS toLabels
                ORDER BY to
                LIMIT 500
                """;
        try {
            return McpResponse.ok(graph.read(cypher, Map.of("q", packageOrType)), SOURCE);
        } catch (Exception e) {
            return failure("dependenciesOf", e);
        }
    }

    @Tool(description = "Incoming dependents (blast radius) of the given type fqn — who transitively depends on it. "
            + "Bounded depth (1..5). Use before refactoring to gauge impact.")
    public McpResponse dependentsOf(String typeFqn) {
        if (typeFqn == null || typeFqn.isBlank()) {
            return McpResponse.error(SOURCE, "typeFqn must not be blank.");
        }
        // Transitive incoming dependents, depth-bounded to keep the query cheap.
        String cypher = """
                MATCH (target:Type)
                WHERE coalesce(target.fqn, target.name) CONTAINS $q
                MATCH (dependent:Type)-[:DEPENDS_ON*1..5]->(target)
                RETURN DISTINCT coalesce(dependent.fqn, dependent.name) AS dependent,
                                coalesce(target.fqn, target.name)       AS target
                ORDER BY dependent
                LIMIT 500
                """;
        try {
            return McpResponse.ok(graph.read(cypher, Map.of("q", typeFqn)), SOURCE);
        } catch (Exception e) {
            return failure("dependentsOf", e);
        }
    }

    @Tool(description = "Find god/highly-coupled classes: types whose outgoing :DEPENDS_ON fan-out is >= threshold "
            + "(default 30). Returns fqn and fanOut, highest first.")
    public McpResponse godClasses(Integer threshold) {
        int t = (threshold == null || threshold <= 0) ? 30 : threshold;
        String cypher = """
                MATCH (type:Type)-[:DEPENDS_ON]->(d:Type)
                WITH type, count(DISTINCT d) AS fanOut
                WHERE fanOut >= $threshold
                RETURN coalesce(type.fqn, type.name) AS fqn, fanOut
                ORDER BY fanOut DESC
                LIMIT 50
                """;
        try {
            return McpResponse.ok(graph.read(cypher, Map.of("threshold", t)), SOURCE);
        } catch (Exception e) {
            return failure("godClasses", e);
        }
    }

    @Tool(description = "Run a jQAssistant scan + analyze via the configured CLI to (re)populate Neo4j. "
            + "No-op (DATA_STALE) when jqassistant.cli/scanDir are not configured — in that case populate the "
            + "graph externally (jqassistant scan/analyze or a docker Neo4j).")
    public McpResponse runScan() {
        if (!properties.isScanConfigured()) {
            return McpResponse.stale(null, SOURCE,
                    "jQAssistant CLI/scanDir not configured — populate Neo4j via jQAssistant externally (see jqassistant/ dir).");
        }
        try {
            String cli = properties.getCli();
            String scanDir = properties.getScanDir();
            ScanResult scan = runProcess(List.of(cli, "scan", "-f", scanDir));
            if (scan.exitCode() != 0) {
                return McpResponse.error(SOURCE,
                        "jQAssistant scan failed (exit " + scan.exitCode() + "): " + scan.output());
            }
            ScanResult analyze = runProcess(List.of(cli, "analyze"));
            if (analyze.exitCode() != 0) {
                return McpResponse.error(SOURCE,
                        "jQAssistant analyze failed (exit " + analyze.exitCode() + "): " + analyze.output());
            }
            return McpResponse.ok(analyze, SOURCE);
        } catch (Exception e) {
            return failure("runScan", e);
        }
    }

    @Tool(description = "ARCHITECTURE_GRAPH summary for the digital twin: node counts per label (Type/Package/Artifact), "
            + "total dependency count, cycle count and layering-violation count. Returns DATA_STALE when Neo4j is unreachable.")
    public McpResponse getState() {
        try {
            Map<String, Long> labelCounts = new LinkedHashMap<>();
            labelCounts.put("Type", countLabel("Type"));
            labelCounts.put("Package", countLabel("Package"));
            labelCounts.put("Artifact", countLabel("Artifact"));

            long dependencyCount = scalarLong(
                    "MATCH ()-[r:DEPENDS_ON]->() RETURN count(r) AS c", "c");
            int cycleCount = detectCycles().size();
            int violationCount = detectLayeringViolations().size();

            GraphState state = new GraphState(
                    labelCounts, dependencyCount, cycleCount, violationCount, Instant.now());
            return McpResponse.ok(state, SOURCE);
        } catch (Exception e) {
            log.warn("getState failed (Neo4j unreachable?): {}", e.toString());
            return McpResponse.stale(null, SOURCE,
                    "Neo4j unreachable — ARCHITECTURE_GRAPH state unavailable: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------- internals

    /** Package-level cycle detection, bounded path length. */
    private List<Map<String, Object>> detectCycles() {
        // A package that transitively depends back on itself => a cycle.
        String cypher = """
                MATCH (p1:Package)-[:DEPENDS_ON]->(p2:Package)
                WHERE coalesce(p1.fqn, p1.name) <> coalesce(p2.fqn, p2.name)
                MATCH (p2)-[:DEPENDS_ON*1..5]->(p1)
                RETURN DISTINCT coalesce(p1.fqn, p1.name) AS packageA,
                                coalesce(p2.fqn, p2.name) AS packageB
                ORDER BY packageA, packageB
                LIMIT 100
                """;
        return graph.read(cypher, Map.of());
    }

    /**
     * Layering rule: controller-layer types must not depend directly on
     * repository-layer types (they should go through the service layer).
     * Package globs are translated to {@code CONTAINS} on the token between the
     * leading/trailing {@code *..} / {@code ..} markers (pragmatic; see
     * {@link #globToken(String)}).
     */
    private List<Map<String, Object>> detectLayeringViolations() {
        JqaProperties.Layering l = properties.getLayering();
        String controllerToken = globToken(l.getControllerPackage());
        String repositoryToken = globToken(l.getRepositoryPackage());
        String cypher = """
                MATCH (c:Type)-[:DEPENDS_ON]->(r:Type)
                WHERE coalesce(c.fqn, c.name) CONTAINS $controller
                  AND coalesce(r.fqn, r.name) CONTAINS $repository
                RETURN coalesce(c.fqn, c.name) AS controller,
                       coalesce(r.fqn, r.name) AS repository,
                       'controller depends directly on repository (bypasses service layer)' AS rule
                ORDER BY controller, repository
                LIMIT 200
                """;
        return graph.read(cypher, Map.of(
                "controller", controllerToken,
                "repository", repositoryToken));
    }

    /**
     * Reduce a jQAssistant package glob (e.g. {@code *..controller..}) to the
     * significant substring (e.g. {@code controller}) for a pragmatic
     * {@code CONTAINS} match on the fqn. We strip leading/trailing {@code *} and
     * {@code .} wildcards; if nothing meaningful remains the original is used.
     */
    static String globToken(String glob) {
        if (glob == null || glob.isBlank()) {
            return "";
        }
        String token = glob.replace("*", "");
        // Trim leading/trailing dot separators left by the wildcard removal.
        token = token.replaceAll("^\\.+", "").replaceAll("\\.+$", "");
        return token.isBlank() ? glob : token;
    }

    private long countLabel(String label) {
        // Label is from a fixed internal allow-list, never user input.
        return scalarLong("MATCH (n:" + label + ") RETURN count(n) AS c", "c");
    }

    private long scalarLong(String cypher, String column) {
        List<Map<String, Object>> rows = graph.read(cypher, Map.of());
        if (rows.isEmpty()) {
            return 0L;
        }
        Object v = rows.get(0).get(column);
        if (v instanceof Number n) {
            return n.longValue();
        }
        return 0L;
    }

    private String applyLimit(String cypher, Integer limit) {
        if (limit == null || limit <= 0) {
            return cypher;
        }
        if (Pattern.compile("(?i)\\bLIMIT\\b").matcher(cypher).find()) {
            return cypher;
        }
        String trimmed = cypher.strip();
        if (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).strip();
        }
        return trimmed + "\nLIMIT " + limit;
    }

    private ScanResult runProcess(List<String> command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command).redirectErrorStream(true);
        Process process = pb.start();
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null && out.length() < MAX_PROCESS_OUTPUT) {
                out.append(line).append('\n');
            }
        }
        boolean finished = process.waitFor(10, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            return new ScanResult(String.join(" ", command), -1,
                    "Timed out after 10 minutes.\n" + out);
        }
        return new ScanResult(String.join(" ", command), process.exitValue(), out.toString());
    }

    private McpResponse failure(String tool, Exception e) {
        log.warn("jqassistant tool {} failed: {}", tool, e.toString());
        return McpResponse.error(SOURCE, "Neo4j query failed in " + tool + ": " + e.getMessage());
    }
}
