package com.dragomitch.ipl.pae.security;

import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves {@link CurrentUser} controller arguments from the JWT of the authenticated request.
 * Registered by {@code WebConfig}.
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return CurrentUser.class.equals(parameter.getParameterType());
  }

  @Override
  public CurrentUser resolveArgument(MethodParameter parameter,
      ModelAndViewContainer mavContainer, NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {
    return fromAuthentication(SecurityContextHolder.getContext().getAuthentication());
  }

  /**
   * Builds the current user from an authentication.
   *
   * @param authentication the authentication of the request, may be null
   * @return the current user
   * @throws AuthenticationCredentialsNotFoundException if the request is not authenticated with a
   *         session JWT
   */
  static CurrentUser fromAuthentication(Authentication authentication) {
    if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
      throw new AuthenticationCredentialsNotFoundException("No authenticated user");
    }
    try {
      return new CurrentUser(Integer.parseInt(jwt.getSubject()),
          jwt.getClaimAsString(CurrentUser.ROLE_CLAIM));
    } catch (NumberFormatException ex) {
      throw new AuthenticationCredentialsNotFoundException("Invalid subject in the session JWT", ex);
    }
  }
}
