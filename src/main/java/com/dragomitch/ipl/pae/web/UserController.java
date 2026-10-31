package com.dragomitch.ipl.pae.web;

import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.validation.ValidationGroups.OnCreate;
import com.dragomitch.ipl.pae.security.CurrentUser;
import com.dragomitch.ipl.pae.uccontrollers.UserUcc;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.BASE + "/users")
public class UserController {

  private final UserUcc userUcc;

  public UserController(UserUcc userUcc) {
    this.userUcc = userUcc;
  }

  /** Public: sign-up. The first user becomes a professor, the next ones students. */
  @PostMapping
  public UserDto signup(
      @RequestBody @Validated({Default.class, OnCreate.class}) UserDto user) {
    return userUcc.signup(user);
  }

  /** Every user, wrapped in {@code {"data": [...]}} (DataTables format). */
  @GetMapping
  @PreAuthorize(ApiPaths.PROFESSOR)
  public DataResponse<UserDto> showAll() {
    return new DataResponse<>(userUcc.showAll());
  }

  /** Promotes the user having that id (the users list of the UI); a professor is unchanged. */
  @PutMapping("/{id}/promote")
  @PreAuthorize(ApiPaths.PROFESSOR)
  public void promoteToProfessor(@PathVariable @Positive int id) {
    userUcc.promoteToProfessor(id);
  }

  /**
   * Promotes the user having that (case-sensitive) username, when only the login is known, and
   * returns it with its new role and version; a professor is unchanged.
   */
  @PutMapping("/by-username/{username}/promote")
  @PreAuthorize(ApiPaths.PROFESSOR)
  public UserDto promoteToProfessorByUsername(
      @PathVariable @NotBlank @Size(max = UserDto.USERNAME_MAX_LENGTH) String username) {
    return userUcc.promoteToProfessorByUsername(username);
  }

  @PutMapping("/edit")
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public UserDto edit(@RequestBody @Valid UserDto user, CurrentUser currentUser) {
    return userUcc.edit(user, currentUser.id(), currentUser.role());
  }
}
