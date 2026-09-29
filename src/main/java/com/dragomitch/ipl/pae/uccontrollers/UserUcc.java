package com.dragomitch.ipl.pae.uccontrollers;

import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.validation.ValidationGroups.OnCreate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;
import java.util.List;
import org.springframework.validation.annotation.Validated;

@Validated
public interface UserUcc {


  /**
   * Return a list of all stored users.
   * 
   * @return a list of users
   */
  List<UserDto> showAll();

  /**
   * Registers a new user.
   * 
   * @param user the user to create
   */
  @Validated({Default.class, OnCreate.class})
  UserDto signup(@NotNull @Valid UserDto user);

  /**
   * Changes the role of the user to professor.
   * 
   * @param userId the id of the user to promote
   * @throws com.dragomitch.ipl.pae.business.exceptions.ResourceNotFoundException if there is no
   *         such user (404)
   * @throws java.util.ConcurrentModificationException if the user changed since it was read (409)
   */
  void promoteToProfessor(@Positive int userId);

  /**
   * Changes the role of the user having that username to professor. This is the natural entry
   * point when only the login is known (a professor promoting a colleague who just signed up);
   * {@link #promoteToProfessor(int)} serves the id-based API route.
   *
   * @param username the username of the user to promote
   * @return the user, with its new role and version
   * @throws com.dragomitch.ipl.pae.business.exceptions.ResourceNotFoundException if there is no
   *         such user (404)
   * @throws java.util.ConcurrentModificationException if the user changed since it was read (409)
   */
  UserDto promoteToProfessorByUsername(
      @NotBlank @Size(max = UserDto.USERNAME_MAX_LENGTH) String username);

  /**
   * Updates the user's information. Note: This method should not be called explicitly.
   * 
   * @param user the UserDto containing the new data
   * @param userId the id of the currently signed in user
   * @param role the role of the currently signed in user
   * @return the updated UserDto container.
   */
  UserDto edit(@NotNull @Valid UserDto user, @Positive int userId, @NotBlank String role);

}
