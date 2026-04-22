package io.alphainfo;

import java.util.Map;

/** Specialised exception classes. Kept together so the SDK is easy to scan. */
final class Exceptions {
  private Exceptions() {}
}

/** Invalid or missing API key (HTTP 401). Not retryable. */
class AuthException extends AlphaInfoException {
  public AuthException() {
    super(
        "Invalid or missing API key. Get a free key at "
            + "https://alphainfo.io/register and pass it to new AlphaInfoClient(\"ai_...\").");
  }

  public AuthException(String message, int statusCode, Map<String, Object> responseData) {
    super(message, statusCode, responseData);
  }
}

/** Rate/quota limit exceeded (HTTP 429). Carries the server's {@code Retry-After}. */
class RateLimitException extends AlphaInfoException {
  private final int retryAfterSeconds;

  public RateLimitException(
      String message, int retryAfterSeconds, int statusCode, Map<String, Object> responseData) {
    super(message, statusCode, responseData);
    this.retryAfterSeconds = retryAfterSeconds;
  }

  /**
   * @return seconds to wait before retrying; 0 when the server did not hint.
   */
  public int getRetryAfterSeconds() {
    return retryAfterSeconds;
  }
}

/** Request validation failure (HTTP 400 / 413 / 422). Not retryable — fix the input. */
class ValidationException extends AlphaInfoException {
  public ValidationException(String message) {
    super(message);
  }

  public ValidationException(String message, int statusCode, Map<String, Object> responseData) {
    super(message, statusCode, responseData);
  }
}

/** Resource not found (HTTP 404). */
class NotFoundException extends AlphaInfoException {
  public NotFoundException(String message, int statusCode, Map<String, Object> responseData) {
    super(message, statusCode, responseData);
  }
}

/** Server-side error (HTTP 5xx). */
class ApiException extends AlphaInfoException {
  public ApiException(String message, int statusCode, Map<String, Object> responseData) {
    super(message, statusCode, responseData);
  }
}

/** Transport-level failure — DNS, TCP, TLS, timeout, I/O. */
class NetworkException extends AlphaInfoException {
  public NetworkException(String message, Throwable cause) {
    super(message, cause);
  }
}
