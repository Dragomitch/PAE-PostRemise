package com.dragomitch.ipl.pae.adapter.in.web.dto;

import com.dragomitch.ipl.pae.domain.validation.ValidationGroups.Reference;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;

/**
 * An option (study programme of the school), identified by its 3-character code. Options are
 * reference data: clients only send their code (in a user).
 *
 * @param code the code, e.g. {@code BIN}
 * @param name the name
 */
public record OptionDto(
    @NotBlank(groups = {Default.class, Reference.class})
        @Size(
            min = OptionDto.CODE_LENGTH,
            max = OptionDto.CODE_LENGTH,
            groups = {Default.class, Reference.class})
        String code,
    String name) {

  public static final int CODE_LENGTH = 3;
}
