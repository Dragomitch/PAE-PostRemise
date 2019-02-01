package com.dragomitch.ipl.pae.persistence;

import com.dragomitch.ipl.pae.business.dto.DocumentDto;

import java.util.List;

public interface MobilityDocumentDao {

  String TABLE_NAME = "mobility_documents";
  String COLUMN_DOCUMENT_ID = "document_id";
  String COLUMN_MOBILITY_ID = "mobility_id";
  String COLUMN_IS_FILLED_IN = "is_filled_in";

  /**
   * Inserts a mobility document into the database.
   * 
   * @param documentId the document's id.
   * @param mobilityId the mobility's id.
   */
  void create(int documentId, int mobilityId);

  /**
   * Queries the database for all documents corresponding to a mobility.
   * 
   * @param mobilityId the id of the mobility for which we want to retrieve documents from the
   *        Database.
   * @return All documents concerned by the mobility.
   */
  List<DocumentDto> findAllByMobility(int mobilityId);

  /**
   * Update the database to insert a new filled document corresponding to a mobility.
   * 
   * @param document the id of the document that has been filled.
   * @param mobility the id of the mobility for which the document has been filled.
   */
  void fillInDocument(int document, int mobility);

}
