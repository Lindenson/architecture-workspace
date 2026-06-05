package eu.transplat.aip.sonar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Externalized SonarQube connection settings. All values come from env vars /
 * config file (see application.yml) — never hardcoded.
 *
 * <p>SonarQube authenticates with a user token presented as the HTTP Basic
 * username (empty password); see {@code RestClientFactory.sonarToken}.
 */
@ConfigurationProperties(prefix = "sonar")
public class SonarProperties {

    /** SonarQube base URL, e.g. https://sonar.your-org.com */
    private String baseUrl = "";

    /** SonarQube user token (Basic-auth username, empty password). */
    private String token = "";

    /** Project keys this server reports on (QUALITY_STATE / DEBT_STATE slice). */
    private List<String> projectKeys = new ArrayList<>();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public List<String> getProjectKeys() {
        return projectKeys;
    }

    public void setProjectKeys(List<String> projectKeys) {
        this.projectKeys = projectKeys;
    }

    /** True when base URL and token are present (vs. placeholder/dev). */
    public boolean isConfigured() {
        return baseUrl != null && !baseUrl.isBlank()
                && token != null && !token.isBlank();
    }
}
