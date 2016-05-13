package presentation.exceptions;

public class UnauthenticatedUserException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public UnauthenticatedUserException() {
    super();
  }

  public UnauthenticatedUserException(String message) {
    super(message);
  }

}
