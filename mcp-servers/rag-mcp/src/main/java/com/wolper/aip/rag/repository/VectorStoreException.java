package com.wolper.aip.rag.repository;

/**
 * Thrown when the backing Postgres/pgvector store cannot answer — connection
 * refused, schema missing, SQL failure.
 *
 * <p>This is the type {@code RagService} catches to emit {@code DATA_STALE}
 * rather than {@code ERROR}: a database that is down is a degraded source, not
 * a broken tool. Anything else propagates as a genuine error.
 */
public class VectorStoreException extends RuntimeException {

    public VectorStoreException(String message) {
        super(message);
    }

    public VectorStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
