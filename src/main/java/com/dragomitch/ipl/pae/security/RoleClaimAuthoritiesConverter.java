package com.dragomitch.ipl.pae.security;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

/**
 * Maps the {@value CurrentUser#ROLE_CLAIM} claim of the session JWT ({@code Professor} or
 * {@code Student}) to the authority {@code ROLE_PROFESSOR} or {@code ROLE_STUDENT}, so that
 * endpoints can be guarded with {@code @PreAuthorize("hasRole('PROFESSOR')")}.
 */
public class RoleClaimAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

  @Override
  public Collection<GrantedAuthority> convert(Jwt jwt) {
    String role = jwt.getClaimAsString(CurrentUser.ROLE_CLAIM);
    if (!StringUtils.hasText(role)) {
      return List.of();
    }
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT)));
  }
}
