package com.dragomitch.ipl.pae.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dragomitch.ipl.pae.adapter.in.web.ApiPaths;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The OpenAPI document ({@code GET /v3/api-docs}) served by the running application, compared
 * with the request mappings of the controllers: a new endpoint must appear in it. Like
 * {@code ApplicationTests}, it needs no database (same properties, so the same cached context).
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = {
    "spring.jpa.properties.jakarta.persistence.database-product-name=PostgreSQL",
    "spring.jpa.properties.jakarta.persistence.database-major-version=16",
    "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false",
    "management.health.db.enabled=false"})
@AutoConfigureRestTestClient
class OpenApiDocumentTest {

  private static final String DOCUMENT = "/v3/api-docs";
  private static final String PROBLEM_JSON = "application/problem+json";

  @Autowired
  private RestTestClient rest;

  @Autowired
  @Qualifier("requestMappingHandlerMapping")
  private RequestMappingHandlerMapping handlerMapping;

  @LocalServerPort
  private int port;

  /** The document, fetched without a session. */
  private JsonNode document() {
    EntityExchangeResult<String> response =
        rest.get().uri(DOCUMENT).exchange().returnResult(String.class);
    assertEquals(HttpStatus.OK, response.getStatus(), response.getResponseBody());
    // valid JSON, or readTree throws
    return JsonMapper.builder().build().readTree(response.getResponseBody());
  }

  private static JsonNode operation(JsonNode document, String path, String method) {
    JsonNode operation = document.path("paths").path(path).path(method);
    assertThat(operation.isObject()).as("%s %s in the document", method, path).isTrue();
    return operation;
  }

  @Test
  void isAnOpenApi3DocumentOfTheApi() {
    JsonNode document = document();
    assertThat(document.path("openapi").asString()).startsWith("3.");
    assertEquals(OpenApiConfig.TITLE, document.path("info").path("title").asString());
    // the build-info goal runs before the tests: the version of pom.xml
    assertThat(document.path("info").path("version").asString()).isNotBlank();
    // only the REST API: no web UI route, no error page, no actuator endpoint
    document.path("paths").propertyNames()
        .forEach(path -> assertThat(path).startsWith(ApiPaths.BASE + "/"));
  }

  @Test
  void everyEndpointOfTheControllersIsDocumentedWithItsMethods() {
    JsonNode paths = document().path("paths");
    List<String> missing = new ArrayList<>();
    int endpoints = 0;
    for (Map.Entry<RequestMappingInfo, HandlerMethod> entry
        : handlerMapping.getHandlerMethods().entrySet()) {
      // the REST controllers of adapter.in.web, not springdoc's own endpoints
      String controllerPackage = entry.getValue().getBeanType().getPackageName();
      if (!controllerPackage.equals(ApiPaths.class.getPackageName())) {
        continue;
      }
      RequestMappingInfo mapping = entry.getKey();
      Set<RequestMethod> methods = mapping.getMethodsCondition().getMethods();
      assertThat(methods).as("HTTP method of %s", entry.getValue()).isNotEmpty();
      for (String path : mapping.getPathPatternsCondition().getPatternValues()) {
        for (RequestMethod method : methods) {
          endpoints++;
          if (!paths.path(path).has(method.name().toLowerCase(Locale.ROOT))) {
            missing.add(method + " " + path + " (" + entry.getValue() + ")");
          }
        }
      }
    }
    assertThat(missing).as("endpoints missing from the OpenAPI document").isEmpty();
    // the reflection found the controllers (the 45 endpoints of EndpointAuthorizationTest)
    assertThat(endpoints).isGreaterThanOrEqualTo(45);
    int operations = 0;
    for (JsonNode pathItem : paths) {
      operations += pathItem.size();
    }
    assertEquals(endpoints, operations, "an operation of the document has no controller method");
  }

  @Test
  void theProblemDetailSchemaHasTheMembersOfTheApiProblems() {
    JsonNode schema = document().path("components").path("schemas")
        .path(OpenApiConfig.PROBLEM_DETAIL);
    assertThat(schema.isObject()).as("ProblemDetail schema").isTrue();
    Set<String> properties = new TreeSet<>(schema.path("properties").propertyNames());
    assertThat(properties).containsExactlyInAnyOrder("type", "title", "status", "detail",
        "instance", "code", "errors", "timestamp", "errorId");
    assertThat(schema.path("properties").path("code").path("enum").valueStream()
        .map(JsonNode::asString)).contains("VALIDATION_FAILED", "UNAUTHENTICATED",
            "CONCURRENT_MODIFICATION", "USERNAME_TAKEN");
    assertThat(schema.path("properties").path("errors").path("items").path("properties")
        .propertyNames()).containsExactlyInAnyOrder("field", "code", "message");
  }

  @Test
  void theErrorResponsesOfTheSignUpReferenceTheProblemDetailSchema() {
    JsonNode responses = operation(document(), ApiPaths.BASE + "/users", "post")
        .path("responses");
    for (String status : List.of("400", "403", "409", "422")) {
      assertEquals(OpenApiConfig.PROBLEM_DETAIL_REF,
          responses.path(status).path("content").path(PROBLEM_JSON).path("schema")
              .path("$ref").asString(), status);
    }
    // public: no session, hence no 401
    assertThat(responses.has("401")).isFalse();
    assertThat(responses.has("200")).isTrue();
  }

  @Test
  void everyErrorResponseIsAProblem() {
    JsonNode paths = document().path("paths");
    List<String> errors = new ArrayList<>();
    for (String path : paths.propertyNames()) {
      for (String method : paths.path(path).propertyNames()) {
        JsonNode responses = paths.path(path).path(method).path("responses");
        for (String status : responses.propertyNames()) {
          if (status.startsWith("4")) {
            errors.add(method + " " + path + " " + status);
            JsonNode content = responses.path(status).path("content");
            assertEquals(Set.of(PROBLEM_JSON), Set.copyOf(content.propertyNames()),
                method + " " + path + " " + status);
            assertEquals(OpenApiConfig.PROBLEM_DETAIL_REF,
                content.path(PROBLEM_JSON).path("schema").path("$ref").asString());
          }
        }
      }
    }
    assertThat(errors).isNotEmpty();
  }

  @Test
  void theErrorResponsesFollowWhatTheEndpointCanAnswer() {
    JsonNode document = document();
    // a read with a role check and a path variable
    assertThat(operation(document, ApiPaths.BASE + "/mobilities/{id}", "get")
        .path("responses").propertyNames())
        .containsExactlyInAnyOrder("200", "400", "401", "403", "404");
    // a state change
    assertThat(operation(document, ApiPaths.BASE + "/mobilities/{id}/confirmPayment", "put")
        .path("responses").propertyNames())
        .containsExactlyInAnyOrder("200", "400", "401", "403", "404", "409", "422");
    // the public list of options: nothing can go wrong for the client
    assertThat(operation(document, ApiPaths.BASE + "/options", "get")
        .path("responses").propertyNames()).containsExactly("200");
    // the sign-in documents its own 401 (bad credentials) and the session cookie
    JsonNode signin = operation(document, ApiPaths.BASE + "/session", "post").path("responses");
    assertThat(signin.path("401").path("description").asString())
        .contains("INVALID_CREDENTIALS");
    assertEquals(OpenApiConfig.PROBLEM_DETAIL_REF,
        signin.path("401").path("content").path(PROBLEM_JSON).path("schema").path("$ref")
            .asString());
    assertThat(signin.path("200").path("headers").has("Set-Cookie")).isTrue();
    assertThat(signin.path("200").path("content").path("application/json").path("schema")
        .path("$ref").asString()).endsWith("/UserDto");
  }

  @Test
  void theSessionCookieAndTheCsrfHeaderAreTheSecuritySchemes() {
    JsonNode document = document();
    JsonNode schemes = document.path("components").path("securitySchemes");
    JsonNode session = schemes.path(OpenApiConfig.SESSION_COOKIE);
    assertEquals("apiKey", session.path("type").asString());
    assertEquals("cookie", session.path("in").asString());
    assertEquals("session", session.path("name").asString());
    JsonNode csrf = schemes.path(OpenApiConfig.CSRF_HEADER);
    assertEquals("apiKey", csrf.path("type").asString());
    assertEquals("header", csrf.path("in").asString());
    assertEquals("X-XSRF-TOKEN", csrf.path("name").asString());
    // the default of the document
    assertEquals("[{\"sessionCookie\":[]}]", document.path("security").toString());

    assertEquals("[{\"sessionCookie\":[]}]",
        operation(document, ApiPaths.BASE + "/users", "get").path("security").toString());
    assertEquals("[{\"sessionCookie\":[],\"csrfHeader\":[]}]",
        operation(document, ApiPaths.BASE + "/users/edit", "put").path("security").toString());
    // public endpoints: the sign-up and the sign-in need the CSRF token only, the options nothing
    assertEquals("[{\"csrfHeader\":[]}]",
        operation(document, ApiPaths.BASE + "/users", "post").path("security").toString());
    assertEquals("[{\"csrfHeader\":[]}]",
        operation(document, ApiPaths.BASE + "/session", "post").path("security").toString());
    assertEquals("[]",
        operation(document, ApiPaths.BASE + "/options", "get").path("security").toString());
  }

  @Test
  void theSignedInUserIsNotARequestParameter() {
    JsonNode document = document();
    // CurrentUser (id, role) comes from the session, never from the request
    for (JsonNode pathItem : document.path("paths")) {
      for (JsonNode operation : pathItem) {
        for (JsonNode parameter : operation.path("parameters")) {
          assertThat(parameter.path("name").asString()).isNotIn("currentUser", "role");
          assertThat(parameter.path("in").asString()).isIn("path", "query");
        }
      }
    }
    assertThat(operation(document, ApiPaths.BASE + "/mobilities", "get").has("parameters"))
        .isFalse();
    // the search criteria of the partners are two query parameters
    assertThat(operation(document, ApiPaths.BASE + "/partners", "get").path("parameters")
        .valueStream().map(parameter -> parameter.path("name").asString()))
        .containsExactly("filter", "value");
  }

  @Test
  void theDtoPropertiesCarryTheirConstraintsAndDescriptions() {
    JsonNode schemas = document().path("components").path("schemas");
    JsonNode term = schemas.path("MobilityChoiceDto").path("properties").path("term");
    assertEquals(1, term.path("minimum").asInt());
    assertEquals(2, term.path("maximum").asInt());
    assertThat(term.path("description").asString()).contains("first term");
    assertThat(schemas.path("MobilityChoiceDto").path("properties").path("preferenceOrder")
        .path("description").asString()).contains("first choice");
    assertThat(schemas.path("MobilityDto").path("properties").path("version")
        .path("description").asString()).contains("CONCURRENT_MODIFICATION");
    // never sent back by the API
    assertThat(schemas.path("UserDto").path("properties").path("password").path("writeOnly")
        .asBoolean()).isTrue();
  }

  @Test
  void theSwaggerUiRedirectsToItsPage() throws Exception {
    HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    HttpResponse<String> redirect = client.send(HttpRequest.newBuilder(
        URI.create("http://localhost:" + port + "/swagger-ui.html")).build(),
        BodyHandlers.ofString());
    assertThat(redirect.statusCode()).isBetween(300, 399);
    assertThat(redirect.headers().firstValue("Location").orElse(""))
        .endsWith("/swagger-ui/index.html");

    EntityExchangeResult<String> page =
        rest.get().uri("/swagger-ui/index.html").exchange().returnResult(String.class);
    assertEquals(HttpStatus.OK, page.getStatus());
    assertThat(page.getResponseBody()).contains("swagger-ui");

    // the UI reads the document from the configuration it is served
    EntityExchangeResult<JsonNode> config =
        rest.get().uri(DOCUMENT + "/swagger-config").exchange().returnResult(JsonNode.class);
    assertEquals(HttpStatus.OK, config.getStatus());
    assertEquals(DOCUMENT, config.getResponseBody().path("url").asString());
  }
}
