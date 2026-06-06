package eu.transplat.aip.rag.service;

import eu.transplat.aip.mcp.common.Confidence;
import eu.transplat.aip.mcp.common.McpResponse;
import eu.transplat.aip.rag.config.RagProperties;
import eu.transplat.aip.rag.domain.Chunk;
import eu.transplat.aip.rag.domain.ContextPack;
import eu.transplat.aip.rag.domain.IndexResult;
import eu.transplat.aip.rag.domain.RagState;
import eu.transplat.aip.rag.domain.SearchHit;
import eu.transplat.aip.rag.domain.SearchResult;
import eu.transplat.aip.rag.domain.UpdateEmbeddingsResult;
import eu.transplat.aip.rag.embedding.EmbeddingService;
import eu.transplat.aip.rag.store.StoredChunk;
import eu.transplat.aip.rag.store.VectorStore;
import eu.transplat.aip.rag.store.VectorStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * RAG knowledge tools. Every method is resilient and honors {@code rag.enabled}:
 * <ul>
 *   <li>feature off → {@code McpResponse.disabled(...)}</li>
 *   <li>Postgres down / SQL failure → {@code McpResponse.stale(...)} (via {@link VectorStoreException})</li>
 *   <li>other failures → {@code McpResponse.error(...)}</li>
 * </ul>
 * No method ever throws out of a {@code @Tool}, and the DB is never touched at
 * startup — schema/queries run lazily per request.
 */
@Service
public class RagService {

    /** Provenance label carried in every {@link McpResponse}. */
    public static final String SOURCE = "rag-mcp:pgvector";

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    /** File extensions we index. */
    private static final Set<String> INDEXABLE = Set.of(
            "md", "adoc", "txt", "dsl", "yaml", "yml", "java");

    private final VectorStore store;
    private final EmbeddingService embeddings;
    private final RagProperties properties;
    private final Path repoRoot;

    public RagService(VectorStore store, EmbeddingService embeddings, RagProperties properties,
                      @Value("${rag.repo-root:}") String repoRootOverride) {
        this.store = store;
        this.embeddings = embeddings;
        this.properties = properties;
        this.repoRoot = resolveRepoRoot(repoRootOverride);
    }

    // ------------------------------------------------------------------ tools

    @Tool(description = "Index the files under a repo-relative path (md/adoc/txt/dsl/yaml/yml/java) into the RAG store: read, chunk, embed (if a vector provider is available) and upsert. Returns filesIndexed and chunks.")
    public McpResponse indexPath(String path) {
        if (disabled()) {
            return disabledResponse();
        }
        try {
            IndexResult result = indexOne(path, deriveSource(path));
            return McpResponse.ok(result, SOURCE);
        } catch (VectorStoreException e) {
            return stale("indexPath", e);
        } catch (Exception e) {
            return error("indexPath", e);
        }
    }

    @Tool(description = "Clear and reindex all configured RAG source directories. Returns a summary of files and chunks indexed.")
    public McpResponse reindexAll() {
        if (disabled()) {
            return disabledResponse();
        }
        try {
            List<String> sources = properties.getSources();
            int files = 0;
            int chunks = 0;
            List<String> done = new ArrayList<>();
            for (String src : sources) {
                store.deleteBySource(src);
                IndexResult r = indexOne(src, src);
                files += r.filesIndexed();
                chunks += r.chunks();
                done.add(src);
            }
            boolean embedded = embeddings.available();
            return McpResponse.ok(
                    new IndexResult(done, files, chunks, embedded, embedded ? "vector" : "fulltext"), SOURCE);
        } catch (VectorStoreException e) {
            return stale("reindexAll", e);
        } catch (Exception e) {
            return error("reindexAll", e);
        }
    }

    @Tool(description = "Semantic/keyword search over the indexed knowledge. Uses vector search when an embedding provider is available, otherwise full-text. Returns ranked chunks {source, ref, score, snippet} and the mode used.")
    public McpResponse search(String query, Integer topK) {
        if (disabled()) {
            return disabledResponse();
        }
        if (query == null || query.isBlank()) {
            return McpResponse.error(SOURCE, "Query must not be empty.");
        }
        int k = clampTopK(topK);
        try {
            boolean vector = embeddings.available();
            List<SearchHit> hits = runSearch(query, k, vector);
            String mode = vector ? "vector" : "fulltext";
            return McpResponse.ok(new SearchResult(query, mode, k, hits), SOURCE,
                    hits.isEmpty() ? Confidence.LOW : Confidence.HIGH);
        } catch (VectorStoreException e) {
            return stale("search", e);
        } catch (Exception e) {
            return error("search", e);
        }
    }

    @Tool(description = "Retrieve a grounding context pack for a query: a single concatenated, citation-tagged context string plus the list of citations, for the agent to ground its answer.")
    public McpResponse retrieveContext(String query, Integer topK) {
        if (disabled()) {
            return disabledResponse();
        }
        if (query == null || query.isBlank()) {
            return McpResponse.error(SOURCE, "Query must not be empty.");
        }
        int k = clampTopK(topK);
        try {
            boolean vector = embeddings.available();
            List<SearchHit> hits = runSearch(query, k, vector);
            String mode = vector ? "vector" : "fulltext";
            String context = buildContext(hits);
            return McpResponse.ok(new ContextPack(query, mode, context, hits), SOURCE,
                    hits.isEmpty() ? Confidence.LOW : Confidence.HIGH);
        } catch (VectorStoreException e) {
            return stale("retrieveContext", e);
        } catch (Exception e) {
            return error("retrieveContext", e);
        }
    }

    @Tool(description = "Recompute embeddings for all stored chunks with the current provider (e.g. after switching provider). Guards against a stored/provider dimension mismatch, which requires a full reindex instead.")
    public McpResponse updateEmbeddings() {
        if (disabled()) {
            return disabledResponse();
        }
        if (!embeddings.available()) {
            return McpResponse.error(SOURCE,
                    "Active embeddings provider '" + embeddings.provider() + "' cannot produce vectors; nothing to update.");
        }
        try {
            Integer stored = store.storedDimension();
            int dim = embeddings.dimension();
            if (stored != null && stored != dim) {
                return McpResponse.ok(new UpdateEmbeddingsResult(
                        embeddings.provider(), dim, 0, true,
                        "Stored dimension " + stored + " != provider dimension " + dim
                                + "; the table is fixed-width, run reindexAll() to rebuild it."), SOURCE,
                        Confidence.MEDIUM);
            }
            List<StoredChunk> chunks = store.allChunks();
            int updated = 0;
            int batch = 32;
            for (int i = 0; i < chunks.size(); i += batch) {
                List<StoredChunk> slice = chunks.subList(i, Math.min(i + batch, chunks.size()));
                List<float[]> vectors = embeddings.embed(slice.stream().map(StoredChunk::content).toList());
                for (int j = 0; j < slice.size() && j < vectors.size(); j++) {
                    updated += store.updateEmbedding(slice.get(j).id(), vectors.get(j));
                }
            }
            return McpResponse.ok(new UpdateEmbeddingsResult(
                    embeddings.provider(), dim, updated, false,
                    "Recomputed embeddings for " + updated + " chunk(s)."), SOURCE);
        } catch (VectorStoreException e) {
            return stale("updateEmbeddings", e);
        } catch (Exception e) {
            return error("updateEmbeddings", e);
        }
    }

    @Tool(description = "KNOWLEDGE/RAG state slice for the digital twin: {enabled, provider, dimension, dbReachable, indexedChunks, sources, lastIndexedAt}. Returns DISABLED when off and DATA_STALE when Postgres is unreachable.")
    public McpResponse getState() {
        if (disabled()) {
            return disabledResponse();
        }
        String provider = embeddings.provider();
        int dim = embeddings.dimension();
        List<String> sources = properties.getSources();
        boolean reachable = store.isReachable();
        if (!reachable) {
            return McpResponse.stale(
                    new RagState(true, provider, dim, false, null, sources, null),
                    SOURCE, "Postgres unreachable — RAG index not queryable.");
        }
        try {
            long count = store.countChunks();
            Instant last = store.lastIndexedAt();
            return McpResponse.ok(
                    new RagState(true, provider, dim, true, count, sources, last), SOURCE,
                    count > 0 ? Confidence.HIGH : Confidence.MEDIUM);
        } catch (VectorStoreException e) {
            return McpResponse.stale(
                    new RagState(true, provider, dim, false, null, sources, null),
                    SOURCE, "RAG store query failed: " + e.getMessage());
        } catch (Exception e) {
            return error("getState", e);
        }
    }

    // -------------------------------------------------------------- internals

    private IndexResult indexOne(String path, String source) {
        Path base = repoRoot.resolve(path).normalize();
        List<Chunk> chunks = new ArrayList<>();
        int files = 0;
        if (!Files.exists(base)) {
            log.warn("indexPath: '{}' does not exist under repo root {}", path, repoRoot);
            return new IndexResult(List.of(path), 0, 0, embeddings.available(),
                    embeddings.available() ? "vector" : "fulltext");
        }
        try (Stream<Path> walk = Files.walk(base)) {
            List<Path> filesToIndex = walk.filter(Files::isRegularFile).filter(this::indexable).toList();
            for (Path file : filesToIndex) {
                String content;
                try {
                    content = Files.readString(file);
                } catch (IOException e) {
                    log.warn("indexPath: cannot read {}: {}", file, e.toString());
                    continue;
                }
                String ref = repoRoot.relativize(file).toString();
                List<String> pieces = Chunker.chunk(content,
                        properties.getChunk().getMaxChars(), properties.getChunk().getOverlap());
                int no = 0;
                for (String piece : pieces) {
                    chunks.add(new Chunk(source, ref, no++, piece));
                }
                files++;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to walk '" + path + "': " + e.getMessage(), e);
        }

        List<float[]> vectors = null;
        boolean embedded = false;
        if (embeddings.available() && !chunks.isEmpty()) {
            vectors = embeddings.embed(chunks.stream().map(Chunk::content).toList());
            embedded = true;
        }
        store.upsertChunks(chunks, vectors);
        return new IndexResult(List.of(path), files, chunks.size(), embedded, embedded ? "vector" : "fulltext");
    }

    private List<SearchHit> runSearch(String query, int k, boolean vector) {
        if (vector) {
            float[] q = embeddings.embed(List.of(query)).get(0);
            return store.vectorSearch(q, k);
        }
        return store.fullTextSearch(query, k);
    }

    private String buildContext(List<SearchHit> hits) {
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (SearchHit h : hits) {
            sb.append("[").append(i++).append("] ")
                    .append(h.ref()).append("#chunk").append(h.chunkNo())
                    .append(" (").append(h.source()).append(")\n")
                    .append(h.snippet().strip())
                    .append("\n\n");
        }
        return sb.toString().strip();
    }

    private boolean indexable(Path p) {
        String name = p.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return false;
        }
        return INDEXABLE.contains(name.substring(dot + 1).toLowerCase());
    }

    /** Derive a source-group label from an arbitrary path (its first segment). */
    private String deriveSource(String path) {
        String p = path.replace('\\', '/');
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        int slash = p.indexOf('/');
        return slash > 0 ? p.substring(0, slash) : p;
    }

    private int clampTopK(Integer topK) {
        if (topK == null || topK <= 0) {
            return properties.getTopK();
        }
        return Math.min(topK, 100);
    }

    private boolean disabled() {
        return !properties.isEnabled();
    }

    private McpResponse disabledResponse() {
        return McpResponse.disabled(SOURCE, "RAG layer disabled for this project (rag.enabled=false)");
    }

    private McpResponse stale(String tool, VectorStoreException e) {
        log.warn("rag tool {} degraded (DB): {}", tool, e.toString());
        return McpResponse.stale(null, SOURCE, e.getMessage());
    }

    private McpResponse error(String tool, Exception e) {
        log.warn("rag tool {} failed: {}", tool, e.toString());
        return McpResponse.error(SOURCE, e.getMessage());
    }

    /**
     * Resolve the repo root. Priority: explicit {@code rag.repo-root} override,
     * else walk up from the working directory until a dir containing
     * {@code mcp-servers} (or the {@code .claude} digital-twin marker) is found,
     * else the working directory itself.
     */
    static Path resolveRepoRoot(String override) {
        if (override != null && !override.isBlank()) {
            return Paths.get(override).toAbsolutePath().normalize();
        }
        Path dir = Paths.get("").toAbsolutePath().normalize();
        Path cursor = dir;
        for (int i = 0; i < 6 && cursor != null; i++) {
            if (Files.isDirectory(cursor.resolve("mcp-servers")) || Files.isDirectory(cursor.resolve(".claude"))) {
                return cursor;
            }
            cursor = cursor.getParent();
        }
        // Fallback: if we are running from inside mcp-servers/rag-mcp, climb two levels.
        if (dir.getFileName() != null && dir.getFileName().toString().equals("rag-mcp")
                && dir.getParent() != null && dir.getParent().getParent() != null) {
            return dir.getParent().getParent();
        }
        return dir;
    }
}
