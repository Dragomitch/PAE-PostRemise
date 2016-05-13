package persistence.implementations;

import business.EntityFactory;
import business.dto.DocumentDto;
import main.ContextManager;
import main.annotations.Inject;
import main.exceptions.FatalException;
import persistence.DocumentDao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

class DocumentDaoImpl implements DocumentDao {

  private static final String SCHEMA_NAME = ContextManager.getProperty(ContextManager.DB_SCHEMA);

  private static final String SELECT_PROGRAMME_DOCUMENTS =
      "SELECT document_id, name, category, programme_id FROM " + SCHEMA_NAME + "." + TABLE_NAME
          + " WHERE programme_id = ?";

  private final EntityFactory entityFactory;
  private final DalBackendServices dalServices;

  /**
   * Sole constructor for explicit invocation.
   * 
   * @param entityFactory an on-demand object dispenser
   * @param dalBackendServices backend services
   */
  @Inject
  public DocumentDaoImpl(EntityFactory entityFactory, DalBackendServices dalServices) {
    this.entityFactory = entityFactory;
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
    DocumentDto document = (DocumentDto) entityFactory.build(DocumentDto.class);
    document.setId(rs.getInt(1));
    document.setName(rs.getString(2));
    document.setCategory(rs.getString(3).charAt(0));
    return document;
  }

}
