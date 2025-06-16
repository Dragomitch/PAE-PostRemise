package com.dragomitch.ipl.pae.errors;

import com.dragomitch.ipl.pae.business.exceptions.ErrorFormat;
import com.dragomitch.ipl.pae.exceptions.FatalException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public final class ErrorManager {

  private static final String FILE_NAME = "errors.json";
  private static Map<Integer, ErrorFormat> errors;

  static {
    load();
  }

  private ErrorManager() {}

  private static void load() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    try (InputStream in = ErrorManager.class.getClassLoader().getResourceAsStream(FILE_NAME)) {
      if (in == null) {
        throw new FatalException("Unable to find " + FILE_NAME);
      }
      errors = mapper.readValue(in, new TypeReference<Map<Integer, ErrorFormat>>() {});
    } catch (IOException ex) {
      throw new FatalException("I/O Error while reading properties file: " + FILE_NAME, ex);
    }
  }

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
