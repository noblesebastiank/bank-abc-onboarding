/**
 * Classification: Trimble Confidential.
 */

package com.bankabc.onboarding.exception;

import java.io.Serial;
import java.time.OffsetDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Default API exception.
 *
 * <p>
 * This class extends RuntimeException and is designed to be the base class
 * for all custom exceptions in the system.
 * </p>
 *
 * <p>
 * It provides structured error responses with:
 * - HTTP Status
 * - Error name
 * - User-friendly error message
 * - Timestamp
 * - Additional parameters (e.g., invalid fields)
 * </p>
 */
@Getter
@JsonPropertyOrder(value = {"status", "errorName", "message", "timestamp", "additionalDetails"})
@JsonIgnoreProperties(value = {"cause", "stackTrace", "suppressed", "localizedMessage"})
public class DefaultApiError extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * HTTP status code associated with this error.
   */
  @JsonProperty("status")
  private final HttpStatus httpStatus;

  /**
   * A short, human-readable error name (similar to `type` in RFC 7807).
   */
  @JsonProperty("errorName")
  private final String errorName;

  /**
   * A human-readable explanation of the error.
   */
  @JsonProperty("message")
  private final String message;

  /**
   * Timestamp when the error occurred.
   */
  @JsonProperty("timestamp")
  private final OffsetDateTime timestamp;

  /**
   * Additional details related to the error, such as invalid fields.
   */
  @JsonProperty("additionalDetails")
  private final Map<String, String> additionalDetails;

  /**
   * Additional message related to the error.
   */
  @JsonIgnore
  private final String additionalMessage;

  /**
   * Constructor for DefaultApiException with essential details.
   *
   * @param httpStatus HTTP status code.
   * @param errorName  Short error name (e.g., "BAD_REQUEST").
   * @param message    Human-readable message.
   */
  public DefaultApiError(final HttpStatus httpStatus, final String errorName, final String message) {
    this(httpStatus, errorName, message, null, null);
  }

  /**
   * Constructor for DefaultApiException with essential details.
   *
   * @param httpStatus        HTTP status code.
   * @param errorName         Short error name (e.g., "BAD_REQUEST").
   * @param message           Human-readable message.
   * @param additionalMessage Additional message related to the error.
   */
  public DefaultApiError(final HttpStatus httpStatus, final String errorName, final String message,
                         final String additionalMessage) {
    this(httpStatus, errorName, message, additionalMessage, null);
  }

  /**
   * Constructor for DefaultApiException with additional details.
   *
   * @param httpStatus        HTTP status code.
   * @param errorName         Short error name.
   * @param message           Error message.
   * @param additionalDetails Extra details like invalid fields.
   */
  public DefaultApiError(final HttpStatus httpStatus, final String errorName, final String message,
                         final Map<String, String> additionalDetails) {
    this(httpStatus, errorName, message, null, additionalDetails);
  }

  /**
   * Constructor for DefaultApiException with additional details.
   *
   * @param httpStatus        HTTP status code.
   * @param errorName         Short error name.
   * @param message           Error message.
   * @param additionalMessage Additional message related to the error.
   * @param additionalDetails Extra details like invalid fields.
   */
  public DefaultApiError(final HttpStatus httpStatus, final String errorName, final String message,
                         final String additionalMessage, final Map<String, String> additionalDetails) {
    super(message);
    this.httpStatus = httpStatus;
    this.errorName = errorName;
    this.message = message;
    this.additionalMessage = additionalMessage;
    this.timestamp = OffsetDateTime.now();
    this.additionalDetails = additionalDetails;
  }

  /**
   * Gets the error message.
   *
   * @return Error message.
   */
  @Override
  public String getMessage() {
    return message;
  }
}

