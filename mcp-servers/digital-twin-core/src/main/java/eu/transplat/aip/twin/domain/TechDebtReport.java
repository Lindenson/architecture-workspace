package eu.transplat.aip.twin.domain;

import java.util.List;

/**
 * TECH_DEBT_REPORT derived from the sonar-mcp QUALITY_STATE/DEBT_STATE slice.
 *
 * @param totalDebtMinutes total technical debt across projects (Sonar sqale_index minutes)
 * @param totalDebtHuman   compact human-readable debt string ("Xd Yh")
 * @param byProject        per-project debt breakdown
 * @param worstGate        worst quality-gate status across projects (ERROR > WARN > OK)
 * @param recommendations  rule-derived recommendations
 */
public record TechDebtReport(
        long totalDebtMinutes,
        String totalDebtHuman,
        List<ProjectDebt> byProject,
        String worstGate,
        List<String> recommendations) {

    /**
     * Per-project debt line.
     *
     * @param projectKey  Sonar project key
     * @param gateStatus  quality-gate status
     * @param debtMinutes debt in minutes
     */
    public record ProjectDebt(String projectKey, String gateStatus, long debtMinutes) {
    }
}
