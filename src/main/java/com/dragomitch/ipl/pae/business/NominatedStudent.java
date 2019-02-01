package com.dragomitch.ipl.pae.business;

import com.dragomitch.ipl.pae.business.dto.NominatedStudentDto;

public interface NominatedStudent extends NominatedStudentDto {

  void checkBankDetails();

  void checkDataIntegrity();

}
