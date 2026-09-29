package com.dragomitch.ipl.pae.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.uccontrollers.SessionUcc;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Behind HTTPS, {@code app.session.cookie-secure=true} adds the {@code Secure} attribute to the
 * session and CSRF cookies; the session validity is configurable.
 */
@WebMvcTest(SessionController.class)
@Import(WebTestConfig.class)
@TestPropertySource(properties = {"app.session.cookie-secure=true", "app.session.validity=30m"})
class SecureSessionCookieTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private EntityFactory entityFactory;

  @MockBean
  private SessionUcc sessionUcc;

  @Test
  void theCookiesAreSecureAndTheSessionLastsTheConfiguredValidity() throws Exception {
    UserDto user = (UserDto) entityFactory.build(UserDto.class);
    user.setId(3);
    user.setRole(UserDto.ROLE_STUDENT);
    when(sessionUcc.signin(any(), any())).thenReturn(user);

    Cookie xsrf = mockMvc.perform(get(ApiPaths.BASE + "/session"))
        .andExpect(cookie().secure("XSRF-TOKEN", true))
        .andReturn().getResponse().getCookie("XSRF-TOKEN");
    mockMvc.perform(post(ApiPaths.BASE + "/session")
            .cookie(xsrf).header("X-XSRF-TOKEN", xsrf.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"stud\",\"password\":\"secret\"}"))
        .andExpect(status().isOk())
        .andExpect(cookie().secure("session", true))
        .andExpect(cookie().maxAge("session", 30 * 60));
  }
}
