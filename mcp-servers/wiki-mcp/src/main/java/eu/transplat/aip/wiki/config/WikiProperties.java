package eu.transplat.aip.wiki.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Externalized Wiki (Confluence Cloud) connection settings. All values come from
 * env vars / config file (see application.yml) — never hardcoded.
 *
 * <p>This source is <b>optional</b>: when {@code enabled=false} every tool returns
 * {@link eu.transplat.aip.mcp.common.McpResponse#disabled} and no network call is
 * made. {@code writeEnabled} additionally gates the mutating tools: when
 * {@code false} (default) the server is read-only and write tools return an error.
 */
@ConfigurationProperties(prefix = "wiki")
public class WikiProperties {

    /** Master switch for the whole KNOWLEDGE_DOCUMENTS source. Default on. */
    private boolean enabled = true;

    /** Confluence Cloud base URL, e.g. https://your-org.atlassian.net/wiki */
    private String baseUrl = "";

    /** Account email used as the Basic-auth username. */
    private String email = "";

    /** Atlassian API token used as the Basic-auth password. */
    private String apiToken = "";

    /** Space keys this server reports on (KNOWLEDGE_DOCUMENTS slice). */
    private List<String> spaceKeys = new ArrayList<>();

    /** Master switch for mutating tools. Default off for safety. */
    private boolean writeEnabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

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

    public List<String> getSpaceKeys() {
        return spaceKeys;
    }

    public void setSpaceKeys(List<String> spaceKeys) {
        this.spaceKeys = spaceKeys;
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
