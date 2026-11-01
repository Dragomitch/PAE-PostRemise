package com.dragomitch.ipl.pae.business.dto;

import com.dragomitch.ipl.pae.business.validation.ValidationGroups.OnCreate;
import com.dragomitch.ipl.pae.business.validation.ValidationGroups.Reference;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.ConvertGroup;
import jakarta.validation.groups.Default;
import java.time.LocalDateTime;

/**
 * A user account. Constraints: {@link Default} for an edition, {@link Default} and
 * {@link OnCreate} (password required) for a sign-up; lengths are those of the {@code users}
 * table.
 */
public interface UserDto extends Entity {

  public static final String ROLE_PROFESSOR = "Professor";
  public static final String ROLE_STUDENT = "Student";

  int USERNAME_MAX_LENGTH = 20;
  int NAME_MAX_LENGTH = 35;
  int EMAIL_MAX_LENGTH = 255;
  int PASSWORD_MAX_LENGTH = 255;

  /**
   * Email addresses: a local part, a domain and a top-level domain of 2 letters or more (stricter
   * than {@code @Email} alone, which accepts {@code user@localhost}). The empty string matches: it
   * is reported by {@code @NotBlank} only.
   */
  String EMAIL_REGEXP = "([_A-Za-z0-9+-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*"
      + "(\\.[A-Za-z]{2,}))?";

  @NotBlank
  @Size(max = NAME_MAX_LENGTH)
  String getFirstName();

  void setFirstName(String firstName);

  @NotBlank
  @Size(max = NAME_MAX_LENGTH)
  String getLastName();

  void setLastName(String lastName);

  @NotBlank
  @Size(max = USERNAME_MAX_LENGTH)
  String getUsername();

  void setUsername(String username);

  void setPassword(String password);

  /** Required at sign-up only: an edition keeps the stored password. */
  @NotBlank(groups = OnCreate.class)
  @Size(max = PASSWORD_MAX_LENGTH, groups = OnCreate.class)
  String getPassword();

  @NotBlank
  @Email(regexp = EMAIL_REGEXP)
  @Size(max = EMAIL_MAX_LENGTH)
  String getEmail();

  void setEmail(String email);

  /** The option of the student, referenced by its code. */
  @NotNull
  @Valid
  @ConvertGroup(from = Default.class, to = Reference.class)
  OptionDto getOption();

  void setOption(OptionDto option);

  LocalDateTime getRegistrationDate();

  void setRegistrationDate(LocalDateTime registrationDate);

  String getRole();

  void setRole(String role);

}
