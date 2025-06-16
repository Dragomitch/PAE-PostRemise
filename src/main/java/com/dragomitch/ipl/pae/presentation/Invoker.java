package com.dragomitch.ipl.pae.presentation;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import com.dragomitch.ipl.pae.exceptions.FatalException;
import com.dragomitch.ipl.pae.logging.LogManager;
import com.dragomitch.ipl.pae.presentation.exceptions.InsufficientPermissionException;
import com.dragomitch.ipl.pae.presentation.exceptions.UnauthenticatedUserException;
import com.dragomitch.ipl.pae.presentation.annotations.HttpBody;
import com.dragomitch.ipl.pae.presentation.annotations.HttpHeader;
import com.dragomitch.ipl.pae.presentation.annotations.HttpParameter;
import com.dragomitch.ipl.pae.presentation.annotations.PathParameter;
import com.dragomitch.ipl.pae.presentation.annotations.Role;
import com.dragomitch.ipl.pae.presentation.annotations.Session;
import com.dragomitch.ipl.pae.presentation.annotations.SessionParameter;

@Component
class Invoker {

  private static Logger logger = LogManager.getLogger(Invoker.class.getName());

  private final JsonSerializer serializer;
  private final SessionManager sessionManager;

  public Invoker(JsonSerializer serializer, SessionManager sessionManager) {
    this.serializer = serializer;
    this.sessionManager = sessionManager;
  }

  /**
   * Invokes the method that handles the request for this route.
   *
   * @param req the HTTP request object that provides information about the request made
   * @param route the route
   * @param resp the HTTP response object that provides HTTP-specific functionality
   * @return the request process output
   * @throws Throwable if the invoked method has thrown an exception during the process
   */
  public Object invoke(HttpServletRequest req, Route route, HttpServletResponse resp)
      throws Throwable {
    if (!hasPermission(req, route.getImplMethod())) {
      throw new InsufficientPermissionException();
    }
    String path = req.getPathInfo();
    Method method = route.getImplMethod();
    Object instance = route.getImplInstance();
    Object[] parameters = mapMethodArguments(req, path, route);
    Object invocationResult = null;
    try {
      method.setAccessible(true);
      invocationResult = method.invoke(instance, parameters);
      handleSessionAnnotation(method, invocationResult, req, resp);
      logger.finer("Method succesfully invoked");
    } catch (InvocationTargetException ex) {
      // An exceptions has been thrown inside the method.
      throw ex.getCause();
    } catch (IllegalAccessException ex) {
      throw new FatalException("Invoking a non-accessible method: " + method.getName(), ex);
    }
    return invocationResult;
  }

  /**
   * Maps all the parameter required for the method to be invoked..
   *
   * @param req the HTTP request object that provides information about the request made
   * @param path the path used to make the request
   * @param route the route that matches the path
   * @return parameters or null if something went wrong
   */
  private Object[] mapMethodArguments(HttpServletRequest req, String path, Route route) {
    Method method = route.getImplMethod();
    Parameter[] parameters = method.getParameters();
    Object[] arguments = new Object[method.getParameterCount()];
    for (int i = 0; i < parameters.length; i++) {
      String parameter = null; // Name of required parameter
      String strValue = null; // Value of required parameter as a string
      Object argument = null; // Value of required parameter
      if (parameters[i].isAnnotationPresent(PathParameter.class)) {
        // Parameter defined in path
        parameter = parameters[i].getAnnotation(PathParameter.class).value();
        strValue = route.getPathTemplate().getParameter(parameter, path);
      } else if (parameters[i].isAnnotationPresent(HttpParameter.class)) {
        // Parameter defined in http parameters
        parameter = parameters[i].getAnnotation(HttpParameter.class).value();
        strValue = req.getParameter(parameter);
      } else if (parameters[i].isAnnotationPresent(HttpHeader.class)) {
        // Parameter defined in http headers
        parameter = parameters[i].getAnnotation(HttpHeader.class).value();
        strValue = req.getHeader(parameter);
      } else if (parameters[i].isAnnotationPresent(SessionParameter.class)) {
        // Parameter defined in session
        parameter = parameters[i].getAnnotation(SessionParameter.class).value();
        argument = sessionManager.getAttribute(parameter, req);
      } else if (parameters[i].isAnnotationPresent(HttpBody.class)) {
        // TODO
        strValue = "";
      } else {
        // No annotation has been found
        throw new FatalException("Method argument n°" + (i + 1) + " is missing annotation.");
      }
      if (argument == null) {
        try {
          argument = serializer.deserialize(strValue, parameters[i].getType());
        } catch (NumberFormatException ex) {
          throw new IllegalArgumentException(
              "Impossible to convert the string to the required type");
        }
      }
      // Updates arguments list
      arguments[i] = argument;
    }
    return arguments;
  }

  /**
   * Checks if the requester is authenticated and has the required permissions.
   *
   * @param req the HTTP request object that provides information about the request made
   * @param method the method to be invoked
   * @return true if the requester has the required permissions, false otherwise
   */
  private boolean hasPermission(HttpServletRequest req, Method method) {
    if (!method.isAnnotationPresent(Role.class)) {
      return true;
    }
    String role;
    try {
      role = (String) sessionManager.getAttribute("userRole", req);
    } catch (ClassCastException ex) {
      throw new FatalException("userRole in session must be a String.", ex);
    }
    if (role == null) {
      throw new UnauthenticatedUserException();
    }
    String[] requiredRoles = method.getDeclaredAnnotation(Role.class).value();
    for (String requiredRole : requiredRoles) {
      if (role.equals(requiredRole)) {
        logger.fine("Requester has right permissions");
        return true;
      }
    }
    logger.info("Requester is missing permission.");
    return false;
  }

  // TODO Javadoc
  private void handleSessionAnnotation(Method method, Object invocationResult,
      HttpServletRequest req, HttpServletResponse resp) {
    if (!method.isAnnotationPresent(Session.class)) {
      return;
    }
    Session annot = method.getAnnotation(Session.class);
    String action = annot.action();
    if (action.equals(Session.DELETE)) {
      sessionManager.removeSession(req, resp);
    } else if (action.equals(Session.SET)) {
      if (annot.fieldNames().length != annot.attributeNames().length) {
        throw new FatalException("Session annotation hasn't been used correctly");
      }
      Map<String, Object> data = new HashMap<String, Object>();
      for (int i = 0; i < annot.attributeNames().length; i++) {
        String attributeName = annot.attributeNames()[i];
        Object attribute;
        try {
          Field field = invocationResult.getClass().getDeclaredField(annot.fieldNames()[i]);
          field.setAccessible(true);
          attribute = field.get(invocationResult);
        } catch (IllegalArgumentException ex) {
          throw new FatalException();
        } catch (IllegalAccessException ex) {
          throw new FatalException("Inaccessible field: " + annot.fieldNames()[i]);
        } catch (NoSuchFieldException ex) {
          throw new FatalException("No such field found: " + annot.fieldNames()[i] + " in "
              + invocationResult.getClass().getName());
        } catch (SecurityException ex) {
          throw new FatalException();
        }
        data.put(attributeName, attribute);
      }
      sessionManager.setAttributes(req, resp, data);
    } else {
      throw new FatalException("Incompatible action in session");
    }
  }

}
