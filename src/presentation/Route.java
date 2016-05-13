package presentation;

import java.lang.reflect.Method;

import presentation.enums.HttpMethod;

public class Route implements Comparable<Route> {

  private HttpMethod httpMethod;
  private PathTemplate pathTemplate;
  private Class<?> implClass;
  private Object implInstance;
  private Method implMethod;
  private String contentType;

  /**
   * Constructs a Route with the specified HTTP method, path template, class, instance and method.
   * 
   * @param httpMethod the HTTP method used during the request
   * @param pathTemplate the path template
   * @param implClass the class that implements
   * @param implInstance the instance from which the method has to be invoked
   * @param implMethod the method to be invoked
   */
  public Route(HttpMethod httpMethod, PathTemplate pathTemplate, Class<?> implClass,
      Object implInstance, Method implMethod, String contentType) {
    this.httpMethod = httpMethod;
    this.pathTemplate = pathTemplate;
    this.implClass = implClass;
    this.implInstance = implInstance;
    this.implMethod = implMethod;
    this.contentType = contentType;
  }

  public HttpMethod getHttpMethod() {
    return httpMethod;
  }

  public PathTemplate getPathTemplate() {
    return pathTemplate;
  }

  public Class<?> getImplClass() {
    return implClass;
  }

  public Object getImplInstance() {
    return implInstance;
  }

  public Method getImplMethod() {
    return implMethod;
  }

  public String getContentType() {
    return contentType;
  }

  @Override
  public int compareTo(Route that) {
    int compare;
    if ((compare = this.httpMethod.getWeight() - that.httpMethod.getWeight()) != 0) {
      return compare;
    }
    return this.pathTemplate.toString().compareTo(that.toString());
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((httpMethod == null) ? 0 : httpMethod.hashCode());
    result = prime * result + ((pathTemplate == null) ? 0 : pathTemplate.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Route other = (Route) obj;
    if (httpMethod != other.httpMethod) {
      return false;
    }
    if (pathTemplate == null) {
      if (other.pathTemplate != null) {
        return false;
      }
    } else if (!pathTemplate.equals(other.pathTemplate)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return this.getHttpMethod() + " " + this.getPathTemplate() + " --> "
        + this.getImplClass().getName() + "." + this.getImplMethod().getName();
  }

}
