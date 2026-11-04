package com.dragomitch.ipl.pae.adapter.in.web.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;

/** Binding and validation of {@link JwtProperties} ({@code app.jwt.*}). */
class JwtPropertiesTest {

  private static final String SECRET_32 = "0123456789abcdef0123456789abcdef";

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
          .withUserConfiguration(Config.class);

  @EnableConfigurationProperties(JwtProperties.class)
  static class Config {}

  @Test
  void aSecretOf32CharactersIsBound() {
    runner
        .withPropertyValues("app.jwt.secret=" + SECRET_32)
        .run(context -> {
          assertThat(context).hasNotFailed();
          assertThat(context.getBean(JwtProperties.class).secret()).isEqualTo(SECRET_32);
        });
  }

  @Test
  void aLongerSecretIsBound() {
    runner
        .withPropertyValues("app.jwt.secret=" + SECRET_32 + SECRET_32)
        .run(context -> assertThat(context).hasNotFailed());
  }

  @Test
  void aMissingSecretRefusesToStartAndNamesJwtSecret() {
    runner.run(context -> {
      assertThat(context).hasFailed();
      assertThat(context.getStartupFailure())
          .rootCause()
          .hasMessageContaining("app.jwt.secret")
          .hasMessageContaining("define the JWT_SECRET environment variable");
    });
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  void aBlankSecretRefusesToStartAndNamesJwtSecret(String secret) {
    runner
        .withPropertyValues("app.jwt.secret=" + secret)
        .run(context -> {
          assertThat(context).hasFailed();
          assertThat(context.getStartupFailure())
              .rootCause()
              .hasMessageContaining("app.jwt.secret")
              .hasMessageContaining("must be set: define the JWT_SECRET environment variable");
        });
  }

  @Test
  void aSecretOf31CharactersRefusesToStart() {
    runner
        .withPropertyValues("app.jwt.secret=" + SECRET_32.substring(1))
        .run(context -> {
          assertThat(context).hasFailed();
          assertThat(context.getStartupFailure())
              .rootCause()
              .hasMessageContaining("app.jwt.secret")
              .hasMessageContaining(
                  "must be at least 32 characters long: set JWT_SECRET to a random string");
        });
  }
}
