package com.dragomitch.ipl.pae.presentation;

import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.ErrorFormat;
import com.dragomitch.ipl.pae.business.exceptions.ErrorManager;
import com.dragomitch.ipl.pae.business.exceptions.RessourceNotFoundException;
import com.dragomitch.ipl.pae.exceptions.FatalException;
import com.dragomitch.ipl.pae.logging.LogManager;
import com.dragomitch.ipl.pae.presentation.exceptions.InsufficientPermissionException;
import com.dragomitch.ipl.pae.presentation.exceptions.RouteNotFoundException;
import com.dragomitch.ipl.pae.presentation.exceptions.UnauthenticatedUserException;

import org.springframework.http.HttpStatus;

import java.util.ConcurrentModificationException;
import org.slf4j.Logger;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

/**
 * Exception handler used to add custom behaviour depending on the exception thrown.
 */
@Component
public class ExceptionHandler extends ResponseHandler {

  private static Logger logger = LogManager.getLogger(ExceptionHandler.class.getName());

  public ExceptionHandler(JsonSerializer jsonSerializer) {
    super(jsonSerializer);
  }

  /**
   * Called when an exception occurred during the process of an HTTP request, it handles the
   * exception and prepares the HTTP response consequently.
   * 
   * @param exception the exception that occurred
   * @param resp the HTTP response object that provides HTTP-specific functionality
   */
  public void handleException(Throwable exception, HttpServletResponse resp) {
    int httpStatusCode;
    ErrorFormat error = null;
    switch (exception) {
      case RouteNotFoundException e ->
          httpStatusCode = HttpStatus.NOT_FOUND.value();
      case RessourceNotFoundException e ->
          httpStatusCode = HttpStatus.NOT_FOUND.value();
      case UnauthenticatedUserException e ->
          httpStatusCode = HttpStatus.UNAUTHORIZED.value();
      case InsufficientPermissionException e ->
          httpStatusCode = HttpStatus.FORBIDDEN.value();
      case IllegalArgumentException e ->
          httpStatusCode = HttpStatus.BAD_REQUEST.value();
      case ConcurrentModificationException e -> {
          error = ErrorManager.getError(ErrorFormat.CONCURRENT_MODIFICATION_120);
          httpStatusCode = HttpStatus.BAD_REQUEST.value();
        }
      case BusinessException e -> {
          error = e.getError();
          httpStatusCode = HttpStatus.BAD_REQUEST.value();
        }
      case FatalException e -> {
          logger.error("FatalException", e);
          httpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
        }
      default -> {
          logger.error("Unexpected exception", exception);
          httpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
        }
    }
    logger.info("Handling {}: {} {}", exception.getClass().getSimpleName(), httpStatusCode,
        HttpStatus.valueOf(httpStatusCode).getReasonPhrase());
    if (exception.getMessage() != null) {
      logger.info("Message: {}", exception.getMessage());
    }
    logger.debug("Stack trace", exception);
    resp.setStatus(httpStatusCode);
    if (error != null) {
      writeResponse(error, "json/application", resp);
    }

  }
}
