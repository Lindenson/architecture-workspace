package eu.transplat.aip.mcp.common.client;

import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Builds preconfigured {@link RestClient}s for upstream systems (Jira, GitHub,
 * Sonar, …). Auth is applied as a default header; credentials are passed in by
 * the caller from externalized config — never read from constants here.
 */
public final class RestClientFactory {

    private RestClientFactory() {
    }

    /** Bearer-token client (GitHub, Sonar token-as-bearer, internal calls). */
    public static RestClient bearer(String baseUrl, String token) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
    }

    /** HTTP Basic client (Jira/Confluence email + API token). */
    public static RestClient basic(String baseUrl, String username, String token) {
        String creds = username + ":" + token;
        String encoded = Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoded)
                .build();
    }

    /** SonarQube uses the token as the Basic username with an empty password. */
    public static RestClient sonarToken(String baseUrl, String token) {
        return basic(baseUrl, token, "");
    }
}
