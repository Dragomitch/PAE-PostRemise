package com.dragomitch.ipl.pae.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.exceptions.InvalidCredentialsException;
import com.dragomitch.ipl.pae.security.CurrentUser;
import com.dragomitch.ipl.pae.security.SessionCookieService;
import com.dragomitch.ipl.pae.uccontrollers.SessionUcc;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * The session: sign-in issues the JWT cookie, the cookie (or an {@code Authorization} header)
 * authenticates the next requests, sign-out removes it, invalid tokens are rejected. The CSRF
 * double-submit flow of the browsers is covered by {@link CsrfCookieFlowTest}.
 */
@WebMvcTest(SessionController.class)
@Import(WebTestConfig.class)
@TestPropertySource(properties = "app.jwt.secret=" + SessionSecurityTest.SECRET)
class SessionSecurityTest {

  static final String SECRET = "session-security-test-secret-0123456789";

  private static final String SIGNIN_BODY = "{\"username\":\"prof1\",\"password\":\"secret\"}";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private SessionCookieService sessionCookieService;

  @Autowired
  private JwtEncoder jwtEncoder;

  @Autowired
  private EntityFactory entityFactory;

  @MockBean
  private SessionUcc sessionUcc;

  private UserDto professor;

  @BeforeEach
  void setUp() {
    professor = (UserDto) entityFactory.build(UserDto.class);
    professor.setId(7);
    professor.setUsername("prof1");
    professor.setPassword("$2a$10$hashedPasswordNeverSentBack");
    professor.setRole(UserDto.ROLE_PROFESSOR);
    when(sessionUcc.signin("prof1", "secret")).thenReturn(professor);
    when(sessionUcc.showAuthenticatedUser(7)).thenReturn(professor);
  }

  private Cookie sessionCookie() {
    return new Cookie(SessionCookieService.COOKIE_NAME, sessionCookieService.createToken(professor));
  }

  private String token(JwtEncoder encoder, Map<String, Object> claims, Instant issuedAt,
      Instant expiresAt) {
    JwtClaimsSet.Builder builder = JwtClaimsSet.builder().issuedAt(issuedAt);
    if (expiresAt != null) {
      builder.expiresAt(expiresAt);
    }
    claims.forEach(builder::claim);
    return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
        builder.build())).getTokenValue();
  }

  @Test
  void signinSetsAnHttpOnlySameSiteLaxSessionCookieAndNeverReturnsThePassword()
      throws Exception {
    MvcResult result = mockMvc.perform(post(ApiPaths.BASE + "/session").with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(SIGNIN_BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(7))
        .andExpect(jsonPath("$.username").value("prof1"))
        .andExpect(jsonPath("$.role").value("Professor"))
        .andExpect(jsonPath("$.password").doesNotExist())
        .andExpect(cookie().exists("session"))
        .andExpect(cookie().httpOnly("session", true))
        .andExpect(cookie().secure("session", false))
        .andExpect(cookie().path("session", "/"))
        .andExpect(cookie().maxAge("session", 12 * 60 * 60))
        .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax")))
        .andReturn();
    String token = result.getResponse().getCookie("session").getValue();
    assertNotNull(token);
    assertFalse(token.isBlank());
  }

  @Test
  void theIssuedCookieAuthenticatesTheNextRequests() throws Exception {
    MvcResult signin = mockMvc.perform(post(ApiPaths.BASE + "/session").with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(SIGNIN_BODY))
        .andReturn();
    Cookie cookie = signin.getResponse().getCookie("session");

    mockMvc.perform(get(ApiPaths.BASE + "/session").cookie(cookie))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(7));
    verify(sessionUcc).showAuthenticatedUser(7);
  }

  @Test
  void anAuthorizationHeaderAlsoAuthenticates() throws Exception {
    mockMvc.perform(get(ApiPaths.BASE + "/session")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + sessionCookieService.createToken(professor)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("prof1"));
  }

  @Test
  void wrongCredentialsAre401() throws Exception {
    when(sessionUcc.signin(any(), any())).thenThrow(new InvalidCredentialsException());
    mockMvc.perform(post(ApiPaths.BASE + "/session").with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(SIGNIN_BODY))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
        .andExpect(cookie().doesNotExist("session"));
  }

  @Test
  void withoutSessionTheCurrentUserIs401() throws Exception {
    mockMvc.perform(get(ApiPaths.BASE + "/session"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
        .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
  }

  @Test
  void signoutExpiresTheCookie() throws Exception {
    mockMvc.perform(delete(ApiPaths.BASE + "/session").cookie(sessionCookie()).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(cookie().value("session", ""))
        .andExpect(cookie().maxAge("session", 0))
        .andExpect(cookie().path("session", "/"))
        .andExpect(cookie().httpOnly("session", true));
  }

  @Test
  void anExpiredTokenIs401() throws Exception {
    Instant past = Instant.now().minus(2, ChronoUnit.HOURS);
    String expired = token(jwtEncoder, Map.of("sub", "7", CurrentUser.ROLE_CLAIM, "Professor"),
        past, past.plus(1, ChronoUnit.HOURS));
    mockMvc.perform(get(ApiPaths.BASE + "/session").cookie(new Cookie("session", expired)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }

  @Test
  void aTokenSignedWithAnotherKeyIs401() throws Exception {
    JwtEncoder otherKey = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(
        "another-secret-another-secret-0123456789".getBytes(StandardCharsets.UTF_8),
        "HmacSHA256")));
    String forged = token(otherKey, Map.of("sub", "7", CurrentUser.ROLE_CLAIM, "Professor"),
        Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS));
    mockMvc.perform(get(ApiPaths.BASE + "/session").cookie(new Cookie("session", forged)))
        .andExpect(status().isUnauthorized());
    mockMvc.perform(get(ApiPaths.BASE + "/session").cookie(new Cookie("session", "garbage")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void aTokenOfTheFormerSessionFormatIs401() throws Exception {
    // SessionManager used to sign {userId, userRole, iat}: no subject, no expiry
    String legacy = token(jwtEncoder, Map.of("userId", 7, "userRole", "Professor"),
        Instant.now(), null);
    mockMvc.perform(get(ApiPaths.BASE + "/session").cookie(new Cookie("session", legacy)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void aStaleCookieDoesNotPreventSigningIn() throws Exception {
    mockMvc.perform(post(ApiPaths.BASE + "/session").with(csrf())
            .cookie(new Cookie("session", "stale-token-from-a-previous-key"))
            .contentType(MediaType.APPLICATION_JSON).content(SIGNIN_BODY))
        .andExpect(status().isOk())
        .andExpect(cookie().exists("session"));
  }

  @Test
  void aRequestAuthenticatedByAnAuthorizationHeaderIsNotSubjectToCsrf() throws Exception {
    // a browser never adds this header by itself: no CSRF risk
    mockMvc.perform(delete(ApiPaths.BASE + "/session")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + sessionCookieService.createToken(professor)))
        .andExpect(status().isOk());
  }

  @Test
  void securityHeadersForbidCachingApiResponses() throws Exception {
    mockMvc.perform(get(ApiPaths.BASE + "/session").cookie(sessionCookie()))
        .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
        .andExpect(header().string(HttpHeaders.SET_COOKIE, not(containsString("JSESSIONID"))));
  }
}
