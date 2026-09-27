package com.dragomitch.ipl.pae.presentation;

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

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the legacy {@link RoutingServlet} with the embedded servlet container. It serves the
 * REST API under {@value #API_MAPPING}; everything else (the web UI in {@code static/}) is left to
 * Spring MVC's DispatcherServlet.
 */
@Configuration
class PresentationConfig {

  static final String API_MAPPING = "/api/1.0/*";

  @Bean
  ServletRegistrationBean<RoutingServlet> routingServlet(DenialReasonUcc denialReasonUcc,
      MobilityUcc mobilityUcc, MobilityChoiceUcc mobilityChoiceUcc,
      NominatedStudentUcc nominatedStudentUcc, OptionUcc optionUcc, PartnerUcc partnerUcc,
      SessionUcc sessionUcc, UserUcc userUcc, PaymentUcc paymentUcc, ProgrammeUcc programmeUcc,
      CountryUcc countryUcc, Invoker invoker, SuccessHandler successHandler,
      ExceptionHandler exceptionHandler) {
    RoutingServlet servlet = new RoutingServlet(denialReasonUcc, mobilityUcc, mobilityChoiceUcc,
        nominatedStudentUcc, optionUcc, partnerUcc, sessionUcc, userUcc, paymentUcc,
        programmeUcc, countryUcc, invoker, successHandler, exceptionHandler);
    ServletRegistrationBean<RoutingServlet> registration =
        new ServletRegistrationBean<>(servlet, API_MAPPING);
    registration.setName("RoutingServlet");
    registration.setLoadOnStartup(1);
    return registration;
  }
}
