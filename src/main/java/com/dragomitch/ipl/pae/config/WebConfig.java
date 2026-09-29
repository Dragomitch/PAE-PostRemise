package com.dragomitch.ipl.pae.config;

import com.dragomitch.ipl.pae.security.CurrentUserArgumentResolver;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
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
 * </ul>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final CurrentUserArgumentResolver currentUserArgumentResolver;

  public WebConfig(CurrentUserArgumentResolver currentUserArgumentResolver) {
    this.currentUserArgumentResolver = currentUserArgumentResolver;
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
