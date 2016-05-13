package business;

import business.dto.NominatedStudentDto;

public interface NominatedStudent extends NominatedStudentDto {

  void checkBankDetails();

  void checkDataIntegrity();

}
