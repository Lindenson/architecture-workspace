package eu.transplat.aip.twin.domain;

import eu.transplat.aip.mcp.common.McpStatus;

/**
 * The merged KNOWLEDGE_STATE for the digital twin, combining the two knowledge
 * sources into one section, each retaining its own status and provenance
 * (mirrors {@link ArchitectureSlice}):
 *
 * <ul>
 *   <li><b>rag</b> — the rag-mcp RAG index state
 *       ({@code enabled}, {@code provider}, {@code dimension}, {@code dbReachable},
 *       {@code indexedChunks}, {@code sources}, {@code lastIndexedAt}) from
 *       {@code GET /api/rag/state}.</li>
 *   <li><b>wiki</b> — the wiki-mcp wiki state ({@code spaces}, {@code generatedAt})
 *       from {@code GET /api/wiki/state}.</li>
 * </ul>
 *
 * <p>The knowledge layer is OPTIONAL and OFF by default. When enabled, it is
 * NON-CRITICAL: a stale source contributes to MEDIUM and to {@code staleSources}
 * but never forces the overall confidence to LOW. A downstream that itself
 * reports {@link McpStatus#DISABLED} is carried through unchanged (not an error)
 * and, per the confidence rule, is excluded from scoring and stale reporting.
 *
 * @param ragStatus  status of the rag-mcp call
 * @param ragSource  provenance of the rag-mcp source
 * @param rag        the RAG index state payload (may be null when stale/disabled)
 * @param wikiStatus status of the wiki-mcp call
 * @param wikiSource provenance of the wiki-mcp source
 * @param wiki       the wiki state payload (may be null when stale/disabled)
 */
public record KnowledgeSlice(
        McpStatus ragStatus,
        String ragSource,
        Object rag,
        McpStatus wikiStatus,
        String wikiSource,
        Object wiki) {

    public static KnowledgeSlice from(DownstreamSlice rag, DownstreamSlice wiki) {
        return new KnowledgeSlice(
                rag.status(), rag.source(), rag.data(),
                wiki.status(), wiki.source(), wiki.data());
    }

    /**
     * True when both knowledge sources are usable: each is either OK or
     * intentionally DISABLED (DISABLED is not a failure).
     */
    public boolean isOk() {
        return usable(ragStatus) && usable(wikiStatus);
    }

    private static boolean usable(McpStatus status) {
        return status == McpStatus.OK || status == McpStatus.DISABLED;
    }
}
