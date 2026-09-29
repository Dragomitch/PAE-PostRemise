package com.dragomitch.ipl.pae.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;

/** JWT signing key handling and password hashing of {@link SecurityConfig}. */
class SecurityConfigTest {

  private static final String SECRET = "0123456789abcdef0123456789abcdef"; // 32 bytes

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   ", "\t\n"})
  void withoutASecretARandom256BitKeyIsGenerated(String secret) {
    SecretKey first = SecurityConfig.buildKey(secret);
    SecretKey second = SecurityConfig.buildKey(secret);

    assertThat(first.getAlgorithm()).isEqualTo("HmacSHA256");
    assertThat(first.getEncoded()).hasSize(32);
    assertThat(first.getEncoded()).isNotEqualTo(second.getEncoded());
  }

  @ParameterizedTest
  @ValueSource(strings = {SECRET, SECRET + "and-some-more-bytes"})
  void aSecretOfAtLeast32BytesIsUsedAsIs(String secret) {
    SecretKey key = SecurityConfig.buildKey(secret);

    assertThat(key.getAlgorithm()).isEqualTo("HmacSHA256");
    assertThat(key.getEncoded()).isEqualTo(secret.getBytes(StandardCharsets.UTF_8));
  }

  @ParameterizedTest
  @ValueSource(strings = {"short", "0123456789abcdef0123456789abcde"}) // 5 and 31 bytes
  void aSecretShorterThan32BytesIsRejected(String secret) {
    assertThatThrownBy(() -> SecurityConfig.buildKey(secret))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("32 bytes");
  }

  @Test
  void theLengthIsCountedInUtf8Bytes() {
    // 16 characters but 32 bytes in UTF-8
    String accented = "éééééééééééééééé";

    assertThat(SecurityConfig.buildKey(accented).getEncoded()).hasSize(32);
  }

  private static Jwt sign(SecurityConfig config, String subject) {
    JwtClaimsSet claims = JwtClaimsSet.builder().subject(subject)
        .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
    return config.jwtEncoder().encode(JwtEncoderParameters.from(
        JwsHeader.with(MacAlgorithm.HS256).build(), claims));
  }

  @Test
  void tokensSignedWithTheConfiguredSecretAreDecodedWithIt() {
    SecurityConfig config = new SecurityConfig(SECRET);

    Jwt token = sign(config, "42");

    assertThat(config.jwtDecoder().decode(token.getTokenValue()).getSubject()).isEqualTo("42");
    // same secret after a restart: still valid
    assertThat(new SecurityConfig(SECRET).jwtDecoder().decode(token.getTokenValue())
        .getSubject()).isEqualTo("42");
  }

  @Test
  void tokensSignedWithAnotherKeyAreRejected() {
    Jwt token = sign(new SecurityConfig(SECRET), "42");

    assertThatThrownBy(() -> new SecurityConfig("").jwtDecoder().decode(token.getTokenValue()))
        .isInstanceOf(BadJwtException.class);
    assertThatThrownBy(() -> new SecurityConfig(SECRET.toUpperCase()).jwtDecoder()
        .decode(token.getTokenValue())).isInstanceOf(BadJwtException.class);
  }

  @Test
  void passwordsAreHashedWithBcrypt() {
    PasswordEncoder encoder = new SecurityConfig(SECRET).passwordEncoder();

    String hash = encoder.encode("secret");

    assertThat(hash).startsWith("$2a$").isNotEqualTo(encoder.encode("secret"));
    assertThat(encoder.matches("secret", hash)).isTrue();
    assertThat(encoder.matches("Secret", hash)).isFalse();
  }
}
