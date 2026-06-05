package eu.transplat.aip.sonar.domain;

import java.util.List;

/**
 * Security posture: open vulnerabilities plus a hotspot count.
 *
 * @param projectKey         project key
 * @param vulnerabilityCount number of open vulnerabilities
 * @param hotspotCount       number of security hotspots ({@code /api/hotspots/search})
 * @param securityRating     security rating ({@code security_rating}), e.g. "A".."E"
 * @param vulnerabilities    a sample of the open vulnerabilities
 */
public record SecuritySummary(
        String projectKey,
        int vulnerabilityCount,
        int hotspotCount,
        String securityRating,
        List<CodeSmell> vulnerabilities) {
}
