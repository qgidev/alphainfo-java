package io.alphainfo;

import java.util.Collections;
import java.util.Map;

/**
 * Base exception for every alphainfo SDK error.
 *
 * <p>Catch this at the boundary; use subclasses or {@code instanceof} to react differently to
 * {@link AuthException}, {@link RateLimitException}, {@link ValidationException}, {@link
 * NotFoundException}, {@link ApiException}, {@link NetworkException}.
 */
public class AlphaInfoException extends RuntimeException {

  private final int statusCode;
  private final Map<String, Object> responseData;

  public AlphaInfoException(String message) {
    this(message, 0, null, null);
  }

  public AlphaInfoException(String message, Throwable cause) {
    this(message, 0, null, cause);
  }

  public AlphaInfoException(String message, int statusCode, Map<String, Object> responseData) {
    this(message, statusCode, responseData, null);
  }

  public AlphaInfoException(
      String message, int statusCode, Map<String, Object> responseData, Throwable cause) {
    super(message, cause);
    this.statusCode = statusCode;
    this.responseData =
        responseData == null ? Collections.emptyMap() : Collections.unmodifiableMap(responseData);
  }

  /**
   * @return the HTTP status code, or 0 for transport-level errors.
   */
  public int getStatusCode() {
    return statusCode;
  }

  /**
   * @return the parsed JSON response body (empty map when not available).
   */
  public Map<String, Object> getResponseData() {
    return responseData;
  }
}
