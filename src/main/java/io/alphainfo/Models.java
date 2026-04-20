package io.alphainfo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Typed response models (public contract). Jackson-annotated POJOs so the
 * SDK compiles cleanly on Java 11 without requiring records.
 */
public final class Models {
    private Models() {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class SemanticResult {
        public final String summary;
        public final String alertLevel;
        public final String recommendedAction;
        public final String trend;
        public final String severity;
        public final Double severityScore;

        @JsonCreator
        public SemanticResult(
                @JsonProperty("summary") String summary,
                @JsonProperty("alert_level") String alertLevel,
                @JsonProperty("recommended_action") String recommendedAction,
                @JsonProperty("trend") String trend,
                @JsonProperty("severity") String severity,
                @JsonProperty("severity_score") Double severityScore) {
            this.summary = summary == null ? "" : summary;
            this.alertLevel = alertLevel == null ? "normal" : alertLevel;
            this.recommendedAction = recommendedAction;
            this.trend = trend;
            this.severity = severity;
            this.severityScore = severityScore;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class AnalysisResult {
        public final double structuralScore;
        public final boolean changeDetected;
        public final double changeScore;
        public final String confidenceBand;
        public final String engineVersion;
        public final String analysisId;
        public final Map<String, Object> metrics;
        public final Map<String, Object> provenance;
        public final SemanticResult semantic;
        public final String warning;
        /** Always populated by server 1.5.12+. The calibration actually applied. */
        public final String domainApplied;
        /** Populated only when the caller passed {@code domain="auto"}. */
        public final DomainInference domainInference;

        @JsonCreator
        public AnalysisResult(
                @JsonProperty("structural_score") double structuralScore,
                @JsonProperty("change_detected") boolean changeDetected,
                @JsonProperty("change_score") double changeScore,
                @JsonProperty("confidence_band") String confidenceBand,
                @JsonProperty("engine_version") String engineVersion,
                @JsonProperty("analysis_id") String analysisId,
                @JsonProperty("metrics") Map<String, Object> metrics,
                @JsonProperty("provenance") Map<String, Object> provenance,
                @JsonProperty("semantic") SemanticResult semantic,
                @JsonProperty("warning") String warning,
                @JsonProperty("domain_applied") String domainApplied,
                @JsonProperty("domain_inference") DomainInference domainInference) {
            this.structuralScore = structuralScore;
            this.changeDetected = changeDetected;
            this.changeScore = changeScore;
            this.confidenceBand = confidenceBand;
            this.engineVersion = engineVersion;
            this.analysisId = analysisId;
            this.metrics = metrics == null ? Collections.emptyMap() : metrics;
            this.provenance = provenance;
            this.semantic = semantic;
            this.warning = warning;
            this.domainApplied = domainApplied;
            this.domainInference = domainInference;
        }
    }

    /**
     * Inference block returned when the caller passed {@code domain="auto"}.
     * Null on the {@link AnalysisResult} for any other domain value.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class DomainInference {
        public final String inferred;
        public final double confidence;
        public final boolean fallbackUsed;
        public final String reasoning;

        @JsonCreator
        public DomainInference(
                @JsonProperty("inferred") String inferred,
                @JsonProperty("confidence") double confidence,
                @JsonProperty("fallback_used") boolean fallbackUsed,
                @JsonProperty("reasoning") String reasoning) {
            this.inferred = inferred;
            this.confidence = confidence;
            this.fallbackUsed = fallbackUsed;
            this.reasoning = reasoning;
        }
    }

    /**
     * 5-dimensional structural fingerprint of a signal.
     *
     * <p>Every {@code sim*} field is {@code null} (not 0.0) when the engine
     * could not compute that dimension. Always check
     * {@link #isComplete()} before calling {@link #getVector()}.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class FingerprintResult {
        public final String analysisId;
        public final double structuralScore;
        public final String confidenceBand;
        public final Double simLocal;
        public final Double simSpectral;
        public final Double simFractal;
        public final Double simTransition;
        public final Double simTrend;
        public final boolean fingerprintAvailable;
        public final String fingerprintReason;

        public FingerprintResult(
                String analysisId,
                double structuralScore,
                String confidenceBand,
                Double simLocal,
                Double simSpectral,
                Double simFractal,
                Double simTransition,
                Double simTrend,
                boolean fingerprintAvailable,
                String fingerprintReason) {
            this.analysisId = analysisId;
            this.structuralScore = structuralScore;
            this.confidenceBand = confidenceBand;
            this.simLocal = simLocal;
            this.simSpectral = simSpectral;
            this.simFractal = simFractal;
            this.simTransition = simTransition;
            this.simTrend = simTrend;
            this.fingerprintAvailable = fingerprintAvailable;
            this.fingerprintReason = fingerprintReason;
        }

        /** @return true when all five similarity dimensions are populated. */
        public boolean isComplete() {
            return fingerprintAvailable;
        }

        /**
         * The 5D fingerprint as a list ready for ANN indexing (pgvector,
         * Qdrant, Faiss). Returns {@code null} when
         * {@link #fingerprintAvailable} is {@code false} — callers must
         * skip on null instead of substituting zeros.
         */
        public List<Double> getVector() {
            if (!fingerprintAvailable) {
                return null;
            }
            if (simLocal == null || simSpectral == null || simFractal == null
                    || simTransition == null || simTrend == null) {
                return null;
            }
            return List.of(simLocal, simSpectral, simFractal, simTransition, simTrend);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class BatchItemResult {
        public final int index;
        public final Double structuralScore;
        public final Boolean changeDetected;
        public final Double changeScore;
        public final String confidenceBand;
        public final String engineVersion;
        public final String analysisId;
        public final Map<String, Object> metrics;
        public final SemanticResult semantic;
        public final String error;

        @JsonCreator
        public BatchItemResult(
                @JsonProperty("index") int index,
                @JsonProperty("structural_score") Double structuralScore,
                @JsonProperty("change_detected") Boolean changeDetected,
                @JsonProperty("change_score") Double changeScore,
                @JsonProperty("confidence_band") String confidenceBand,
                @JsonProperty("engine_version") String engineVersion,
                @JsonProperty("analysis_id") String analysisId,
                @JsonProperty("metrics") Map<String, Object> metrics,
                @JsonProperty("semantic") SemanticResult semantic,
                @JsonProperty("error") String error) {
            this.index = index;
            this.structuralScore = structuralScore;
            this.changeDetected = changeDetected;
            this.changeScore = changeScore;
            this.confidenceBand = confidenceBand;
            this.engineVersion = engineVersion;
            this.analysisId = analysisId;
            this.metrics = metrics;
            this.semantic = semantic;
            this.error = error;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class BatchResult {
        public final List<BatchItemResult> results;
        public final int analysesConsumed;
        public final int totalSignals;

        @JsonCreator
        public BatchResult(
                @JsonProperty("results") List<BatchItemResult> results,
                @JsonProperty("analyses_consumed") int analysesConsumed,
                @JsonProperty("total_signals") int totalSignals) {
            this.results = results == null ? Collections.emptyList() : results;
            this.analysesConsumed = analysesConsumed;
            this.totalSignals = totalSignals;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class MatrixResult {
        public final List<List<Double>> matrix;
        public final List<String> labels;
        public final int nSignals;
        public final int nPairs;
        public final int analysesConsumed;

        @JsonCreator
        public MatrixResult(
                @JsonProperty("matrix") List<List<Double>> matrix,
                @JsonProperty("labels") List<String> labels,
                @JsonProperty("n_signals") int nSignals,
                @JsonProperty("n_pairs") int nPairs,
                @JsonProperty("analyses_consumed") int analysesConsumed) {
            this.matrix = matrix == null ? Collections.emptyList() : matrix;
            this.labels = labels == null ? Collections.emptyList() : labels;
            this.nSignals = nSignals;
            this.nPairs = nPairs;
            this.analysesConsumed = analysesConsumed;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class ChannelResult {
        public final Double structuralScore;
        public final Boolean changeDetected;
        public final Double changeScore;
        public final String confidenceBand;
        public final String engineVersion;
        public final String error;

        @JsonCreator
        public ChannelResult(
                @JsonProperty("structural_score") Double structuralScore,
                @JsonProperty("change_detected") Boolean changeDetected,
                @JsonProperty("change_score") Double changeScore,
                @JsonProperty("confidence_band") String confidenceBand,
                @JsonProperty("engine_version") String engineVersion,
                @JsonProperty("error") String error) {
            this.structuralScore = structuralScore;
            this.changeDetected = changeDetected;
            this.changeScore = changeScore;
            this.confidenceBand = confidenceBand;
            this.engineVersion = engineVersion;
            this.error = error;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class VectorResult {
        public final double structuralScore;
        public final double changeScore;
        public final boolean changeDetected;
        public final String confidenceBand;
        public final String analysisId;
        public final String engineVersion;
        public final Map<String, ChannelResult> channels;
        public final String warning;

        @JsonCreator
        public VectorResult(
                @JsonProperty("structural_score") double structuralScore,
                @JsonProperty("change_score") double changeScore,
                @JsonProperty("change_detected") boolean changeDetected,
                @JsonProperty("confidence_band") String confidenceBand,
                @JsonProperty("analysis_id") String analysisId,
                @JsonProperty("engine_version") String engineVersion,
                @JsonProperty("channels") Map<String, ChannelResult> channels,
                @JsonProperty("warning") String warning) {
            this.structuralScore = structuralScore;
            this.changeScore = changeScore;
            this.changeDetected = changeDetected;
            this.confidenceBand = confidenceBand;
            this.analysisId = analysisId;
            this.engineVersion = engineVersion;
            this.channels = channels == null ? Collections.emptyMap() : channels;
            this.warning = warning;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class HealthStatus {
        public final String status;
        public final String version;
        public final String message;
        public final Double uptimeSeconds;
        public final Map<String, String> services;

        @JsonCreator
        public HealthStatus(
                @JsonProperty("status") String status,
                @JsonProperty("version") String version,
                @JsonProperty("message") String message,
                @JsonProperty("uptime_seconds") Double uptimeSeconds,
                @JsonProperty("services") Map<String, String> services) {
            this.status = status == null ? "" : status;
            this.version = version == null ? "" : version;
            this.message = message == null ? "" : message;
            this.uptimeSeconds = uptimeSeconds;
            this.services = services;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class RateLimitInfo {
        public final int limit;
        public final int remaining;
        public final long reset;

        public RateLimitInfo(int limit, int remaining, long reset) {
            this.limit = limit;
            this.remaining = remaining;
            this.reset = reset;
        }
    }

    /** Input builder for {@link AlphaInfoClient#analyze}. */
    public static final class AnalyzeRequest {
        public List<Double> signal;
        public double samplingRate;
        public String domain = "generic";
        public List<Double> baseline;
        public Map<String, Object> metadata;
        public Boolean includeSemantic;
        public Boolean useMultiscale;

        public AnalyzeRequest signal(List<Double> s) { this.signal = s; return this; }
        public AnalyzeRequest samplingRate(double s) { this.samplingRate = s; return this; }
        public AnalyzeRequest domain(String d) { this.domain = d; return this; }
        public AnalyzeRequest baseline(List<Double> b) { this.baseline = b; return this; }
        public AnalyzeRequest metadata(Map<String, Object> m) { this.metadata = m; return this; }
        public AnalyzeRequest includeSemantic(boolean v) { this.includeSemantic = v; return this; }
        public AnalyzeRequest useMultiscale(boolean v) { this.useMultiscale = v; return this; }
    }

    /** Input builder for {@link AlphaInfoClient#analyzeBatch}. */
    public static final class BatchRequest {
        public List<List<Double>> signals;
        public double samplingRate;
        public String domain = "generic";
        public List<List<Double>> baselines;
        public Boolean includeSemantic;
        public Boolean useMultiscale;

        public BatchRequest signals(List<List<Double>> s) { this.signals = s; return this; }
        public BatchRequest samplingRate(double s) { this.samplingRate = s; return this; }
        public BatchRequest domain(String d) { this.domain = d; return this; }
        public BatchRequest baselines(List<List<Double>> b) { this.baselines = b; return this; }
        public BatchRequest includeSemantic(boolean v) { this.includeSemantic = v; return this; }
        public BatchRequest useMultiscale(boolean v) { this.useMultiscale = v; return this; }
    }

    /** Input builder for {@link AlphaInfoClient#analyzeMatrix}. */
    public static final class MatrixRequest {
        public List<List<Double>> signals;
        public double samplingRate;
        public String domain = "generic";
        public Boolean useMultiscale;

        public MatrixRequest signals(List<List<Double>> s) { this.signals = s; return this; }
        public MatrixRequest samplingRate(double s) { this.samplingRate = s; return this; }
        public MatrixRequest domain(String d) { this.domain = d; return this; }
        public MatrixRequest useMultiscale(boolean v) { this.useMultiscale = v; return this; }
    }

    /** Input builder for {@link AlphaInfoClient#analyzeVector}. */
    public static final class VectorRequest {
        public Map<String, List<Double>> channels;
        public double samplingRate;
        public String domain = "generic";
        public Map<String, List<Double>> baselines;
        public Boolean includeSemantic;
        public Boolean useMultiscale;

        public VectorRequest channels(Map<String, List<Double>> c) { this.channels = c; return this; }
        public VectorRequest samplingRate(double s) { this.samplingRate = s; return this; }
        public VectorRequest domain(String d) { this.domain = d; return this; }
        public VectorRequest baselines(Map<String, List<Double>> b) { this.baselines = b; return this; }
        public VectorRequest includeSemantic(boolean v) { this.includeSemantic = v; return this; }
        public VectorRequest useMultiscale(boolean v) { this.useMultiscale = v; return this; }
    }
}
