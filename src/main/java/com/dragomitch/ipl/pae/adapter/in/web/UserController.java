package com.dragomitch.ipl.pae.adapter.in.web;

import com.dragomitch.ipl.pae.domain.dto.UserDto;
import com.dragomitch.ipl.pae.domain.validation.ValidationGroups.OnCreate;
import com.dragomitch.ipl.pae.adapter.in.web.security.CurrentUser;
import com.dragomitch.ipl.pae.domain.port.in.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Users", description = "Sign-up, the list of users, promotion to professor, edition "
    + "of the own data.")
@RestController
@RequestMapping(ApiPaths.BASE + "/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  /** Public: sign-up. The first user becomes a professor, the next ones students. */
  @PostMapping
  public UserDto signup(
      @RequestBody @Validated({Default.class, OnCreate.class}) UserDto user) {
    return userService.signup(user);
  }

  /** Every user, wrapped in {@code {"data": [...]}} (DataTables format). */
  @GetMapping
  @PreAuthorize(ApiPaths.PROFESSOR)
  public DataResponse<UserDto> showAll() {
    return new DataResponse<>(userService.showAll());
  }

  /** Promotes the user having that id (the users list of the UI); a professor is unchanged. */
  @PutMapping("/{id}/promote")
  @PreAuthorize(ApiPaths.PROFESSOR)
  public void promoteToProfessor(@PathVariable @Positive int id) {
    userService.promoteToProfessor(id);
  }

  /**
   * Promotes the user having that (case-sensitive) username, when only the login is known, and
   * returns it with its new role and version; a professor is unchanged.
   */
  @PutMapping("/by-username/{username}/promote")
  @PreAuthorize(ApiPaths.PROFESSOR)
  public UserDto promoteToProfessorByUsername(
      @PathVariable @NotBlank @Size(max = UserDto.USERNAME_MAX_LENGTH) String username) {
    return userService.promoteToProfessorByUsername(username);
  }

  @Operation(summary = "Updates the data of the signed-in user")
  @PutMapping("/edit")
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public UserDto edit(@RequestBody @Valid UserDto user, CurrentUser currentUser) {
    return userService.edit(user, currentUser.id(), currentUser.role());
  }
}
