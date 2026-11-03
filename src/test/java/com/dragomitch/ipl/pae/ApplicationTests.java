package com.dragomitch.ipl.pae;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dragomitch.ipl.pae.domain.model.EntityFactory;
import com.dragomitch.ipl.pae.domain.dto.UserDto;
import com.dragomitch.ipl.pae.adapter.in.web.security.SessionCookieService;

import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

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
class ApplicationTests {

  @Autowired
  private TestRestTemplate rest;

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

  private HttpEntity<Void> withSessionCookie(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.COOKIE, SessionCookieService.COOKIE_NAME + "=" + token);
    return new HttpEntity<>(headers);
  }

  @Test
  void apiRoutesRequireASession() {
    ResponseEntity<String> response = rest.getForEntity("/api/1.0/users", String.class);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertTrue(response.getBody().contains("\"code\":\"UNAUTHENTICATED\""), response.getBody());
    assertNull(response.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE),
        "no HTTP Basic or Bearer challenge expected");
    // the SPA gets its CSRF cookie even from a 401
    List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
    assertTrue(cookies != null && cookies.stream().anyMatch(c -> c.startsWith("XSRF-TOKEN=")),
        String.valueOf(cookies));
    assertFalse(cookies.stream().anyMatch(c -> c.startsWith("JSESSIONID")), "stateless");
  }

  @Test
  void aStudentCannotCallAProfessorEndpoint() {
    ResponseEntity<String> response = rest.exchange("/api/1.0/users", HttpMethod.GET,
        withSessionCookie(token(2, UserDto.ROLE_STUDENT)), String.class);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertTrue(response.getBody().contains("\"code\":\"ACCESS_DENIED\""), response.getBody());
  }

  @Test
  void unknownApiRoutesAre404ForAnAuthenticatedUser() {
    assertEquals(HttpStatus.UNAUTHORIZED,
        rest.getForEntity("/api/1.0/doesNotExist", String.class).getStatusCode());
    ResponseEntity<String> response = rest.exchange("/api/1.0/doesNotExist", HttpMethod.GET,
        withSessionCookie(token(1, UserDto.ROLE_PROFESSOR)), String.class);
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void stateChangingRequestsWithoutCsrfTokenAreRefused() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    ResponseEntity<String> response = rest.postForEntity("/api/1.0/session",
        new HttpEntity<>("{\"username\":\"a\",\"password\":\"b\"}", headers), String.class);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertTrue(response.getBody().contains("\"code\":\"ACCESS_DENIED\""), response.getBody());
  }

  @Test
  void legacyWebUiIsServed() {
    ResponseEntity<String> page = rest.getForEntity("/", String.class);
    assertEquals(HttpStatus.OK, page.getStatusCode());
    assertTrue(page.getBody().contains("js/app.js"), "index.html expected");

    ResponseEntity<byte[]> flag = rest.getForEntity("/images/flags/BE.png", byte[].class);
    assertEquals(HttpStatus.OK, flag.getStatusCode());
  }

  @Test
  void clientSideRoutesServeTheWebUi() {
    for (String route : List.of("/partenaires", "/demandes-de-mobilite", "/mobilites",
        "/etudiants", "/paiements", "/mes-donnees-personnelles", "/connexion")) {
      ResponseEntity<String> page = rest.getForEntity(route, String.class);
      assertEquals(HttpStatus.OK, page.getStatusCode(), route);
      assertTrue(page.getBody().contains("js/app.js"), "index.html expected for " + route);
    }

    assertEquals(HttpStatus.NOT_FOUND,
        rest.getForEntity("/js/doesNotExist.js", String.class).getStatusCode());
  }

  @Test
  void healthIsPublicAndShowsNoDetail() {
    ResponseEntity<JsonNode> response = rest.getForEntity("/actuator/health", JsonNode.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("UP", response.getBody().get("status").asText());
    // show-details=never: nothing about the components (database, disk space) is disclosed
    assertFalse(response.getBody().has("components"), response.getBody().toString());
    assertFalse(response.getBody().has("details"), response.getBody().toString());
    assertFalse(response.getHeaders().containsKey(HttpHeaders.WWW_AUTHENTICATE));
  }

  @Test
  void theHealthProbesArePublic() {
    for (String probe : List.of("/actuator/health/liveness", "/actuator/health/readiness")) {
      ResponseEntity<String> response = rest.getForEntity(probe, String.class);
      assertEquals(HttpStatus.OK, response.getStatusCode(), probe);
      assertEquals("{\"status\":\"UP\"}", response.getBody(), probe);
    }
  }

  @Test
  void otherActuatorEndpointsRequireASession() {
    for (String path : List.of("/actuator", "/actuator/info", "/actuator/env", "/actuator/metrics",
        "/actuator/beans")) {
      ResponseEntity<String> response = rest.getForEntity(path, String.class);
      assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(), path);
      assertTrue(response.getBody().contains("\"code\":\"UNAUTHENTICATED\""), response.getBody());
    }
  }

  @Test
  void onlyHealthAndInfoAreExposedToASignedInUser() {
    HttpEntity<Void> session = withSessionCookie(token(1, UserDto.ROLE_PROFESSOR));
    assertEquals(HttpStatus.OK,
        rest.exchange("/actuator/info", HttpMethod.GET, session, String.class).getStatusCode());
    for (String path : List.of("/actuator/env", "/actuator/metrics", "/actuator/beans",
        "/actuator/heapdump", "/actuator/prometheus", "/actuator/loggers")) {
      assertEquals(HttpStatus.NOT_FOUND,
          rest.exchange(path, HttpMethod.GET, session, String.class).getStatusCode(), path);
    }
  }

  @Test
  void angularDevServerMayCallTheApi() throws Exception {
    // java.net.http is used because HttpURLConnection (TestRestTemplate) drops the Origin header
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
