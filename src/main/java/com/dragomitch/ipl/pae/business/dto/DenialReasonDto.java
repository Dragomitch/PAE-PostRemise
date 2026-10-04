package com.dragomitch.ipl.pae.business.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A reason given by a professor to reject a mobility choice or to cancel a mobility. */
public interface DenialReasonDto extends Entity {

  int REASON_MAX_LENGTH = 300;

  @NotBlank
  @Size(max = REASON_MAX_LENGTH)
  String getReason();

  void setReason(String reason);
}
