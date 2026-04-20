package io.alphainfo;

import static org.junit.jupiter.api.Assertions.*;

import io.alphainfo.Models.AnalyzeRequest;
import io.alphainfo.Models.FingerprintResult;
import java.util.ArrayList;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AlphaInfoClientTest {

    private MockWebServer server;

    @BeforeEach
    void setup() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void teardown() throws Exception {
        server.shutdown();
    }

    private AlphaInfoClient newClient() {
        return new AlphaInfoClient("ai_test", server.url("/").toString());
    }

    private static List<Double> zeros(int n) {
        List<Double> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(0.0);
        return out;
    }

    @Test
    void constantsMatchServer() {
        assertEquals(192, AlphaInfoConstants.MIN_FINGERPRINT_SAMPLES);
        assertEquals(50, AlphaInfoConstants.MIN_FINGERPRINT_SAMPLES_WITH_BASELINE);
    }

    @Test
    void emptyApiKeyThrowsValidation() {
        ValidationException ex =
                assertThrows(ValidationException.class, () -> new AlphaInfoClient(""));
        assertTrue(ex.getMessage().contains("alphainfo.io/register"));
    }

    @Test
    void fingerprintCompletePath() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\n"
                        + "  \"analysis_id\": \"abc\",\n"
                        + "  \"structural_score\": 0.9,\n"
                        + "  \"change_detected\": false,\n"
                        + "  \"change_score\": 0.1,\n"
                        + "  \"confidence_band\": \"stable\",\n"
                        + "  \"engine_version\": \"t\",\n"
                        + "  \"metrics\": {\n"
                        + "    \"sim_local\": 0.9,\n"
                        + "    \"sim_spectral\": 0.85,\n"
                        + "    \"sim_fractal\": 0.8,\n"
                        + "    \"sim_transition\": 0.91,\n"
                        + "    \"sim_trend\": 0.88,\n"
                        + "    \"fingerprint_available\": true,\n"
                        + "    \"fingerprint_reason\": null\n"
                        + "  }\n"
                        + "}"));
        try (var c = newClient()) {
            FingerprintResult fp = c.fingerprint(
                    new AnalyzeRequest()
                            .signal(zeros(AlphaInfoConstants.MIN_FINGERPRINT_SAMPLES))
                            .samplingRate(1));
            assertTrue(fp.isComplete());
            assertNotNull(fp.getVector());
            assertEquals(5, fp.getVector().size());
            assertEquals(0.9, fp.getVector().get(0));
        }
    }

    @Test
    void fingerprintIncompleteReturnsNullVector() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\n"
                        + "  \"analysis_id\": \"abc\",\n"
                        + "  \"structural_score\": 0.5,\n"
                        + "  \"change_detected\": false,\n"
                        + "  \"change_score\": 0.5,\n"
                        + "  \"confidence_band\": \"transition\",\n"
                        + "  \"engine_version\": \"t\",\n"
                        + "  \"metrics\": {\n"
                        + "    \"sim_local\": null,\n"
                        + "    \"sim_spectral\": null,\n"
                        + "    \"sim_fractal\": null,\n"
                        + "    \"sim_transition\": null,\n"
                        + "    \"sim_trend\": null,\n"
                        + "    \"fingerprint_available\": false,\n"
                        + "    \"fingerprint_reason\": \"signal_too_short\"\n"
                        + "  }\n"
                        + "}"));
        try (var c = newClient()) {
            FingerprintResult fp = c.fingerprint(new AnalyzeRequest().signal(zeros(20)).samplingRate(1));
            assertFalse(fp.isComplete());
            assertNull(fp.getVector(), "vector must be null when incomplete (no silent zeros)");
            assertNull(fp.simLocal);
            assertEquals("signal_too_short", fp.fingerprintReason);
        }
    }

    @Test
    void auth401MapsToAuthException() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"detail\":\"Invalid API key\"}"));
        try (var c = newClient()) {
            assertThrows(AuthException.class,
                    () -> c.analyze(new AnalyzeRequest().signal(zeros(10)).samplingRate(1)));
        }
    }

    @Test
    void rate429MapsWithRetryAfter() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(429)
                .setHeader("Content-Type", "application/json")
                .setHeader("Retry-After", "42")
                .setBody("{\"detail\":\"Rate limit exceeded\"}"));
        try (var c = newClient()) {
            RateLimitException ex = assertThrows(RateLimitException.class,
                    () -> c.analyze(new AnalyzeRequest().signal(zeros(10)).samplingRate(1)));
            assertEquals(42, ex.getRetryAfterSeconds());
        }
    }

    @Test
    void rateLimitHeadersCaptured() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setHeader("X-RateLimit-Limit", "100")
                .setHeader("X-RateLimit-Remaining", "73")
                .setHeader("X-RateLimit-Reset", "1234567890")
                .setBody("{\n"
                        + "  \"analysis_id\": \"a\",\n"
                        + "  \"structural_score\": 0.9,\n"
                        + "  \"change_detected\": false,\n"
                        + "  \"change_score\": 0.1,\n"
                        + "  \"confidence_band\": \"stable\",\n"
                        + "  \"engine_version\": \"t\"\n"
                        + "}"));
        try (var c = newClient()) {
            assertNull(c.getRateLimitInfo());
            c.analyze(new AnalyzeRequest().signal(zeros(200)).samplingRate(1));
            assertNotNull(c.getRateLimitInfo());
            assertEquals(100, c.getRateLimitInfo().limit);
            assertEquals(73, c.getRateLimitInfo().remaining);
        }
    }

    @Test
    void auditReplayEmptyIdFailsLocally() {
        try (var c = newClient()) {
            assertThrows(ValidationException.class, () -> c.auditReplay(""));
        }
    }

    // ── Bloco 1.2 — close() cleanup ──────────────────────────────────────

    @Test
    void closeIsIdempotent() {
        var c = newClient();
        c.close();
        c.close(); // must not throw
    }

    @Test
    void tryWithResourcesDoesNotLeak() {
        // The point of this test is simply to exercise AutoCloseable at compile
        // time and prove that close() runs without throwing in the happy path.
        try (var c = newClient()) {
            assertNotNull(c);
        }
    }
}
