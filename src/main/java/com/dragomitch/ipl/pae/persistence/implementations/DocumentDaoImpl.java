package com.dragomitch.ipl.pae.persistence.implementations;

import com.dragomitch.ipl.pae.business.DtoFactory;
import com.dragomitch.ipl.pae.business.dto.DocumentDto;
import org.springframework.stereotype.Repository;
import com.dragomitch.ipl.pae.exceptions.FatalException;
import com.dragomitch.ipl.pae.persistence.DocumentDao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
class DocumentDaoImpl implements DocumentDao {

  private static final String SCHEMA_NAME = "student_exchange_tools";

  private static final String SELECT_PROGRAMME_DOCUMENTS =
      "SELECT document_id, name, category, programme_id FROM " + SCHEMA_NAME + "." + TABLE_NAME
          + " WHERE programme_id = ?";

  private final DtoFactory dtoFactory;
  private final DalBackendServices dalServices;

  /**
   * Sole constructor for explicit invocation.
   * 
   * @param dtoFactory an on-demand object dispenser
   * @param dalBackendServices backend services
   */
  
  public DocumentDaoImpl(DtoFactory dtoFactory, DalBackendServices dalServices) {
    this.dtoFactory = dtoFactory;
    this.dalServices = dalServices;
  }

  @Override
  public List<DocumentDto> findAllByProgramme(int programmeId) {
    List<DocumentDto> documents = new ArrayList<DocumentDto>();
    try (PreparedStatement stmt = dalServices.prepareStatement(SELECT_PROGRAMME_DOCUMENTS)) {
      stmt.setInt(1, programmeId);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          documents.add(populateDocumentDto(rs));
        }
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG);
    }
    return documents;
  }

  private DocumentDto populateDocumentDto(ResultSet rs) throws SQLException {
    DocumentDto document = (DocumentDto) dtoFactory.create(DocumentDto.class);
    document.setId(rs.getInt(1));
    document.setName(rs.getString(2));
    document.setCategory(rs.getString(3).charAt(0));
    return document;
  }

}
