package eu.transplat.aip.rag;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test: the application context loads with security disabled, the
 * embeddings provider set to {@code none} (so no ONNX model is downloaded or
 * loaded — fast and network-free), and placeholder DB credentials pointing at a
 * port nothing listens on. Proves the RESILIENCE requirement: the RAG server
 * boots without Postgres and without touching the network.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "aip.security.enabled=false",
        "rag.embeddings.provider=none",
        "spring.datasource.url=jdbc:postgresql://localhost:1/architecture_ai_test",
        "spring.datasource.username=test",
        "spring.datasource.password="
})
class RagMcpApplicationTests {

    @Test
    void contextLoads() {
        // Context startup is the assertion.
    }
}
