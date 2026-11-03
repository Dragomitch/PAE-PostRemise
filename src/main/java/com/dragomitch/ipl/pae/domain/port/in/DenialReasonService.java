package com.dragomitch.ipl.pae.domain.port.in;

import com.dragomitch.ipl.pae.domain.model.DenialReason;
import java.util.List;

public interface DenialReasonService {

  /**
   * Create a new denial reason.
   *
   * @param denialReason the denial reason to be created
   * @return the newly created denial reason
   */
  DenialReason create(DenialReason denialReason);

  /**
   * Return a list of all stored denial reason.
   *
   * @return a list of denial reason
   */
  List<DenialReason> showAll();

  /**
   * Update the denial reason.
   *
   * @param id the denial reason id to be updated
   * @param denialReason the denial reason with updated information
   * @return the updated denial reason
   */
  DenialReason edit(int id, DenialReason denialReason);
}
