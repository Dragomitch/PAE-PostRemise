package com.dragomitch.ipl.pae.business.exceptions;

/**
 * Thrown when a user cannot be authenticated (unknown username or wrong password). Answered with
 * 401.
 */
public class UnauthenticatedUserException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public UnauthenticatedUserException() {
    super();
  }

  public UnauthenticatedUserException(String message) {
    super(message);
  }

}
