package com.dragomitch.ipl.pae.adapter.in.web.security;

import com.dragomitch.ipl.pae.domain.model.Role;

/**
 * The authenticated user of the current request, taken from the session JWT.
 *
 * <p>Declare a parameter of this type on a controller method to receive it (see {@link
 * CurrentUserArgumentResolver}); the endpoint must require authentication.
 *
 * @param id the user id (JWT subject)
 * @param role the user role (JWT {@value #ROLE_CLAIM} claim, {@code Professor} or {@code Student})
 */
public record CurrentUser(int id, Role role) {

  /** Name of the JWT claim holding the user role. */
  public static final String ROLE_CLAIM = "role";
}
