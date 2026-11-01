package com.dragomitch.ipl.pae.business.dto;

import com.dragomitch.ipl.pae.business.validation.ValidationGroups.Reference;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;

/**
 * An option (study programme of the school), identified by its 3-character code. Options are
 * reference data: clients only send their code (in a user, in a partner option).
 */
public interface OptionDto {

  int CODE_LENGTH = 3;

  @NotBlank(groups = {Default.class, Reference.class})
  @Size(min = CODE_LENGTH, max = CODE_LENGTH, groups = {Default.class, Reference.class})
  String getCode();

  void setCode(String code);

  String getName();

  void setName(String name);

  int getVersion();

  void setVersion(int version);
}
