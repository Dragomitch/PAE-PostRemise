package com.dragomitch.ipl.pae.domain.port.out;

import com.dragomitch.ipl.pae.domain.model.DenialReason;
import java.util.List;
import java.util.Optional;

public interface DenialReasonRepository {

  /**
   * Inserts a denialReason into the database.
   *
   * @param denialReason the denialReason to be inserted in the database
   * @return the denialReason inserted into the DB.
   */
  DenialReason create(DenialReason denialReason);

  /**
   * Retrieve a denialReason with the id passed in parameter.
   *
   * @param id the id of the DenialReason we want to retrieve from the Database.
   * @return the DenialReason corresponding to the denialReason row having the param id as id, or
   *     empty if there is none
   */
  Optional<DenialReason> findById(int id);

  /**
   * Queries the database for all denialReasons.
   *
   * @return all denialReasons
   */
  List<DenialReason> findAll();

  /**
   * Update an row in the Database with the data provided by the denialReason Object. The id of the
   * row affected is the one into the denialReason object.
   *
   * <p>The denial_reasons table has no version column, so there is no optimistic locking: the last
   * write wins. Like the other repositories, an update that matches no row is reported as a {@link
   * java.util.ConcurrentModificationException}.
   *
   * @param denialReason the new data we want to set for the id
   * @return the denialReason containing the updated data
   * @throws java.util.ConcurrentModificationException if no denial reason has that id
   */
  DenialReason update(DenialReason denialReason);
}
