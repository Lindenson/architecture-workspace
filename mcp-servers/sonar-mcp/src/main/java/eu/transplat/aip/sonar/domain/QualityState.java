package eu.transplat.aip.sonar.domain;

import java.time.Instant;
import java.util.List;

/**
 * QUALITY_STATE / DEBT_STATE slice across all configured project keys. This is
 * the contract consumed by the digital-twin orchestrator.
 *
 * @param projects         per-project quality snapshots
 * @param worstGate        worst gate status across projects (ERROR &gt; WARN &gt; OK &gt; NONE)
 * @param totalDebtMinutes summed technical debt across projects
 * @param generatedAt      server-side timestamp
 */
public record QualityState(
        List<ProjectQuality> projects,
        String worstGate,
        long totalDebtMinutes,
        Instant generatedAt) {

    /**
     * Per-project quality snapshot.
     *
     * @param key             project key
     * @param gateStatus      quality-gate status
     * @param bugs            bug count
     * @param vulnerabilities vulnerability count
     * @param codeSmells      code-smell count
     * @param coveragePct     coverage percentage
     * @param debt            technical debt in minutes
     */
    public record ProjectQuality(
            String key,
            String gateStatus,
            long bugs,
            long vulnerabilities,
            long codeSmells,
            Double coveragePct,
            long debt) {
    }
}
