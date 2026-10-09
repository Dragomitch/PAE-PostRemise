package com.dragomitch.ipl.pae.business.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** An option offered by a partner, in one of its departments. */
public interface PartnerOptionDto extends OptionDto {

  int DEPARTMENT_MAX_LENGTH = 100;

  @NotBlank
  @Size(max = DEPARTMENT_MAX_LENGTH)
  String getDepartement();

  void setDepartement(String departement);
}
