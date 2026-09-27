package com.dragomitch.ipl.pae.business.exceptions;

import com.dragomitch.ipl.pae.exceptions.FatalException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Catalogue of the error messages declared in {@value #FILE_NAME} (classpath resource), indexed
 * by the error codes of {@link ErrorFormat}.
 */
public final class ErrorManager {

  static final String FILE_NAME = "errors.json";

  private ErrorManager() {
    throw new UnsupportedOperationException();
  }

  /**
   * Loaded on first use (initialization-on-demand holder), so callers never observe an empty
   * catalogue whatever the start-up or test execution order.
   */
  private static final class Catalogue {
    private static final Map<Integer, ErrorFormat> ERRORS = load();

    private static Map<Integer, ErrorFormat> load() {
      try (InputStream in = ErrorManager.class.getClassLoader().getResourceAsStream(FILE_NAME)) {
        if (in == null) {
          throw new FatalException("Error catalogue not found on the classpath: " + FILE_NAME);
        }
        return new ObjectMapper().readValue(in, new TypeReference<Map<Integer, ErrorFormat>>() {});
      } catch (IOException ex) {
        throw new FatalException("I/O Error while reading the error catalogue: " + FILE_NAME, ex);
      }
    }
  }

  /**
   * Return the error of the code specified in parameter
   *
   * @param errorCode the number of the error to fetch.
   * @return an ErrorFormat object if it exist.
   */
  public static ErrorFormat getError(int errorCode) {
    ErrorFormat error = Catalogue.ERRORS.get(errorCode);
    if (error == null) {
      return null;
    }
    error = error.buildClone();
    error.setErrorCode(errorCode);
    return error;
  }
}
