package com.dragomitch.ipl.pae.persistence;

import com.dragomitch.ipl.pae.business.dto.DenialReasonDto;

import java.util.List;

public interface DenialReasonDao {

  String TABLE_NAME = "denial_reasons";
  String COLUMN_ID = "reason_id";
  String COLUMN_REASON = "reason";

  /**
   * Inserts a denialReason into the database.
   * 
   * @param denialReason the denialReason to be inserted in the database
   * @return the denialReasonDto inserted into the DB.
   */
  DenialReasonDto create(DenialReasonDto denialReason);

  /**
   * Retrieve a denialReason with the id passed in parameter.
   * 
   * @param id the id of the DenialReason we want to retrieve from the Database.
   * @return the DenialReasonDto corresponding to the denialReason row having the param id as id.
   */
  DenialReasonDto findById(int id);

  /**
   * Queries the database for all denialReasons.
   * 
   * @return all denialReasons
   */
  List<DenialReasonDto> findAll();

  /**
   * Update an row in the Database with the data provided by the denialReason Object. The id of the
   * row affected is the one into the denialReason object.
   *
   * <p>The denial_reasons table has no version column, so there is no optimistic locking: the last
   * write wins. Like the other DAOs, an update that matches no row is reported as a
   * {@link java.util.ConcurrentModificationException}.
   * 
   * @param denialReasonDto the new data we want to set for the id
   * @return the denialReasonDto containing the updated data
   * @throws java.util.ConcurrentModificationException if no denial reason has that id
   */
  DenialReasonDto update(DenialReasonDto denialReasonDto);

}
