package com.dragomitch.ipl.pae.business.dto;

import com.dragomitch.ipl.pae.business.validation.ValidationGroups.Reference;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;

/**
 * A country (reference data), identified by its ISO 3166-1 alpha-2 code. Clients reference it by
 * its code (nationality, country of an address or of a mobility choice).
 */
public interface CountryDto {

  int CODE_LENGTH = 2;
  int NAME_MAX_LENGTH = 100;

  @NotBlank(groups = {Default.class, Reference.class})
  @Size(min = CODE_LENGTH, max = CODE_LENGTH, groups = {Default.class, Reference.class})
  String getCountryCode();

  void setCountryCode(String countryCode);

  @NotBlank
  @Size(max = NAME_MAX_LENGTH)
  String getName();

  void setName(String name);

  /** The mobility programme of the country. */
  @NotNull
  @Valid
  ProgrammeDto getProgramme();

  void setProgramme(ProgrammeDto programme);

  int getVersion();

  void setVersion(int version);
}
