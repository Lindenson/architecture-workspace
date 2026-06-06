package eu.transplat.aip.jqa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Externalized jQAssistant settings. All values come from env vars / config file
 * (see application.yml) — never hardcoded.
 *
 * <p>The server reads a Neo4j database that jQAssistant has populated with the
 * Java bytecode dependency graph (nodes {@code :Type}, {@code :Package},
 * {@code :Artifact}; relationship {@code :DEPENDS_ON}). It is the
 * ARCHITECTURE_GRAPH source for the digital twin (MVP-2).
 */
@ConfigurationProperties(prefix = "jqassistant")
public class JqaProperties {

    @NestedConfigurationProperty
    private Neo4j neo4j = new Neo4j();

    @NestedConfigurationProperty
    private Layering layering = new Layering();

    /** Optional directory of compiled classes/jars for {@code runScan()} to scan. */
    private String scanDir = "";

    /** Optional path to a jQAssistant CLI executable for {@code runScan()}. */
    private String cli = "";

    public Neo4j getNeo4j() {
        return neo4j;
    }

    public void setNeo4j(Neo4j neo4j) {
        this.neo4j = neo4j;
    }

    public Layering getLayering() {
        return layering;
    }

    public void setLayering(Layering layering) {
        this.layering = layering;
    }

    public String getScanDir() {
        return scanDir;
    }

    public void setScanDir(String scanDir) {
        this.scanDir = scanDir;
    }

    public String getCli() {
        return cli;
    }

    public void setCli(String cli) {
        this.cli = cli;
    }

    /** True when a CLI executable and a scan directory are both configured. */
    public boolean isScanConfigured() {
        return cli != null && !cli.isBlank()
                && scanDir != null && !scanDir.isBlank();
    }

    /** Neo4j (bolt) connection settings for the jQAssistant-populated database. */
    public static class Neo4j {

        /** Bolt URI, e.g. bolt://localhost:7687. */
        private String uri = "bolt://localhost:7687";

        /** Database user. */
        private String user = "neo4j";

        /** Database password (sourced from env/config, never hardcoded). */
        private String password = "";

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    /**
     * Configurable package globs used to derive layering rules. jQAssistant-style
     * globs (e.g. {@code *..controller..}) are translated to substring/regex
     * matching on the fully-qualified name; see {@code JqaService}.
     */
    public static class Layering {

        private String controllerPackage = "*..controller..";
        private String servicePackage = "*..service..";
        private String repositoryPackage = "*..repository..";

        public String getControllerPackage() {
            return controllerPackage;
        }

        public void setControllerPackage(String controllerPackage) {
            this.controllerPackage = controllerPackage;
        }

        public String getServicePackage() {
            return servicePackage;
        }

        public void setServicePackage(String servicePackage) {
            this.servicePackage = servicePackage;
        }

        public String getRepositoryPackage() {
            return repositoryPackage;
        }

        public void setRepositoryPackage(String repositoryPackage) {
            this.repositoryPackage = repositoryPackage;
        }
    }
}
