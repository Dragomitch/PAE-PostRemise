package com.dragomitch.ipl.pae.web;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.Violations;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.business.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.exceptions.FatalException;
import com.dragomitch.ipl.pae.uccontrollers.MobilityUcc;
import com.dragomitch.ipl.pae.uccontrollers.PartnerUcc;
import com.dragomitch.ipl.pae.uccontrollers.UserUcc;

import jakarta.validation.ConstraintViolationException;
import java.util.ConcurrentModificationException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * The error contract of the API: every error is an RFC 9457 problem
 * ({@code application/problem+json}) with {@code type}, {@code title}, {@code status},
 * {@code detail}, {@code instance}, {@code code}, {@code timestamp} (and {@code errors} for a
 * validation failure), localized in French (default) or English from {@code Accept-Language}.
 */
@WebMvcTest
@Import(WebTestConfig.class)
class ProblemDetailsTest {

  private static final String PROBLEM_JSON = "application/problem+json";
  private static final String USERS = ApiPaths.BASE + "/users";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private EntityFactory entityFactory;

  @MockBean
  private UserUcc userUcc;
  @MockBean
  private PartnerUcc partnerUcc;
  @MockBean
  private MobilityUcc mobilityUcc;
  @MockBean
  private com.dragomitch.ipl.pae.uccontrollers.CountryUcc countryUcc;
  @MockBean
  private com.dragomitch.ipl.pae.uccontrollers.DenialReasonUcc denialReasonUcc;
  @MockBean
  private com.dragomitch.ipl.pae.uccontrollers.MobilityChoiceUcc mobilityChoiceUcc;
  @MockBean
  private com.dragomitch.ipl.pae.uccontrollers.NominatedStudentUcc nominatedStudentUcc;
  @MockBean
  private com.dragomitch.ipl.pae.uccontrollers.OptionUcc optionUcc;
  @MockBean
  private com.dragomitch.ipl.pae.uccontrollers.PaymentUcc paymentUcc;
  @MockBean
  private com.dragomitch.ipl.pae.uccontrollers.ProgrammeUcc programmeUcc;
  @MockBean
  private com.dragomitch.ipl.pae.uccontrollers.SessionUcc sessionUcc;

  private ResultActions perform(MockHttpServletRequestBuilder request, String language)
      throws Exception {
    if (language != null) {
      request.header(HttpHeaders.ACCEPT_LANGUAGE, language);
    }
    return mockMvc.perform(request);
  }

  /** The members every problem has. */
  private static ResultActions assertProblem(ResultActions result, int status, String code,
      String instance) throws Exception {
    String kebab = code.toLowerCase().replace('_', '-');
    return result.andExpect(status().is(status))
        .andExpect(content().contentType(PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("urn:pae:problem:" + kebab))
        .andExpect(jsonPath("$.status").value(status))
        .andExpect(jsonPath("$.code").value(code))
        .andExpect(jsonPath("$.instance").value(instance))
        .andExpect(jsonPath("$.timestamp").value(matchesPattern("\\d{4}-\\d\\d-\\d\\dT.*Z")));
  }

  // ----- validation (400 VALIDATION_FAILED with errors) -----

  @ParameterizedTest(name = "{0}")
  @CsvSource(delimiter = '|', value = {
      "fr|Données invalides|Certaines informations sont invalides. Corrigez les champs indiqués."
          + "|doit être une adresse e-mail valide|doit contenir au plus 20 caractères"
          + "|ne peut pas être vide",
      "en|Invalid data|Some information is invalid. Correct the fields indicated."
          + "|must be a valid email address|must contain at most 20 characters|must not be blank"})
  void anInvalidBodyIsAValidationProblemListingTheFields(String language, String title,
      String detail, String emailMessage, String sizeMessage, String blankMessage)
      throws Exception {
    String body = "{\"username\":\"" + "u".repeat(21) + "\",\"password\":\"secret\","
        + "\"firstName\":\"John\",\"lastName\":\"\",\"email\":\"john.example.test\","
        + "\"option\":{\"code\":\"BIN\"}}";

    assertProblem(perform(post(USERS).with(csrf()).contentType(MediaType.APPLICATION_JSON)
        .content(body), language), 400, "VALIDATION_FAILED", USERS)
        .andExpect(jsonPath("$.title").value(title))
        .andExpect(jsonPath("$.detail").value(detail))
        .andExpect(jsonPath("$.errors", hasSize(3)))
        .andExpect(jsonPath("$.errors[?(@.field == 'email')].code").value("Email"))
        .andExpect(jsonPath("$.errors[?(@.field == 'email')].message").value(emailMessage))
        .andExpect(jsonPath("$.errors[?(@.field == 'username')].code").value("Size"))
        .andExpect(jsonPath("$.errors[?(@.field == 'username')].message").value(sizeMessage))
        .andExpect(jsonPath("$.errors[?(@.field == 'lastName')].code").value("NotBlank"))
        .andExpect(jsonPath("$.errors[?(@.field == 'lastName')].message").value(blankMessage));
    verifyNoInteractions(userUcc);
  }

  @Test
  void nestedAndCustomConstraintsAreReportedWithTheirPath() throws Exception {
    String body = TestBodies.NOMINATED_STUDENT.replace("BE68539007547034", "BE68539007547035")
        .replace("\"countryCode\":\"BE\"}}", "\"countryCode\":\"BEL\"}}");

    perform(post(ApiPaths.BASE + "/nominatedStudents").with(csrf()).with(TestUsers.student())
        .contentType(MediaType.APPLICATION_JSON).content(body), "en")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[*].field")
            .value(containsInAnyOrder("iban", "address.country.countryCode")))
        .andExpect(jsonPath("$.errors[?(@.field == 'iban')].message").value(
            "must be a valid IBAN (for example BE68539007547034, without spaces)"))
        .andExpect(jsonPath("$.errors[?(@.field == 'address.country.countryCode')].message")
            .value("must contain exactly 2 characters"));
  }

  @Test
  void aConstraintOnAPathVariableIsAValidationProblem() throws Exception {
    assertProblem(perform(put(USERS + "/0/promote").with(csrf()).with(TestUsers.professor()),
        "en"), 400, "VALIDATION_FAILED", USERS + "/0/promote")
        .andExpect(jsonPath("$.errors[0].field").value("id"))
        .andExpect(jsonPath("$.errors[0].code").value("Positive"))
        .andExpect(jsonPath("$.errors[0].message").value("must be greater than 0"));
    verifyNoInteractions(userUcc);
  }

  @Test
  void aConstraintOnAQueryParameterIsAValidationProblem() throws Exception {
    perform(get(ApiPaths.BASE + "/mobilityChoices?filter=everything")
        .with(TestUsers.professor()), null)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.errors[0].field").value("filter"))
        .andExpect(jsonPath("$.errors[0].code").value("Pattern"))
        .andExpect(jsonPath("$.errors[0].message")
            .value("doit être all, active, canceled, rejected ou passed"));
  }

  @Test
  void aClassLevelConstraintOfTheQueryIsReportedOnItsField() throws Exception {
    perform(get(ApiPaths.BASE + "/partners?filter=country").with(TestUsers.student()), "en")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.errors[0].field").value("value"))
        .andExpect(jsonPath("$.errors[0].code").value("FilterValueRequired"))
        .andExpect(jsonPath("$.errors[0].message").value("is required with this filter"));
  }

  @Test
  void theMethodValidationOfAUseCaseIsAValidationProblem() throws Exception {
    UserDto user = (UserDto) entityFactory.build(UserDto.class);
    ConstraintViolationException violation = new ConstraintViolationException(
        Violations.validator().validate(user));
    when(userUcc.edit(any(), anyInt(), anyString())).thenThrow(violation);

    perform(put(USERS + "/edit").with(csrf()).with(TestUsers.student())
        .contentType(MediaType.APPLICATION_JSON).content(TestBodies.USER), "en")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("email", "firstName",
            "lastName", "option", "username")));
  }

  // ----- business errors -----

  @ParameterizedTest(name = "{0}")
  @CsvSource(delimiter = '|', value = {
      "fr|Nom d’utilisateur indisponible|Le nom d’utilisateur « jdoe » est déjà utilisé.",
      "en|Username unavailable|The username “jdoe” is already taken."})
  void aBusinessErrorIsAProblemWithItsCode(String language, String title, String detail)
      throws Exception {
    when(userUcc.signup(any())).thenThrow(new BusinessException(ErrorCode.USERNAME_TAKEN, "jdoe"));

    assertProblem(perform(post(USERS).with(csrf()).contentType(MediaType.APPLICATION_JSON)
        .content(TestBodies.USER), language), 409, "USERNAME_TAKEN", USERS)
        .andExpect(jsonPath("$.title").value(title))
        .andExpect(jsonPath("$.detail").value(detail))
        .andExpect(jsonPath("$.errors").doesNotExist());
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource(delimiter = '|', value = {
      "fr|Ressource introuvable|La ressource demandée n’existe pas.",
      "en|Resource not found|The requested resource does not exist."})
  void anUnknownResourceIs404(String language, String title, String detail) throws Exception {
    when(partnerUcc.showOne(99)).thenThrow(new ResourceNotFoundException());

    assertProblem(perform(get(ApiPaths.BASE + "/partners/99").with(TestUsers.student()),
        language), 404, "RESOURCE_NOT_FOUND", ApiPaths.BASE + "/partners/99")
        .andExpect(jsonPath("$.title").value(title))
        .andExpect(jsonPath("$.detail").value(detail));
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource(delimiter = '|', value = {
      "fr|Ressource introuvable|Aucune ressource ne correspond à cette adresse.",
      "en|Resource not found|No resource matches this address."})
  void anUnknownRouteIs404(String language, String title, String detail) throws Exception {
    assertProblem(perform(get(ApiPaths.BASE + "/nothing").with(TestUsers.student()), language),
        404, "RESOURCE_NOT_FOUND", ApiPaths.BASE + "/nothing")
        .andExpect(jsonPath("$.title").value(title))
        .andExpect(jsonPath("$.detail").value(detail));
  }

  // ----- security -----

  @ParameterizedTest(name = "{0}")
  @CsvSource(delimiter = '|', value = {
      "fr|Authentification requise|Vous devez être connecté pour effectuer cette action. Votre "
          + "session a peut-être expiré : reconnectez-vous.",
      "en|Authentication required|You must be signed in to do this. Your session may have "
          + "expired: sign in again."})
  void anAnonymousCallIs401(String language, String title, String detail) throws Exception {
    assertProblem(perform(get(USERS), language), 401, "UNAUTHENTICATED", USERS)
        .andExpect(jsonPath("$.title").value(title))
        .andExpect(jsonPath("$.detail").value(detail));
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource(delimiter = '|', value = {
      "fr|Accès refusé|Vous n’avez pas les droits nécessaires pour effectuer cette action.",
      "en|Access denied|You are not allowed to do this."})
  void aForbiddenCallIs403(String language, String title, String detail) throws Exception {
    assertProblem(perform(get(USERS).with(TestUsers.student()), language), 403, "ACCESS_DENIED",
        USERS)
        .andExpect(jsonPath("$.title").value(title))
        .andExpect(jsonPath("$.detail").value(detail));
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource(delimiter = '|', value = {
      "fr|Identifiants incorrects|Nom d’utilisateur et/ou mot de passe incorrect.",
      "en|Invalid credentials|Incorrect username and/or password."})
  void wrongCredentialsAre401InvalidCredentials(String language, String title, String detail)
      throws Exception {
    when(sessionUcc.signin(any(), any()))
        .thenThrow(new com.dragomitch.ipl.pae.business.exceptions.InvalidCredentialsException());

    assertProblem(perform(post(ApiPaths.BASE + "/session").with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"username\":\"jdoe\",\"password\":\"wrong\"}"), language), 401,
        "INVALID_CREDENTIALS", ApiPaths.BASE + "/session")
        .andExpect(jsonPath("$.title").value(title))
        .andExpect(jsonPath("$.detail").value(detail));
  }

  // ----- errors of Spring MVC -----

  @ParameterizedTest(name = "{0}")
  @CsvSource(delimiter = '|', value = {
      "fr|Méthode non autorisée|La méthode DELETE n’est pas prise en charge par cette ressource.",
      "en|Method not allowed|The method DELETE is not supported by this resource."})
  void anUnsupportedMethodIs405(String language, String title, String detail) throws Exception {
    assertProblem(perform(delete(USERS).with(csrf()).with(TestUsers.professor()), language), 405,
        "METHOD_NOT_ALLOWED", USERS)
        .andExpect(jsonPath("$.title").value(title))
        .andExpect(jsonPath("$.detail").value(detail));
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource(delimiter = '|', value = {
      "fr|Requête illisible|Le contenu de la requête est illisible (JSON mal formé ou valeur de "
          + "type incorrect).",
      "en|Unreadable request|The request content cannot be read (malformed JSON or value of a "
          + "wrong type)."})
  void aMalformedJsonBodyIs400(String language, String title, String detail) throws Exception {
    assertProblem(perform(post(USERS).with(csrf()).contentType(MediaType.APPLICATION_JSON)
        .content("{not json"), language), 400, "MALFORMED_REQUEST", USERS)
        .andExpect(jsonPath("$.title").value(title))
        .andExpect(jsonPath("$.detail").value(detail));
  }

  @Test
  void aWronglyTypedParameterIs400() throws Exception {
    assertProblem(perform(put(USERS + "/abc/promote").with(csrf()).with(TestUsers.professor()),
        "en"), 400, "MALFORMED_REQUEST", USERS + "/abc/promote")
        .andExpect(jsonPath("$.title").value("Invalid parameter"))
        .andExpect(jsonPath("$.detail")
            .value("The value “abc” of the parameter “id” does not have the expected type."));
  }

  @Test
  void anUnsupportedContentTypeIs415() throws Exception {
    assertProblem(perform(post(USERS).with(csrf()).contentType(MediaType.TEXT_PLAIN)
        .content("hello"), "fr"), 415, "UNSUPPORTED_MEDIA_TYPE", USERS)
        .andExpect(jsonPath("$.title").value("Format non pris en charge"))
        .andExpect(jsonPath("$.detail")
            .value(org.hamcrest.Matchers.startsWith("Le format « text/plain")));
  }

  // ----- persistence -----

  @ParameterizedTest(name = "{0}")
  @CsvSource(delimiter = '|', value = {
      "fr|Modification concurrente|Ces données ont été modifiées par quelqu’un d’autre entre-temps."
          + " Rechargez-les puis recommencez.",
      "en|Concurrent modification|This data was modified by someone else in the meantime. Reload "
          + "it and try again."})
  void anOptimisticLockFailureIs409(String language, String title, String detail)
      throws Exception {
    when(mobilityUcc.confirmPayment(4, 2))
        .thenThrow(new OptimisticLockingFailureException("stale version 2 of mobility 4"));

    assertProblem(perform(put(ApiPaths.BASE + "/mobilities/4/confirmPayment?version=2")
        .with(csrf()).with(TestUsers.professor()), language), 409, "CONCURRENT_MODIFICATION",
        ApiPaths.BASE + "/mobilities/4/confirmPayment")
        .andExpect(jsonPath("$.title").value(title))
        .andExpect(jsonPath("$.detail").value(detail));
  }

  @Test
  void theConcurrentModificationOfTheJdbcDaosIs409Too() throws Exception {
    when(mobilityUcc.confirmPayment(4, 2)).thenThrow(new ConcurrentModificationException());

    perform(put(ApiPaths.BASE + "/mobilities/4/confirmPayment?version=2").with(csrf())
        .with(TestUsers.professor()), null)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONCURRENT_MODIFICATION"));
  }

  @Test
  void aDatabaseConstraintIs409WithoutTheSqlError() throws Exception {
    when(mobilityUcc.confirmPayment(4, 2)).thenThrow(new DataIntegrityViolationException(
        "ERROR: duplicate key value violates unique constraint \"payments_pkey\""));

    assertProblem(perform(put(ApiPaths.BASE + "/mobilities/4/confirmPayment?version=2")
        .with(csrf()).with(TestUsers.professor()), "en"), 409, "DATA_CONFLICT",
        ApiPaths.BASE + "/mobilities/4/confirmPayment")
        .andExpect(jsonPath("$.detail").value("The change conflicts with the stored data."))
        .andExpect(content().string(not(containsString("payments_pkey"))));
  }

  @Test
  void anEmptyResultIs404() throws Exception {
    when(partnerUcc.showOne(5)).thenThrow(new EmptyResultDataAccessException(1));

    assertProblem(perform(get(ApiPaths.BASE + "/partners/5").with(TestUsers.student()), "en"),
        404, "RESOURCE_NOT_FOUND", ApiPaths.BASE + "/partners/5");
  }

  // ----- server errors -----

  @ParameterizedTest(name = "{0}")
  @CsvSource(delimiter = '|', value = {
      "fr|Erreur interne|Une erreur inattendue est survenue. Réessayez plus tard ou communiquez la "
          + "référence ",
      "en|Internal error|An unexpected error occurred. Try again later or give the reference "})
  void anUnexpectedErrorIs500WithoutLeakingAnything(String language, String title,
      String detailStart) throws Exception {
    when(partnerUcc.showOne(7)).thenThrow(new FatalException("Database error",
        new IllegalStateException("SELECT secret FROM student_exchange_tools.users")));

    assertProblem(perform(get(ApiPaths.BASE + "/partners/7").with(TestUsers.student()),
        language), 500, "INTERNAL_ERROR", ApiPaths.BASE + "/partners/7")
        .andExpect(jsonPath("$.title").value(title))
        .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.startsWith(detailStart)))
        .andExpect(jsonPath("$.errorId").value(matchesPattern("[0-9a-f-]{36}")))
        .andExpect(content().string(not(containsString("SELECT"))))
        .andExpect(content().string(not(containsString("Database error"))))
        .andExpect(content().string(not(containsString("Exception"))));
  }

  @Test
  void theErrorIdIsTheReferenceGivenInTheDetail() throws Exception {
    when(partnerUcc.showOne(7)).thenThrow(new IllegalStateException("boom"));

    String body = perform(get(ApiPaths.BASE + "/partners/7").with(TestUsers.student()), "en")
        .andExpect(status().isInternalServerError())
        .andReturn().getResponse().getContentAsString();
    String errorId = com.jayway.jsonpath.JsonPath.read(body, "$.errorId");
    String detail = com.jayway.jsonpath.JsonPath.read(body, "$.detail");
    org.junit.jupiter.api.Assertions.assertTrue(detail.contains(errorId), detail);
  }

  // ----- languages -----

  @ParameterizedTest(name = "Accept-Language: {0}")
  @CsvSource(delimiter = '|', value = {
      "en-US,en;q=0.9|Access denied",
      "en-GB|Access denied",
      "fr-BE,fr;q=0.9|Accès refusé",
      "de-DE,de;q=0.9|Accès refusé",
      "de-DE,en;q=0.5|Access denied",
      "*|Accès refusé"})
  void theLanguageIsNegotiatedAmongFrenchAndEnglish(String acceptLanguage, String title)
      throws Exception {
    perform(get(USERS).with(TestUsers.student()), acceptLanguage)
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.title").value(title));
  }

  @Test
  void frenchIsTheDefaultLanguage() throws Exception {
    perform(get(USERS), null)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.title").value("Authentification requise"));
  }

  @Test
  void everyProblemHasTheSameMembers() throws Exception {
    when(userUcc.showAll()).thenReturn(List.of());
    perform(delete(USERS).with(csrf()).with(TestUsers.professor()), "en")
        .andExpect(jsonPath("$.*", hasSize(7)));
  }
}
