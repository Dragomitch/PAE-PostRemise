package com.dragomitch.ipl.pae.web;

import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.security.CurrentUser;
import com.dragomitch.ipl.pae.uccontrollers.UserUcc;

import org.springframework.security.access.prepost.PreAuthorize;
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
  public UserDto signup(@RequestBody UserDto user) {
    return userUcc.signup(user);
  }

  /** Every user, wrapped in {@code {"data": [...]}} (DataTables format). */
  @GetMapping
  @PreAuthorize(ApiPaths.PROFESSOR)
  public DataResponse<UserDto> showAll() {
    return new DataResponse<>(userUcc.showAll());
  }

  @PutMapping("/{id}/promote")
  @PreAuthorize(ApiPaths.PROFESSOR)
  public void promoteToProfessor(@PathVariable int id) {
    userUcc.promoteToProfessor(id);
  }

  @PutMapping("/edit")
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public UserDto edit(@RequestBody UserDto user, CurrentUser currentUser) {
    return userUcc.edit(user, currentUser.id(), currentUser.role());
  }
}
