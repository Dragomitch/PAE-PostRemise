package com.dragomitch.ipl.pae.uccontrollers;

import com.dragomitch.ipl.pae.business.dto.UserDto;

public interface SessionUcc {

  /**
   * The name of the session attribute that refers to the user id.
   */
  public static final String USER_ID = "userId";

  /**
   * The name of the session attribute that refers to the user role.
   */
  public static final String USER_ROLE = "userRole";

  /**
   * Authenticate a user and store it in session.
   * 
   * @param username the username
   * @param password the user password
   * @return the authenticated user
   */
  UserDto signin(String username, String password);

  /**
   * Return the authenticated user.
   * 
   * @param id the authenticated user
   * @return the authenticated user
   */
  UserDto showAuthenticatedUser(int id);

  /**
   * Sign out the authenticated user.
   */
  void signout();

}
