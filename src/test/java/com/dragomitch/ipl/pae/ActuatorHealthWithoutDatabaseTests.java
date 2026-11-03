package com.dragomitch.ipl.pae;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * The database indicator of {@code /actuator/health} is part of the status the container health
 * check reads: with the database unreachable (nothing listens on port 1) the endpoint answers 503
 * {@code DOWN}, still without any detail, and still without a session. The liveness probe stays UP:
 * a database outage is no reason to restart the application.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = {
    "spring.jpa.properties.jakarta.persistence.database-product-name=PostgreSQL",
    "spring.jpa.properties.jakarta.persistence.database-major-version=16",
    "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false",
    "spring.datasource.url=jdbc:postgresql://localhost:1/unreachable",
    // the smallest timeout Hikari accepts, so that the check fails at once
    "spring.datasource.hikari.connection-timeout=250"})
class ActuatorHealthWithoutDatabaseTests {

  @Autowired
  private TestRestTemplate rest;

  @Test
  void theDatabaseCountsInTheHealthStatus() {
    ResponseEntity<JsonNode> response = rest.getForEntity("/actuator/health", JsonNode.class);
    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    assertEquals("DOWN", response.getBody().get("status").asText());
    assertFalse(response.getBody().has("components"), response.getBody().toString());
    assertFalse(response.getBody().has("details"), response.getBody().toString());
  }

  @Test
  void theLivenessProbeDoesNotDependOnTheDatabase() {
    ResponseEntity<String> response =
        rest.getForEntity("/actuator/health/liveness", String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("{\"status\":\"UP\"}", response.getBody());
  }
}
