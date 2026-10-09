package com.dragomitch.ipl.pae.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Loads the deferred CSRF token on every request so that the {@code XSRF-TOKEN} cookie is always
 * issued: a single-page application needs it before its first state-changing request (sign-in
 * included) and never renders a server-side form that would load it.
 */
public final class CsrfCookieFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    if (csrfToken != null) {
      // Render the token value to a cookie by causing the deferred token to be loaded
      csrfToken.getToken();
    }
    filterChain.doFilter(request, response);
  }
}
