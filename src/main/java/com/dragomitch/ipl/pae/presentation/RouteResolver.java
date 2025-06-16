package com.dragomitch.ipl.pae.presentation;

import com.dragomitch.ipl.pae.logging.LogManager;
import com.dragomitch.ipl.pae.presentation.annotations.ApiCollection;
import com.dragomitch.ipl.pae.presentation.enums.HttpMethod;
import com.dragomitch.ipl.pae.presentation.exceptions.RouteNotFoundException;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.slf4j.Logger;

class RouteResolver {
  private Logger logger = LogManager.getLogger(this.getClass().getName());

  private SortedSet<Route> routes;

  /**
   * Initialize routes contained in use case controllers.
   * 
   * @param useCaseControllers a Set containing all use case controllers from which routes have to
   *        be initialized
   */
  void initializeRoutes(Set<Object> useCaseControllers) {
    logger.info("Initializing routes");
    this.routes = new TreeSet<Route>();
    for (Object ucc : useCaseControllers) {
      String endpoint = "";
      if (ucc.getClass().isAnnotationPresent(ApiCollection.class)) {
        endpoint = ucc.getClass().getAnnotation(ApiCollection.class).endpoint();
      }
      for (Method method : ucc.getClass().getDeclaredMethods()) {
        if (method.isAnnotationPresent(com.dragomitch.ipl.pae.presentation.annotations.Route.class)) {
          com.dragomitch.ipl.pae.presentation.annotations.Route annotRoute =
              method.getDeclaredAnnotation(com.dragomitch.ipl.pae.presentation.annotations.Route.class);
          PathTemplate pathTemplate = new PathTemplate(endpoint + annotRoute.template());
          Route route = new Route(annotRoute.method(), pathTemplate, ucc.getClass(), ucc, method,
              annotRoute.contentType());
          routes.add(route);
        }
      }
    }
      logger.info(routes.size() + " routes sucessfully initialized: \n" + dumpRoutes());
  }

  /**
   * Looks for a route matching both an HTTP method and a path.
   * 
   * @param httpMethod the HTTP method with which the request was made.
   * @param path the URL the client sent when it made the request
   * @return the required Route instance
   * @throws RouteNotFoundException if no route matching the request has been found
   */
  Route findRoute(HttpMethod httpMethod, String path) {
    logger.info("Looking for the route matching: " + httpMethod + " " + path);
    for (Route route : routes) {
      if (route.getHttpMethod() != httpMethod) {
        continue;
      }
      if (route.getPathTemplate().matches(path)) {
        logger.info("Route found: " + route);
        return route;
      }
    }
    logger.info("No route matching path found");
    throw new RouteNotFoundException();
  }

  /**
   * Builds a character string representation of all routes initialized. Mainly used in debugging.
   * 
   * @return a String
   */
  String dumpRoutes() {
    StringBuilder str = new StringBuilder();
    for (Route route : routes) {
      if (str.length() > 0) {
        str.append("\n");
      }
      str.append("\t" + route);
    }
    return str.toString();
  }

}
