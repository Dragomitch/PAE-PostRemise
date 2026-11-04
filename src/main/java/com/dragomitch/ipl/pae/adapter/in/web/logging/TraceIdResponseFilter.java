package com.dragomitch.ipl.pae.adapter.in.web.logging;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Hands the trace id of the request to its client, so that a user can quote it with an error.
 *
 * <p>Spring Boot's HTTP server observation (a filter registered just ahead of this one) opens a
 * span for every request, continuing the W3C {@code traceparent} of the caller when there is one;
 * its trace id is in every log line of the request ({@code [traceId-spanId]} in the plain pattern,
 * {@code trace.id} in ECS). This filter writes that id to the {@value #HEADER} response header
 * before the request goes on (so that it is set however the response ends, a 401 of the security
 * chain included), keeps it in the {@value #ATTRIBUTE} request attribute for {@code
 * ApiExceptionHandler}, and logs the outcome of the request at DEBUG.
 */
public final class TraceIdResponseFilter extends OncePerRequestFilter {

  /** Response header carrying the trace id of the request (32 hexadecimal characters). */
  public static final String HEADER = "X-Trace-Id";

  /** Request attribute holding the same trace id, for the error log of the exception handler. */
  public static final String ATTRIBUTE = TraceIdResponseFilter.class.getName() + ".traceId";

  private static final Logger log = LoggerFactory.getLogger(TraceIdResponseFilter.class);

  private final Tracer tracer;

  /**
   * @param tracer the tracer of the application, whose current span is the request's
   */
  public TraceIdResponseFilter(Tracer tracer) {
    this.tracer = tracer;
  }

  /**
   * The trace id of a request, as this filter recorded it.
   *
   * @param request the current request
   * @return the trace id, or {@code null} when the filter did not run or no span was open
   */
  public static @Nullable String traceIdOf(HttpServletRequest request) {
    return request.getAttribute(ATTRIBUTE) instanceof String traceId ? traceId : null;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Span span = tracer.currentSpan();
    String traceId = span == null ? null : span.context().traceId();
    if (traceId == null || traceId.isEmpty()) {
      filterChain.doFilter(request, response);
      return;
    }
    response.setHeader(HEADER, traceId);
    request.setAttribute(ATTRIBUTE, traceId);
    long start = System.nanoTime();
    try {
      filterChain.doFilter(request, response);
    } finally {
      if (log.isDebugEnabled()) {
        log.debug(
            "{} {} {} in {} ms",
            request.getMethod(),
            request.getRequestURI(),
            response.getStatus(),
            (System.nanoTime() - start) / 1_000_000);
      }
    }
  }
}
