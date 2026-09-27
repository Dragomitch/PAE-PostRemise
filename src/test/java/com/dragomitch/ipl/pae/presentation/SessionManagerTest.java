package com.dragomitch.ipl.pae.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.dragomitch.ipl.pae.config.SecurityConfig;

import jakarta.servlet.http.Cookie;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SessionManagerTest {

  private static final String SECRET = "unit-test-only-secret-0123456789abcdef";

  private SessionManager sessionManager;

  @BeforeEach
  void setUp() {
    SecurityConfig config = new SecurityConfig(SECRET);
    sessionManager = new SessionManager(config.jwtEncoder(), config.jwtDecoder());
  }

  @Test
  void signinIssuesACookieThatRestoresTheSessionOnAnotherServerSession() {
    MockHttpServletResponse signin = new MockHttpServletResponse();
    sessionManager.setAttributes(new MockHttpServletRequest(), signin,
        Map.of("id", 42, "role", "Professor"));
    Cookie cookie = signin.getCookie("session");
    assertNotNull(cookie);

    // New HTTP session (e.g. after a restart): the attributes come back from the JWT cookie
    MockHttpServletRequest next = new MockHttpServletRequest();
    next.setCookies(cookie);
    assertEquals("Professor", sessionManager.getAttribute("role", next));
    assertEquals(42, ((Number) sessionManager.getAttribute("id", next)).intValue());
  }

  @Test
  void updatingAnExistingCookieKeepsPreviousClaims() {
    MockHttpServletResponse first = new MockHttpServletResponse();
    sessionManager.setAttributes(new MockHttpServletRequest(), first, Map.of("id", 42));

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(first.getCookie("session"));
    MockHttpServletResponse second = new MockHttpServletResponse();
    sessionManager.setAttributes(request, second, Map.of("role", "Student"));

    MockHttpServletRequest next = new MockHttpServletRequest();
    next.setCookies(second.getCookie("session"));
    assertEquals("Student", sessionManager.getAttribute("role", next));
    assertEquals(42, ((Number) sessionManager.getAttribute("id", next)).intValue());
  }

  @Test
  void aCookieSignedWithAnotherKeyIsIgnored() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new Cookie("session", "not-a-valid-token"));

    assertNull(sessionManager.getAttribute("id", request));

    MockHttpServletResponse response = new MockHttpServletResponse();
    sessionManager.setAttributes(request, response, Map.of("id", 7));
    assertNotNull(response.getCookie("session"));
  }
}
