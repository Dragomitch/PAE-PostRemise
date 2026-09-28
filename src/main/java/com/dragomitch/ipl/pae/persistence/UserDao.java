package com.dragomitch.ipl.pae.persistence;

import com.dragomitch.ipl.pae.business.dto.UserDto;

import java.util.List;

public interface UserDao {

  String TABLE_NAME = "users";
  String COLUMN_ID = "user_id";
  String COLUMN_USERNAME = "username";
  String COLUMN_LAST_NAME = "last_name";
  String COLUMN_FIRST_NAME = "first_name";
  String COLUMN_EMAIL = "email";
  String COLUMN_PASSWORD = "password";
  String COLUMN_ROLE = "role";
  String COLUMN_OPTION = "option";
  String COLUMN_REGISTRATION_DATE = "registration_date";
  String COLUMN_VERSION = "version";

  int USERNAME_MAX_LENGTH = 20;
  int LAST_NAME_MAX_LENGTH = 35;
  int FIRST_NAME_MAX_LENGTH = 35;
  int EMAIL_MAX_LENGTH = 255;
  int PASSWORD_MAX_LENGTH = 255;

  /**
   * Inserts a user into the database.
   * 
   * @param user the user to be inserted in the database
   * @return the userDto inserted into the DB.
   */
  UserDto create(UserDto user);

  /**
   * Retrieve a user with the id passed in parameter.
   * 
   * @param id the id of the User we want to retrieve from the Database.
   * @return the UserDto corresponding to the user row having the param id as id.
   */
  UserDto findById(int id);

  /**
   * Queries the database for all users.
   * 
   * @return all users.
   */
  List<UserDto> findAll();

  /**
   * Queries the database for a tuple matching a certain condition.
   * 
   * @param columnName the column to be used in the SQL query
   * @param columnValue the comparison value
   * @return first result found that meets the conditions.
   */
  UserDto findBy(String columnName, String columnValue);

  /**
   * Changes the role of the user having that id to 'professor' and increments its version, with
   * optimistic locking. Only the role and the version are written: prefer it to
   * {@link #update(UserDto)} when the id is known (e.g. from the API path).
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
   * Updates the user data in the database with the contents of the UserDto's fields.
   * 
   * @param user the user data to update
   */
  void update(UserDto user);

  /**
   * Checks if the users database table is empty.
   * 
   * @return true if the table is empty, false otherwise.
   */
  boolean isEmpty();
}
