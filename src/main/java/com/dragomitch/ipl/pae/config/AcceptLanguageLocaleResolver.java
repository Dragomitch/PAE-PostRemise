package com.dragomitch.ipl.pae.config;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * Chooses the language of a request among the supported ones from its {@code Accept-Language}
 * header, with the RFC 4647 "lookup" matching of {@link Locale#lookup} and the header's quality
 * values ({@code en-US} gives English, {@code de-DE,en;q=0.5} English as well). The default
 * locale is used when the header is missing, malformed or matches no supported language
 * ({@code *}, {@code de}): unlike {@link AcceptHeaderLocaleResolver}, which then answers with
 * the servlet container's locale, the result never depends on the server.
 */
public class AcceptLanguageLocaleResolver extends AcceptHeaderLocaleResolver {

  /**
   * Creates the resolver.
   *
   * @param supportedLocales the languages of the messages
   * @param defaultLocale the language used when the client accepts none of them
   */
  public AcceptLanguageLocaleResolver(List<Locale> supportedLocales, Locale defaultLocale) {
    setSupportedLocales(supportedLocales);
    setDefaultLocale(defaultLocale);
  }

  @Override
  public Locale resolveLocale(HttpServletRequest request) {
    Locale defaultLocale = getDefaultLocale();
    String header = request.getHeader(HttpHeaders.ACCEPT_LANGUAGE);
    if (!StringUtils.hasText(header)) {
      return defaultLocale;
    }
    try {
      Locale match = Locale.lookup(Locale.LanguageRange.parse(header), getSupportedLocales());
      return match != null ? match : defaultLocale;
    } catch (IllegalArgumentException ex) {
      // malformed header
      return defaultLocale;
    }
  }
}
