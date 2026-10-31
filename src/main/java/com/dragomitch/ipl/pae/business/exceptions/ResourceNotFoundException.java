package com.dragomitch.ipl.pae.business.exceptions;

/**
 * The resource identified by the request (usually an id of the URL) does not exist: answered
 * with 404 and the {@link ErrorCode#RESOURCE_NOT_FOUND} problem.
 */
public class ResourceNotFoundException extends BusinessException {

  private static final long serialVersionUID = 1L;

  public ResourceNotFoundException() {
    super(ErrorCode.RESOURCE_NOT_FOUND);
  }
}
