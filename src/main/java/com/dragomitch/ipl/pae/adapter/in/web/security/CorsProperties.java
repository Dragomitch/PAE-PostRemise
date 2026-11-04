package com.dragomitch.ipl.pae.adapter.in.web.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Cross-origin settings of the API ({@code app.cors.*}).
 *
 * @param allowedOrigins the origins allowed to call the API from a browser ({@code
 *     app.cors.allowed-origins}, a comma-separated list, set with the {@code
 *     APP_CORS_ALLOWED_ORIGINS} environment variable; default {@code http://localhost:4200}, the
 *     Angular dev server). The session is carried by a cookie, so credentials are allowed and the
 *     origins must be listed explicitly: {@code CorsConfig} rejects an empty list and {@code *}.
 */
@ConfigurationProperties("app.cors")
public record CorsProperties(
    @DefaultValue(CorsProperties.DEFAULT_ALLOWED_ORIGINS) List<String> allowedOrigins) {

  /** Default value of {@code app.cors.allowed-origins}: the Angular dev server. */
  public static final String DEFAULT_ALLOWED_ORIGINS = "http://localhost:4200";
}
