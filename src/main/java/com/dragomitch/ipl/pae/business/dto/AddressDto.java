package com.dragomitch.ipl.pae.business.dto;

import com.dragomitch.ipl.pae.business.validation.ValidationGroups.Reference;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.ConvertGroup;
import jakarta.validation.groups.Default;

/** A postal address (of a nominated student or of a partner). Lengths: {@code addresses}. */
public interface AddressDto extends Entity {

  int STREET_MAX_LENGTH = 80;
  int NUMBER_MAX_LENGTH = 10;
  int CITY_MAX_LENGTH = 60;
  int POSTAL_CODE_MAX_LENGTH = 10;
  int REGION_MAX_LENGTH = 60;

  @NotBlank
  @Size(max = STREET_MAX_LENGTH)
  String getStreet();

  void setStreet(String street);

  /** The street number (with its box, e.g. {@code 69/4B}). */
  @NotBlank
  @Size(max = NUMBER_MAX_LENGTH)
  String getNumber();

  void setNumber(String number);

  /** The country, referenced by its code. */
  @NotNull
  @Valid
  @ConvertGroup(from = Default.class, to = Reference.class)
  CountryDto getCountry();

  void setCountry(CountryDto country);

  @NotBlank
  @Size(max = CITY_MAX_LENGTH)
  String getCity();

  void setCity(String city);

  @NotBlank
  @Size(max = POSTAL_CODE_MAX_LENGTH)
  String getPostalCode();

  void setPostalCode(String postalCode);

  /** The region, possibly empty but always sent. */
  @NotNull
  @Size(max = REGION_MAX_LENGTH)
  String getRegion();

  void setRegion(String region);
}
