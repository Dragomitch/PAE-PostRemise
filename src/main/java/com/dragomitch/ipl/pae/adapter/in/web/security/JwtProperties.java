package com.dragomitch.ipl.pae.adapter.in.web.security;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Settings of the session JWT ({@code app.jwt.*}). The application does not start without a valid
 * secret.
 *
 * @param secret the key that signs and verifies the session JWT with HMAC-SHA256 ({@code
 *     app.jwt.secret}, read from the {@code JWT_SECRET} environment variable by {@code
 *     application.properties}): at least 32 characters (the value is never printed when it is too
 *     short), which are at least the 32 bytes that HS256 requires. Generate one with {@code openssl
 *     rand -hex 32} and keep it stable across restarts and replicas, otherwise every session is
 *     invalidated.
 */
@Validated
@ConfigurationProperties("app.jwt")
public record JwtProperties(
    @NotBlank(message = "must be set: define the JWT_SECRET environment variable") String secret) {

  /** The minimum length of the secret, in characters (at least the 32 bytes HS256 requires). */
  public static final int MIN_SECRET_LENGTH = 32;

  /**
   * Length rule of the secret. It is a constraint on this method rather than {@code @Size} on the
   * component because Spring Boot prints the rejected value of a failed constraint in the start-up
   * failure, and a secret that is one character short must not end up in the log. A blank secret is
   * left to {@code @NotBlank}.
   *
   * @return whether the secret is blank (reported by {@code @NotBlank}) or long enough
   */
  @AssertTrue(
      message =
          "app.jwt.secret must be at least 32 characters long: set JWT_SECRET to a random string"
              + " such as the output of 'openssl rand -hex 32'")
  boolean isSecretLongEnough() {
    return secret == null || secret.isBlank() || secret.length() >= MIN_SECRET_LENGTH;
  }
}
