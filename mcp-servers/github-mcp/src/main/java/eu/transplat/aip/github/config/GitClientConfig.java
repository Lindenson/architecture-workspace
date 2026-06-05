package eu.transplat.aip.github.config;

import eu.transplat.aip.github.client.GitClient;
import eu.transplat.aip.github.client.GitHubClient;
import eu.transplat.aip.github.client.GitLabClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the single active {@link GitClient} implementation based on
 * {@code git.provider}. Defaults to GitHub. The bean is always constructible
 * with placeholder credentials so the server starts in any environment.
 */
@Configuration
public class GitClientConfig {

    private static final Logger log = LoggerFactory.getLogger(GitClientConfig.class);

    @Bean
    public GitClient gitClient(GitProperties props) {
        String provider = props.getProvider() == null ? "github" : props.getProvider().trim().toLowerCase();
        if ("gitlab".equals(provider)) {
            log.info("git.provider=gitlab -> using GitLabClient (apiUrl={})", props.getGitlab().getApiUrl());
            return new GitLabClient(props.getGitlab());
        }
        if (!"github".equals(provider)) {
            log.warn("Unknown git.provider='{}', defaulting to github", provider);
        }
        log.info("git.provider=github -> using GitHubClient (apiUrl={}, org={})",
                props.getGithub().getApiUrl(), props.getGithub().getOrg());
        return new GitHubClient(props.getGithub());
    }
}
