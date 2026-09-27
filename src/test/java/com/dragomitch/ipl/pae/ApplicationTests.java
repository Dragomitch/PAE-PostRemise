package com.dragomitch.ipl.pae;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Boots the whole application (real beans, embedded server) and checks the HTTP wiring. None of
 * the requests reach the database, so no PostgreSQL instance is needed.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ApplicationTests {

  @Autowired
  private TestRestTemplate rest;

  @Test
  void unknownApiRouteIsAnsweredByTheRoutingServlet() {
    ResponseEntity<String> response = rest.getForEntity("/api/1.0/doesNotExist", String.class);
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void apiRoutesStillRequireASession() {
    // Spring Security must not answer first: the 401 comes from the route's @Role check.
    ResponseEntity<String> response = rest.getForEntity("/api/1.0/users", String.class);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertTrue(response.getHeaders().getFirst("WWW-Authenticate") == null,
        "no HTTP Basic challenge expected");
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
    ResponseEntity<String> page = rest.getForEntity("/partenaires", String.class);
    assertEquals(HttpStatus.OK, page.getStatusCode());
    assertTrue(page.getBody().contains("js/app.js"), "index.html expected");

    assertEquals(HttpStatus.NOT_FOUND,
        rest.getForEntity("/js/doesNotExist.js", String.class).getStatusCode());
  }
}
