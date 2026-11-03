package com.dragomitch.ipl.pae.adapter.in.web.dto;

import com.dragomitch.ipl.pae.adapter.in.web.validation.Bic;
import com.dragomitch.ipl.pae.adapter.in.web.validation.Iban;
import com.dragomitch.ipl.pae.adapter.in.web.validation.PhoneNumber;
import com.dragomitch.ipl.pae.adapter.in.web.validation.ValidationGroups.OnCreate;
import com.dragomitch.ipl.pae.adapter.in.web.validation.ValidationGroups.Reference;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.ConvertGroup;
import jakarta.validation.groups.Default;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A nominated student: his user account (the components and constraints of {@link UserDto}) and his
 * personal data. Lengths: {@code users} and {@code nominated_students}.
 *
 * @param id the id of the user
 * @param firstName see {@link UserDto}
 * @param lastName see {@link UserDto}
 * @param username see {@link UserDto}
 * @param password see {@link UserDto}
 * @param email see {@link UserDto}
 * @param option see {@link UserDto}
 * @param registrationDate see {@link UserDto}
 * @param role see {@link UserDto}
 * @param title Mr, M, Mrs or Ms
 * @param birthdate the birth date
 * @param nationality the nationality, referenced by its country code
 * @param address the address
 * @param phoneNumber the phone number
 * @param gender M, F or O
 * @param nbrPassedYears the number of years already passed at the school
 * @param iban the IBAN of the bank account
 * @param cardHolder the holder of the bank account; the student himself when empty
 * @param bankName the name of the bank
 * @param bic the BIC of the bank
 * @param version the version (the user's)
 */
public record NominatedStudentDto(
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
    @NotBlank
        @Pattern(
            regexp = "(" + NominatedStudentDto.TITLE_REGEXP + ")?",
            message = "{pae.validation.title.message}")
        String title,
    @JsonFormat(pattern = "yyyy-MM-dd") @NotNull @Past LocalDate birthdate,
    @NotNull @Valid @ConvertGroup(from = Default.class, to = Reference.class)
        CountryDto nationality,
    @NotNull @Valid AddressDto address,
    @NotBlank @Size(max = NominatedStudentDto.PHONE_NUMBER_MAX_LENGTH) @PhoneNumber
        String phoneNumber,
    @NotBlank
        @Pattern(
            regexp = "(" + NominatedStudentDto.GENDER_REGEXP + ")?",
            message = "{pae.validation.gender.message}")
        String gender,
    @Positive int nbrPassedYears,
    @NotBlank @Size(max = NominatedStudentDto.IBAN_MAX_LENGTH) @Iban String iban,
    @Size(max = NominatedStudentDto.CARD_HOLDER_MAX_LENGTH) String cardHolder,
    @NotBlank @Size(max = NominatedStudentDto.BANK_NAME_MAX_LENGTH) String bankName,
    @NotBlank @Bic String bic,
    @Schema(description = ApiDocs.VERSION) int version) {

  /** Accepted titles (an empty value is reported by {@code @NotBlank} only). */
  public static final String TITLE_REGEXP = "Mr|M|Mrs|Ms";

  /** Accepted genders: male, female, other. */
  public static final String GENDER_REGEXP = "M|F|O";

  public static final int PHONE_NUMBER_MAX_LENGTH = 15;
  public static final int IBAN_MAX_LENGTH = 31;
  public static final int CARD_HOLDER_MAX_LENGTH = 35;
  public static final int BANK_NAME_MAX_LENGTH = 60;
}
