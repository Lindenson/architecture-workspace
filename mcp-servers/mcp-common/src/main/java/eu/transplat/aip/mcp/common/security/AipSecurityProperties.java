package eu.transplat.aip.mcp.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Internal auth shared by all AIP MCP servers. The single bearer token the
 * Claude agent (and the orchestrator) present to every server. Sourced from the
 * {@code AIP_INTERNAL_TOKEN} env var / config file — never hardcoded.
 */
@ConfigurationProperties(prefix = "aip.security")
public class AipSecurityProperties {

    /** Shared internal bearer token. If blank, auth is disabled (dev only). */
    private String internalToken = "";

    /** When false, the auth filter is not registered (useful for local dev). */
    private boolean enabled = true;

    public String getInternalToken() {
        return internalToken;
    }

    public void setInternalToken(String internalToken) {
        this.internalToken = internalToken;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
