package business.exceptions;


/**
 * This exception may be thrown when a requested ressource does not exist.
 */
public class RessourceNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public RessourceNotFoundException() {
    super();
  }

  public RessourceNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public RessourceNotFoundException(String message) {
    super(message);
  }

  public RessourceNotFoundException(Throwable cause) {
    super(cause);
  }

}
