package com.dragomitch.ipl.pae.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.UserDto;
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

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * The authorization matrix of the whole API, derived from the former {@code @Role} annotations:
 * every endpoint is called anonymously (401 unless public), with each role that is not allowed
 * (403) and with each allowed role (2xx). The use cases are mocked.
 */
@WebMvcTest
@Import(WebTestConfig.class)
class EndpointAuthorizationTest {

  private static final String API = ApiPaths.BASE;

  /** Who may call an endpoint. */
  enum Access {
    PUBLIC, PROFESSOR, STUDENT, PROFESSOR_OR_STUDENT;

    boolean allows(String role) {
      return switch (this) {
        case PUBLIC, PROFESSOR_OR_STUDENT -> true;
        case PROFESSOR -> UserDto.ROLE_PROFESSOR.equals(role);
        case STUDENT -> UserDto.ROLE_STUDENT.equals(role);
      };
    }
  }

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private EntityFactory entityFactory;

  @MockBean
  private CountryUcc countryUcc;
  @MockBean
  private DenialReasonUcc denialReasonUcc;
  @MockBean
  private MobilityChoiceUcc mobilityChoiceUcc;
  @MockBean
  private MobilityUcc mobilityUcc;
  @MockBean
  private NominatedStudentUcc nominatedStudentUcc;
  @MockBean
  private OptionUcc optionUcc;
  @MockBean
  private PartnerUcc partnerUcc;
  @MockBean
  private PaymentUcc paymentUcc;
  @MockBean
  private ProgrammeUcc programmeUcc;
  @MockBean
  private SessionUcc sessionUcc;
  @MockBean
  private UserUcc userUcc;

  @BeforeEach
  void signinSucceeds() {
    UserDto user = (UserDto) entityFactory.build(UserDto.class);
    user.setId(1);
    user.setRole(UserDto.ROLE_PROFESSOR);
    when(sessionUcc.signin(any(), any())).thenReturn(user);
  }

  /**
   * Every endpoint of the API: method, path (as the legacy UI calls it), JSON body (null when
   * none) and who may call it.
   */
  static Stream<Arguments> endpoints() {
    return Stream.of(
        // session
        Arguments.of("POST", "/session", "{\"username\":\"u\",\"password\":\"p\"}", Access.PUBLIC),
        Arguments.of("GET", "/session", null, Access.PROFESSOR_OR_STUDENT),
        Arguments.of("DELETE", "/session", null, Access.PROFESSOR_OR_STUDENT),
        // users
        Arguments.of("POST", "/users", "{}", Access.PUBLIC),
        Arguments.of("GET", "/users", null, Access.PROFESSOR),
        Arguments.of("PUT", "/users/3/promote", null, Access.PROFESSOR),
        Arguments.of("PUT", "/users/edit", "{}", Access.PROFESSOR_OR_STUDENT),
        // countries
        Arguments.of("GET", "/countries", null, Access.PROFESSOR_OR_STUDENT),
        Arguments.of("GET", "/countries/BE", null, Access.PROFESSOR_OR_STUDENT),
        // denial reasons
        Arguments.of("POST", "/denialReasons", "{\"reason\":\"r\"}", Access.PROFESSOR),
        Arguments.of("GET", "/denialReasons", null, Access.PROFESSOR),
        Arguments.of("PUT", "/denialReasons/1", "{\"reason\":\"r\"}", Access.PROFESSOR),
        // mobility choices
        Arguments.of("POST", "/mobilityChoice", "{}", Access.PROFESSOR_OR_STUDENT),
        Arguments.of("GET", "/mobilityChoices?filter=all", null, Access.PROFESSOR_OR_STUDENT),
        Arguments.of("GET", "/mobilityChoices/count", null, Access.PROFESSOR_OR_STUDENT),
        Arguments.of("PUT", "/mobilityChoices/1/cancel?reason=r", null, Access.STUDENT),
        Arguments.of("PUT", "/mobilityChoices/1/reject?reason=1", null, Access.PROFESSOR),
        Arguments.of("PUT", "/mobilityChoices/1/confirm", null, Access.PROFESSOR),
        Arguments.of("PUT", "/mobilityChoices/1/confirmWithNewPartner", "{}",
            Access.PROFESSOR_OR_STUDENT),
        Arguments.of("GET", "/mobilityChoices/export", null, Access.PROFESSOR),
        // mobilities
        Arguments.of("GET", "/mobilities", null, Access.PROFESSOR_OR_STUDENT),
        Arguments.of("GET", "/mobilities/1", null, Access.PROFESSOR_OR_STUDENT),
        Arguments.of("PUT", "/mobilities/1/confirmProEcoEncoding?version=1", null,
            Access.PROFESSOR),
        Arguments.of("PUT", "/mobilities/1/confirmSecondSoftwareEncoding?version=1", null,
            Access.PROFESSOR),
        Arguments.of("PUT", "/mobilities/1/confirmPayment?version=1", null, Access.PROFESSOR),
        Arguments.of("PUT", "/mobilities/1/confirmDocument?document=2&version=1", null,
            Access.PROFESSOR),
        Arguments.of("PUT", "/mobilities/1/cancel?version=1&denialReason=1", null,
            Access.PROFESSOR_OR_STUDENT),
        Arguments.of("GET", "/mobilities/1/export", null, Access.PROFESSOR),
        // nominated students
        Arguments.of("POST", "/nominatedStudents", "{}", Access.PROFESSOR_OR_STUDENT),
        Arguments.of("GET", "/nominatedStudents/2", null, Access.PROFESSOR_OR_STUDENT),
        Arguments.of("GET", "/nominatedStudents", null, Access.PROFESSOR),
        Arguments.of("PUT", "/nominatedStudents/2", "{}", Access.PROFESSOR_OR_STUDENT),
        // options
        Arguments.of("GET", "/options", null, Access.PUBLIC),
        Arguments.of("GET", "/options/BIN", null, Access.PROFESSOR_OR_STUDENT),
        // partners
        Arguments.of("POST", "/partners", "{}", Access.PROFESSOR_OR_STUDENT),
        Arguments.of("GET", "/partners/1", null, Access.PROFESSOR_OR_STUDENT),
        Arguments.of("GET", "/partners?filter=country&value=BE", null,
            Access.PROFESSOR_OR_STUDENT),
        Arguments.of("PUT", "/partners/1", "{}", Access.PROFESSOR),
        Arguments.of("POST", "/partners/1", "{\"code\":\"BIN\"}", Access.PROFESSOR_OR_STUDENT),
        Arguments.of("GET", "/partners/partnersOptions/1", null, Access.PROFESSOR_OR_STUDENT),
        Arguments.of("PUT", "/partners/1/restore", null, Access.PROFESSOR_OR_STUDENT),
        // payments
        Arguments.of("GET", "/payments", null, Access.PROFESSOR),
        // programmes
        Arguments.of("GET", "/programmes", null, Access.PROFESSOR_OR_STUDENT),
        Arguments.of("GET", "/programmes/1", null, Access.PROFESSOR_OR_STUDENT));
  }

  private MockHttpServletRequestBuilder call(String method, String path, String body) {
    MockHttpServletRequestBuilder builder =
        request(HttpMethod.valueOf(method), API + path).with(csrf());
    if (body != null) {
      builder.contentType(MediaType.APPLICATION_JSON).content(body);
    }
    return builder;
  }

  @ParameterizedTest(name = "{0} {1} anonymously")
  @MethodSource("endpoints")
  void anonymousCallsAreRejectedUnlessPublic(String method, String path, String body,
      Access access) throws Exception {
    if (access == Access.PUBLIC) {
      mockMvc.perform(call(method, path, body)).andExpect(status().is2xxSuccessful());
    } else {
      mockMvc.perform(call(method, path, body))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.errorCode").value(101));
    }
  }

  @ParameterizedTest(name = "{0} {1} as professor")
  @MethodSource("endpoints")
  void professor(String method, String path, String body, Access access) throws Exception {
    expectAccess(method, path, body, access, UserDto.ROLE_PROFESSOR, TestUsers.professor());
  }

  @ParameterizedTest(name = "{0} {1} as student")
  @MethodSource("endpoints")
  void student(String method, String path, String body, Access access) throws Exception {
    expectAccess(method, path, body, access, UserDto.ROLE_STUDENT, TestUsers.student());
  }

  private void expectAccess(String method, String path, String body, Access access, String role,
      RequestPostProcessor user) throws Exception {
    if (access.allows(role)) {
      mockMvc.perform(call(method, path, body).with(user))
          .andExpect(status().is2xxSuccessful());
    } else {
      mockMvc.perform(call(method, path, body).with(user))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.errorCode").value(103));
    }
  }

  @ParameterizedTest(name = "{0} {1} without CSRF token")
  @MethodSource("endpoints")
  void stateChangingCallsNeedTheCsrfToken(String method, String path, String body,
      Access access) throws Exception {
    MockHttpServletRequestBuilder builder =
        request(HttpMethod.valueOf(method), API + path).with(TestUsers.professor());
    if (body != null) {
      builder.contentType(MediaType.APPLICATION_JSON).content(body);
    }
    if ("GET".equals(method)) {
      mockMvc.perform(builder).andExpect(status().is2xxSuccessful());
    } else {
      mockMvc.perform(builder)
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.errorCode").value(103));
    }
  }

  @Test
  void pathsAreMatchedCaseInsensitivelyLikeTheFormerRouter() throws Exception {
    // declared /mobilitychoices/{id}/reject and /denialreasons before the migration
    mockMvc.perform(request(HttpMethod.PUT, API + "/mobilitychoices/1/reject?reason=1")
        .with(csrf()).with(TestUsers.professor())).andExpect(status().isOk());
    mockMvc.perform(request(HttpMethod.GET, API + "/denialreasons")
        .with(TestUsers.professor())).andExpect(status().isOk());
  }

  @Test
  void unknownApiPathsNeedAuthenticationThenAre404() throws Exception {
    mockMvc.perform(request(HttpMethod.GET, API + "/doesNotExist"))
        .andExpect(status().isUnauthorized());
    mockMvc.perform(request(HttpMethod.GET, API + "/doesNotExist").with(TestUsers.student()))
        .andExpect(status().isNotFound());
  }
}
