package com.dragomitch.ipl.pae.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;

/**
 * Finds the session JWT of a request: the standard {@code Authorization: Bearer} header first
 * (API clients), then the {@value SessionCookieService#COOKIE_NAME} cookie (browsers).
 *
 * <p>Nothing is resolved on the public endpoints (sign-in, sign-up, ...): they do not need a
 * session, and a stale cookie (expired, or signed with a previous key) must not turn a sign-in
 * into a 401.
 */
public class SessionCookieBearerTokenResolver implements BearerTokenResolver {

  private final BearerTokenResolver headerResolver = new DefaultBearerTokenResolver();
  private final RequestMatcher publicEndpoints;

  /**
   * Creates the resolver.
   *
   * @param publicEndpoints the requests for which no token is resolved
   */
  public SessionCookieBearerTokenResolver(RequestMatcher publicEndpoints) {
    this.publicEndpoints = publicEndpoints;
  }

  @Override
  public String resolve(HttpServletRequest request) {
    if (publicEndpoints.matches(request)) {
      return null;
    }
    String token = headerResolver.resolve(request);
    if (token != null) {
      return token;
    }
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (SessionCookieService.COOKIE_NAME.equals(cookie.getName())
            && StringUtils.hasText(cookie.getValue())) {
          return cookie.getValue();
        }
      }
    }
    return null;
  }
}
