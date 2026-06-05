package eu.transplat.aip.sonar.client;

import eu.transplat.aip.mcp.common.client.RestClientFactory;
import eu.transplat.aip.sonar.config.SonarProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Thin wrapper around the SonarQube Web API. Each method performs a single GET
 * and returns the raw JSON as a {@code Map} so the service layer can flatten it
 * into domain records. The {@link RestClient} is built lazily from
 * {@link SonarProperties} (token as Basic username, empty password).
 *
 * <p>This class never swallows errors itself — exceptions propagate to the
 * service, which converts them into a {@code DATA_STALE} / {@code ERROR}
 * {@link eu.transplat.aip.mcp.common.McpResponse}.
 */
@Component
public class SonarClient {

    /** Headline measures requested from {@code /api/measures/component}. */
    public static final String DEFAULT_METRIC_KEYS =
            "bugs,vulnerabilities,code_smells,coverage,sqale_index,"
                    + "reliability_rating,security_rating,sqale_rating,ncloc,duplicated_lines_density";

    private final SonarProperties properties;

    public SonarClient(SonarProperties properties) {
        this.properties = properties;
    }

    private RestClient client() {
        return RestClientFactory.sonarToken(properties.getBaseUrl(), properties.getToken());
    }

    /** GET /api/qualitygates/project_status?projectKey=... */
    public Map<String, Object> qualityGate(String projectKey) {
        return client().get()
                .uri(uri -> uri.path("/api/qualitygates/project_status")
                        .queryParam("projectKey", projectKey)
                        .build())
                .retrieve()
                .body(MAP);
    }

    /** GET /api/measures/component?component=...&amp;metricKeys=... */
    public Map<String, Object> measures(String projectKey) {
        return measures(projectKey, DEFAULT_METRIC_KEYS);
    }

    /** GET /api/measures/component?component=...&amp;metricKeys=... */
    public Map<String, Object> measures(String projectKey, String metricKeys) {
        return client().get()
                .uri(uri -> uri.path("/api/measures/component")
                        .queryParam("component", projectKey)
                        .queryParam("metricKeys", metricKeys)
                        .build())
                .retrieve()
                .body(MAP);
    }

    /** GET /api/issues/search?componentKeys=...&amp;types=...&amp;statuses=OPEN,CONFIRMED&amp;ps=... */
    public Map<String, Object> issues(String projectKey, String types, int pageSize) {
        return client().get()
                .uri(uri -> uri.path("/api/issues/search")
                        .queryParam("componentKeys", projectKey)
                        .queryParam("types", types)
                        .queryParam("statuses", "OPEN,CONFIRMED")
                        .queryParam("ps", pageSize)
                        .build())
                .retrieve()
                .body(MAP);
    }

    /** GET /api/hotspots/search?projectKey=... */
    public Map<String, Object> hotspots(String projectKey) {
        return client().get()
                .uri(uri -> uri.path("/api/hotspots/search")
                        .queryParam("projectKey", projectKey)
                        .build())
                .retrieve()
                .body(MAP);
    }

    @SuppressWarnings("unchecked")
    private static final Class<Map<String, Object>> MAP = (Class<Map<String, Object>>) (Class<?>) Map.class;
}
