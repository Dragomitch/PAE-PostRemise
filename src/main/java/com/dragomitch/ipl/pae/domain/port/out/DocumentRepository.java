package com.dragomitch.ipl.pae.domain.port.out;

import com.dragomitch.ipl.pae.domain.dto.DocumentDto;

import java.util.List;

public interface DocumentRepository {

  String TABLE_NAME = "documents";
  String COLUMN_ID = "document_id";
  String COLUMN_NAME = "name";
  String COLUMN_CATEGORY = "category";
  String COLUMN_PROGRAMME_ID = "programme_id";

  /**
   * Queries the database for all documents corresponding to a programme.
   * 
   * @param programmeId the id of the programme for which we want to retrieve documents from the
   *        Database.
   * @return all documents concerned by the programme
   */
  public List<DocumentDto> findAllByProgramme(int programmeId);
}
