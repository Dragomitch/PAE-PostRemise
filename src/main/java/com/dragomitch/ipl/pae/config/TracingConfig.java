package com.dragomitch.ipl.pae.config;

import com.dragomitch.ipl.pae.adapter.in.web.logging.TraceIdResponseFilter;
import io.micrometer.tracing.Tracer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Request correlation (roadmap S18). Micrometer Tracing on the OpenTelemetry bridge is configured
 * by Spring Boot ({@code management.tracing.*} in {@code application.properties}: every request
 * sampled, nothing exported since no exporter is on the classpath); this class only adds the filter
 * that answers the trace id in the {@code X-Trace-Id} header.
 */
@Configuration
public class TracingConfig {

  /**
   * Spring Boot's HTTP server observation filter runs at {@code HIGHEST_PRECEDENCE + 1}, after the
   * CORS filter ({@link CorsConfig}); the trace id filter comes right after it, so that the span is
   * open and the header is set before Spring Security can end the request.
   */
  static final int TRACE_ID_FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 2;

  /**
   * Registers the filter for every request.
   *
   * @param tracer the tracer configured by Spring Boot
   * @return the filter registration
   */
  @Bean
  public FilterRegistrationBean<TraceIdResponseFilter> traceIdResponseFilterRegistration(
      Tracer tracer) {
    FilterRegistrationBean<TraceIdResponseFilter> registration =
        new FilterRegistrationBean<>(new TraceIdResponseFilter(tracer));
    registration.setName("traceIdResponseFilter");
    registration.addUrlPatterns("/*");
    registration.setOrder(TRACE_ID_FILTER_ORDER);
    return registration;
  }
}
