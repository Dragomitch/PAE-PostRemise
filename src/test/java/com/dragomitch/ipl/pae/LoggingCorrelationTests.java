package com.dragomitch.ipl.pae;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.dragomitch.ipl.pae.adapter.in.web.logging.TraceIdResponseFilter;
import com.dragomitch.ipl.pae.adapter.in.web.security.SessionCookieService;
import com.dragomitch.ipl.pae.domain.model.Option;
import com.dragomitch.ipl.pae.domain.model.Role;
import com.dragomitch.ipl.pae.domain.model.User;
import com.dragomitch.ipl.pae.domain.port.in.OptionService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.JsonNode;

/**
 * Request correlation (roadmap S18) on the whole application, without a database as in {@link
 * ApplicationTests}: every response answers the trace id of its request in {@code X-Trace-Id}, the
 * log lines of the request carry the same id (Spring Boot's log correlation), an incoming W3C
 * {@code traceparent} is continued, and the errors are logged with their trace id. The option use
 * case is mocked so that {@code GET /api/1.0/options} answers without PostgreSQL; the request log
 * of {@link TraceIdResponseFilter} is raised to DEBUG to have a line of a successful request.
 */
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.jpa.properties.jakarta.persistence.database-product-name=PostgreSQL",
      "spring.jpa.properties.jakarta.persistence.database-major-version=16",
      "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false",
      "spring.flyway.enabled=false",
      "management.health.db.enabled=false",
      "logging.level.com.dragomitch.ipl.pae.adapter.in.web.logging=DEBUG"
    })
@AutoConfigureRestTestClient
@ExtendWith(OutputCaptureExtension.class)
class LoggingCorrelationTests {

  private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";

  @Autowired private RestTestClient rest;

  @Autowired private SessionCookieService sessionCookieService;

  @MockitoBean private OptionService optionService;

  private static String traceIdOf(EntityExchangeResult<?> response) {
    String traceId = response.getResponseHeaders().getFirst(TraceIdResponseFilter.HEADER);
    assertNotNull(traceId, "X-Trace-Id expected");
    assertTrue(traceId.matches("[0-9a-f]{32}"), traceId);
    return traceId;
  }

  /** The captured lines containing a text. */
  static List<String> linesWith(CapturedOutput output, String text) {
    return output.getOut().lines().filter(line -> line.contains(text)).toList();
  }

  @Test
  void theLogLinesOfARequestCarryTheTraceIdOfItsResponse(CapturedOutput output) {
    Option option = new Option();
    option.setCode("INF");
    option.setName("Informatique");
    when(optionService.showAll()).thenReturn(List.of(option));

    EntityExchangeResult<String> response =
        rest.get().uri("/api/1.0/options").exchange().returnResult(String.class);

    assertEquals(HttpStatus.OK, response.getStatus());
    String traceId = traceIdOf(response);
    List<String> lines = linesWith(output, traceId);
    assertTrue(
        lines.stream().anyMatch(line -> line.contains("GET /api/1.0/options 200")),
        output.getOut());
    // the correlation of the default pattern: [traceId-spanId]
    assertTrue(
        lines.stream().anyMatch(line -> line.contains("[" + traceId + "-")),
        String.join("\n", lines));
  }

  @Test
  void eachRequestGetsItsOwnTraceId() {
    when(optionService.showAll()).thenReturn(List.of());
    String first =
        traceIdOf(rest.get().uri("/api/1.0/options").exchange().returnResult(Void.class));
    String second =
        traceIdOf(rest.get().uri("/api/1.0/options").exchange().returnResult(Void.class));
    assertTrue(!first.equals(second), first);
  }

  @Test
  void theTraceOfAW3cTraceparentIsContinued(CapturedOutput output) {
    when(optionService.showAll()).thenReturn(List.of());

    EntityExchangeResult<String> response =
        rest.get()
            .uri("/api/1.0/options")
            .header("traceparent", "00-" + TRACE_ID + "-00f067aa0ba902b7-01")
            .exchange()
            .returnResult(String.class);

    assertEquals(TRACE_ID, traceIdOf(response));
    assertTrue(
        linesWith(output, TRACE_ID).stream()
            .anyMatch(line -> line.contains("GET /api/1.0/options 200")),
        output.getOut());
  }

  @Test
  void aRequestRefusedByTheSecurityChainAnswersItsTraceIdAndLogsIt(CapturedOutput output) {
    EntityExchangeResult<String> response =
        rest.get().uri("/api/1.0/users").exchange().returnResult(String.class);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatus());
    String traceId = traceIdOf(response);
    assertTrue(
        linesWith(output, "(trace " + traceId + ") on /api/1.0/users: 401").size() == 1,
        output.getOut());
  }

  @Test
  void aClientErrorIsLoggedWithItsTraceId(CapturedOutput output) {
    User professor = new User();
    professor.setId(1);
    professor.setRole(Role.PROFESSOR);
    EntityExchangeResult<String> response =
        rest.get()
            .uri("/api/1.0/doesNotExist")
            .header(
                HttpHeaders.COOKIE,
                SessionCookieService.COOKIE_NAME
                    + "="
                    + sessionCookieService.createToken(professor))
            .exchange()
            .returnResult(String.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatus());
    String traceId = traceIdOf(response);
    List<String> lines = linesWith(output, "(trace " + traceId + ") on /api/1.0/doesNotExist: 404");
    assertEquals(1, lines.size(), output.getOut());
    assertTrue(lines.get(0).contains(" INFO "), lines.get(0));
  }

  @Test
  void aServerErrorIsLoggedWithItsErrorIdAndItsTraceId(CapturedOutput output) {
    when(optionService.showAll()).thenThrow(new IllegalStateException("boom"));

    EntityExchangeResult<JsonNode> response =
        rest.get().uri("/api/1.0/options").exchange().returnResult(JsonNode.class);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatus());
    String traceId = traceIdOf(response);
    String errorId = response.getResponseBody().get("errorId").asString();
    // the problem keeps its members: the trace id is in the header, not in the body
    assertTrue(!response.getResponseBody().has("traceId"), response.getResponseBody().toString());
    List<String> lines =
        linesWith(output, "Error " + errorId + " (trace " + traceId + ") on /api/1.0/options: 500");
    assertEquals(1, lines.size(), output.getOut());
    assertTrue(lines.get(0).contains(" ERROR "), lines.get(0));
    assertTrue(lines.get(0).contains("[" + traceId + "-"), lines.get(0));
  }
}
