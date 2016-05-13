package business;

import business.dto.DenialReasonDto;

public interface DenialReason extends DenialReasonDto {

  void checkDataIntegrity();

}
