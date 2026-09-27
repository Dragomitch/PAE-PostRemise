package com.dragomitch.ipl.pae.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Unit tests of {@link CorsConfig}: the policy it builds and the servlet filter it registers,
 * exercised with mock requests (no application context).
 */
class CorsConfigTest {

  private static final String DEV_ORIGIN = "http://localhost:4200";

  private final CorsConfig corsConfig = new CorsConfig();

  @Test
  void defaultPolicyAllowsTheAngularDevServerWithCredentials() {
    CorsConfiguration config = configFor(CorsConfig.DEFAULT_ALLOWED_ORIGINS, "/api/1.0/users");

    assertEquals(List.of(DEV_ORIGIN), config.getAllowedOrigins());
    assertEquals(CorsConfig.ALLOWED_METHODS, config.getAllowedMethods());
    assertEquals(Boolean.TRUE, config.getAllowCredentials());
    assertEquals(CorsConfig.MAX_AGE_SECONDS, config.getMaxAge());
  }

  @Test
  void allowedOriginsAreReadAsATrimmedCommaSeparatedList() {
    CorsConfiguration config =
        configFor(" http://localhost:4200 , https://ema.example.org,, ", "/api/1.0/session");

    assertEquals(List.of(DEV_ORIGIN, "https://ema.example.org"),
        config.getAllowedOrigins());
  }

  @Test
  void wildcardOriginIsRejectedBecauseCredentialsAreAllowed() {
    assertThrows(IllegalArgumentException.class, () -> corsConfig.corsConfigurationSource("*"));
  }

  @Test
  void emptyOriginListIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> corsConfig.corsConfigurationSource(" , "));
  }

  @Test
  void preflightFromAllowedOriginIsAnsweredWithoutReachingTheChain() throws Exception {
    MockHttpServletRequest request = preflight(DEV_ORIGIN, "POST", "content-type");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter(CorsConfig.DEFAULT_ALLOWED_ORIGINS).doFilter(request, response, chain);

    assertEquals(200, response.getStatus());
    assertNull(chain.getRequest(), "a preflight must not reach security or the API");
    assertEquals(DEV_ORIGIN, response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    assertEquals("true", response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    assertEquals(String.join(",", CorsConfig.ALLOWED_METHODS),
        response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS));
    assertEquals("content-type", response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS));
    assertEquals(String.valueOf(CorsConfig.MAX_AGE_SECONDS),
        response.getHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE));
  }

  @Test
  void preflightFromUnknownOriginIsRejected() throws Exception {
    MockHttpServletRequest request = preflight("http://evil.example.com", "POST", "content-type");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter(CorsConfig.DEFAULT_ALLOWED_ORIGINS).doFilter(request, response, chain);

    assertEquals(403, response.getStatus());
    assertNull(chain.getRequest());
    assertNull(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
  }

  @Test
  void actualRequestFromAllowedOriginGetsCorsHeadersAndContinues() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/1.0/users");
    request.addHeader(HttpHeaders.ORIGIN, DEV_ORIGIN);
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter(CorsConfig.DEFAULT_ALLOWED_ORIGINS).doFilter(request, response, chain);

    assertSame(request, chain.getRequest());
    assertEquals(DEV_ORIGIN, response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    assertEquals("true", response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    assertEquals("Content-Disposition",
        response.getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS));
  }

  @Test
  void sameOriginRequestIsLeftUntouched() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/1.0/users");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter(CorsConfig.DEFAULT_ALLOWED_ORIGINS).doFilter(request, response, chain);

    assertSame(request, chain.getRequest());
    assertNull(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
  }

  @Test
  void filterIsRegisteredForEveryPathBeforeSpringSecurity() {
    FilterRegistrationBean<CorsFilter> registration =
        corsConfig.corsFilterRegistration(corsConfig.corsConfigurationSource(DEV_ORIGIN));

    assertEquals(Ordered.HIGHEST_PRECEDENCE, registration.getOrder());
    assertEquals(Set.of("/*"), registration.getUrlPatterns());
  }

  private CorsConfiguration configFor(String allowedOrigins, String path) {
    CorsConfigurationSource source = corsConfig.corsConfigurationSource(allowedOrigins);
    CorsConfiguration config = source.getCorsConfiguration(new MockHttpServletRequest("GET", path));
    assertNotNull(config);
    return config;
  }

  private CorsFilter filter(String allowedOrigins) {
    return corsConfig.corsFilterRegistration(corsConfig.corsConfigurationSource(allowedOrigins))
        .getFilter();
  }

  private static MockHttpServletRequest preflight(String origin, String method, String headers) {
    MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/1.0/users");
    request.addHeader(HttpHeaders.ORIGIN, origin);
    request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, method);
    request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, headers);
    return request;
  }
}
