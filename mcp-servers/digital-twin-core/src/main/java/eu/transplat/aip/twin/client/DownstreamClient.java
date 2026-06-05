package eu.transplat.aip.twin.client;

import eu.transplat.aip.mcp.common.Confidence;
import eu.transplat.aip.mcp.common.McpStatus;
import eu.transplat.aip.mcp.common.client.RestClientFactory;
import eu.transplat.aip.twin.config.DigitalTwinProperties;
import eu.transplat.aip.twin.domain.DownstreamSlice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Fans out to a downstream MCP server's read-only {@code /api/<svc>/state}
 * endpoint over plain HTTP REST and normalizes the returned {@code McpResponse}
 * envelope into a {@link DownstreamSlice}.
 *
 * <p>RESILIENCE: this client never throws. A connection error, timeout or
 * non-2xx response is logged and turned into a {@code DATA_STALE} slice so the
 * orchestrator can lower confidence rather than fail the whole call.
 */
@Component
public class DownstreamClient {

    private static final Logger log = LoggerFactory.getLogger(DownstreamClient.class);

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final DigitalTwinProperties properties;

    public DownstreamClient(DigitalTwinProperties properties) {
        this.properties = properties;
    }

    /**
     * GET {@code <baseUrl>/api/<svc>/state} with a bearer token and normalize the
     * response. If {@code baseUrl} is blank the downstream is treated as not
     * configured and a stale slice is returned.
     *
     * @param baseUrl downstream base URL
     * @param svc     downstream service segment (e.g. "jira", "github", "sonar")
     * @param label   provenance label used when the call itself fails
     */
    public DownstreamSlice fetchState(String baseUrl, String svc, String label) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return DownstreamSlice.unreachable(label, svc + "-mcp: base URL not configured");
        }
        String path = "/api/" + svc + "/state";
        try {
            RestClient client = RestClientFactory.bearer(baseUrl, properties.getInternalToken());
            Map<String, Object> body = client.get()
                    .uri(path)
                    .retrieve()
                    .body(MAP_TYPE);
            if (body == null) {
                return DownstreamSlice.unreachable(label, svc + "-mcp: empty response from " + path);
            }
            return normalize(body, label);
        } catch (Exception e) {
            log.warn("downstream {} unreachable at {}{}: {}", svc, baseUrl, path, e.toString());
            return DownstreamSlice.unreachable(label,
                    svc + "-mcp: unreachable (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }

    /** Normalize a parsed {@code McpResponse}-shaped map into a {@link DownstreamSlice}. */
    private DownstreamSlice normalize(Map<String, Object> body, String fallbackSource) {
        McpStatus status = parseStatus(body.get("status"));
        Confidence confidence = parseConfidence(body.get("confidence"));
        String source = body.get("source") != null ? String.valueOf(body.get("source")) : fallbackSource;
        String message = body.get("message") != null ? String.valueOf(body.get("message")) : null;
        Object data = body.get("data");
        return new DownstreamSlice(status, source, confidence, data, message);
    }

    private static McpStatus parseStatus(Object raw) {
        if (raw == null) {
            return McpStatus.DATA_STALE;
        }
        try {
            return McpStatus.valueOf(String.valueOf(raw));
        } catch (IllegalArgumentException e) {
            return McpStatus.DATA_STALE;
        }
    }

    private static Confidence parseConfidence(Object raw) {
        if (raw == null) {
            return Confidence.LOW;
        }
        try {
            return Confidence.valueOf(String.valueOf(raw));
        } catch (IllegalArgumentException e) {
            return Confidence.LOW;
        }
    }
}
