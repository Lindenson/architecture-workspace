package eu.transplat.aip.mcp.common.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

/**
 * Auto-registers the {@link InternalTokenAuthFilter} for every AIP MCP server.
 * Activated automatically via {@code AutoConfiguration.imports}. The filter runs
 * only when {@code aip.security.enabled=true} (default) and a token is set, so
 * local dev without a token stays open.
 */
@AutoConfiguration
@EnableConfigurationProperties(AipSecurityProperties.class)
public class McpCommonAutoConfiguration {

    @Bean
    public FilterRegistrationBean<InternalTokenAuthFilter> aipInternalTokenAuthFilter(
            AipSecurityProperties properties) {
        FilterRegistrationBean<InternalTokenAuthFilter> registration = new FilterRegistrationBean<>();
        boolean active = properties.isEnabled() && StringUtils.hasText(properties.getInternalToken());
        if (active) {
            registration.setFilter(new InternalTokenAuthFilter(properties.getInternalToken()));
            registration.addUrlPatterns("/*");
        } else {
            // No token configured — register a pass-through disabled filter.
            registration.setFilter(new InternalTokenAuthFilter(properties.getInternalToken()));
            registration.setEnabled(false);
        }
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
