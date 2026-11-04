package com.dragomitch.ipl.pae.adapter.in.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import com.dragomitch.ipl.pae.adapter.in.web.security.CurrentUser;
import com.dragomitch.ipl.pae.adapter.in.web.security.RoleClaimAuthoritiesConverter;
import com.dragomitch.ipl.pae.domain.model.Role;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Authenticated requests for the MockMvc tests: a session JWT with the user id as subject and the
 * role claim, mapped to authorities exactly as in production.
 *
 * <p>Unlike spring-security-test's {@code jwt()} post-processor, which marks the request as exempt
 * from CSRF (as for a bearer token in a header), this keeps the CSRF protection of a
 * cookie-authenticated browser request: state-changing calls also need {@code csrf()}.
 */
public final class TestUsers {

  public static final int PROFESSOR_ID = 1;
  public static final int STUDENT_ID = 2;

  private TestUsers() {}

  public static RequestPostProcessor professor() {
    return user(PROFESSOR_ID, Role.PROFESSOR);
  }

  public static RequestPostProcessor student() {
    return user(STUDENT_ID, Role.STUDENT);
  }

  public static RequestPostProcessor user(int id, Role role) {
    Jwt jwt =
        Jwt.withTokenValue("test-token")
            .header("alg", "HS256")
            .subject(String.valueOf(id))
            .claim(CurrentUser.ROLE_CLAIM, role.label())
            .build();
    return authentication(
        new JwtAuthenticationToken(jwt, new RoleClaimAuthoritiesConverter().convert(jwt)));
  }
}
