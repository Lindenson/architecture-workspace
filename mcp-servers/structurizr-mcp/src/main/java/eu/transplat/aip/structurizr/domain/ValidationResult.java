package eu.transplat.aip.structurizr.domain;

import java.util.List;

/**
 * Outcome of {@code validateModel}: whether the DSL parses, plus any errors and
 * best-effort model sanity warnings.
 *
 * @param valid    true when the DSL parsed without error
 * @param errors   hard failures (e.g. parse exception messages)
 * @param warnings soft sanity findings (e.g. container with no components)
 */
public record ValidationResult(
        boolean valid,
        List<String> errors,
        List<String> warnings) {
}
