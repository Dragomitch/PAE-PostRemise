package com.dragomitch.ipl.pae.presentation;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.ErrorFormat;
import com.dragomitch.ipl.pae.business.exceptions.RessourceNotFoundException;
import com.dragomitch.ipl.pae.exceptions.FatalException;
import com.dragomitch.ipl.pae.logging.LogManager;
import com.dragomitch.ipl.pae.presentation.exceptions.InsufficientPermissionException;
import com.dragomitch.ipl.pae.presentation.exceptions.RouteNotFoundException;
import com.dragomitch.ipl.pae.presentation.exceptions.UnauthenticatedUserException;

import org.eclipse.jetty.http.HttpStatus;

import java.util.ConcurrentModificationException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

/**
 * Exception handler used to add custom behaviour depending on the exception thrown.
 */
public class ExceptionHandler extends ResponseHandler {

  private static Logger logger = LogManager.getLogger(ExceptionHandler.class.getName());

  public ExceptionHandler(EntityFactory entityFactory) {
    super(entityFactory);
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
    if (exception instanceof RouteNotFoundException
        || exception instanceof RessourceNotFoundException) {
      httpStatusCode = HttpStatus.NOT_FOUND_404;
    } else if (exception instanceof UnauthenticatedUserException) {
      httpStatusCode = HttpStatus.UNAUTHORIZED_401;
    } else if (exception instanceof InsufficientPermissionException) {
      httpStatusCode = HttpStatus.FORBIDDEN_403;
    } else if (exception instanceof IllegalArgumentException) {
      httpStatusCode = HttpStatus.BAD_REQUEST_400;
    } else if (exception instanceof ConcurrentModificationException) {
      error = new ErrorFormat(ErrorFormat.CONCURRENT_MODIFICATION_120);
      httpStatusCode = HttpStatus.BAD_REQUEST_400;
    } else if (exception instanceof BusinessException) {
      error = ((BusinessException) exception).getError();
      httpStatusCode = HttpStatus.BAD_REQUEST_400;
    } else if (exception instanceof FatalException) {
      logger.log(Level.SEVERE, "FatalException", exception);
      httpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR_500;
    } else {
      httpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR_500;
    }
    logger.info("Handling " + exception.getClass().getSimpleName() + ": " + httpStatusCode + " "
        + HttpStatus.getMessage(httpStatusCode));
    if (exception.getMessage() != null) {
      logger.info("Message: " + exception.getMessage());
    }
    if (exception.getCause() != null) {
      logger.log(Level.INFO, "Cause: ", exception.getCause());
    }
    // TODO Supprimer cela avant la remise
    exception.printStackTrace();
    resp.setStatus(httpStatusCode);
    if (error != null) {
      writeResponse(error, "json/application", resp);
    }

  }
}
