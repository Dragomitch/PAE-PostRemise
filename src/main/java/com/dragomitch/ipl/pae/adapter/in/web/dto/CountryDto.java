package com.dragomitch.ipl.pae.adapter.in.web.dto;

import com.dragomitch.ipl.pae.domain.validation.ValidationGroups.Reference;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;

/**
 * A country (reference data), identified by its ISO 3166-1 alpha-2 code. Clients reference it by
 * its code (nationality, country of an address or of a mobility choice).
 *
 * @param countryCode the code, e.g. {@code BE}
 * @param name the name, in French
 * @param programme the mobility programme of the country
 * @param version the version of the row
 */
public record CountryDto(
    @NotBlank(groups = {Default.class, Reference.class})
        @Size(
            min = CountryDto.CODE_LENGTH,
            max = CountryDto.CODE_LENGTH,
            groups = {Default.class, Reference.class})
        String countryCode,
    @NotBlank @Size(max = CountryDto.NAME_MAX_LENGTH) String name,
    @NotNull @Valid ProgrammeDto programme,
    int version) {

  public static final int CODE_LENGTH = 2;
  public static final int NAME_MAX_LENGTH = 100;
}
