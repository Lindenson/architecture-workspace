package eu.transplat.aip.jqa.domain;

/**
 * Outcome of a jQAssistant CLI scan/analyze run.
 *
 * @param command  the command line that was executed
 * @param exitCode process exit code (0 = success)
 * @param output   captured stdout/stderr (possibly truncated)
 */
public record ScanResult(String command, int exitCode, String output) {
}
