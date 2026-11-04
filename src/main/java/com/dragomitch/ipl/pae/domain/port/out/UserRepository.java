package com.dragomitch.ipl.pae.domain.port.out;

import com.dragomitch.ipl.pae.domain.model.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository {

  /**
   * Inserts a user into the database.
   *
   * @param user the user to be inserted in the database
   * @return the user inserted into the DB.
   */
  User create(User user);

  /**
   * Retrieve a user with the id passed in parameter.
   *
   * @param id the id of the User we want to retrieve from the Database.
   * @return the User corresponding to the user row having the param id as id, or empty if there is
   *     none
   */
  Optional<User> findById(int id);

  /**
   * Queries the database for all users.
   *
   * @return all users.
   */
  List<User> findAll();

  /**
   * The user having that username, compared exactly (case-sensitively).
   *
   * @param username the username
   * @return the user, or empty if no user has that username
   */
  Optional<User> findByUsername(String username);

  /**
   * The user having that email address, compared exactly (case-sensitively).
   *
   * @param email the email address
   * @return the user, or empty if no user has that address
   */
  Optional<User> findByEmail(String email);

  /**
   * Changes the role of the user having that id to 'professor' and increments its version, with
   * optimistic locking. Only the role and the version are written: prefer it to {@link
   * #update(User)} when the id is known (e.g. from the API path).
   *
   * @param userId the user id
   * @param expectedVersion the version the caller read
   * @return the new version of the user
   * @throws java.util.ConcurrentModificationException if no user has that id and version
   */
  int promoteToProfessor(int userId, int expectedVersion);

  /**
   * Same as {@link #promoteToProfessor(int, int)} for a user known by its (unique) username, e.g.
   * the login a professor gives to promote a colleague.
   *
   * @param username the username (case-sensitive)
   * @param expectedVersion the version the caller read
   * @return the new version of the user
   * @throws java.util.ConcurrentModificationException if no user has that username and version
   */
  int promoteToProfessor(String username, int expectedVersion);

  /**
   * Updates the user data in the database with the contents of the User's fields.
   *
   * @param user the user data to update
   */
  void update(User user);

  /**
   * Checks if the users database table is empty.
   *
   * @return true if the table is empty, false otherwise.
   */
  boolean isEmpty();
}
