package eu.transplat.aip.structurizr.domain;

import java.util.List;

/**
 * Heuristic architecture-drift report produced by {@code detectDrift}: a
 * best-effort, case-insensitive comparison between the names declared in the
 * Structurizr model (containers + components) and the actual component/package
 * names observed in the code (e.g. from jqassistant-mcp).
 *
 * @param inModelNotInCode model element names with no matching code name (possibly removed/renamed in code)
 * @param inCodeNotInModel code names with no matching model element (model drift — code ahead of the DSL)
 * @param matched          names matched on both sides
 * @param heuristic        always true — name matching is fuzzy (case-insensitive contains), not authoritative
 */
public record DriftReport(
        List<String> inModelNotInCode,
        List<String> inCodeNotInModel,
        List<String> matched,
        boolean heuristic) {
}
