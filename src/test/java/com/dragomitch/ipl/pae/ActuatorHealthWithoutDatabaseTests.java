package com.dragomitch.ipl.pae;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.JsonNode;

/**
 * The database indicator of {@code /actuator/health} is part of the status the container health
 * check reads: with the database unreachable (nothing listens on port 1) the endpoint answers 503
 * {@code DOWN}, still without any detail, and still without a session. The liveness probe stays UP:
 * a database outage is no reason to restart the application.
 */
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.jpa.properties.jakarta.persistence.database-product-name=PostgreSQL",
      "spring.jpa.properties.jakarta.persistence.database-major-version=16",
      "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false",
      // no database: Flyway would fail the start-up (FlywayBaselineIT runs the migrations)
      "spring.flyway.enabled=false",
      "spring.datasource.url=jdbc:postgresql://localhost:1/unreachable",
      // the smallest timeout Hikari accepts, so that the check fails at once
      "spring.datasource.hikari.connection-timeout=250"
    })
@AutoConfigureRestTestClient
class ActuatorHealthWithoutDatabaseTests {

  @Autowired private RestTestClient rest;

  @Test
  void theDatabaseCountsInTheHealthStatus() {
    EntityExchangeResult<JsonNode> response =
        rest.get().uri("/actuator/health").exchange().returnResult(JsonNode.class);
    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatus());
    JsonNode body = response.getResponseBody();
    assertEquals("DOWN", body.get("status").asString());
    assertFalse(body.has("components"), body.toString());
    assertFalse(body.has("details"), body.toString());
  }

  @Test
  void theLivenessProbeDoesNotDependOnTheDatabase() {
    EntityExchangeResult<String> response =
        rest.get().uri("/actuator/health/liveness").exchange().returnResult(String.class);
    assertEquals(HttpStatus.OK, response.getStatus());
    assertEquals("{\"status\":\"UP\"}", response.getResponseBody());
  }
}
