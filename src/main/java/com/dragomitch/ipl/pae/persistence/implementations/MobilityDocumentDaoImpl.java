package com.dragomitch.ipl.pae.persistence.implementations;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.DocumentDto;
import com.dragomitch.ipl.pae.context.ContextManager;
import com.dragomitch.ipl.pae.annotations.Inject;
import com.dragomitch.ipl.pae.exceptions.FatalException;
import com.dragomitch.ipl.pae.persistence.DocumentDao;
import com.dragomitch.ipl.pae.persistence.MobilityDocumentDao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

class MobilityDocumentDaoImpl implements MobilityDocumentDao {

  private static final String SCHEMA_NAME = ContextManager.getProperty(ContextManager.DB_SCHEMA);

  private static final String INSERT_QUERY =
      "INSERT INTO " + SCHEMA_NAME + "." + TABLE_NAME + " (" + COLUMN_DOCUMENT_ID + ", "
          + COLUMN_MOBILITY_ID + ", " + COLUMN_IS_FILLED_IN + ", version) VALUES (?, ?, ?, 1)";

  private static final String SELECT_MOBILITY_DOCUMENTS = "SELECT d." + COLUMN_DOCUMENT_ID + ", d."
      + DocumentDao.COLUMN_NAME + ", d." + DocumentDao.COLUMN_CATEGORY + ", md."
      + COLUMN_IS_FILLED_IN + " FROM " + SCHEMA_NAME + "." + DocumentDao.TABLE_NAME + " d, "
      + SCHEMA_NAME + "." + TABLE_NAME + " md WHERE d." + DocumentDao.COLUMN_ID + " = md."
      + COLUMN_DOCUMENT_ID + " AND " + COLUMN_MOBILITY_ID + " = ?";

  private static final String UPDATE_FILL_DOCUMENT = "UPDATE " + SCHEMA_NAME + "." + TABLE_NAME
      + " " + "SET (" + COLUMN_IS_FILLED_IN + ", version) = (?, version+1) WHERE "
      + COLUMN_DOCUMENT_ID + " = ? AND " + COLUMN_MOBILITY_ID + " = ?";

  private final EntityFactory entityFactory;
  private final DalBackendServices dalBackendServices;

  /**
   * Sole constructor for explicit invocation.
   * 
   * @param entityFactory an on-demand object dispenser
   * @param dalBackendServices backend services
   */
  @Inject
  public MobilityDocumentDaoImpl(EntityFactory entityFactory,
      DalBackendServices dalBackendServices) {
    this.entityFactory = entityFactory;
    this.dalBackendServices = dalBackendServices;
  }

  @Override
  public void create(int documentId, int mobilityId) {
    try (PreparedStatement stmt = dalBackendServices.prepareStatement(INSERT_QUERY)) {
      stmt.setInt(1, documentId);
      stmt.setInt(2, mobilityId);
      stmt.setBoolean(3, false);
      stmt.execute();
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG);
    }
  }

  @Override
  public List<DocumentDto> findAllByMobility(int mobilityId) {
    List<DocumentDto> documents = new ArrayList<DocumentDto>();
    try (PreparedStatement stmt = dalBackendServices.prepareStatement(SELECT_MOBILITY_DOCUMENTS)) {
      stmt.setInt(1, mobilityId);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          documents.add(populateDocumentDto(rs));
        }
      }
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
    return documents;
  }

  @Override
  public void fillInDocument(int document, int mobility) {
    try (PreparedStatement stmt = dalBackendServices.prepareStatement(UPDATE_FILL_DOCUMENT)) {
      stmt.setBoolean(1, true);
      stmt.setInt(2, document);
      stmt.setInt(3, mobility);
      stmt.execute();
    } catch (SQLException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
  }

  private DocumentDto populateDocumentDto(ResultSet rs) throws SQLException {
    DocumentDto document = (DocumentDto) entityFactory.build(DocumentDto.class);
    document.setId(rs.getInt(1));
    document.setName(rs.getString(2));
    document.setCategory(rs.getString(3).charAt(0));
    document.setFilledIn(rs.getBoolean(4));
    return document;
  }
}
