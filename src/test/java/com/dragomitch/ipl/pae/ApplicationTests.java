package com.dragomitch.ipl.pae;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
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

  @LocalServerPort
  private int port;

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

  @Test
  void angularDevServerMayCallTheLegacyApi() throws Exception {
    // java.net.http is used because HttpURLConnection (TestRestTemplate) drops the Origin header
    HttpClient client = HttpClient.newHttpClient();
    URI session = URI.create("http://localhost:" + port + "/api/1.0/session");

    HttpResponse<String> preflight = client.send(HttpRequest.newBuilder(session)
        .method("OPTIONS", BodyPublishers.noBody())
        .header("Origin", "http://localhost:4200")
        .header("Access-Control-Request-Method", "POST")
        .build(), BodyHandlers.ofString());
    assertEquals(200, preflight.statusCode());
    assertEquals("http://localhost:4200",
        preflight.headers().firstValue("Access-Control-Allow-Origin").orElse(null));

    HttpResponse<String> rejected = client.send(HttpRequest.newBuilder(session)
        .header("Origin", "http://evil.example.com")
        .build(), BodyHandlers.ofString());
    assertEquals(403, rejected.statusCode());
  }
}
