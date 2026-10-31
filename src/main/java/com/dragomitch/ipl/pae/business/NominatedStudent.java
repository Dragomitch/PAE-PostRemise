package com.dragomitch.ipl.pae.business;

import com.dragomitch.ipl.pae.business.dto.NominatedStudentDto;

/**
 * Business object of a {@link NominatedStudentDto}. Its validation rules are the Bean Validation
 * constraints declared on the getters of {@link NominatedStudentDto}.
 */
public interface NominatedStudent extends NominatedStudentDto {

  /**
   * Checks that the bank details needed to pay the student are known.
   *
   * @throws com.dragomitch.ipl.pae.business.exceptions.BusinessException
   *     {@code INCOMPLETE_BANK_DETAILS} if the IBAN, the BIC or the bank name is missing
   */
  void checkBankDetails();

}
