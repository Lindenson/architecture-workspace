package eu.transplat.aip.sonar.domain;

/**
 * A single open code smell (from {@code /api/issues/search}).
 *
 * @param rule      rule key, e.g. "java:S1118"
 * @param severity  issue severity, e.g. "MAJOR"
 * @param component component (file) the issue is in
 * @param message   issue message
 * @param line      line number, or null when not line-bound
 */
public record CodeSmell(
        String rule,
        String severity,
        String component,
        String message,
        Integer line) {
}
