package eu.transplat.aip.twin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized configuration for the digital-twin orchestrator. Holds the
 * shared internal bearer token presented to every downstream MCP server and the
 * base URLs of those servers. All values come from env vars / config file (see
 * {@code application.yml}) — never hardcoded.
 */
@ConfigurationProperties(prefix = "digital-twin")
public class DigitalTwinProperties {

    /** Shared internal bearer token presented to downstream MCP servers. */
    private String internalToken = "";

    /** Base URLs of the downstream MCP servers the orchestrator fans out to. */
    private Downstream downstream = new Downstream();

    /** Optional capability toggles (e.g. the knowledge layer). */
    private Features features = new Features();

    public String getInternalToken() {
        return internalToken;
    }

    public void setInternalToken(String internalToken) {
        this.internalToken = internalToken;
    }

    public Downstream getDownstream() {
        return downstream;
    }

    public void setDownstream(Downstream downstream) {
        this.downstream = downstream;
    }

    public Features getFeatures() {
        return features;
    }

    public void setFeatures(Features features) {
        this.features = features;
    }

    /** Optional, configurable capability toggles. */
    public static class Features {

        /** The knowledge layer (rag-mcp + wiki-mcp). OFF by default. */
        private Knowledge knowledge = new Knowledge();

        public Knowledge getKnowledge() {
            return knowledge;
        }

        public void setKnowledge(Knowledge knowledge) {
            this.knowledge = knowledge;
        }

        /**
         * Knowledge layer feature flag. When {@code enabled} is {@code false}
         * (the default) the orchestrator emits a {@code DISABLED} knowledge slice
         * and never calls rag-mcp/wiki-mcp; the slice does not affect confidence
         * nor appear in {@code staleSources}.
         */
        public static class Knowledge {

            /** Whether the knowledge layer is enabled for this project. Default false. */
            private boolean enabled = false;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }
        }
    }

    /** Base URLs of each downstream MCP server. */
    public static class Downstream {

        /** DELIVERY_STATE source (Jira). Live (MVP-1). */
        private String jiraMcp = "";
        /** CODE_STATE source (GitHub). Live (MVP-1). */
        private String githubMcp = "";
        /** QUALITY_STATE / DEBT_STATE source (SonarQube). Live (MVP-1). */
        private String sonarMcp = "";
        /** ARCHITECTURE_STATE source (Structurizr). Planned (MVP-2+). */
        private String structurizrMcp = "";
        /** ARCHITECTURE_STATE source (jQAssistant). Planned (MVP-2+). */
        private String jqassistantMcp = "";
        /** KNOWLEDGE_STATE source (Wiki). Used only when the knowledge feature flag is on. */
        private String wikiMcp = "";
        /** KNOWLEDGE_STATE source (RAG). Used only when the knowledge feature flag is on. */
        private String ragMcp = "";

        public String getJiraMcp() {
            return jiraMcp;
        }

        public void setJiraMcp(String jiraMcp) {
            this.jiraMcp = jiraMcp;
        }

        public String getGithubMcp() {
            return githubMcp;
        }

        public void setGithubMcp(String githubMcp) {
            this.githubMcp = githubMcp;
        }

        public String getSonarMcp() {
            return sonarMcp;
        }

        public void setSonarMcp(String sonarMcp) {
            this.sonarMcp = sonarMcp;
        }

        public String getStructurizrMcp() {
            return structurizrMcp;
        }

        public void setStructurizrMcp(String structurizrMcp) {
            this.structurizrMcp = structurizrMcp;
        }

        public String getJqassistantMcp() {
            return jqassistantMcp;
        }

        public void setJqassistantMcp(String jqassistantMcp) {
            this.jqassistantMcp = jqassistantMcp;
        }

        public String getWikiMcp() {
            return wikiMcp;
        }

        public void setWikiMcp(String wikiMcp) {
            this.wikiMcp = wikiMcp;
        }

        public String getRagMcp() {
            return ragMcp;
        }

        public void setRagMcp(String ragMcp) {
            this.ragMcp = ragMcp;
        }
    }
}
