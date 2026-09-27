package com.dragomitch.ipl.pae.web;

import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.ErrorFormat;
import com.dragomitch.ipl.pae.business.exceptions.ErrorManager;
import com.dragomitch.ipl.pae.business.exceptions.InsufficientPermissionException;
import com.dragomitch.ipl.pae.business.exceptions.RessourceNotFoundException;
import com.dragomitch.ipl.pae.business.exceptions.UnauthenticatedUserException;
import com.dragomitch.ipl.pae.exceptions.FatalException;

import java.util.ConcurrentModificationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns the exceptions of the API into HTTP responses carrying the error catalogue format
 * ({@link ErrorFormat}: {@code errorCode}, {@code developerMessage}, {@code userMessage},
 * {@code details}). It also renders the 401 and 403 raised by Spring Security (see
 * {@code ExceptionResolverSecurityHandler}), so every API error goes through this class.
 *
 * <p>Errors of the Spring MVC infrastructure (malformed JSON body, wrongly typed parameter,
 * unknown route, ...) keep Spring Boot's default handling.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

  private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorFormat> handleBusiness(BusinessException ex) {
    ErrorFormat error = ex.getError();
    if (error == null) {
      // error code missing from the catalogue
      return respond(HttpStatus.BAD_REQUEST, ErrorFormat.INVALID_INPUT_DATA_110, ex);
    }
    return respond(HttpStatus.BAD_REQUEST, error, ex);
  }

  @ExceptionHandler(ConcurrentModificationException.class)
  public ResponseEntity<ErrorFormat> handleConcurrentModification(
      ConcurrentModificationException ex) {
    return respond(HttpStatus.BAD_REQUEST, ErrorFormat.CONCURRENT_MODIFICATION_120, ex);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorFormat> handleIllegalArgument(IllegalArgumentException ex) {
    return respond(HttpStatus.BAD_REQUEST, ErrorFormat.INVALID_PARAMETERS_130, ex);
  }

  @ExceptionHandler(RessourceNotFoundException.class)
  public ResponseEntity<ErrorFormat> handleNotFound(RessourceNotFoundException ex) {
    return respond(HttpStatus.NOT_FOUND, ErrorFormat.RESOURCE_NOT_FOUND_104, ex);
  }

  @ExceptionHandler({AuthenticationException.class, UnauthenticatedUserException.class})
  public ResponseEntity<ErrorFormat> handleUnauthenticated(RuntimeException ex) {
    return respond(HttpStatus.UNAUTHORIZED, ErrorFormat.UNAUTHENTICATED_101, ex);
  }

  /**
   * Access denied by a role check ({@code @PreAuthorize}), by a use case (the resource belongs to
   * someone else) or by the CSRF protection.
   */
  @ExceptionHandler({AccessDeniedException.class, InsufficientPermissionException.class})
  public ResponseEntity<ErrorFormat> handleAccessDenied(RuntimeException ex) {
    if (trustResolver.isAnonymous(SecurityContextHolder.getContext().getAuthentication())) {
      return respond(HttpStatus.UNAUTHORIZED, ErrorFormat.UNAUTHENTICATED_101, ex);
    }
    return respond(HttpStatus.FORBIDDEN, ErrorFormat.ACCESS_DENIED_103, ex);
  }

  @ExceptionHandler(FatalException.class)
  public ResponseEntity<ErrorFormat> handleFatal(FatalException ex) {
    logger.error("FatalException", ex);
    return respond(HttpStatus.INTERNAL_SERVER_ERROR, ErrorFormat.INTERNAL_ERROR_100, ex);
  }

  private ResponseEntity<ErrorFormat> respond(HttpStatus status, int errorCode, Exception ex) {
    ErrorFormat error = ErrorManager.getError(errorCode);
    if (error == null) {
      error = new ErrorFormat(errorCode);
    }
    return respond(status, error, ex);
  }

  private ResponseEntity<ErrorFormat> respond(HttpStatus status, ErrorFormat error, Exception ex) {
    if (error.getDetails() == null) {
      // the legacy UI iterates over the details of a 400
      error.setDetails(List.of());
    }
    logger.info("Handling {}: {} {}{}", ex.getClass().getSimpleName(), status.value(),
        status.getReasonPhrase(), ex.getMessage() == null ? "" : " - " + ex.getMessage());
    return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(error);
  }
}
