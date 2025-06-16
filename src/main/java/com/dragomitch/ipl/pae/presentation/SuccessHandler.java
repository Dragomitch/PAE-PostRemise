package com.dragomitch.ipl.pae.presentation;

import com.dragomitch.ipl.pae.business.EntityFactory;
import org.slf4j.Logger;
import jakarta.servlet.http.HttpServletResponse;

import com.dragomitch.ipl.pae.logging.LogManager;
import org.springframework.http.HttpStatus;

/**
 * Success handler used when a request has been successfully processed.
 */
class SuccessHandler extends ResponseHandler {

  private static Logger logger = LogManager.getLogger(SuccessHandler.class.getName());

  public SuccessHandler(EntityFactory entityFactory) {
    super(entityFactory);
  }

  /**
   * Outputs the request response.
   *
   * @param ob the object to output
   * @param contentType the MIME Type of the response
   * @param resp the HTTP response object that provides HTTP-specific functionality
   */
  public void handleSuccess(Object ob, String contentType, HttpServletResponse resp) {
    logger.debug("Handling success");
    if (ob != null) {
      writeResponse(ob, contentType, resp);
    }
    resp.setStatus(HttpStatus.OK.value());
  }

}
