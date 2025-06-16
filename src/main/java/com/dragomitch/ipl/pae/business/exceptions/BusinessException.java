package com.dragomitch.ipl.pae.business.exceptions;

import java.util.LinkedList;
import java.util.List;
import com.dragomitch.ipl.pae.errors.ErrorManager;

/**
 * This exception may be thrown when an error occurs during a business related operation.
 */
public class BusinessException extends RuntimeException {

  private static final long serialVersionUID = 1L;
  private List<Integer> violations;
  private int errorCode;

  /**
   * Constructs a BusinessException with the specified cause and error.
   *
   * @param errorCode the error code
   * @param violations the list of violations for that error.
   * @param cause the cause (which is saved for later retrieval by the Throwable.getCause() method). (A null value is permitted, and indicates that the cause is
   * nonexistent or unknown.)
   */
  public BusinessException(int errorCode, List<Integer> violations, Throwable cause) {
    super(cause);
    this.errorCode = errorCode;
    this.violations = violations;
  }

  /**
   * Return the errorFormat object describing the error.
   *
   * @return an ErrorFormat with the violations details rightly inserted
   */
  public ErrorFormat getError() {
    ErrorFormat error = ErrorManager.getError(errorCode);
    if (violations != null) {
      List<ErrorFormat> errorsOccured = new LinkedList<ErrorFormat>();
      for (int curError : violations) {
        errorsOccured.add(ErrorManager.getError(curError));
      }
      error.setDetails(errorsOccured);
    }
    return error;
  }

  public BusinessException(int errorCode, List<Integer> violations) {
    this(errorCode, violations, null);
  }

  public BusinessException(int errorCode, Throwable cause) {
    super(cause);
    this.errorCode = errorCode;
  }

  public BusinessException(int errorCode) {
    this(errorCode, null, null);
  }
}
