package com.dragomitch.ipl.pae.presentation;

import com.dragomitch.ipl.pae.business.EntityFactory;
import java.io.IOException;
import org.slf4j.Logger;
import jakarta.servlet.http.HttpServletResponse;
import com.dragomitch.ipl.pae.exceptions.FatalException;
import com.dragomitch.ipl.pae.logging.LogManager;

public class ResponseHandler {

  private static Logger logger = LogManager.getLogger(ResponseHandler.class.getName());

  protected JsonSerializer jsonSerializer;

  public ResponseHandler(EntityFactory entityFactory) {
    this.jsonSerializer = new JsonSerializer(entityFactory);
  }

  /**
   * Serializes the output object {@code ob} and writes it in the Http Response.
   *
   * @param ob the output object to write
   * @param resp the HTTP response object that provides HTTP-specific functionality
   */
  protected void writeResponse(Object ob, String contentType, HttpServletResponse resp) {
    logger.debug("Writing response");
    String format;
    if (contentType.equals("text/csv")) {
      resp.setContentType("text/csv");
      try {
        format = (String) ob;
      } catch (ClassCastException ex) {
        throw new FatalException("Impossible to write csv; Bad response type.", ex);
      }
    } else {
      resp.setContentType(jsonSerializer.getContentType());
      format = jsonSerializer.serialize(ob);
    }
    resp.setCharacterEncoding("UTF-8");
    try {
      resp.setContentLength(format.getBytes("UTF-8").length);
      resp.getOutputStream().write(format.getBytes("UTF-8"));
    } catch (IOException ex) {
      throw new FatalException("Impossible to write http response", ex);
    }
  }
}
