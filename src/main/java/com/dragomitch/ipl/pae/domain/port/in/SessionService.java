package com.dragomitch.ipl.pae.domain.port.in;

import com.dragomitch.ipl.pae.domain.model.User;
import org.jspecify.annotations.Nullable;

public interface SessionService {

  /**
   * Checks the credentials of a user. Issuing the session itself is the web layer's job.
   *
   * @param username the username
   * @param password the user password
   * @return the authenticated user
   * @throws com.dragomitch.ipl.pae.domain.exceptions.InvalidCredentialsException if the credentials
   *     are wrong
   */
  User signin(String username, String password);

  /**
   * Return the authenticated user.
   *
   * @param id the authenticated user
   * @return the authenticated user, null if the account no longer exists
   */
  @Nullable User showAuthenticatedUser(int id);
}
