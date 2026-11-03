package com.dragomitch.ipl.pae.adapter.in.web.dto;

import com.dragomitch.ipl.pae.domain.validation.ValidationGroups.Reference;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * A mobility programme (reference data: Erasmus+, Erabel...). Clients reference it by its id (the
 * programme of a mobility choice).
 *
 * @param id the id; positive when the programme is referenced
 * @param programmeName the name
 * @param externalSoftName the external software in which the mobilities of this programme are
 *     encoded
 */
public record ProgrammeDto(
    @Positive(groups = Reference.class) @PositiveOrZero int id,
    @NotBlank @Size(max = ProgrammeDto.NAME_MAX_LENGTH) String programmeName,
    @NotBlank @Size(max = ProgrammeDto.NAME_MAX_LENGTH) String externalSoftName) {

  public static final int NAME_MAX_LENGTH = 100;
}
