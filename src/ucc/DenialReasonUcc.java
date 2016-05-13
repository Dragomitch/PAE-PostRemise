package ucc;

import java.util.List;

import business.dto.DenialReasonDto;

public interface DenialReasonUcc {

  /**
   * Create a new denial reason.
   * 
   * @param denialReason the denial reason to be created
   * @return the newly created denial reason
   */
  DenialReasonDto create(DenialReasonDto denialReason);

  /**
   * Return a list of all stored denial reason.
   * 
   * @return a list of denial reason
   */
  List<DenialReasonDto> showAll();

  /**
   * Update the denial reason.
   * 
   * @param id the denial reason id to be updated
   * @param denialReason the denial reason with updated information
   * @return the updated denial reason
   */
  DenialReasonDto edit(int id, DenialReasonDto denialReason);
}
