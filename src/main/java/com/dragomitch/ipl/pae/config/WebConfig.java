package com.dragomitch.ipl.pae.config;

import com.dragomitch.ipl.pae.security.CurrentUserArgumentResolver;

import java.util.List;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Spring MVC settings of the REST API.
 *
 * <ul>
 * <li>Paths are matched case-insensitively, as the former hand-made router did: the legacy UI
 * calls e.g. {@code /mobilityChoices/1/reject} and {@code /denialReasons} for routes that were
 * declared {@code /mobilitychoices/{id}/reject} and {@code /denialreasons}.</li>
 * <li>Controllers receive the authenticated user as a {@code CurrentUser} argument.</li>
 * <li>The locale of a request (language of the error messages) comes from its
 * {@code Accept-Language} header: French or English, French when the header is missing or asks
 * for another language.</li>
 * </ul>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final CurrentUserArgumentResolver currentUserArgumentResolver;

  public WebConfig(CurrentUserArgumentResolver currentUserArgumentResolver) {
    this.currentUserArgumentResolver = currentUserArgumentResolver;
  }

  /** Languages of the messages (error problems, validation messages). */
  public static final List<Locale> SUPPORTED_LOCALES = List.of(Locale.FRENCH, Locale.ENGLISH);

  /** Language of the product, used when the client accepts none of the supported ones. */
  public static final Locale DEFAULT_LOCALE = Locale.FRENCH;

  /**
   * Resolves the locale from {@code Accept-Language} among the supported ones. Declared under
   * the name the {@link DispatcherServlet} looks up, which replaces Spring Boot's default.
   *
   * @return the locale resolver
   */
  @Bean(name = DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME)
  public LocaleResolver localeResolver() {
    return new AcceptLanguageLocaleResolver(SUPPORTED_LOCALES, DEFAULT_LOCALE);
  }

  @Override
  public void configurePathMatch(PathMatchConfigurer configurer) {
    PathPatternParser parser = new PathPatternParser();
    parser.setCaseSensitive(false);
    configurer.setPatternParser(parser);
  }

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(currentUserArgumentResolver);
  }
}
