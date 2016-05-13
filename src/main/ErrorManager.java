package main;

import business.exceptions.ErrorFormat;
import main.exceptions.FatalException;

import com.owlike.genson.GenericType;
import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;

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
    Genson genson =
        new GensonBuilder().useConstructorWithArguments(false).useMethods(true).create();
    try {
      String contentFile = new String(Files.readAllBytes(Paths.get(FILE_NAME)), "UTF-8");
      ErrorManager.errors =
          genson.deserialize(contentFile, new GenericType<Map<Integer, ErrorFormat>>() {});
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
