package com.dragomitch.ipl.pae.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.function.Supplier;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

/**
 * CSRF token handling for JavaScript clients, as recommended by the Spring Security reference
 * ("Single-Page Applications"): the {@code X-XSRF-TOKEN} header sent by the web UIs carries the
 * raw value of the {@code XSRF-TOKEN} cookie, whereas a token rendered in a server-side page
 * (request parameter) is BREACH-protected (XOR-masked).
 */
public final class SpaCsrfTokenRequestHandler extends CsrfTokenRequestAttributeHandler {

  private final CsrfTokenRequestHandler delegate = new XorCsrfTokenRequestAttributeHandler();

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response,
      Supplier<CsrfToken> csrfToken) {
    // BREACH protection for a token rendered in a response body
    delegate.handle(request, response, csrfToken);
  }

  @Override
  public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
    if (StringUtils.hasText(request.getHeader(csrfToken.getHeaderName()))) {
      // plain token read by the SPA from the cookie
      return super.resolveCsrfTokenValue(request, csrfToken);
    }
    return delegate.resolveCsrfTokenValue(request, csrfToken);
  }
}
