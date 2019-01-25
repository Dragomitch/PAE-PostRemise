package com.dragomitch.ipl.pae.business;

import com.dragomitch.ipl.pae.business.dto.DenialReasonDto;

public interface DenialReason extends DenialReasonDto {

  void checkDataIntegrity();

}
