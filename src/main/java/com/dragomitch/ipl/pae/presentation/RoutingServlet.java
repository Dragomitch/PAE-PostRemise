package com.dragomitch.ipl.pae.presentation;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.annotations.Inject;
import com.dragomitch.ipl.pae.logging.LogManager;
import com.dragomitch.ipl.pae.presentation.enums.HttpMethod;
import com.dragomitch.ipl.pae.uccontrollers.CountryUcc;
import com.dragomitch.ipl.pae.uccontrollers.DenialReasonUcc;
import com.dragomitch.ipl.pae.uccontrollers.MobilityChoiceUcc;
import com.dragomitch.ipl.pae.uccontrollers.MobilityUcc;
import com.dragomitch.ipl.pae.uccontrollers.NominatedStudentUcc;
import com.dragomitch.ipl.pae.uccontrollers.OptionUcc;
import com.dragomitch.ipl.pae.uccontrollers.PartnerUcc;
import com.dragomitch.ipl.pae.uccontrollers.PaymentUcc;
import com.dragomitch.ipl.pae.uccontrollers.ProgrammeUcc;
import com.dragomitch.ipl.pae.uccontrollers.SessionUcc;
import com.dragomitch.ipl.pae.uccontrollers.UserUcc;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RoutingServlet extends HttpServlet {

  private static final long serialVersionUID = 650341630398081136L;

  private static Logger logger = LogManager.getLogger(RoutingServlet.class.getName());

  private final transient RouteResolver routeResolver = new RouteResolver();
  private final transient Invoker invoker = new Invoker();
  private final transient SuccessHandler successHandler;
  private final transient ExceptionHandler exceptionHandler;
  private final transient String contentTypeOptions = "application/json";

  private Set<Object> useCaseControllers;

  /**
   * Sole constructor for explicit invocation.
   * 
   * @param userUcc use case controller for user
   */
  @Inject
  public RoutingServlet(DenialReasonUcc denialReasonUcc, MobilityUcc mobilityUcc,
      MobilityChoiceUcc mobilityChoiceUcc, NominatedStudentUcc nominatedStudentUcc,
      OptionUcc optionUcc, PartnerUcc partnerUcc, SessionUcc sessionUcc, UserUcc userUcc,
      PaymentUcc paymentUcc, ProgrammeUcc programmeUcc, CountryUcc countryUcc,
      EntityFactory entityFactory) {
    super();
    logger.log(Level.INFO, "Initializing Servlet");
    this.successHandler = new SuccessHandler(entityFactory);
    this.exceptionHandler = new ExceptionHandler(entityFactory);
    this.useCaseControllers = new HashSet<Object>();
    this.useCaseControllers.add(denialReasonUcc);
    this.useCaseControllers.add(mobilityUcc);
    this.useCaseControllers.add(mobilityChoiceUcc);
    this.useCaseControllers.add(nominatedStudentUcc);
    this.useCaseControllers.add(optionUcc);
    this.useCaseControllers.add(partnerUcc);
    this.useCaseControllers.add(sessionUcc);
    this.useCaseControllers.add(userUcc);
    this.useCaseControllers.add(paymentUcc);
    this.useCaseControllers.add(programmeUcc);
    this.useCaseControllers.add(countryUcc);
    routeResolver.initializeRoutes(useCaseControllers);
    logger.log(Level.INFO, "Servlet sucessfully initialized");
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.servlet.http.HttpServlet#service(javax.servlet.http.HttpServletRequest,
   * javax.servlet.http.HttpServletResponse)
   */
  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    if(HttpMethod.valueOf(req.getMethod()).equals(HttpMethod.OPTIONS)){

      successHandler.handleSuccess("", contentTypeOptions, resp, null);
    }
    try {
      // Looking for the right route
      Route route = routeResolver.findRoute(HttpMethod.valueOf(req.getMethod()), req.getPathInfo());
      // Invoking the Ucc method related to the route
      Object invocationResult = invoker.invoke(req, route, resp);
      String additionalValues = "";
      if( req.getPathInfo().equals("/session")){
        //TODO Modify the place where we set the JSESSIONID to not send to the line the headers should solve the problem

        additionalValues = ",\"access_token\":\"";
        String cookie = "";

        for (String tempCookie: resp.getHeaders("Set-Cookie")) {
          if(tempCookie.startsWith("session=")){
            cookie = tempCookie.replace("session=", "");
            cookie = cookie.substring(0, cookie.indexOf(';')) + "\"";
          }
        }
        additionalValues += cookie;
      }
      successHandler.handleSuccess(invocationResult, route.getContentType(), resp, additionalValues);
    } catch (Throwable ex) {
      exceptionHandler.handleException(ex, resp);
    }
  }

}
