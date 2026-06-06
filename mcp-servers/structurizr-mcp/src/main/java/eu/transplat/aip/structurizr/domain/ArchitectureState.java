package eu.transplat.aip.structurizr.domain;

/**
 * ARCHITECTURE_MODEL state slice consumed by the digital twin at
 * {@code GET /api/structurizr/state}.
 *
 * @param workspaceName workspace name (null if not parsed)
 * @param systems       number of software systems
 * @param containers    total number of containers across systems
 * @param components    total number of components across containers
 * @param relationships number of relationships
 * @param views         number of views
 * @param parsedOk      true when the DSL parsed successfully
 * @param workspacePath the resolved path to the DSL file
 */
public record ArchitectureState(
        String workspaceName,
        int systems,
        int containers,
        int components,
        int relationships,
        int views,
        boolean parsedOk,
        String workspacePath) {
}
