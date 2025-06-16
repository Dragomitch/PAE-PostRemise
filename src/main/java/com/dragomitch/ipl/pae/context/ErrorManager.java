package com.dragomitch.ipl.pae.context;

import com.dragomitch.ipl.pae.business.exceptions.ErrorFormat;
import com.dragomitch.ipl.pae.exceptions.FatalException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class ErrorManager {

  private static final String FILE_NAME = ContextManager.getProperty(ContextManager.ENV_ERRORS);

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
    try {
      String contentFile = new String(
          Files.readAllBytes(Paths.get(ContextManager.getCurrentAbsolutePath() + FILE_NAME)),
          "UTF-8");
      ErrorManager.errors = mapper.readValue(contentFile,
          new TypeReference<Map<Integer, ErrorFormat>>() {});
    } catch (IOException ex) {
      throw new FatalException("I/O Error while reading properties file: " + FILE_NAME, ex);
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
