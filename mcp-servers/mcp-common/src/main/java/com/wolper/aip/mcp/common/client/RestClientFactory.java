package com.wolper.aip.mcp.common.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * Builds preconfigured {@link RestClient}s for upstream systems (Jira, GitHub,
 * Sonar, …). Auth is applied as a default header; credentials are passed in by
 * the caller from externalized config — never read from constants here.
 *
 * <h2>Timeouts are not optional</h2>
 * Every client built here carries a connect timeout and a read timeout, and no
 * factory method omits them.
 *
 * <p>The resilience contract written across this platform — "a server answers
 * {@code DATA_STALE} rather than failing when its upstream is down" — used to
 * hold only for a <em>refused</em> connection. It did not hold for a
 * <em>hanging</em> one, which is the more common production failure: Jira under
 * load, Confluence in a GC pause, Neo4j on a heavy traversal. The socket is
 * open, the request is accepted, and no response ever arrives.
 *
 * <p>With no read timeout the calling thread waits forever, and because
 * digital-twin-core fans out to seven sources, one hung upstream stalled
 * {@code SHOW_PROJECT_STATE} entirely: the agent got no answer, no
 * {@code DATA_STALE} and no error — it simply waited. A tool that never returns
 * is worse than one that returns bad news, because nothing downstream can react
 * to it.
 *
 * <p>The values are deliberately short. These are interactive calls behind an
 * agent conversation, not batch jobs: a {@code DATA_STALE} slice delivered in
 * time is worth more than a complete one delivered late.
 */
public final class RestClientFactory {

    /** Time to establish the TCP connection. A refused host fails far sooner than this. */
    public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);

    /** Time to wait for a response once connected. This is the one that catches a hang. */
    public static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(15);

    private RestClientFactory() {
    }

    /**
     * A request factory whose timeouts apply to every call made through it,
     * with no way for a caller to opt out.
     */
    private static JdkClientHttpRequestFactory requestFactory(Duration connectTimeout,
                                                              Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(readTimeout);
        return factory;
    }

    private static RestClient.Builder base(String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory(DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT));
    }

    /** Bearer-token client (GitHub, Sonar token-as-bearer, internal calls). */
    public static RestClient bearer(String baseUrl, String token) {
        return base(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
    }

    /** HTTP Basic client (Jira/Confluence email + API token). */
    public static RestClient basic(String baseUrl, String username, String token) {
        String creds = username + ":" + token;
        String encoded = Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
        return base(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoded)
                .build();
    }

    /** SonarQube uses the token as the Basic username with an empty password. */
    public static RestClient sonarToken(String baseUrl, String token) {
        return basic(baseUrl, token, "");
    }

    /**
     * Bearer client with explicit timeouts, for the rare call that genuinely
     * needs longer — a full jQAssistant rescan, say. Prefer the defaults: a
     * timeout raised "just to be safe" is how the infinite wait got here in the
     * first place.
     */
    public static RestClient bearer(String baseUrl, String token,
                                    Duration connectTimeout, Duration readTimeout) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory(connectTimeout, readTimeout))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
    }
}
