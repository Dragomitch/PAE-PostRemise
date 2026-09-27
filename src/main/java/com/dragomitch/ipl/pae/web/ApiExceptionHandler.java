package com.dragomitch.ipl.pae.web;

import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.Instant;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * The single place where API errors are turned into HTTP responses: every error is an RFC 9457
 * problem ({@code application/problem+json}, Spring's {@link ProblemDetail}):
 *
 * <pre>
 * {
 *   "type": "urn:pae:problem:username-taken",
 *   "title": "Conflit",
 *   "status": 409,
 *   "detail": "Le nom d’utilisateur « alice » est déjà utilisé.",
 *   "instance": "/api/1.0/users",
 *   "code": "USERNAME_TAKEN",
 *   "timestamp": "2026-09-27T10:15:30Z"
 * }
 * </pre>
 *
 * <ul>
 * <li>{@code code} (always present) is an {@link ErrorCode} name, the key clients branch on;</li>
 * <li>{@code title} and {@code detail} are localized from the request {@code Accept-Language}
 * (French by default, English) by the {@code MessageSource}, through Spring's
 * {@link ErrorResponse} message codes: {@code problem.<CODE>.title/detail} for the
 * {@linkplain BusinessException business errors}, {@code problemDetail.title.<exception class>}
 * and {@code problemDetail.<exception class>} for the errors of Spring MVC (405, 415, malformed
 * JSON...), handled natively by {@link ResponseEntityExceptionHandler};</li>
 * <li>{@code errors} lists the broken constraints of a {@code VALIDATION_FAILED} problem:
 * {@code [{"field": "email", "code": "Email", "message": "..."}]};</li>
 * <li>5xx problems never describe the failure: they carry an {@code errorId} also written in the
 * error log with the stack trace.</li>
 * </ul>
 *
 * <p>The 401 and 403 raised by the Spring Security filters are handed to this class by
 * {@code ExceptionResolverSecurityHandler}.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

  /** Problem property: the {@link ErrorCode} name. */
  public static final String CODE = BusinessException.CODE_PROPERTY;
  /** Problem property: the {@link FieldViolation}s of a validation failure. */
  public static final String ERRORS = "errors";
  /** Problem property: when the error occurred (ISO-8601). */
  public static final String TIMESTAMP = "timestamp";
  /** Problem property of a 5xx: the identifier of the error in the logs. */
  public static final String ERROR_ID = "errorId";

  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  private static final URI BLANK_TYPE = URI.create("about:blank");

  private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();

  // ----- validation -----

  /** {@code @Valid @RequestBody} (or a validated model attribute) breaking its constraints. */
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
      HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    return validationFailed(ex, FieldViolation.of(ex.getBindingResult()), headers, request);
  }

  /** Constraints on the parameters of a controller method (path variables, query parameters). */
  @Override
  protected ResponseEntity<Object> handleHandlerMethodValidationException(
      HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status,
      WebRequest request) {
    return validationFailed(ex, FieldViolation.of(ex), headers, request);
  }

  /** Constraints of a use case ({@code @Validated} service called with invalid arguments). */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex,
      WebRequest request) {
    return validationFailed(ex, FieldViolation.of(ex.getConstraintViolations()), new HttpHeaders(),
        request);
  }

  private ResponseEntity<Object> validationFailed(Exception ex, List<FieldViolation> errors,
      HttpHeaders headers, WebRequest request) {
    ProblemDetail body = problem(ex, ErrorCode.VALIDATION_FAILED);
    body.setProperty(ERRORS, errors);
    return handleExceptionInternal(ex, body, headers, ErrorCode.VALIDATION_FAILED.status(),
        request);
  }

  // ----- security -----

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<Object> handleAuthentication(AuthenticationException ex,
      WebRequest request) {
    return respond(ex, ErrorCode.UNAUTHENTICATED, request);
  }

  /**
   * Access denied by a role check ({@code @PreAuthorize}) or by the CSRF protection: 401 for an
   * anonymous user (he must sign in first), 403 otherwise.
   */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
    boolean anonymous =
        trustResolver.isAnonymous(SecurityContextHolder.getContext().getAuthentication());
    return respond(ex, anonymous ? ErrorCode.UNAUTHENTICATED : ErrorCode.ACCESS_DENIED, request);
  }

  // ----- persistence -----

  /**
   * Stale version: the JDBC DAOs and the use cases throw a {@link ConcurrentModificationException},
   * Spring Data an {@link OptimisticLockingFailureException}.
   */
  @ExceptionHandler({ConcurrentModificationException.class,
      OptimisticLockingFailureException.class})
  public ResponseEntity<Object> handleConcurrentModification(RuntimeException ex,
      WebRequest request) {
    return respond(ex, ErrorCode.CONCURRENT_MODIFICATION, request);
  }

  /** A database constraint refused the change (the SQL error is logged, never returned). */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Object> handleDataIntegrityViolation(DataIntegrityViolationException ex,
      WebRequest request) {
    log.warn("Data integrity violation on {}: {}", path(request),
        ex.getMostSpecificCause().toString());
    return respond(ex, ErrorCode.DATA_CONFLICT, request);
  }

  @ExceptionHandler(EmptyResultDataAccessException.class)
  public ResponseEntity<Object> handleEmptyResult(EmptyResultDataAccessException ex,
      WebRequest request) {
    return respond(ex, ErrorCode.RESOURCE_NOT_FOUND, request);
  }

  // ----- anything else -----

  /** Unexpected exception: 500 without any detail of the failure. */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
    return respond(ex, ErrorCode.INTERNAL_ERROR, request);
  }

  private ResponseEntity<Object> respond(Exception ex, ErrorCode code, WebRequest request) {
    return handleExceptionInternal(ex, problem(ex, code), new HttpHeaders(), code.status(),
        request);
  }

  /**
   * Builds the problem of an error code with Spring's {@link ErrorResponse} machinery, so that
   * its title and detail are resolved like those of the other problems.
   */
  private ProblemDetail problem(Exception ex, ErrorCode code, Object... detailArguments) {
    ProblemDetail problem = ErrorResponse.builder(ex, code.status(), code.name())
        .type(code.type())
        .titleMessageCode(code.titleMessageCode())
        .detailMessageCode(code.detailMessageCode())
        .detailMessageArguments(detailArguments)
        .build()
        .updateAndGetBody(getMessageSource(), LocaleContextHolder.getLocale());
    problem.setProperty(CODE, code.name());
    return problem;
  }

  /**
   * Every response goes through here (the Spring MVC exceptions handled by the superclass
   * included): the problem gets its {@code code}, {@code type}, {@code instance} and
   * {@code timestamp}, and the error is logged.
   */
  @Override
  protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body,
      HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
    ResponseEntity<Object> response =
        super.handleExceptionInternal(ex, body, headers, statusCode, request);
    if (response != null && response.getBody() instanceof ProblemDetail problem) {
      complete(problem, ex, statusCode, request);
    } else {
      logHandled(ex, statusCode, request, null);
    }
    return response;
  }

  private void complete(ProblemDetail problem, Exception ex, HttpStatusCode status,
      WebRequest request) {
    ErrorCode code = codeOf(problem, status);
    problem.setProperty(CODE, code.name());
    if (problem.getType() == null || BLANK_TYPE.equals(problem.getType())) {
      problem.setType(code.type());
    }
    if (problem.getInstance() == null) {
      String path = path(request);
      if (path != null) {
        problem.setInstance(URI.create(path));
      }
    }
    String errorId = null;
    if (status.is5xxServerError()) {
      // never describe a server failure to the client: the details stay in the log
      errorId = UUID.randomUUID().toString();
      problem.setProperty(ERROR_ID, errorId);
      Locale locale = LocaleContextHolder.getLocale();
      MessageSource messages = getMessageSource();
      ErrorCode generic =
          code == ErrorCode.SERVICE_UNAVAILABLE ? code : ErrorCode.INTERNAL_ERROR;
      if (messages != null) {
        problem.setTitle(messages.getMessage(generic.titleMessageCode(), null,
            HttpStatus.valueOf(status.value()).getReasonPhrase(), locale));
        problem.setDetail(messages.getMessage(generic.detailMessageCode(),
            new Object[] {errorId}, null, locale));
      } else {
        problem.setDetail(null);
      }
    }
    problem.setProperty(TIMESTAMP, Instant.now().toString());
    logHandled(ex, status, request, errorId);
  }

  private static ErrorCode codeOf(ProblemDetail problem, HttpStatusCode status) {
    Object code = problem.getProperties() == null ? null : problem.getProperties().get(CODE);
    if (code instanceof String name) {
      try {
        return ErrorCode.valueOf(name);
      } catch (IllegalArgumentException ex) {
        // not one of ours: fall back on the status
      }
    }
    return ErrorCode.forStatus(status.value());
  }

  private static void logHandled(Exception ex, HttpStatusCode status, WebRequest request,
      @Nullable String errorId) {
    if (errorId != null) {
      log.error("Error {} on {}: {}", errorId, path(request), status.value(), ex);
    } else if (log.isInfoEnabled()) {
      log.info("Handling {} on {}: {}{}", ex.getClass().getSimpleName(), path(request),
          status.value(), ex.getMessage() == null ? "" : " - " + ex.getMessage());
    }
  }

  @Nullable
  private static String path(WebRequest request) {
    if (request instanceof NativeWebRequest nativeRequest) {
      HttpServletRequest servletRequest = nativeRequest.getNativeRequest(HttpServletRequest.class);
      if (servletRequest != null) {
        return servletRequest.getRequestURI();
      }
    }
    return null;
  }
}
