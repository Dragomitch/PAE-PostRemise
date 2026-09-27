package com.dragomitch.ipl.pae.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ModelAndView;

/**
 * Hands the 401 (authentication required or failed) and 403 (access denied, invalid CSRF token)
 * raised by the Spring Security filters to Spring MVC's {@link HandlerExceptionResolver}, so that
 * they are rendered by the same {@code @RestControllerAdvice} as the errors thrown by the
 * controllers: the error format of the API is defined in a single place.
 *
 * <p>These errors happen before the {@code DispatcherServlet}, which normally exposes the locale
 * of the request: the handler resolves it with the application's {@link LocaleResolver} so that
 * the problems are localized exactly like the others.
 */
public class ExceptionResolverSecurityHandler implements AuthenticationEntryPoint,
    AccessDeniedHandler {

  private final HandlerExceptionResolver resolver;
  @Nullable
  private final LocaleResolver localeResolver;

  public ExceptionResolverSecurityHandler(HandlerExceptionResolver resolver,
      @Nullable LocaleResolver localeResolver) {
    this.resolver = resolver;
    this.localeResolver = localeResolver;
  }

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException authException) throws IOException {
    resolve(request, response, authException, HttpServletResponse.SC_UNAUTHORIZED);
  }

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response,
      AccessDeniedException accessDeniedException) throws IOException {
    resolve(request, response, accessDeniedException, HttpServletResponse.SC_FORBIDDEN);
  }

  private void resolve(HttpServletRequest request, HttpServletResponse response,
      Exception exception, int fallbackStatus) throws IOException {
    LocaleContext previous = LocaleContextHolder.getLocaleContext();
    if (localeResolver != null) {
      Locale locale = localeResolver.resolveLocale(request);
      LocaleContextHolder.setLocale(locale);
    }
    try {
      ModelAndView handled = resolver.resolveException(request, response, null, exception);
      if (handled == null && !response.isCommitted()) {
        response.sendError(fallbackStatus);
      }
    } finally {
      LocaleContextHolder.setLocaleContext(previous);
    }
  }
}
