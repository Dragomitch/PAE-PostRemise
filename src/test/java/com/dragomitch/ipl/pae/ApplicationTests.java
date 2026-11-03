package com.dragomitch.ipl.pae;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dragomitch.ipl.pae.domain.model.EntityFactory;
import com.dragomitch.ipl.pae.domain.dto.UserDto;
import com.dragomitch.ipl.pae.adapter.in.web.security.SessionCookieService;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.JsonNode;

/**
 * Boots the whole application (real beans, embedded server, real security chain) and checks the
 * HTTP wiring. None of the requests reach the database, so no PostgreSQL instance is needed:
 * Hibernate is told the database (PostgreSQL 16) instead of reading it from a connection at
 * start-up, and picks its dialect from it.
 *
 * <p>For the same reason the database health indicator is switched off here
 * ({@code management.health.db.enabled=false}), otherwise {@code /actuator/health} would be DOWN
 * (and slow: the connection pool waits 30 seconds for a database). That the indicator is wired and
 * counts in the status is checked by {@link ActuatorHealthWithoutDatabaseTests}.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = {
    "spring.jpa.properties.jakarta.persistence.database-product-name=PostgreSQL",
    "spring.jpa.properties.jakarta.persistence.database-major-version=16",
    "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false",
    "management.health.db.enabled=false"})
@AutoConfigureRestTestClient
class ApplicationTests {

  @Autowired
  private RestTestClient rest;

  @Autowired
  private SessionCookieService sessionCookieService;

  @Autowired
  private EntityFactory entityFactory;

  @LocalServerPort
  private int port;

  private String token(int id, String role) {
    UserDto user = (UserDto) entityFactory.build(UserDto.class);
    user.setId(id);
    user.setRole(role);
    return sessionCookieService.createToken(user);
  }

  private <T> EntityExchangeResult<T> get(String path, Class<T> type) {
    return rest.get().uri(path).exchange().returnResult(type);
  }

  private EntityExchangeResult<String> get(String path) {
    return get(path, String.class);
  }

  private EntityExchangeResult<String> getWithSessionCookie(String path, String token) {
    return rest.get().uri(path)
        .header(HttpHeaders.COOKIE, SessionCookieService.COOKIE_NAME + "=" + token)
        .exchange().returnResult(String.class);
  }

  @Test
  void apiRoutesRequireASession() {
    EntityExchangeResult<String> response = get("/api/1.0/users");
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatus());
    assertTrue(response.getResponseBody().contains("\"code\":\"UNAUTHENTICATED\""),
        response.getResponseBody());
    assertNull(response.getResponseHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE),
        "no HTTP Basic or Bearer challenge expected");
    // the SPA gets its CSRF cookie even from a 401
    List<String> cookies = response.getResponseHeaders().get(HttpHeaders.SET_COOKIE);
    assertTrue(cookies != null && cookies.stream().anyMatch(c -> c.startsWith("XSRF-TOKEN=")),
        String.valueOf(cookies));
    assertFalse(cookies.stream().anyMatch(c -> c.startsWith("JSESSIONID")), "stateless");
  }

  @Test
  void apiRoutesRequireASessionWhateverTheCaseOfThePath() {
    // the controllers match paths case-insensitively, so the security rules must as well
    EntityExchangeResult<String> response = get("/API/1.0/users");
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatus());
    assertTrue(response.getResponseBody().contains("\"code\":\"UNAUTHENTICATED\""),
        response.getResponseBody());

    response = getWithSessionCookie("/API/1.0/Users", token(2, UserDto.ROLE_STUDENT));
    assertEquals(HttpStatus.FORBIDDEN, response.getStatus());
    assertTrue(response.getResponseBody().contains("\"code\":\"ACCESS_DENIED\""),
        response.getResponseBody());
  }

  @Test
  void aStudentCannotCallAProfessorEndpoint() {
    EntityExchangeResult<String> response =
        getWithSessionCookie("/api/1.0/users", token(2, UserDto.ROLE_STUDENT));
    assertEquals(HttpStatus.FORBIDDEN, response.getStatus());
    assertTrue(response.getResponseBody().contains("\"code\":\"ACCESS_DENIED\""),
        response.getResponseBody());
  }

  @Test
  void unknownApiRoutesAre404ForAnAuthenticatedUser() {
    assertEquals(HttpStatus.UNAUTHORIZED, get("/api/1.0/doesNotExist").getStatus());
    EntityExchangeResult<String> response =
        getWithSessionCookie("/api/1.0/doesNotExist", token(1, UserDto.ROLE_PROFESSOR));
    assertEquals(HttpStatus.NOT_FOUND, response.getStatus());
  }

  @Test
  void stateChangingRequestsWithoutCsrfTokenAreRefused() {
    EntityExchangeResult<String> response = rest.post().uri("/api/1.0/session")
        .contentType(MediaType.APPLICATION_JSON)
        .body("{\"username\":\"a\",\"password\":\"b\"}")
        .exchange().returnResult(String.class);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatus());
    assertTrue(response.getResponseBody().contains("\"code\":\"ACCESS_DENIED\""),
        response.getResponseBody());
  }

  @Test
  void legacyWebUiIsServed() {
    EntityExchangeResult<String> page = get("/");
    assertEquals(HttpStatus.OK, page.getStatus());
    assertTrue(page.getResponseBody().contains("js/app.js"), "index.html expected");

    EntityExchangeResult<byte[]> flag = get("/images/flags/BE.png", byte[].class);
    assertEquals(HttpStatus.OK, flag.getStatus());
  }

  @Test
  void clientSideRoutesServeTheWebUi() {
    for (String route : List.of("/partenaires", "/demandes-de-mobilite", "/mobilites",
        "/etudiants", "/paiements", "/mes-donnees-personnelles", "/connexion")) {
      EntityExchangeResult<String> page = get(route);
      assertEquals(HttpStatus.OK, page.getStatus(), route);
      assertTrue(page.getResponseBody().contains("js/app.js"),
          "index.html expected for " + route);
    }

    assertEquals(HttpStatus.NOT_FOUND, get("/js/doesNotExist.js").getStatus());
  }

  @Test
  void healthIsPublicAndShowsNoDetail() {
    EntityExchangeResult<JsonNode> response = get("/actuator/health", JsonNode.class);
    assertEquals(HttpStatus.OK, response.getStatus());
    JsonNode body = response.getResponseBody();
    assertEquals("UP", body.get("status").asString());
    // show-details=never: nothing about the components (database, disk space) is disclosed
    assertFalse(body.has("components"), body.toString());
    assertFalse(body.has("details"), body.toString());
    assertFalse(response.getResponseHeaders().containsHeader(HttpHeaders.WWW_AUTHENTICATE));
  }

  @Test
  void theHealthProbesArePublic() {
    for (String probe : List.of("/actuator/health/liveness", "/actuator/health/readiness")) {
      EntityExchangeResult<String> response = get(probe);
      assertEquals(HttpStatus.OK, response.getStatus(), probe);
      assertEquals("{\"status\":\"UP\"}", response.getResponseBody(), probe);
    }
  }

  @Test
  void otherActuatorEndpointsRequireASession() {
    for (String path : List.of("/actuator", "/actuator/info", "/actuator/env", "/actuator/metrics",
        "/actuator/beans")) {
      EntityExchangeResult<String> response = get(path);
      assertEquals(HttpStatus.UNAUTHORIZED, response.getStatus(), path);
      assertTrue(response.getResponseBody().contains("\"code\":\"UNAUTHENTICATED\""),
          response.getResponseBody());
    }
  }

  @Test
  void onlyHealthAndInfoAreExposedToASignedInUser() {
    String session = token(1, UserDto.ROLE_PROFESSOR);
    assertEquals(HttpStatus.OK, getWithSessionCookie("/actuator/info", session).getStatus());
    for (String path : List.of("/actuator/env", "/actuator/metrics", "/actuator/beans",
        "/actuator/heapdump", "/actuator/prometheus", "/actuator/loggers")) {
      assertEquals(HttpStatus.NOT_FOUND, getWithSessionCookie(path, session).getStatus(), path);
    }
  }

  @Test
  void theOpenApiDocumentAndTheSwaggerUiArePublic() {
    for (String path : List.of("/v3/api-docs", "/v3/api-docs.yaml", "/v3/api-docs/swagger-config",
        "/swagger-ui/index.html")) {
      EntityExchangeResult<String> response = get(path);
      assertEquals(HttpStatus.OK, response.getStatus(), path);
      assertFalse(response.getResponseHeaders().containsHeader(HttpHeaders.WWW_AUTHENTICATE),
          path);
    }
    // served like the controllers, whatever the case of the path, and public all the same
    assertEquals(HttpStatus.OK, get("/V3/API-DOCS").getStatus());
    // the document describes the API, the API itself still needs a session
    assertEquals(HttpStatus.UNAUTHORIZED, get("/api/1.0/mobilities").getStatus());
  }

  @Test
  void angularDevServerMayCallTheApi() throws Exception {
    // java.net.http directly: the request must carry the browser's Origin header as written here
    HttpClient client = HttpClient.newHttpClient();
    URI session = URI.create("http://localhost:" + port + "/api/1.0/session");

    HttpResponse<String> preflight = client.send(HttpRequest.newBuilder(session)
        .method("OPTIONS", BodyPublishers.noBody())
        .header("Origin", "http://localhost:4200")
        .header("Access-Control-Request-Method", "POST")
        .header("Access-Control-Request-Headers", "content-type,x-xsrf-token")
        .build(), BodyHandlers.ofString());
    assertEquals(200, preflight.statusCode());
    assertEquals("http://localhost:4200",
        preflight.headers().firstValue("Access-Control-Allow-Origin").orElse(null));
    assertEquals("true",
        preflight.headers().firstValue("Access-Control-Allow-Credentials").orElse(null));
    String allowedHeaders =
        preflight.headers().firstValue("Access-Control-Allow-Headers").orElse("").toLowerCase();
    assertTrue(allowedHeaders.contains("x-xsrf-token"), allowedHeaders);

    HttpResponse<String> rejected = client.send(HttpRequest.newBuilder(session)
        .header("Origin", "http://evil.example.com")
        .build(), BodyHandlers.ofString());
    assertEquals(403, rejected.statusCode());
  }
}
