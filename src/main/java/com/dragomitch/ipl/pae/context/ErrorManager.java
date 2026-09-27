package com.dragomitch.ipl.pae.context;

import com.dragomitch.ipl.pae.business.exceptions.ErrorFormat;
import com.dragomitch.ipl.pae.exceptions.FatalException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class ErrorManager {

  private static Map<Integer, ErrorFormat> errors;

  private ErrorManager() {
    throw new UnsupportedOperationException();
  }

  /**
   * Load the correctly the class.
   */
  public static void load() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    String fileName = ContextManager.getProperty(ContextManager.ENV_ERRORS);
    try (InputStream in = ContextManager.openResource(fileName)) {
      ErrorManager.errors = mapper.readValue(in, new TypeReference<Map<Integer, ErrorFormat>>() {});
    } catch (IOException ex) {
      throw new FatalException("I/O Error while reading error file: " + fileName, ex);
    }
  }

  /**
   * Return the error of the code specified in parameter
   * 
   * @param errorCode the number of the error to fetch.
   * @return an ErrorFormat object if it exist.
   */
  public static ErrorFormat getError(int errorCode) {
    ErrorFormat error = errors.get(errorCode);
    if (error == null) {
      return null;
    }
    error = error.buildClone();
    error.setErrorCode(errorCode);
    return error;
  }
}
