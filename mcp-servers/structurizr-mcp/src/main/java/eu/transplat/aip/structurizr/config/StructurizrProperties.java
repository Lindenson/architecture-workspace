package eu.transplat.aip.structurizr.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized Structurizr settings. All values come from env vars / config
 * file (see application.yml) — never hardcoded.
 *
 * <p>File-based DSL parsing is the primary mechanism for MVP-2; the optional
 * Structurizr API settings are reserved for a later cloud/on-prem integration.
 */
@ConfigurationProperties(prefix = "structurizr")
public class StructurizrProperties {

    /** Path to the Structurizr DSL workspace file (the C4 model source of truth). */
    private String workspacePath = "../architecture/c4/workspace.dsl";

    /** Optional Structurizr API connection (cloud / on-prem). Unused in MVP-2. */
    private Api api = new Api();

    public String getWorkspacePath() {
        return workspacePath;
    }

    public void setWorkspacePath(String workspacePath) {
        this.workspacePath = workspacePath;
    }

    public Api getApi() {
        return api;
    }

    public void setApi(Api api) {
        this.api = api;
    }

    /** Optional Structurizr API connection settings (not required for file-based parsing). */
    public static class Api {

        /** Structurizr API base URL (cloud service or on-premises). */
        private String url = "";

        /** Structurizr API key. */
        private String key = "";

        /** Structurizr API secret. */
        private String secret = "";

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        /** True when URL, key and secret are all present. */
        public boolean isConfigured() {
            return url != null && !url.isBlank()
                    && key != null && !key.isBlank()
                    && secret != null && !secret.isBlank();
        }
    }
}
