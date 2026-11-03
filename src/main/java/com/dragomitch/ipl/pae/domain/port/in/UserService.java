package com.dragomitch.ipl.pae.domain.port.in;

import com.dragomitch.ipl.pae.domain.model.Role;
import com.dragomitch.ipl.pae.domain.model.User;
import java.util.List;

public interface UserService {

  /**
   * Return a list of all stored users.
   *
   * @return a list of users
   */
  List<User> showAll();

  /**
   * Registers a new user.
   *
   * @param user the user to create
   */
  User signup(User user);

  /**
   * Changes the role of the user to professor.
   *
   * @param userId the id of the user to promote
   * @throws com.dragomitch.ipl.pae.domain.exceptions.ResourceNotFoundException if there is no such
   *     user (404)
   * @throws java.util.ConcurrentModificationException if the user changed since it was read (409)
   */
  void promoteToProfessor(int userId);

  /**
   * Changes the role of the user having that username to professor. This is the natural entry point
   * when only the login is known (a professor promoting a colleague who just signed up); {@link
   * #promoteToProfessor(int)} serves the id-based API route.
   *
   * @param username the username of the user to promote
   * @return the user, with its new role and version
   * @throws com.dragomitch.ipl.pae.domain.exceptions.ResourceNotFoundException if there is no such
   *     user (404)
   * @throws java.util.ConcurrentModificationException if the user changed since it was read (409)
   */
  User promoteToProfessorByUsername(String username);

  /**
   * Updates the user's information. Note: This method should not be called explicitly.
   *
   * @param user the User containing the new data
   * @param userId the id of the currently signed in user
   * @param role the role of the currently signed in user
   * @return the updated User container.
   */
  User edit(User user, int userId, Role role);
}
