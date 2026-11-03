package com.dragomitch.ipl.pae.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.info.BuildProperties;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.method.HandlerMethod;

/**
 * The parts of {@link OpenApiConfig} that the document served by the application
 * ({@code OpenApiDocumentTest}) does not show: the version without build information and the
 * operations springdoc might hand over without responses or HTTP method.
 */
class OpenApiConfigTest {

  private final OpenApiConfig config = new OpenApiConfig();

  /** Handlers to build operations from. */
  static class Handlers {

    @GetMapping("/public")
    public void publicRead() {
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PROFESSOR')")
    public void change() {
    }

    public void unmapped() {
    }
  }

  private static HandlerMethod handler(String name) throws NoSuchMethodException {
    return new HandlerMethod(new Handlers(), Handlers.class.getMethod(name));
  }

  @Test
  void theVersionIsTheOneOfTheBuild() {
    Properties build = new Properties();
    build.setProperty("version", "1.2.3");
    StaticListableBeanFactory beans = new StaticListableBeanFactory();
    beans.addBean("buildProperties", new BuildProperties(build));

    OpenAPI document = config.openApi(beans.getBeanProvider(BuildProperties.class));

    assertEquals("1.2.3", document.getInfo().getVersion());
    assertEquals(OpenApiConfig.TITLE, document.getInfo().getTitle());
  }

  @Test
  void withoutBuildInformationTheVersionIsDev() {
    OpenAPI document =
        config.openApi(new StaticListableBeanFactory().getBeanProvider(BuildProperties.class));

    assertEquals(OpenApiConfig.DEV_VERSION, document.getInfo().getVersion());
  }

  @Test
  void anOperationWithoutResponsesGetsItsErrorResponses() throws Exception {
    Operation operation = new Operation()
        .addParametersItem(new Parameter().name("id").in("path"));

    OpenApiConfig.customize(operation, handler("change"));

    assertThat(operation.getResponses().keySet())
        .containsExactlyInAnyOrder("400", "401", "403", "404", "409", "422");
    assertThat(operation.getSecurity()).hasSize(1);
    assertThat(operation.getSecurity().get(0).keySet())
        .containsExactly(OpenApiConfig.SESSION_COOKIE, OpenApiConfig.CSRF_HEADER);
  }

  @Test
  void aPublicReadWithoutParametersHasNoErrorResponseAndNoSecurity() throws Exception {
    Operation operation = new Operation()
        .responses(new ApiResponses().addApiResponse("200", new ApiResponse().description("OK")));

    OpenApiConfig.customize(operation, handler("publicRead"));

    assertThat(operation.getResponses().keySet()).containsExactly("200");
    assertThat(operation.getResponses().get("200").getContent()).isNull();
    assertThat(operation.getSecurity()).isEmpty();
  }

  @Test
  void aHandlerWithoutRequestMappingIsTreatedAsARead() throws Exception {
    Operation operation = new Operation().requestBody(
        new io.swagger.v3.oas.models.parameters.RequestBody());

    OpenApiConfig.customize(operation, handler("unmapped"));

    assertThat(operation.getResponses().keySet()).containsExactly("400");
    assertThat(operation.getSecurity()).isEqualTo(List.of());
  }

  @Test
  void aDeclaredErrorResponseIsAProblem() throws Exception {
    Operation operation = new Operation().responses(new ApiResponses()
        .addApiResponse("401", new ApiResponse().description("Bad credentials")));

    OpenApiConfig.customize(operation, handler("publicRead"));

    ApiResponse unauthorized = operation.getResponses().get("401");
    assertEquals("Bad credentials", unauthorized.getDescription());
    assertEquals(OpenApiConfig.PROBLEM_DETAIL_REF, unauthorized.getContent()
        .get("application/problem+json").getSchema().get$ref());
  }
}
