package com.dragomitch.ipl.pae.adapter.in.web.dto;

import com.dragomitch.ipl.pae.domain.validation.ValidationGroups.Reference;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;

/**
 * An option offered by a partner, in one of its departments.
 *
 * @param code the code of the option (see {@link OptionDto})
 * @param name the name of the option
 * @param departement the department of the partner
 */
public record PartnerOptionDto(
    @NotBlank(groups = {Default.class, Reference.class})
        @Size(
            min = OptionDto.CODE_LENGTH,
            max = OptionDto.CODE_LENGTH,
            groups = {Default.class, Reference.class})
        String code,
    String name,
    @NotBlank @Size(max = PartnerOptionDto.DEPARTMENT_MAX_LENGTH) String departement) {

  public static final int DEPARTMENT_MAX_LENGTH = 100;
}
