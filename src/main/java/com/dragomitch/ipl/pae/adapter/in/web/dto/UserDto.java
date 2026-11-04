package com.dragomitch.ipl.pae.adapter.in.web.dto;

import com.dragomitch.ipl.pae.adapter.in.web.validation.ValidationGroups.OnCreate;
import com.dragomitch.ipl.pae.adapter.in.web.validation.ValidationGroups.Reference;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.ConvertGroup;
import jakarta.validation.groups.Default;
import java.time.LocalDateTime;

/**
 * A user account. Constraints: {@link Default} for an edition, {@link Default} and {@link OnCreate}
 * (password required) for a sign-up; lengths are those of the {@code users} table.
 *
 * @param id the id; positive when the user is referenced (the applicant of a mobility choice)
 * @param firstName the first name
 * @param lastName the last name
 * @param username the login
 * @param password required at sign-up only (an edition keeps the stored password), never sent back
 * @param email the email address
 * @param option the option of the student, referenced by its code
 * @param registrationDate the sign-up date, set by the server
 * @param role {@code Professor} or {@code Student}, set by the server
 * @param version the version
 */
public record UserDto(
    @Positive(groups = Reference.class) int id,
    @NotBlank @Size(max = UserDto.NAME_MAX_LENGTH) String firstName,
    @NotBlank @Size(max = UserDto.NAME_MAX_LENGTH) String lastName,
    @NotBlank @Size(max = UserDto.USERNAME_MAX_LENGTH) String username,
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @Schema(accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank(groups = OnCreate.class)
        @Size(max = UserDto.PASSWORD_MAX_LENGTH, groups = OnCreate.class)
        String password,
    @NotBlank @Email(regexp = UserDto.EMAIL_REGEXP) @Size(max = UserDto.EMAIL_MAX_LENGTH)
        String email,
    @NotNull @Valid @ConvertGroup(from = Default.class, to = Reference.class) OptionDto option,
    LocalDateTime registrationDate,
    String role,
    @Schema(description = ApiDocs.VERSION) int version) {

  public static final int USERNAME_MAX_LENGTH = 20;
  public static final int NAME_MAX_LENGTH = 35;
  public static final int EMAIL_MAX_LENGTH = 255;
  public static final int PASSWORD_MAX_LENGTH = 255;

  /**
   * Email addresses: a local part, a domain and a top-level domain of 2 letters or more (stricter
   * than {@code @Email} alone, which accepts {@code user@localhost}). The empty string matches: it
   * is reported by {@code @NotBlank} only.
   */
  public static final String EMAIL_REGEXP =
      "([_A-Za-z0-9+-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*"
          + "(\\.[A-Za-z]{2,}))?";
}
