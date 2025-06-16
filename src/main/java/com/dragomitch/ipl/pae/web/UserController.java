package com.dragomitch.ipl.pae.web;

import com.dragomitch.ipl.pae.uccontrollers.UserUcc;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/1.0/users")
public class UserController {

  private final UserUcc userUcc;

  @Autowired
  public UserController(UserUcc userUcc) {
    this.userUcc = userUcc;
  }

  @GetMapping
  public Map<String, Object> showAll() {
    return userUcc.showAll();
  }

  @PostMapping
  public UserDto signup(@RequestBody UserDto user) {
    return userUcc.signup(user);
  }
}
