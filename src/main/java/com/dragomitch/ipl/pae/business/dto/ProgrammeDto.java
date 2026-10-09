package com.dragomitch.ipl.pae.business.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** A mobility programme (reference data: Erasmus+, Erabel...). */
public interface ProgrammeDto extends Entity {

  int NAME_MAX_LENGTH = 100;

  @Override
  @PositiveOrZero
  int getId();

  @NotBlank
  @Size(max = NAME_MAX_LENGTH)
  String getProgrammeName();

  void setProgrammeName(String programmeName);

  /** The external software in which the mobilities of this programme are encoded. */
  @NotBlank
  @Size(max = NAME_MAX_LENGTH)
  String getExternalSoftName();

  void setExternalSoftName(String softwareName);
}
