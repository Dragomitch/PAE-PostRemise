package com.dragomitch.ipl.pae.config;

import com.dragomitch.ipl.pae.security.CsrfCookieFilter;
import com.dragomitch.ipl.pae.security.CurrentUser;
import com.dragomitch.ipl.pae.security.ExceptionResolverSecurityHandler;
import com.dragomitch.ipl.pae.security.RoleClaimAuthoritiesConverter;
import com.dragomitch.ipl.pae.security.SessionCookieBearerTokenResolver;
import com.dragomitch.ipl.pae.security.SessionProperties;
import com.dragomitch.ipl.pae.security.SpaCsrfTokenRequestHandler;
import com.dragomitch.ipl.pae.web.ApiPaths;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import jakarta.servlet.DispatcherType;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Objects;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

/**
 * Spring Security configuration: stateless authentication by the session JWT, role checks on the
 * controllers, CSRF protection for the cookie-authenticated browser clients.
 *
 * <ul>
 * <li><b>Authentication</b>: the JWT issued at sign-in ({@code SessionController}) is sent back
 * by browsers in the HttpOnly {@code session} cookie (or by API clients in an
 * {@code Authorization: Bearer} header) and validated by the OAuth2 resource server support
 * (HS256 signature with {@code app.jwt.secret}, {@code exp}, mandatory {@code sub} and
 * {@code role}). No HTTP session is created.</li>
 * <li><b>Authorization</b>: every {@code /api/**} endpoint requires an authenticated user, except
 * the {@linkplain #publicApiEndpoints public ones}; roles are checked on the controllers with
 * {@code @PreAuthorize} ({@code ROLE_PROFESSOR} / {@code ROLE_STUDENT} from the {@code role}
 * claim). The static web UI, its client-side routes and the error page are public.</li>
 * <li><b>CSRF</b>: the double-submit cookie pattern for single-page applications: the
 * {@code XSRF-TOKEN} cookie (readable by JavaScript) is issued on every response and must be sent
 * back in the {@code X-XSRF-TOKEN} header of every state-changing request, sign-in included.</li>
 * <li><b>Errors</b>: 401 and 403 are rendered by the MVC exception handler
 * ({@code ApiExceptionHandler}) like every other API error: RFC 9457 problems
 * ({@code UNAUTHENTICATED}, {@code ACCESS_DENIED}) localized from {@code Accept-Language}.</li>
 * </ul>
 *
 * <p>CORS is handled by the servlet filter of {@link CorsConfig}, ahead of this chain.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(SessionProperties.class)
public class SecurityConfig {

  private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

  private static final String HMAC_ALGORITHM = "HmacSHA256";
  private static final int MIN_SECRET_BYTES = 32;

  private final SecretKey jwtKey;

  public SecurityConfig(@Value("${app.jwt.secret:}") String jwtSecret) {
    this.jwtKey = buildKey(jwtSecret);
  }

  static SecretKey buildKey(String secret) {
    byte[] bytes;
    if (secret == null || secret.isBlank()) {
      logger.warn("app.jwt.secret (JWT_SECRET) is not set: using a random key, "
          + "sessions will not survive a restart. Set JWT_SECRET outside local development.");
      bytes = new byte[MIN_SECRET_BYTES];
      new SecureRandom().nextBytes(bytes);
    } else {
      bytes = secret.getBytes(StandardCharsets.UTF_8);
      if (bytes.length < MIN_SECRET_BYTES) {
        throw new IllegalStateException(
            "app.jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes long for HS256");
      }
    }
    return new SecretKeySpec(bytes, HMAC_ALGORITHM);
  }

  /**
   * The API endpoints that anonymous users may call: sign-in, sign-up and the list of options
   * (shown on the sign-up form). Matched like the controllers (case-insensitively).
   *
   * @param introspector the Spring MVC handler mappings
   * @return the matcher of the public endpoints
   */
  @Bean
  public RequestMatcher publicApiEndpoints(HandlerMappingIntrospector introspector) {
    MvcRequestMatcher.Builder mvc = new MvcRequestMatcher.Builder(introspector);
    return new OrRequestMatcher(
        mvc.pattern(HttpMethod.POST, ApiPaths.BASE + "/session"),
        mvc.pattern(HttpMethod.POST, ApiPaths.BASE + "/users"),
        mvc.pattern(HttpMethod.GET, ApiPaths.BASE + "/options"));
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http,
      RequestMatcher publicApiEndpoints, HandlerMappingIntrospector introspector,
      BearerTokenResolver bearerTokenResolver, JwtAuthenticationConverter jwtAuthenticationConverter,
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver,
      ObjectProvider<LocaleResolver> localeResolver,
      SessionProperties sessionProperties) throws Exception {
    ExceptionResolverSecurityHandler errorHandler = new ExceptionResolverSecurityHandler(
        handlerExceptionResolver, localeResolver.getIfAvailable());
    CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
    csrfTokenRepository.setCookiePath("/");
    csrfTokenRepository.setCookieCustomizer(cookie -> cookie
        .sameSite("Lax")
        .secure(sessionProperties.cookieSecure()));
    MvcRequestMatcher.Builder mvc = new MvcRequestMatcher.Builder(introspector);

    http
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .requestCache(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .csrf(csrf -> csrf
            .csrfTokenRepository(csrfTokenRepository)
            .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
            // The default CsrfAuthenticationStrategy renews the token whenever the session
            // management filter sees an authentication that is not stored in a session, which,
            // with stateless JWT authentication, means on every authenticated request: the
            // XSRF-TOKEN cookie would be expired after each call. The token is per browser, and
            // sign-in happens in a controller, so there is nothing to renew.
            .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy())
            .withObjectPostProcessor(requireCsrfUnlessAuthorizationHeader()))
        .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
        .authorizeHttpRequests(auth -> auth
            // SPA fallback (forward:/index.html) and the error page
            .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
            .requestMatchers(publicApiEndpoints).permitAll()
            .requestMatchers(mvc.pattern(ApiPaths.BASE + "/**")).authenticated()
            // the web UI: static files and client-side routes
            .anyRequest().permitAll())
        .oauth2ResourceServer(oauth2 -> oauth2
            .bearerTokenResolver(bearerTokenResolver)
            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
            .authenticationEntryPoint(errorHandler)
            .accessDeniedHandler(errorHandler))
        .exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint(errorHandler)
            .accessDeniedHandler(errorHandler));
    return http.build();
  }

  /**
   * The OAuth2 resource server support exempts from CSRF every request on which the
   * {@link BearerTokenResolver} finds a token, which is right for a token sent in an
   * {@code Authorization} header (a browser never adds it by itself) but would disable the
   * protection for the {@code session} cookie, which the browser attaches to cross-site requests.
   * This post-processor gives the CSRF filter its final rule: every state-changing request needs
   * the CSRF token unless it carries an {@code Authorization: Bearer} header.
   */
  private static ObjectPostProcessor<CsrfFilter> requireCsrfUnlessAuthorizationHeader() {
    RequestMatcher authorizationHeader = request -> {
      String header = request.getHeader(HttpHeaders.AUTHORIZATION);
      return header != null && header.regionMatches(true, 0, "Bearer ", 0, 7);
    };
    return new ObjectPostProcessor<>() {
      @Override
      public <O extends CsrfFilter> O postProcess(O filter) {
        filter.setRequireCsrfProtectionMatcher(new AndRequestMatcher(
            CsrfFilter.DEFAULT_CSRF_MATCHER, new NegatedRequestMatcher(authorizationHeader)));
        return filter;
      }
    };
  }

  @Bean
  public BearerTokenResolver bearerTokenResolver(RequestMatcher publicApiEndpoints) {
    return new SessionCookieBearerTokenResolver(publicApiEndpoints);
  }

  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(new RoleClaimAuthoritiesConverter());
    return converter;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public JwtEncoder jwtEncoder() {
    return new NimbusJwtEncoder(new ImmutableSecret<>(jwtKey));
  }

  /**
   * Decodes and validates session tokens: HS256 signature, {@code exp}/{@code nbf} (with the
   * default clock skew of 60 seconds), and the claims every session token carries. Tokens issued
   * by the former presentation layer (no {@code sub}, no {@code exp}) are therefore rejected.
   *
   * @return the decoder
   */
  @Bean
  public JwtDecoder jwtDecoder() {
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withSecretKey(jwtKey).macAlgorithm(MacAlgorithm.HS256).build();
    OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
        JwtValidators.createDefault(),
        new JwtClaimValidator<Object>(JwtClaimNames.SUB, Objects::nonNull),
        new JwtClaimValidator<Object>(JwtClaimNames.EXP, Objects::nonNull),
        new JwtClaimValidator<Object>(CurrentUser.ROLE_CLAIM, Objects::nonNull));
    decoder.setJwtValidator(validator);
    return decoder;
  }
}
