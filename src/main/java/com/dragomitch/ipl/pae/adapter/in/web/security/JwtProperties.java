package com.dragomitch.ipl.pae.adapter.in.web.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Settings of the session JWT ({@code app.jwt.*}). The application does not start without a valid
 * secret.
 *
 * @param secret the key that signs and verifies the session JWT with HMAC-SHA256 ({@code
 *     app.jwt.secret}, read from the {@code JWT_SECRET} environment variable by {@code
 *     application.properties}): at least 32 characters, which are at least the 32 bytes that HS256
 *     requires. Generate one with {@code openssl rand -hex 32} and keep it stable across restarts
 *     and replicas, otherwise every session is invalidated.
 */
@Validated
@ConfigurationProperties("app.jwt")
public record JwtProperties(
    @NotBlank(message = "must be set: define the JWT_SECRET environment variable")
        @Size(
            min = 32,
            message =
                "must be at least {min} characters long: set JWT_SECRET to a random string such as"
                    + " the output of 'openssl rand -hex 32'")
        String secret) {}
