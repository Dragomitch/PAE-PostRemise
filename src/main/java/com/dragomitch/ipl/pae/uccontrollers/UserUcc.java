package com.dragomitch.ipl.pae.uccontrollers;

import com.dragomitch.ipl.pae.business.dto.UserDto;

import java.util.Map;

public interface UserUcc {


  /**
   * Return a list of all stored users.
   * 
   * @return a list of denial reason
   */
  Map<String, Object> showAll();

  /**
   * Registers a new user.
   * 
   * @param user the user to create
   */
  UserDto signup(UserDto user);

  /**
   * Changes the role of the user to professor.
   * 
   * @param userId the id of the user to promote
   */
  void promoteToProfessor(int userId);

  /**
   * Updates the user's information. Note: This method should not be called explicitly.
   * 
   * @param user the UserDto containing the new data
   * @param userId the id of the currently signed in user
   * @param role the role of the currently signed in user
   * @return the updated UserDto container.
   */
  UserDto edit(UserDto user, int userId, String role);

}
