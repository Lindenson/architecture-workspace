package eu.transplat.aip.jira.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Externalized Jira (Atlassian Cloud) connection settings. All values come from
 * env vars / config file (see application.yml) — never hardcoded.
 *
 * <p>{@code writeEnabled} gates the mutating tools: when {@code false} (default)
 * the server is read-only and write tools return an error.
 */
@ConfigurationProperties(prefix = "jira")
public class JiraProperties {

    /** Atlassian Cloud base URL, e.g. https://your-org.atlassian.net */
    private String baseUrl = "";

    /** Account email used as the Basic-auth username. */
    private String email = "";

    /** Atlassian API token used as the Basic-auth password. */
    private String apiToken = "";

    /** Project keys this server reports on (DELIVERY_STATE slice). */
    private List<String> projectKeys = new ArrayList<>();

    /** Master switch for mutating tools. Default off for safety. */
    private boolean writeEnabled = false;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public List<String> getProjectKeys() {
        return projectKeys;
    }

    public void setProjectKeys(List<String> projectKeys) {
        this.projectKeys = projectKeys;
    }

    public boolean isWriteEnabled() {
        return writeEnabled;
    }

    public void setWriteEnabled(boolean writeEnabled) {
        this.writeEnabled = writeEnabled;
    }

    /** True when base URL and credentials are present (vs. placeholder/dev). */
    public boolean isConfigured() {
        return baseUrl != null && !baseUrl.isBlank()
                && email != null && !email.isBlank()
                && apiToken != null && !apiToken.isBlank();
    }
}
