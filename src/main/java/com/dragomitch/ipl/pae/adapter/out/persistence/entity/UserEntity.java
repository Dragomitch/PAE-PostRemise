package com.dragomitch.ipl.pae.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

/** A user of the application ({@code users}): a student or a professor. */
@Entity
@Table(name = "users")
public class UserEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_id")
  private Integer id;

  @Column(name = "username", nullable = false, length = 20)
  private String username;

  @Column(name = "last_name", nullable = false, length = 35)
  private String lastName;

  @Column(name = "first_name", nullable = false, length = 35)
  private String firstName;

  @Column(name = "email", nullable = false, length = 255)
  private String email;

  /** The BCrypt hash of the password. */
  @Column(name = "password", nullable = false, length = 255)
  private String password;

  @Column(name = "role", nullable = false, length = 15)
  private String role;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "option", nullable = false)
  private OptionEntity option;

  @Column(name = "registration_date", nullable = false, updatable = false)
  private LocalDateTime registrationDate;

  @Version
  @Column(name = "version", nullable = false)
  private Integer version;

  public Integer getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public OptionEntity getOption() {
    return option;
  }

  public void setOption(OptionEntity option) {
    this.option = option;
  }

  public LocalDateTime getRegistrationDate() {
    return registrationDate;
  }

  public void setRegistrationDate(LocalDateTime registrationDate) {
    this.registrationDate = registrationDate;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }
}
