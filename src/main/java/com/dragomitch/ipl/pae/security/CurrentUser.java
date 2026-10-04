package com.dragomitch.ipl.pae.security;

/**
 * The authenticated user of the current request, taken from the session JWT.
 *
 * <p>Declare a parameter of this type on a controller method to receive it (see
 * {@link CurrentUserArgumentResolver}); the endpoint must require authentication.
 *
 * @param id the user id (JWT subject)
 * @param role the user role, {@code Professor} or {@code Student} (JWT {@value #ROLE_CLAIM} claim)
 */
public record CurrentUser(int id, String role) {

  /** Name of the JWT claim holding the user role. */
  public static final String ROLE_CLAIM = "role";

}
