package eu.transplat.aip.github.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for the Git provider this server talks to. Sourced from
 * {@code git.*} in application.yml / external config — never hardcoded.
 *
 * <p>Both provider blocks are always bound; only the one selected by
 * {@link #provider} is wired into a {@code GitClient} bean.
 */
@ConfigurationProperties(prefix = "git")
public class GitProperties {

    /** Either {@code github} or {@code gitlab}. */
    private String provider = "github";

    private GitHub github = new GitHub();

    private GitLab gitlab = new GitLab();

    /** Repositories the digital-twin tracks, as {@code owner/name} (GitHub) or project paths/ids (GitLab). */
    private List<String> repositories = new ArrayList<>();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public GitHub getGithub() {
        return github;
    }

    public void setGithub(GitHub github) {
        this.github = github;
    }

    public GitLab getGitlab() {
        return gitlab;
    }

    public void setGitlab(GitLab gitlab) {
        this.gitlab = gitlab;
    }

    public List<String> getRepositories() {
        return repositories;
    }

    public void setRepositories(List<String> repositories) {
        this.repositories = repositories == null ? new ArrayList<>() : repositories;
    }

    /** GitHub REST connection settings. */
    public static class GitHub {
        private String apiUrl = "https://api.github.com";
        private String token = "";
        private String org = "";

        public String getApiUrl() {
            return apiUrl;
        }

        public void setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getOrg() {
            return org;
        }

        public void setOrg(String org) {
            this.org = org;
        }
    }

    /** GitLab v4 API connection settings. */
    public static class GitLab {
        private String apiUrl = "";
        private String token = "";

        public String getApiUrl() {
            return apiUrl;
        }

        public void setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}
