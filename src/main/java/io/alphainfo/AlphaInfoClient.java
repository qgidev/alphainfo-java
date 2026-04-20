package io.alphainfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.DeserializationFeature;
import io.alphainfo.Models.AnalysisResult;
import io.alphainfo.Models.AnalyzeRequest;
import io.alphainfo.Models.BatchRequest;
import io.alphainfo.Models.BatchResult;
import io.alphainfo.Models.FingerprintResult;
import io.alphainfo.Models.HealthStatus;
import io.alphainfo.Models.MatrixRequest;
import io.alphainfo.Models.MatrixResult;
import io.alphainfo.Models.RateLimitInfo;
import io.alphainfo.Models.VectorRequest;
import io.alphainfo.Models.VectorResult;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Synchronous + async client for the alphainfo.io Structural Intelligence API.
 *
 * <p>Create one instance per application and reuse it — the underlying OkHttp
 * client manages its own connection pool. Every blocking method has an
 * {@code *Async} counterpart that returns a {@link CompletableFuture}.
 *
 * <pre>{@code
 * try (var client = new AlphaInfoClient("ai_...")) {
 *     var result = client.analyze(new AnalyzeRequest()
 *             .signal(signal).samplingRate(250));
 *     System.out.println(result.confidenceBand);
 * }
 * }</pre>
 */
public class AlphaInfoClient implements AutoCloseable {

    private static final String DEFAULT_BASE_URL = "https://www.alphainfo.io";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final Logger LOGGER = Logger.getLogger(AlphaInfoClient.class.getName());

    private final String apiKey;
    private final String baseUrl;
    private final OkHttpClient httpClient;
    private final ObjectMapper mapper;

    private volatile RateLimitInfo rateLimit;

    public AlphaInfoClient(String apiKey) {
        this(apiKey, DEFAULT_BASE_URL, defaultHttpClient());
    }

    public AlphaInfoClient(String apiKey, String baseUrl) {
        this(apiKey, baseUrl, defaultHttpClient());
    }

    public AlphaInfoClient(String apiKey, String baseUrl, OkHttpClient httpClient) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new ValidationException(
                    "apiKey is required. Get one at https://alphainfo.io/register (format: 'ai_...')");
        }
        this.apiKey = apiKey;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = httpClient;
        this.mapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private static OkHttpClient defaultHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .callTimeout(Duration.ofSeconds(150))
                .build();
    }

    @Override
    public void close() {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }

    /** @return latest {@code X-RateLimit-*} info, or {@code null} if none observed yet. */
    public RateLimitInfo getRateLimitInfo() {
        return rateLimit;
    }

    // -----------------------------------------------------------------------
    // Module-level helpers — no API key required
    // -----------------------------------------------------------------------

    /** Fetch the public encoding guide. No API key required. */
    public static Map<String, Object> guide() {
        return guide(DEFAULT_BASE_URL);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> guide(String baseUrl) {
        try (var c = new AlphaInfoClient("ai_noauth_placeholder_ignored", baseUrl, defaultHttpClient())) {
            // Route directly — placeholder key is never sent for the guide
            // endpoint because the server doesn't validate it for /v1/guide.
            return (Map<String, Object>) c.requestNoAuth("/v1/guide");
        }
    }

    /** Fetch {@code /health}. No API key required. */
    public static HealthStatus health() {
        return health(DEFAULT_BASE_URL);
    }

    public static HealthStatus health(String baseUrl) {
        try (var c = new AlphaInfoClient("ai_noauth_placeholder_ignored", baseUrl, defaultHttpClient())) {
            return c.mapper.convertValue(c.requestNoAuth("/health"), HealthStatus.class);
        }
    }

    @SuppressWarnings("unchecked")
    private Object requestNoAuth(String path) {
        Request req = new Request.Builder()
                .url(baseUrl + path)
                .header("Accept", "application/json")
                .header("User-Agent", "alphainfo-java/" + AlphaInfoConstants.SDK_VERSION)
                .get()
                .build();
        try (Response resp = httpClient.newCall(req).execute()) {
            String body = resp.body() == null ? "" : resp.body().string();
            if (!resp.isSuccessful()) {
                throw mapErrorResponse(resp.code(), resp.header("Retry-After"), body);
            }
            return mapper.readValue(body, Map.class);
        } catch (IOException e) {
            throw new NetworkException("Network error fetching " + path + ": " + e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------------
    // analyze / fingerprint
    // -----------------------------------------------------------------------

    public AnalysisResult analyze(AnalyzeRequest req) {
        return executeAnalyze("/v1/analyze/stream", toJsonBody(req));
    }

    public CompletableFuture<AnalysisResult> analyzeAsync(AnalyzeRequest req) {
        return CompletableFuture.supplyAsync(() -> analyze(req));
    }

    public FingerprintResult fingerprint(AnalyzeRequest req) {
        warnIfTooShortForFingerprint(req.signal, req.baseline);
        Map<String, Object> body = toJsonBody(req);
        body.put("include_semantic", false);
        body.put("use_multiscale", false);
        AnalysisResult full = executeAnalyze("/v1/analyze/stream", body);
        return toFingerprint(full);
    }

    public CompletableFuture<FingerprintResult> fingerprintAsync(AnalyzeRequest req) {
        return CompletableFuture.supplyAsync(() -> fingerprint(req));
    }

    private void warnIfTooShortForFingerprint(
            java.util.List<Double> signal, java.util.List<Double> baseline) {
        if (signal == null) return;
        int n = signal.size();
        int threshold = baseline != null && !baseline.isEmpty()
                ? AlphaInfoConstants.MIN_FINGERPRINT_SAMPLES_WITH_BASELINE
                : AlphaInfoConstants.MIN_FINGERPRINT_SAMPLES;
        if (n >= threshold) return;
        String qualifier = (baseline != null && !baseline.isEmpty()) ? "with baseline" : "without baseline";
        LOGGER.log(
                Level.WARNING,
                "Signal has {0} samples; the 5D fingerprint needs >={1} {2}. "
                        + "Response will likely come back with fingerprint_available=false "
                        + "(reason=signal_too_short). Use analyze() for shorter signals.",
                new Object[] {n, threshold, qualifier});
    }

    @SuppressWarnings("unchecked")
    private FingerprintResult toFingerprint(AnalysisResult r) {
        Map<String, Object> m = r.metrics == null ? Collections.emptyMap() : r.metrics;
        Double simLocal = asDouble(m.get("sim_local"));
        Double simSpectral = asDouble(m.get("sim_spectral"));
        Double simFractal = asDouble(m.get("sim_fractal"));
        Double simTransition = asDouble(m.get("sim_transition"));
        Double simTrend = asDouble(m.get("sim_trend"));
        boolean available;
        String reason;
        if (m.containsKey("fingerprint_available")) {
            Object v = m.get("fingerprint_available");
            available = v instanceof Boolean && (Boolean) v;
            Object rv = m.get("fingerprint_reason");
            reason = rv instanceof String ? (String) rv : null;
        } else {
            available = simLocal != null && simSpectral != null && simFractal != null
                    && simTransition != null && simTrend != null;
            reason = available ? null : "internal_error";
        }
        return new FingerprintResult(
                r.analysisId, r.structuralScore, r.confidenceBand,
                simLocal, simSpectral, simFractal, simTransition, simTrend,
                available, reason);
    }

    private static Double asDouble(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).doubleValue();
        return null;
    }

    // -----------------------------------------------------------------------
    // batch / matrix / vector
    // -----------------------------------------------------------------------

    public BatchResult analyzeBatch(BatchRequest req) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("signals", req.signals);
        body.put("sampling_rate", req.samplingRate);
        body.put("domain", req.domain == null ? "generic" : req.domain);
        if (req.baselines != null) body.put("baselines", req.baselines);
        if (req.includeSemantic != null) body.put("include_semantic", req.includeSemantic);
        if (req.useMultiscale != null) body.put("use_multiscale", req.useMultiscale);
        return deserialize(post("/v1/analyze/batch", body), BatchResult.class);
    }

    public MatrixResult analyzeMatrix(MatrixRequest req) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("signals", req.signals);
        body.put("sampling_rate", req.samplingRate);
        body.put("domain", req.domain == null ? "generic" : req.domain);
        if (req.useMultiscale != null) body.put("use_multiscale", req.useMultiscale);
        return deserialize(post("/v1/analyze/matrix", body), MatrixResult.class);
    }

    public VectorResult analyzeVector(VectorRequest req) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("channels", req.channels);
        body.put("sampling_rate", req.samplingRate);
        body.put("domain", req.domain == null ? "generic" : req.domain);
        if (req.baselines != null) body.put("baselines", req.baselines);
        if (req.includeSemantic != null) body.put("include_semantic", req.includeSemantic);
        if (req.useMultiscale != null) body.put("use_multiscale", req.useMultiscale);
        return deserialize(post("/v1/analyze/vector", body), VectorResult.class);
    }

    // -----------------------------------------------------------------------
    // audit / meta
    // -----------------------------------------------------------------------

    public Map<String, Object> auditReplay(String analysisId) {
        if (analysisId == null || analysisId.isEmpty()) {
            throw new ValidationException("analysisId cannot be empty");
        }
        String path = "/v1/audit/replay/" + URLEncoder.encode(analysisId, StandardCharsets.UTF_8);
        return mapResponse(get(path));
    }

    public Object auditList(int limit) {
        String path = "/v1/audit/list?limit=" + URLEncoder.encode(String.valueOf(limit), StandardCharsets.UTF_8);
        return mapResponse(get(path));
    }

    public Object plans() {
        return mapResponse(get("/api/plans"));
    }

    public Object version() {
        return mapResponse(get("/v1/version"));
    }

    // -----------------------------------------------------------------------
    // HTTP plumbing
    // -----------------------------------------------------------------------

    private AnalysisResult executeAnalyze(String path, Map<String, Object> body) {
        return deserialize(post(path, body), AnalysisResult.class);
    }

    private Map<String, Object> toJsonBody(AnalyzeRequest req) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("signal", req.signal);
        body.put("sampling_rate", req.samplingRate);
        body.put("domain", req.domain == null ? "generic" : req.domain);
        if (req.baseline != null) body.put("baseline", req.baseline);
        if (req.metadata != null) body.put("metadata", req.metadata);
        if (req.includeSemantic != null) body.put("include_semantic", req.includeSemantic);
        if (req.useMultiscale != null) body.put("use_multiscale", req.useMultiscale);
        return body;
    }

    private String post(String path, Object body) {
        try {
            RequestBody rb = RequestBody.create(mapper.writeValueAsBytes(body), JSON);
            Request req = new Request.Builder()
                    .url(baseUrl + path)
                    .header("X-API-Key", apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("User-Agent", "alphainfo-java/" + AlphaInfoConstants.SDK_VERSION)
                    .post(rb)
                    .build();
            return executeCall(req, path);
        } catch (IOException e) {
            throw new NetworkException("Failed to serialize request: " + e.getMessage(), e);
        }
    }

    private String get(String path) {
        Request req = new Request.Builder()
                .url(baseUrl + path)
                .header("X-API-Key", apiKey)
                .header("Accept", "application/json")
                .header("User-Agent", "alphainfo-java/" + AlphaInfoConstants.SDK_VERSION)
                .get()
                .build();
        return executeCall(req, path);
    }

    private String executeCall(Request req, String path) {
        try (Response resp = httpClient.newCall(req).execute()) {
            captureRateLimit(resp);
            String body = resp.body() == null ? "" : resp.body().string();
            if (!resp.isSuccessful()) {
                throw mapErrorResponse(resp.code(), resp.header("Retry-After"), body);
            }
            return body;
        } catch (IOException e) {
            throw new NetworkException("Network error on " + path + ": " + e.getMessage(), e);
        }
    }

    private void captureRateLimit(Response resp) {
        String limit = resp.header("X-RateLimit-Limit");
        if (limit == null || limit.isEmpty()) return;
        try {
            int l = Integer.parseInt(limit);
            int r = Integer.parseInt(
                    resp.header("X-RateLimit-Remaining") == null ? "0" : resp.header("X-RateLimit-Remaining"));
            long rs = Long.parseLong(
                    resp.header("X-RateLimit-Reset") == null ? "0" : resp.header("X-RateLimit-Reset"));
            this.rateLimit = new RateLimitInfo(l, r, rs);
        } catch (NumberFormatException ignored) {
            // keep previous value
        }
    }

    private <T> T deserialize(String body, Class<T> type) {
        try {
            return mapper.readValue(body, type);
        } catch (IOException e) {
            throw new ApiException("Failed to parse response: " + e.getMessage(), 0, null);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapResponse(String body) {
        try {
            return mapper.readValue(body, Map.class);
        } catch (IOException e) {
            throw new ApiException("Failed to parse response: " + e.getMessage(), 0, null);
        }
    }

    @SuppressWarnings("unchecked")
    private AlphaInfoException mapErrorResponse(int status, String retryAfterHeader, String body) {
        Map<String, Object> parsed;
        try {
            parsed = body.isEmpty() ? new HashMap<>() : mapper.readValue(body, Map.class);
        } catch (IOException e) {
            parsed = new HashMap<>();
        }
        Object detail = parsed.get("detail");
        String msg = null;
        if (detail instanceof String) {
            msg = (String) detail;
        } else if (detail instanceof Map) {
            Object m = ((Map<String, Object>) detail).get("message");
            if (m instanceof String) msg = (String) m;
        }

        switch (status) {
            case 401:
                return new AuthException(
                        msg == null
                                ? "Invalid or missing API key. Get a free key at https://alphainfo.io/register."
                                : msg,
                        status, parsed);
            case 400:
            case 413:
            case 422:
                return new ValidationException(
                        msg == null ? ("Validation failed (HTTP " + status + ")") : msg, status, parsed);
            case 404:
                return new NotFoundException(msg == null ? "Not found" : msg, status, parsed);
            case 429:
                int retryAfter = 0;
                if (retryAfterHeader != null) {
                    try {
                        retryAfter = Integer.parseInt(retryAfterHeader);
                    } catch (NumberFormatException ignored) {
                    }
                }
                return new RateLimitException(
                        msg == null ? "Rate limit exceeded" : msg, retryAfter, status, parsed);
            default:
                if (status >= 500) {
                    return new ApiException(
                            msg == null ? ("Server error (HTTP " + status + ")") : msg, status, parsed);
                }
                return new ApiException("HTTP " + status + ": " + (msg == null ? "" : msg), status, parsed);
        }
    }
}
