package com.dragomitch.ipl.pae.domain.exceptions;

import java.util.Arrays;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A business rule was broken: the use case refuses the operation with one of the {@linkplain
 * ErrorCode error codes}.
 *
 * <p>The web adapter ({@code ApiExceptionHandler}) answers it with the HTTP status of its code and
 * an RFC 9457 problem whose {@code type} and {@code code} come from the error code. The localized
 * {@code title} and {@code detail} are resolved from the {@code MessageSource} with the message
 * codes {@code problem.<CODE>.title} and {@code problem.<CODE>.detail}; the detail may use the
 * {@linkplain #getDetailMessageArguments() arguments} given to the constructor ({@code {0}}, {@code
 * {1}}...).
 *
 * <p>Validation constraints are not business exceptions: they are Bean Validation constraints on
 * the web DTOs and on the request parameters, reported as {@link ErrorCode#VALIDATION_FAILED}.
 */
public class BusinessException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final ErrorCode errorCode;

  private final Object[] detailArguments;

  /**
   * Creates the exception of an error code.
   *
   * @param errorCode the error
   * @param detailArguments the arguments of the localized detail message, if it has some
   */
  public BusinessException(ErrorCode errorCode, Object... detailArguments) {
    this(errorCode, null, detailArguments);
  }

  /**
   * Creates the exception of an error code caused by another exception.
   *
   * @param errorCode the error
   * @param cause the cause, may be null
   * @param detailArguments the arguments of the localized detail message, if it has some
   */
  public BusinessException(
      ErrorCode errorCode, @Nullable Throwable cause, Object... detailArguments) {
    super(null, cause);
    this.errorCode = Objects.requireNonNull(errorCode, "errorCode");
    this.detailArguments = detailArguments.clone();
  }

  /** The error of this exception. */
  public ErrorCode getErrorCode() {
    return errorCode;
  }

  /** The arguments of the localized detail message, possibly none. */
  public Object[] getDetailMessageArguments() {
    return detailArguments.clone();
  }

  /** For the logs only: the code and its arguments, never shown to the client. */
  @Override
  public String getMessage() {
    return errorCode.name()
        + (detailArguments.length == 0 ? "" : " " + Arrays.toString(detailArguments));
  }
}
