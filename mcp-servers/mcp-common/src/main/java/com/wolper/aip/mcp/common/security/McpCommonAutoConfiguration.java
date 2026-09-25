package com.wolper.aip.mcp.common.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(McpCommonAutoConfiguration.class);

    @Bean
    public FilterRegistrationBean<InternalTokenAuthFilter> aipInternalTokenAuthFilter(
            AipSecurityProperties properties) {
        FilterRegistrationBean<InternalTokenAuthFilter> registration = new FilterRegistrationBean<>();
        boolean active = properties.isEnabled() && StringUtils.hasText(properties.getInternalToken());
        if (active) {
            registration.setFilter(new InternalTokenAuthFilter(properties.getInternalToken()));
            registration.addUrlPatterns("/*");
            log.info("AIP internal-token authentication is ACTIVE on this server.");
        } else {
            // Fail-open is intentional for local development, but it must never
            // be SILENT. A server that believes it is protected and is not is
            // worse than one that is knowingly open: the whole point of this
            // platform is that a system states its real condition out loud.
            registration.setFilter(new InternalTokenAuthFilter(properties.getInternalToken()));
            registration.setEnabled(false);
            if (!properties.isEnabled()) {
                log.warn("AIP internal-token authentication is DISABLED "
                        + "(aip.security.enabled=false). Every endpoint on this server is OPEN.");
            } else {
                log.warn("AIP internal-token authentication is OFF because no token is configured. "
                        + "Every endpoint on this server is OPEN. Set AIP_INTERNAL_TOKEN "
                        + "(see .env.example) to turn it on.");
            }
        }
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
