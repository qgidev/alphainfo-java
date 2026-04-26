package io.alphainfo;

/**
 * Public constants for the alphainfo client.
 *
 * <p>Values are kept in sync with the server's {@code
 * signal_requirements.fingerprint_minimum_samples} field in {@code /v1/guide}.
 */
public final class AlphaInfoConstants {

  private AlphaInfoConstants() {
    // Not instantiable.
  }

  /**
   * Minimum signal length required for a full 5-dimensional fingerprint when no baseline is
   * provided. Below this the server returns {@code fingerprint_available = false} with {@code
   * fingerprint_reason = "signal_too_short"}.
   */
  public static final int MIN_FINGERPRINT_SAMPLES = 192;

  /**
   * Minimum signal length with a comparable baseline. The baseline supplies the reference window so
   * the engine can decompose shorter signals.
   */
  public static final int MIN_FINGERPRINT_SAMPLES_WITH_BASELINE = 50;

  /** SDK version string for the {@code User-Agent} header. */
  public static final String SDK_VERSION = "1.5.27";
}
