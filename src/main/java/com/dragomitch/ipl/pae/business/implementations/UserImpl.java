package com.dragomitch.ipl.pae.business.implementations;

import com.dragomitch.ipl.pae.business.User;
import com.dragomitch.ipl.pae.business.dto.OptionDto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.LocalDateTime;

class UserImpl implements User, Serializable {

  private static final long serialVersionUID = 1L;

  private int id;
  private String firstName;
  private String lastName;
  private String username;
  private String password;
  private String email;
  private OptionDto option;
  private LocalDateTime registrationDate;
  private String role; // Student or Professor
  private int version;

  @Override
  public int getId() {
    return id;
  }

  @Override
  public void setId(int id) {
    this.id = id;
  }

  @Override
  public String getFirstName() {
    return firstName;
  }

  @Override
  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  @Override
  public String getLastName() {
    return lastName;
  }

  @Override
  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public void setUsername(String username) {
    this.username = username;
  }

  @Override
  // Accepted from clients (signup) but never sent back to them.
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public String getPassword() {
    return password;
  }

  @Override
  public void setPassword(String password) {
    this.password = password;
  }

  @Override
  public String getEmail() {
    return email;
  }

  @Override
  public void setEmail(String email) {
    this.email = email;
  }

  @Override
  public OptionDto getOption() {
    return option;
  }

  @Override
  public void setOption(OptionDto option) {
    this.option = option;
  }

  @Override
  public LocalDateTime getRegistrationDate() {
    return registrationDate;
  }

  @Override
  public void setRegistrationDate(LocalDateTime registrationDate) {
    this.registrationDate = registrationDate;
  }

  @Override
  public void setRole(String role) {
    this.role = role;
  }

  @Override
  public String getRole() {
    return role;
  }

  @Override
  public int getVersion() {
    return version;
  }

  @Override
  public void setVersion(int version) {
    this.version = version;
  }

}
