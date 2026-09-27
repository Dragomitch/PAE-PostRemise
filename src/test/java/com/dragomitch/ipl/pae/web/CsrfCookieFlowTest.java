package com.dragomitch.ipl.pae.web;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.uccontrollers.SessionUcc;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The CSRF double-submit flow of the web UIs, with the real cookie token repository: the
 * {@code XSRF-TOKEN} cookie comes with the first response, and every state-changing request
 * (sign-in included) must echo it in the {@code X-XSRF-TOKEN} header.
 *
 * <p>This class never uses spring-security-test's {@code csrf()}: it replaces the token
 * repository of the (cached) security filter chain for good. The property below gives the class
 * a context of its own.
 */
@WebMvcTest(SessionController.class)
@Import(WebTestConfig.class)
@TestPropertySource(properties = "test.context=csrf-cookie-flow")
class CsrfCookieFlowTest {

  private static final String XSRF_COOKIE = "XSRF-TOKEN";
  private static final String XSRF_HEADER = "X-XSRF-TOKEN";
  private static final String SIGNIN_BODY = "{\"username\":\"stud\",\"password\":\"secret\"}";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private EntityFactory entityFactory;

  @MockBean
  private SessionUcc sessionUcc;

  @BeforeEach
  void setUp() {
    UserDto user = (UserDto) entityFactory.build(UserDto.class);
    user.setId(2);
    user.setRole(UserDto.ROLE_STUDENT);
    when(sessionUcc.signin(any(), any())).thenReturn(user);
    when(sessionUcc.showAuthenticatedUser(2)).thenReturn(user);
  }

  private Cookie fetchXsrfCookie() throws Exception {
    // the legacy UI starts with GET /session (401 when signed out): the cookie comes anyway
    Cookie xsrf = mockMvc.perform(get(ApiPaths.BASE + "/session"))
        .andExpect(status().isUnauthorized())
        .andExpect(cookie().exists(XSRF_COOKIE))
        .andExpect(cookie().httpOnly(XSRF_COOKIE, false))
        .andExpect(cookie().path(XSRF_COOKIE, "/"))
        .andReturn().getResponse().getCookie(XSRF_COOKIE);
    assertNotNull(xsrf);
    return xsrf;
  }

  @Test
  void signinWithoutTheCsrfTokenIs403() throws Exception {
    mockMvc.perform(post(ApiPaths.BASE + "/session")
            .contentType(MediaType.APPLICATION_JSON).content(SIGNIN_BODY))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errorCode").value(103))
        .andExpect(cookie().doesNotExist("session"));
  }

  @Test
  void theBrowserFlowSignsInAndOutWithTheDoubleSubmittedToken() throws Exception {
    Cookie xsrf = fetchXsrfCookie();

    Cookie session = mockMvc.perform(post(ApiPaths.BASE + "/session").cookie(xsrf)
            .header(XSRF_HEADER, xsrf.getValue())
            .contentType(MediaType.APPLICATION_JSON).content(SIGNIN_BODY))
        .andExpect(status().isOk())
        .andReturn().getResponse().getCookie("session");
    assertNotNull(session);

    // authenticated requests keep the CSRF cookie as it is (neither renewed nor expired)
    mockMvc.perform(get(ApiPaths.BASE + "/session").cookie(xsrf, session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(2))
        .andExpect(cookie().doesNotExist(XSRF_COOKIE));

    // the session cookie alone does not allow a state change: this is what a CSRF attack sends
    mockMvc.perform(delete(ApiPaths.BASE + "/session").cookie(xsrf, session))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errorCode").value(103));
    mockMvc.perform(delete(ApiPaths.BASE + "/session").cookie(xsrf, session)
            .header(XSRF_HEADER, "forged"))
        .andExpect(status().isForbidden());

    mockMvc.perform(delete(ApiPaths.BASE + "/session").cookie(xsrf, session)
            .header(XSRF_HEADER, xsrf.getValue()))
        .andExpect(status().isOk())
        .andExpect(cookie().maxAge("session", 0));
  }
}
