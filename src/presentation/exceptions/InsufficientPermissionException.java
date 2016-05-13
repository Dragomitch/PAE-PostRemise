package presentation.exceptions;

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
