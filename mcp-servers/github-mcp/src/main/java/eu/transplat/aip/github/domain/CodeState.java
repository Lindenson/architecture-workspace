package eu.transplat.aip.github.domain;

import java.time.Instant;
import java.util.List;

/**
 * Aggregate CODE_STATE across all configured repositories — the contract the
 * digital-twin orchestrator consumes from {@code getState()}.
 *
 * @param repos              per-repository snapshots
 * @param openPrTotal        total open pull/merge requests across all repos
 * @param mostRecentlyActive repo with the most recent commit, may be {@code null}
 * @param generatedAt        server-side generation timestamp
 */
public record CodeState(
        List<RepoSnapshot> repos,
        int openPrTotal,
        String mostRecentlyActive,
        Instant generatedAt) {
}
