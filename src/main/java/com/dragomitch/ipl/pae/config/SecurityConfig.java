package com.dragomitch.ipl.pae.config;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration
public class SecurityConfig {

  private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

  private static final String HMAC_ALGORITHM = "HmacSHA256";
  private static final int MIN_SECRET_BYTES = 32;

  private final SecretKey jwtKey;

  public SecurityConfig(@Value("${app.jwt.secret:}") String jwtSecret) {
    this.jwtKey = buildKey(jwtSecret);
  }

  static SecretKey buildKey(String secret) {
    byte[] bytes;
    if (secret == null || secret.isBlank()) {
      logger.warn("app.jwt.secret (JWT_SECRET) is not set: using a random key, "
          + "sessions will not survive a restart. Set JWT_SECRET outside local development.");
      bytes = new byte[MIN_SECRET_BYTES];
      new SecureRandom().nextBytes(bytes);
    } else {
      bytes = secret.getBytes(StandardCharsets.UTF_8);
      if (bytes.length < MIN_SECRET_BYTES) {
        throw new IllegalStateException(
            "app.jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes long for HS256");
      }
    }
    return new SecretKeySpec(bytes, HMAC_ALGORITHM);
  }

  /**
   * Authentication and role checks are performed by the legacy presentation layer
   * ({@code SessionManager} JWT cookie + {@code @Role} on routes), so Spring Security must not
   * put its default HTTP Basic login in front of the API and the web UI.
   *
   * @param http the security builder
   * @return the filter chain
   * @throws Exception if the chain cannot be built
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public JwtEncoder jwtEncoder() {
    return new NimbusJwtEncoder(new ImmutableSecret<>(jwtKey));
  }

  @Bean
  public JwtDecoder jwtDecoder() {
    return NimbusJwtDecoder.withSecretKey(jwtKey).build();
  }
}
