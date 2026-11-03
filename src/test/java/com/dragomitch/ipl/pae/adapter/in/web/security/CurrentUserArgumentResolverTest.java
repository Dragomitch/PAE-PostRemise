package com.dragomitch.ipl.pae.adapter.in.web.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dragomitch.ipl.pae.domain.model.Role;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** The current user is read from the subject and the role claim of the session JWT. */
class CurrentUserArgumentResolverTest {

  private static JwtAuthenticationToken session(String subject, String role) {
    Jwt.Builder jwt =
        Jwt.withTokenValue("token")
            .header("alg", "HS256")
            .subject(subject)
            .issuedAt(Instant.EPOCH)
            .expiresAt(Instant.EPOCH.plusSeconds(60));
    if (role != null) {
      jwt.claim(CurrentUser.ROLE_CLAIM, role);
    }
    return new JwtAuthenticationToken(jwt.build());
  }

  @Test
  void theIdAndTheRoleComeFromTheJwt() {
    assertThat(CurrentUserArgumentResolver.fromAuthentication(session("42", "Professor")))
        .isEqualTo(new CurrentUser(42, Role.PROFESSOR));
    assertThat(CurrentUserArgumentResolver.fromAuthentication(session("7", "Student")))
        .isEqualTo(new CurrentUser(7, Role.STUDENT));
  }

  @Test
  void anotherAuthenticationIsRefused() {
    assertThatThrownBy(() -> CurrentUserArgumentResolver.fromAuthentication(null))
        .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    assertThatThrownBy(() ->
            CurrentUserArgumentResolver.fromAuthentication(
                new UsernamePasswordAuthenticationToken("jdoe", "secret")))
        .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
  }

  @Test
  void aSubjectThatIsNotAnIdIsRefused() {
    assertThatThrownBy(
            () -> CurrentUserArgumentResolver.fromAuthentication(session("jdoe", "Student")))
        .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
        .hasMessageContaining("subject");
  }

  @ParameterizedTest
  @ValueSource(strings = {"Admin", "STUDENT", ""})
  void anUnknownRoleIsRefused(String role) {
    assertThatThrownBy(() -> CurrentUserArgumentResolver.fromAuthentication(session("42", role)))
        .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
        .hasMessageContaining("role");
  }

  @Test
  void aMissingRoleIsRefused() {
    assertThatThrownBy(() -> CurrentUserArgumentResolver.fromAuthentication(session("42", null)))
        .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
        .hasMessageContaining("role");
  }
}
