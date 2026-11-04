package com.dragomitch.ipl.pae.adapter.in.web.dto;

import com.dragomitch.ipl.pae.adapter.in.web.validation.ValidationGroups.Reference;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * A reason given by a professor to reject a mobility choice or to cancel a mobility.
 *
 * @param id the id
 * @param reason the text
 * @param version the version (the table has none: always 0)
 */
public record DenialReasonDto(
    @Positive(groups = Reference.class) int id,
    @NotBlank @Size(max = DenialReasonDto.REASON_MAX_LENGTH) String reason,
    @Schema(description = ApiDocs.VERSION) int version) {

  public static final int REASON_MAX_LENGTH = 300;
}
