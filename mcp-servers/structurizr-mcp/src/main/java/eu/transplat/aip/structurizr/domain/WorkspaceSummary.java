package eu.transplat.aip.structurizr.domain;

import java.util.List;

/**
 * High-level summary of a parsed Structurizr workspace (the C4 model), returned
 * by {@code readWorkspace}.
 *
 * @param name              workspace name
 * @param description        workspace description
 * @param people            names of all people in the model
 * @param softwareSystems   software systems with their containers
 * @param relationshipCount total number of relationships in the model
 * @param viewCount         total number of views defined
 */
public record WorkspaceSummary(
        String name,
        String description,
        List<String> people,
        List<SoftwareSystemSummary> softwareSystems,
        int relationshipCount,
        int viewCount) {

    /**
     * @param name           software system name
     * @param containerCount number of containers
     * @param containers     the containers
     */
    public record SoftwareSystemSummary(
            String name,
            int containerCount,
            List<ContainerSummary> containers) {
    }

    /**
     * @param name           container name
     * @param technology     container technology
     * @param componentCount number of components inside the container
     */
    public record ContainerSummary(
            String name,
            String technology,
            int componentCount) {
    }
}
