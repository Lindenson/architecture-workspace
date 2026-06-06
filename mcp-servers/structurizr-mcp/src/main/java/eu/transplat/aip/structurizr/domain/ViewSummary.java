package eu.transplat.aip.structurizr.domain;

/**
 * A single view defined in the workspace, returned by {@code getViews}.
 *
 * @param key   view key (unique identifier)
 * @param type  view type (e.g. SystemContext, Container, Component, …)
 * @param title view title / description
 */
public record ViewSummary(
        String key,
        String type,
        String title) {
}
