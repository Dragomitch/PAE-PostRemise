package com.dragomitch.ipl.pae.business.dto;

import com.dragomitch.ipl.pae.business.validation.Bic;
import com.dragomitch.ipl.pae.business.validation.Iban;
import com.dragomitch.ipl.pae.business.validation.PhoneNumber;
import com.dragomitch.ipl.pae.business.validation.ValidationGroups.Reference;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.ConvertGroup;
import jakarta.validation.groups.Default;
import java.time.LocalDate;

/**
 * The personal data of a nominated student (on top of his user account, whose constraints also
 * apply). Lengths: {@code nominated_students}.
 */
public interface NominatedStudentDto extends UserDto {

  /** Accepted titles (an empty value is reported by {@code @NotBlank} only). */
  String TITLE_REGEXP = "Mr|M|Mrs|Ms";
  /** Accepted genders: male, female, other. */
  String GENDER_REGEXP = "M|F|O";
  int PHONE_NUMBER_MAX_LENGTH = 15;
  int IBAN_MAX_LENGTH = 31;
  int CARD_HOLDER_MAX_LENGTH = 35;
  int BANK_NAME_MAX_LENGTH = 60;

  @NotBlank
  @Pattern(regexp = "(" + TITLE_REGEXP + ")?", message = "{pae.validation.title.message}")
  String getTitle();

  void setTitle(String title);

  @NotNull
  @Past
  LocalDate getBirthdate();

  void setBirthdate(LocalDate birthdate);

  /** The nationality, referenced by its country code. */
  @NotNull
  @Valid
  @ConvertGroup(from = Default.class, to = Reference.class)
  CountryDto getNationality();

  void setNationality(CountryDto nationality);

  @NotNull
  @Valid
  AddressDto getAddress();

  void setAddress(AddressDto address);

  @NotBlank
  @Size(max = PHONE_NUMBER_MAX_LENGTH)
  @PhoneNumber
  String getPhoneNumber();

  void setPhoneNumber(String phoneNumber);

  @NotBlank
  @Pattern(regexp = "(" + GENDER_REGEXP + ")?", message = "{pae.validation.gender.message}")
  String getGender();

  void setGender(String gender);

  /** Number of years already passed at the school. */
  @Positive
  int getNbrPassedYears();

  void setNbrPassedYears(int nbrPassedYears);

  @NotBlank
  @Size(max = IBAN_MAX_LENGTH)
  @Iban
  String getIban();

  void setIban(String iban);

  /** The holder of the bank account; the student himself when empty. */
  @Size(max = CARD_HOLDER_MAX_LENGTH)
  String getCardHolder();

  void setCardHolder(String cardHolder);

  @NotBlank
  @Size(max = BANK_NAME_MAX_LENGTH)
  String getBankName();

  void setBankName(String bankName);

  @NotBlank
  @Bic
  String getBic();

  void setBic(String bic);

}
