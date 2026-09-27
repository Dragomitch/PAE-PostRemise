package com.dragomitch.ipl.pae.security;

import com.dragomitch.ipl.pae.business.dto.UserDto;

import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/**
 * Issues and clears the {@value #COOKIE_NAME} cookie that carries the session JWT.
 *
 * <p>The token is signed with HS256 ({@code app.jwt.secret}) and holds the user id as subject, the
 * role in the {@value CurrentUser#ROLE_CLAIM} claim, {@code iat} and {@code exp}. The cookie is
 * {@code HttpOnly} (unreadable from JavaScript), {@code SameSite=Lax}, valid for the whole site
 * ({@code Path=/}) and {@code Secure} when {@code app.session.cookie-secure} is set.
 */
@Component
public class SessionCookieService {

  /** Name of the cookie holding the session JWT. */
  public static final String COOKIE_NAME = "session";

  private final JwtEncoder jwtEncoder;
  private final SessionProperties properties;
  private final Clock clock;

  @Autowired
  public SessionCookieService(JwtEncoder jwtEncoder, SessionProperties properties) {
    this(jwtEncoder, properties, Clock.systemUTC());
  }

  SessionCookieService(JwtEncoder jwtEncoder, SessionProperties properties, Clock clock) {
    this.jwtEncoder = jwtEncoder;
    this.properties = properties;
    this.clock = clock;
  }

  /**
   * Creates a signed session token for a user.
   *
   * @param user the authenticated user
   * @return the compact JWT
   */
  public String createToken(UserDto user) {
    Instant now = clock.instant();
    JwtClaimsSet claims = JwtClaimsSet.builder()
        .subject(String.valueOf(user.getId()))
        .claim(CurrentUser.ROLE_CLAIM, user.getRole())
        .issuedAt(now)
        .expiresAt(now.plus(properties.validity()))
        .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }

  /**
   * Builds the cookie that opens a session for a user.
   *
   * @param user the authenticated user
   * @return the {@code Set-Cookie} value
   */
  public ResponseCookie sessionCookie(UserDto user) {
    return baseCookie(createToken(user)).maxAge(properties.validity()).build();
  }

  /**
   * Builds the cookie that removes the session cookie from the browser.
   *
   * @return the {@code Set-Cookie} value
   */
  public ResponseCookie expiredCookie() {
    return baseCookie("").maxAge(0).build();
  }

  private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
    return ResponseCookie.from(COOKIE_NAME, value)
        .httpOnly(true)
        .secure(properties.cookieSecure())
        .sameSite("Lax")
        .path("/");
  }
}
