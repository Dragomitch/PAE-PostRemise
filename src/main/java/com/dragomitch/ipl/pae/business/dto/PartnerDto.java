package com.dragomitch.ipl.pae.business.dto;

import com.dragomitch.ipl.pae.business.validation.PhoneNumber;
import com.dragomitch.ipl.pae.business.validation.ValidationGroups.OnCreate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * A partner institution or company. Constraints: {@link jakarta.validation.groups.Default} for
 * an edition, {@code Default} and {@link OnCreate} (at least one option) for a creation.
 * Lengths: {@code partners}.
 */
public interface PartnerDto extends Entity {

  int NAME_MAX_LENGTH = 255;
  int ORGANISATION_TYPE_MAX_LENGTH = 60;
  int EMAIL_MAX_LENGTH = 255;
  int WEBSITE_MAX_LENGTH = 255;
  int PHONE_NUMBER_MAX_LENGTH = 15;

  @NotBlank
  @Size(max = NAME_MAX_LENGTH)
  String getLegalName();

  void setLegalName(String legalName);

  @NotBlank
  @Size(max = NAME_MAX_LENGTH)
  String getBusinessName();

  void setBusinessName(String businessName);

  @NotBlank
  @Size(max = NAME_MAX_LENGTH)
  String getFullName();

  void setFullName(String fullName);

  /** TPE, PME, ETI or TGE for companies, free text otherwise. */
  @NotBlank
  @Size(max = ORGANISATION_TYPE_MAX_LENGTH)
  String getOrganisationType();

  void setOrganisationType(String organisationType);

  @Positive
  int getEmployeeCount();

  void setEmployeeCount(int employeeCount);

  @NotNull
  @Valid
  AddressDto getAddress();

  void setAddress(AddressDto address);

  @NotBlank
  @Email(regexp = UserDto.EMAIL_REGEXP)
  @Size(max = EMAIL_MAX_LENGTH)
  String getEmail();

  void setEmail(String email);

  @Size(max = WEBSITE_MAX_LENGTH)
  String getWebsite();

  void setWebsite(String website);

  @NotBlank
  @Size(max = PHONE_NUMBER_MAX_LENGTH)
  @PhoneNumber
  String getPhoneNumber();

  void setPhoneNumber(String phoneNumber);

  boolean isOfficial();

  void setStatus(boolean status);

  void setOfficial(boolean status);

  boolean isArchived();

  void setArchived(boolean archived);

  boolean isArchivable();

  void setArchivable(boolean archivable);

  /** The options offered; a new partner offers at least one (an edition only adds options). */
  @NotEmpty(groups = OnCreate.class)
  @Valid
  List<PartnerOptionDto> getOptions();

  void setOptions(List<PartnerOptionDto> options);

  ProgrammeDto getProgramme();

  void setProgramme(ProgrammeDto programme);

}
