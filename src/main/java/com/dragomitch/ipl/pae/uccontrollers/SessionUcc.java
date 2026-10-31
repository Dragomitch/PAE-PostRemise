package com.dragomitch.ipl.pae.uccontrollers;

import com.dragomitch.ipl.pae.business.dto.UserDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

@Validated
public interface SessionUcc {

  /**
   * Checks the credentials of a user. Issuing the session itself is the web layer's job.
   * 
   * @param username the username
   * @param password the user password
   * @return the authenticated user
   * @throws com.dragomitch.ipl.pae.business.exceptions.InvalidCredentialsException if the
   *         credentials are wrong
   */
  UserDto signin(@NotBlank String username, @NotBlank String password);

  /**
   * Return the authenticated user.
   * 
   * @param id the authenticated user
   * @return the authenticated user
   */
  UserDto showAuthenticatedUser(@Positive int id);

}
