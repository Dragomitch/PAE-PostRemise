package com.dragomitch.ipl.pae.exceptions;

public class FatalException extends RuntimeException {

  public static final String DATABASE_ERROR_MSG = "Database error";
  public static final String LAZY_LOADING_ERROR_MSG =
      "Operation not completed: instance is not fully loaded.";

  private static final long serialVersionUID = 1L;

  public FatalException() {
    super();
  }

  public FatalException(String message) {
    super(message);
  }

  public FatalException(Throwable cause) {
    super(cause);
  }

  public FatalException(String message, Throwable cause) {
    super(message, cause);
  }

}
