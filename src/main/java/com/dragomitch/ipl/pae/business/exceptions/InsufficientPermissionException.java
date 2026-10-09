package com.dragomitch.ipl.pae.business.exceptions;

/**
 * Thrown by a use case when the requester is authenticated but not allowed to perform the
 * operation on this resource (e.g. a student reading another student's data): answered with 403
 * and the {@link ErrorCode#ACCESS_DENIED} problem, like a failed role check.
 */
public class InsufficientPermissionException extends BusinessException {

  private static final long serialVersionUID = 1L;

  public InsufficientPermissionException() {
    super(ErrorCode.ACCESS_DENIED);
  }
}
