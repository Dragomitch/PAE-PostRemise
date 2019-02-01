package com.dragomitch.ipl.pae.business.dto;

import java.time.LocalDateTime;

public interface UserDto extends Entity {

  public static final String ROLE_PROFESSOR = "Professor";
  public static final String ROLE_STUDENT = "Student";

  String getFirstName();

  void setFirstName(String firstName);

  String getLastName();

  void setLastName(String lastName);

  String getUsername();

  void setUsername(String username);

  void setPassword(String password);

  String getPassword();

  String getEmail();

  void setEmail(String email);

  OptionDto getOption();

  void setOption(OptionDto option);

  LocalDateTime getRegistrationDate();

  void setRegistrationDate(LocalDateTime registrationDate);

  String getRole();

  void setRole(String role);

}
