package presentation.exceptions;

public class RouteNotFoundException extends RuntimeException {

  private static final long serialVersionUID = -763762218923429455L;

  public RouteNotFoundException() {
    super();
  }

  public RouteNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public RouteNotFoundException(String message) {
    super(message);
  }

  public RouteNotFoundException(Throwable cause) {
    super(cause);
  }

}
