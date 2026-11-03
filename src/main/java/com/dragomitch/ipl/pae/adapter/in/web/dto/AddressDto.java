package com.dragomitch.ipl.pae.adapter.in.web.dto;

import com.dragomitch.ipl.pae.adapter.in.web.validation.ValidationGroups.Reference;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.ConvertGroup;
import jakarta.validation.groups.Default;

/**
 * A postal address (of a nominated student or of a partner). Lengths: {@code addresses}.
 *
 * @param id the id
 * @param street the street
 * @param number the street number (with its box, e.g. {@code 69/4B})
 * @param country the country, referenced by its code
 * @param city the city
 * @param postalCode the postal code
 * @param region the region, possibly empty but always sent
 * @param version the version
 */
public record AddressDto(
    @Positive(groups = Reference.class) int id,
    @NotBlank @Size(max = AddressDto.STREET_MAX_LENGTH) String street,
    @NotBlank @Size(max = AddressDto.NUMBER_MAX_LENGTH) String number,
    @NotNull @Valid @ConvertGroup(from = Default.class, to = Reference.class) CountryDto country,
    @NotBlank @Size(max = AddressDto.CITY_MAX_LENGTH) String city,
    @NotBlank @Size(max = AddressDto.POSTAL_CODE_MAX_LENGTH) String postalCode,
    @NotNull @Size(max = AddressDto.REGION_MAX_LENGTH) String region,
    @Schema(description = ApiDocs.VERSION) int version) {

  public static final int STREET_MAX_LENGTH = 80;
  public static final int NUMBER_MAX_LENGTH = 10;
  public static final int CITY_MAX_LENGTH = 60;
  public static final int POSTAL_CODE_MAX_LENGTH = 10;
  public static final int REGION_MAX_LENGTH = 60;
}
