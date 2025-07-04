package com.dragomitch.ipl.pae.presentation;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;
import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.dragomitch.ipl.pae.context.ContextManager;
import com.dragomitch.ipl.pae.logging.LogManager;
import com.dragomitch.ipl.pae.utils.DataValidationUtils;

public class SessionManager {

  /**
   * The name of the session cookie.
   */
  private static final String COOKIE_NAME = "session";

  private static Logger logger = LogManager.getLogger(SessionManager.class.getName());

  private final String jwtSecret;
  private final JwtEncoder jwtEncoder;
  private final JwtDecoder jwtDecoder;

  public SessionManager(JwtEncoder jwtEncoder, JwtDecoder jwtDecoder) {
    this.jwtSecret = ContextManager.getProperty("secret_key");
    this.jwtEncoder = jwtEncoder;
    this.jwtDecoder = jwtDecoder;
  }

  /**
   * Adds all map entries to the current session and to the session cookie.
   * 
   * @param req the HTTP request object that provides information about the request made
   * @param resp the HTTP response object that provides HTTP-specific functionality
   * @param data the data to be added in the current session
   */
  public void setAttributes(HttpServletRequest req, HttpServletResponse resp,
      Map<String, Object> data) {
    setSessionAttributes(data, req.getSession());
    Cookie sessionCookie = findSessionCookie(req);
    if (sessionCookie != null) {
      Map<String, Object> claims = decodeToken(sessionCookie.getValue());
      claims.putAll(data);
      String token = encodeToken(claims);
      sessionCookie.setValue(token);
    } else {
      String token = encodeToken(data);
      sessionCookie = new Cookie(COOKIE_NAME, token);
      sessionCookie.setMaxAge(60 * 60 * 24 * 365);
    }
    resp.addCookie(sessionCookie);
  }

  /**
   * Returns a session attribute.
   * 
   * @param attribute the attribute name to be returned
   * @param req the HTTP request object that provides information about the request made
   * @return the required attribute, null if the attribute is not set
   */
  public Object getAttribute(String attribute, HttpServletRequest req) {
    if (!DataValidationUtils.isAValidString(attribute)) {
      throw new IllegalArgumentException("Bad key");
    }
    if (req == null) {
      throw new IllegalArgumentException("Request cannot be null");
    }
    Object value = req.getSession().getAttribute(attribute);
    // If the value is empty, this means that the session may have expired. Then, we looked for the
    // attribute in the session cookie
    if (value == null) {
      if (req.getCookies() != null) {
        for (Cookie cookie : req.getCookies()) {
          if (cookie.getName().equals(COOKIE_NAME)) {
            Map<String, Object> cookieAttributes;
            if ((cookieAttributes = decodeToken(cookie.getValue())) != null) {
              setSessionAttributes(cookieAttributes, req.getSession());
              value = cookieAttributes.get(attribute);
            }
          }
        }
      }
    }
    return value;
  }

  /**
   * Sets all map entries as session attributes.
   * 
   * @param data the attributes to be set in session
   * @param session the HTTP session object
   */
  private void setSessionAttributes(Map<String, Object> data, HttpSession session) {
    for (Entry<String, Object> entry : data.entrySet()) {
      session.setAttribute(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Looks for the session cookie in the list of cookies.
   * 
   * @param req the HTTP request object that provides information about the request made
   * @return the session cookie, null if the session cookie has not been set
   */
  private Cookie findSessionCookie(HttpServletRequest req) {
    if (req.getCookies() != null) {
      for (Cookie cookie : req.getCookies()) {
        if (cookie.getName().equals(COOKIE_NAME)) {
          return cookie;
        }
      }
    }
    return null;
  }

  /**
   * Removes all attributes from the current session and deletes the session cookie.
   * 
   * @param req the HTTP request object that provides information about the request made
   * @param resp the HTTP response object that provides HTTP-specific functionality
   */
  public void removeSession(HttpServletRequest req, HttpServletResponse resp) {
    if (req.getSession() == null || req.getCookies() == null) {
      throw new IllegalArgumentException("Request or response cannot be null");
    }
    // Removing all attributes from current session
    Enumeration<String> attributeNames = req.getSession().getAttributeNames();
    while (attributeNames.hasMoreElements()) {
      req.getSession().removeAttribute(attributeNames.nextElement());
    }
    // Deleting the session cookie
    for (Cookie cookie : req.getCookies()) {
      if (cookie.getName().equals(COOKIE_NAME)) {
        cookie.setValue(null);
        cookie.setMaxAge(0);
        resp.addCookie(cookie);
        return;
      }
    }
  }

  /**
   * Encrypts a map into a JSON web token.
   * 
   * @param claims the data to be encoded
   * @return the encrypted JSON web token
   */
  private String encodeToken(Map<String, Object> claims) {
    if (claims == null) {
      return "";
    }
    claims.put("timestamp", Instant.now());
    JwtClaimsSet.Builder builder = JwtClaimsSet.builder();
    claims.forEach(builder::claim);
    JwtClaimsSet claimSet = builder.build();
    return jwtEncoder.encode(JwtEncoderParameters.from(claimSet)).getTokenValue();
  }

  /**
   * Removes encryption from a JSON web token.
   * 
   * @param token the JSON web token to be unencrypted
   * @return a map containing data from the encrypted token
   */
  private Map<String, Object> decodeToken(String token) {
    if (token != null) {
      try {
        Jwt decoded = jwtDecoder.decode(token);
        return decoded.getClaims();
      } catch (JwtException ex) {
        logger.info("JWT: Invalid token", ex);
        return null;
      }
    }
    return null;
  }

}
