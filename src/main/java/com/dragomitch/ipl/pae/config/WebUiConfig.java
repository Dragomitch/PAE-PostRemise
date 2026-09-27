package com.dragomitch.ipl.pae.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * The legacy web UI (static/index.html) is a single-page application whose router uses
 * {@code history.pushState} paths such as {@code /partenaires}. Serve index.html for those paths
 * so that reloading or bookmarking a page works; the client-side router then renders it.
 */
@Configuration
public class WebUiConfig implements WebMvcConfigurer {

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    // single lowercase path segment without a file extension, e.g. /demandes-de-mobilite
    registry.addViewController("/{page:[a-z0-9-]+}").setViewName("forward:/index.html");
  }
}
