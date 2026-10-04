package com.dragomitch.ipl.pae.business.exceptions;

/**
 * Thrown by a use case when the requester is authenticated but not allowed to perform the
 * operation on this resource (e.g. a student reading another student's data). Answered with 403.
 */
public class InsufficientPermissionException extends RuntimeException {

  private static final long serialVersionUID = 4764879821608811916L;

  public InsufficientPermissionException() {
    super();
  }

  public InsufficientPermissionException(String message, Throwable cause) {
    super(message, cause);
  }

  public InsufficientPermissionException(String message) {
    super(message);
  }

  public InsufficientPermissionException(Throwable cause) {
    super(cause);
  }


}
