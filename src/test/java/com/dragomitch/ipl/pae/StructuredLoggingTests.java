package com.dragomitch.ipl.pae;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.dragomitch.ipl.pae.adapter.in.web.logging.TraceIdResponseFilter;
import com.dragomitch.ipl.pae.domain.port.in.OptionService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The opt-in structured console output: with {@code logging.structured.format.console=ecs} (what
 * {@code LOGGING_STRUCTURED_FORMAT=ecs} sets, as docker-compose.yml does) every line is one JSON
 * object of the Elastic Common Schema, and the lines of a request carry its trace id in {@code
 * trace.id}. Same application without a database as {@link LoggingCorrelationTests}.
 *
 * <p>Logback is configured once per JVM, by the last application context that started: the context
 * of this class is closed after it ({@link DirtiesContext}) and the plain console pattern is put
 * back, so that the test contexts cached before this one do not log JSON afterwards.
 */
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.jpa.properties.jakarta.persistence.database-product-name=PostgreSQL",
      "spring.jpa.properties.jakarta.persistence.database-major-version=16",
      "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false",
      "spring.flyway.enabled=false",
      "management.health.db.enabled=false",
      "logging.level.com.dragomitch.ipl.pae.adapter.in.web.logging=DEBUG",
      "logging.structured.format.console=ecs"
    })
@AutoConfigureRestTestClient
@ExtendWith(OutputCaptureExtension.class)
@DirtiesContext
class StructuredLoggingTests {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Autowired private RestTestClient rest;

  @MockitoBean private OptionService optionService;

  @AfterAll
  static void restoreThePlainConsolePattern() {
    LoggingSystem logging = LoggingSystem.get(StructuredLoggingTests.class.getClassLoader());
    logging.cleanUp();
    logging.beforeInitialize();
    StandardEnvironment environment = new StandardEnvironment();
    environment
        .getPropertySources()
        .addFirst(
            new MapPropertySource(
                "plainLogging",
                Map.of(
                    "spring.application.name",
                    "pae-gestionnaire-erasmus",
                    LoggingSystem.EXPECT_CORRELATION_ID_PROPERTY,
                    "true")));
    logging.initialize(new LoggingInitializationContext(environment), null, null);
  }

  @Test
  void eachLineOfARequestIsAnEcsJsonObjectWithItsTraceId(CapturedOutput output) {
    when(optionService.showAll()).thenReturn(List.of());

    EntityExchangeResult<String> response =
        rest.get().uri("/api/1.0/options").exchange().returnResult(String.class);

    assertEquals(HttpStatus.OK, response.getStatus());
    String traceId = response.getResponseHeaders().getFirst(TraceIdResponseFilter.HEADER);
    assertNotNull(traceId);
    List<JsonNode> lines =
        output
            .getOut()
            .lines()
            .filter(line -> line.contains(traceId))
            .map(line -> JSON.readTree(line))
            .toList();
    assertFalse(lines.isEmpty(), output.getOut());
    JsonNode request =
        lines.stream()
            .filter(line -> text(line, "message").startsWith("GET /api/1.0/options 200 in "))
            .findFirst()
            .orElseThrow(() -> new AssertionError(output.getOut()));
    assertEquals("DEBUG", text(request, "log.level"));
    assertEquals(traceId, text(request, "trace.id"));
    assertTrue(text(request, "span.id").matches("[0-9a-f]{16}"), request.toString());
    assertTrue(text(request, "@timestamp").matches("\\d{4}-\\d\\d-\\d\\dT.*"), request.toString());
    assertEquals(
        TraceIdResponseFilter.class.getName(), text(request, "log.logger"), request.toString());
  }

  @Test
  void everyLineIsOneJsonObject(CapturedOutput output) {
    when(optionService.showAll()).thenReturn(List.of());
    rest.get().uri("/api/1.0/options").exchange().returnResult(Void.class);
    for (String line : output.getOut().lines().filter(l -> !l.isBlank()).toList()) {
      JsonNode node = JSON.readTree(line);
      assertTrue(node.isObject(), line);
      assertFalse(text(node, "@timestamp").isEmpty(), line);
      assertFalse(text(node, "log.level").isEmpty(), line);
      assertTrue(node.has("message"), line);
    }
  }

  /**
   * A member of an ECS line, given by its dotted name: Spring Boot writes the nested form ({@code
   * "log": {"level": ...}}), the dotted one ({@code "log.level"}) is accepted as well.
   */
  private static String text(JsonNode line, String name) {
    JsonNode flat = line.get(name);
    if (flat != null) {
      return flat.asString();
    }
    JsonNode node = line;
    for (String part : name.split("\\.")) {
      node = node.path(part);
    }
    return node.isMissingNode() ? "" : node.asString();
  }
}
