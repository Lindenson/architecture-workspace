package com.wolper.aip.mcp.common.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The resilience contract says a server answers {@code DATA_STALE} instead of
 * failing when its upstream is down. That used to hold only for a REFUSED
 * connection. A HANGING one — socket open, request accepted, no response —
 * blocked the calling thread forever, and with a seven-way fan-out that stalled
 * SHOW_PROJECT_STATE entirely.
 *
 * <p>This test stands up a server that accepts connections and deliberately
 * never replies, which is the failure the old code could not survive.
 */
class RestClientFactoryTimeoutTest {

    /** Accepts a connection, then holds it open saying nothing. */
    private static final class BlackHoleServer implements AutoCloseable {
        private final ServerSocket server;
        private final Thread acceptor;
        private final CountDownLatch connected = new CountDownLatch(1);

        BlackHoleServer() throws IOException {
            server = new ServerSocket(0);
            acceptor = new Thread(() -> {
                try (Socket s = server.accept()) {
                    connected.countDown();
                    Thread.sleep(Duration.ofSeconds(30));   // outlives the read timeout
                } catch (Exception ignored) {
                    // closing the server socket ends the thread; nothing to report
                }
            });
            acceptor.setDaemon(true);
            acceptor.start();
        }

        String url() {
            return "http://127.0.0.1:" + server.getLocalPort();
        }

        @Override
        public void close() throws IOException {
            server.close();
            acceptor.interrupt();
        }
    }

    @Test
    @DisplayName("a hanging upstream fails fast instead of blocking forever")
    void read_timeout_is_enforced() throws Exception {
        try (BlackHoleServer upstream = new BlackHoleServer()) {
            RestClient client = RestClientFactory.bearer(
                    upstream.url(), "token", Duration.ofSeconds(2), Duration.ofMillis(600));

            Instant start = Instant.now();
            assertThrows(ResourceAccessException.class,
                    () -> client.get().uri("/anything").retrieve().body(String.class),
                    "a read timeout must surface as an exception the caller can convert to DATA_STALE");
            Duration elapsed = Duration.between(start, Instant.now());

            assertTrue(elapsed.compareTo(Duration.ofSeconds(10)) < 0,
                    "must give up promptly; waited " + elapsed);
        }
    }

    @Test
    @DisplayName("the defaults are set, short, and connect is stricter than read")
    void defaults_are_sane() {
        // Interactive calls behind an agent conversation, not batch jobs. If
        // these ever grow past a minute, the tool has stopped being usable long
        // before it times out.
        assertNotNull(RestClientFactory.DEFAULT_CONNECT_TIMEOUT);
        assertNotNull(RestClientFactory.DEFAULT_READ_TIMEOUT);
        assertTrue(RestClientFactory.DEFAULT_CONNECT_TIMEOUT.compareTo(Duration.ofSeconds(30)) < 0);
        assertTrue(RestClientFactory.DEFAULT_READ_TIMEOUT.compareTo(Duration.ofSeconds(60)) < 0);
        assertTrue(
                RestClientFactory.DEFAULT_CONNECT_TIMEOUT.compareTo(RestClientFactory.DEFAULT_READ_TIMEOUT) <= 0,
                "connecting should never be allowed to take longer than answering");
    }

    @Test
    @DisplayName("a refused connection also fails fast — the case that already worked")
    void refused_connection_fails_fast() throws Exception {
        int deadPort;
        try (ServerSocket s = new ServerSocket(0)) {
            deadPort = s.getLocalPort();
        }   // closed: nothing is listening now

        RestClient client = RestClientFactory.bearer("http://127.0.0.1:" + deadPort, "token");

        Instant start = Instant.now();
        assertThrows(ResourceAccessException.class,
                () -> client.get().uri("/anything").retrieve().body(String.class));
        assertTrue(Duration.between(start, Instant.now()).compareTo(Duration.ofSeconds(10)) < 0);
    }

    @Test
    @DisplayName("every factory method produces a client with timeouts")
    void no_factory_method_skips_timeouts() throws Exception {
        // The guarantee is "no way to opt out". If someone adds a factory method
        // without a requestFactory, this is the test that should have caught it —
        // so it exercises each one against a black hole rather than trusting the
        // source to look right.
        try (BlackHoleServer upstream = new BlackHoleServer()) {
            RestClient[] clients = {
                    RestClientFactory.bearer(upstream.url(), "t"),
                    RestClientFactory.basic(upstream.url(), "user", "t"),
                    RestClientFactory.sonarToken(upstream.url(), "t"),
            };
            for (RestClient client : clients) {
                Instant start = Instant.now();
                assertThrows(ResourceAccessException.class,
                        () -> client.get().uri("/anything").retrieve().body(String.class));
                assertTrue(Duration.between(start, Instant.now()).compareTo(Duration.ofSeconds(40)) < 0,
                        "a factory method without timeouts would hang here");
            }
        }
    }
}
