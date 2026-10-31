package com.dragomitch.ipl.pae.business.exceptions;

import java.util.Arrays;
import java.util.Objects;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.Nullable;
import org.springframework.web.ErrorResponseException;

/**
 * A business rule was broken: the use case refuses the operation with one of the
 * {@linkplain ErrorCode error codes}.
 *
 * <p>It is a Spring {@link org.springframework.web.ErrorResponse}: it carries its HTTP status and
 * an RFC 9457 {@link ProblemDetail} whose {@code type} and {@code code} come from the error code.
 * The localized {@code title} and {@code detail} are resolved from the {@code MessageSource} by
 * {@link #updateAndGetBody} (called by {@code ResponseEntityExceptionHandler}) with the message
 * codes {@code problem.<CODE>.title} and {@code problem.<CODE>.detail}; the detail may use the
 * {@linkplain #getDetailMessageArguments() arguments} given to the constructor ({@code {0}},
 * {@code {1}}...).
 *
 * <p>Validation constraints are not business exceptions: they are Bean Validation constraints on
 * the DTOs and on the use-case methods, reported as {@link ErrorCode#VALIDATION_FAILED}.
 */
public class BusinessException extends ErrorResponseException {

  private static final long serialVersionUID = 1L;

  /** Name of the problem property holding the error code. */
  public static final String CODE_PROPERTY = "code";

  private final ErrorCode errorCode;

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
  public BusinessException(ErrorCode errorCode, @Nullable Throwable cause,
      Object... detailArguments) {
    super(Objects.requireNonNull(errorCode, "errorCode").status(), problemOf(errorCode), cause,
        errorCode.detailMessageCode(), detailArguments);
    this.errorCode = errorCode;
  }

  private static ProblemDetail problemOf(ErrorCode errorCode) {
    ProblemDetail problem = ProblemDetail.forStatus(errorCode.status());
    problem.setType(errorCode.type());
    problem.setProperty(CODE_PROPERTY, errorCode.name());
    return problem;
  }

  /** The error of this exception. */
  public ErrorCode getErrorCode() {
    return errorCode;
  }

  @Override
  public String getTitleMessageCode() {
    return errorCode.titleMessageCode();
  }

  @Override
  public String getDetailMessageCode() {
    return errorCode.detailMessageCode();
  }

  /** For the logs only: the code and its arguments, never shown to the client. */
  @Override
  public String getMessage() {
    Object[] arguments = getDetailMessageArguments();
    return errorCode.name()
        + (arguments == null || arguments.length == 0 ? "" : " " + Arrays.toString(arguments));
  }
}
