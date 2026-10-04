package com.dragomitch.ipl.pae.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Cross-origin configuration for the API, so the Angular dev server (http://localhost:4200 by
 * default) can call the backend directly.
 *
 * <p>CORS is applied by a servlet {@link CorsFilter} registered ahead of every other filter
 * (Spring Security included): preflight requests are answered there and never reach the
 * authentication or CSRF filters. The source is also exposed as the
 * {@code corsConfigurationSource} bean, the name Spring Security's {@code http.cors()} looks up,
 * so the security chain ({@link SecurityConfig}) applies the same policy. Every request header is
 * allowed, {@code X-XSRF-TOKEN} (CSRF token of the web UIs) included.
 *
 * <p>Allowed origins come from the {@code app.cors.allowed-origins} property (comma-separated),
 * which can be overridden with the {@code APP_CORS_ALLOWED_ORIGINS} environment variable or a
 * {@code --app.cors.allowed-origins=...} argument. The session is carried by a cookie, so
 * credentials are allowed and origins must be listed explicitly ({@code *} is rejected).
 */
@Configuration
public class CorsConfig {

  /** Default value of {@code app.cors.allowed-origins}: the Angular dev server. */
  public static final String DEFAULT_ALLOWED_ORIGINS = "http://localhost:4200";

  static final List<String> ALLOWED_METHODS =
      List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

  static final long MAX_AGE_SECONDS = 3600L;

  /**
   * Builds the CORS policy applied to every path.
   *
   * @param allowedOrigins comma-separated list of origins allowed to call the API
   * @return the CORS configuration source
   * @throws IllegalArgumentException if no origin is configured or if {@code *} is used
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource(
      @Value("${app.cors.allowed-origins:" + DEFAULT_ALLOWED_ORIGINS + "}") String allowedOrigins) {
    List<String> origins = Arrays.stream(allowedOrigins.split(","))
        .map(String::trim)
        .filter(StringUtils::hasText)
        .toList();
    if (origins.isEmpty()) {
      throw new IllegalArgumentException("app.cors.allowed-origins must list at least one origin");
    }

    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(origins);
    config.setAllowedMethods(ALLOWED_METHODS);
    config.setAllowedHeaders(List.of(CorsConfiguration.ALL));
    config.setExposedHeaders(List.of("Content-Disposition"));
    config.setAllowCredentials(true);
    config.setMaxAge(MAX_AGE_SECONDS);
    // Fail at start-up rather than on the first request if "*" is combined with credentials.
    config.validateAllowCredentials();

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  /**
   * Registers the CORS filter for every request, before Spring Security's filter chain.
   *
   * <p>The bean is deliberately not called {@code corsFilter}: Spring Security's
   * {@code http.cors()} looks up a bean of that name and expects a {@link CorsFilter}, not a
   * registration.
   *
   * @param corsConfigurationSource the CORS policy
   * @return the filter registration
   */
  @Bean
  public FilterRegistrationBean<CorsFilter> corsFilterRegistration(
      CorsConfigurationSource corsConfigurationSource) {
    FilterRegistrationBean<CorsFilter> registration =
        new FilterRegistrationBean<>(new CorsFilter(corsConfigurationSource));
    registration.setName("corsFilter");
    registration.addUrlPatterns("/*");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registration;
  }
}
