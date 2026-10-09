package com.dragomitch.ipl.pae.business.exceptions;

/**
 * Sign-in refused: unknown username or wrong password (the client is not told which). Answered
 * with 401 and the {@link ErrorCode#INVALID_CREDENTIALS} problem.
 */
public class InvalidCredentialsException extends BusinessException {

  private static final long serialVersionUID = 1L;

  public InvalidCredentialsException() {
    super(ErrorCode.INVALID_CREDENTIALS);
  }
}
