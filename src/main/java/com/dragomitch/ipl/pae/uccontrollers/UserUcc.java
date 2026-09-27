package com.dragomitch.ipl.pae.uccontrollers;

import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.validation.ValidationGroups.OnCreate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
   */
  void promoteToProfessor(@Positive int userId);

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
