package com.dragomitch.ipl.pae.adapter.in.web.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** Binding of {@link CorsProperties} ({@code app.cors.*}). */
class CorsPropertiesTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(Config.class);

  @EnableConfigurationProperties(CorsProperties.class)
  static class Config {}

  @Test
  void withoutAPropertyTheAngularDevServerIsAllowed() {
    runner.run(context ->
        assertThat(context.getBean(CorsProperties.class).allowedOrigins())
            .containsExactly("http://localhost:4200"));
  }

  @Test
  void severalOriginsAreReadFromACommaSeparatedValue() {
    runner
        .withPropertyValues(
            "app.cors.allowed-origins=http://localhost:4200, https://ema.example.org,http://example.test")
        .run(context ->
            assertThat(context.getBean(CorsProperties.class).allowedOrigins())
                .containsExactly(
                    "http://localhost:4200", "https://ema.example.org", "http://example.test"));
  }

  @Test
  void aSingleOriginReplacesTheDefault() {
    runner
        .withPropertyValues("app.cors.allowed-origins=https://ema.example.org")
        .run(context ->
            assertThat(context.getBean(CorsProperties.class).allowedOrigins())
                .containsExactly("https://ema.example.org"));
  }
}
