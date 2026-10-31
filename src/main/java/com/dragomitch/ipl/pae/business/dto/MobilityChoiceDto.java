package com.dragomitch.ipl.pae.business.dto;

import com.dragomitch.ipl.pae.business.validation.ValidationGroups.Reference;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.ConvertGroup;
import jakarta.validation.groups.Default;
import java.time.LocalDateTime;

/**
 * An application of a student for a mobility. Lengths: {@code mobility_choices}.
 */
public interface MobilityChoiceDto extends Entity {

  /** Highest preference order of a mobility choice (1 = first choice). */
  int MAX_ORDER_CHOICE = 3;
  /** Highest term (1 = first term, 2 = second term). */
  int MAX_TERM_CHOICE = 2;
  /** Studies abroad. */
  String MOBILITY_TYPE_SMS = "SMS";
  /** Internship abroad. */
  String MOBILITY_TYPE_SMP = "SMP";
  int CANCELLATION_REASON_MAX_LENGTH = 500;

  /** The applicant, referenced by id. */
  @NotNull
  @Valid
  @ConvertGroup(from = Default.class, to = Reference.class)
  UserDto getUser();

  void setUser(UserDto user);

  @Min(1)
  @Max(MAX_ORDER_CHOICE)
  int getPreferenceOrder();

  void setPreferenceOrder(int preferenceOrder);

  @NotBlank
  @Pattern(regexp = "(" + MOBILITY_TYPE_SMS + "|" + MOBILITY_TYPE_SMP + ")?",
      message = "{pae.validation.mobilityType.message}")
  String getMobilityType();

  void setMobilityType(String mobilityType);

  @Positive
  int getAcademicYear();

  void setAcademicYear(int academicYear);

  @Min(1)
  @Max(MAX_TERM_CHOICE)
  int getTerm();

  void setTerm(int term);

  /** The mobility programme, referenced by id. */
  @NotNull
  @Valid
  @ConvertGroup(from = Default.class, to = Reference.class)
  ProgrammeDto getProgramme();

  void setProgramme(ProgrammeDto programme);

  /** The destination country, referenced by its code; optional. */
  @Valid
  @ConvertGroup(from = Default.class, to = Reference.class)
  CountryDto getCountry();

  void setCountry(CountryDto country);

  LocalDateTime getSubmissionDate();

  void setSubmissionDate(LocalDateTime submissionDate);

  DenialReasonDto getDenialReason();

  void setDenialReason(DenialReasonDto denialReason);

  @Size(max = CANCELLATION_REASON_MAX_LENGTH)
  String getCancellationReason();

  void setCancellationReason(String cancellationReason);

  PartnerDto getPartner();

  void setPartner(PartnerDto partner);
}
