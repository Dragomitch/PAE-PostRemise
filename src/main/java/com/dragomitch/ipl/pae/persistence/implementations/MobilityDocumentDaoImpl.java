package com.dragomitch.ipl.pae.persistence.implementations;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.DocumentDto;
import com.dragomitch.ipl.pae.persistence.MobilityDocumentDao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ConcurrentModificationException;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * {@link MobilityDocumentDao} with Spring's {@link JdbcClient}: the link table between mobilities
 * and the documents of their programme (composite key, read joined with the documents).
 */
@Repository
class MobilityDocumentDaoImpl implements MobilityDocumentDao {

  private static final String SQL_INSERT = """
      INSERT INTO student_exchange_tools.mobility_documents
        (document_id, mobility_id, is_filled_in, version) VALUES (?, ?, FALSE, 1)""";

  private static final String SQL_SELECT_BY_MOBILITY = """
      SELECT d.document_id, d.name, d.category, md.is_filled_in
        FROM student_exchange_tools.documents d
        JOIN student_exchange_tools.mobility_documents md ON d.document_id = md.document_id
       WHERE md.mobility_id = ?""";

  private static final String SQL_FILL_IN = """
      UPDATE student_exchange_tools.mobility_documents
         SET (is_filled_in, version) = (TRUE, version + 1)
       WHERE document_id = ? AND mobility_id = ?""";

  private final EntityFactory entityFactory;
  private final JdbcClient jdbcClient;

  MobilityDocumentDaoImpl(EntityFactory entityFactory, JdbcClient jdbcClient) {
    this.entityFactory = entityFactory;
    this.jdbcClient = jdbcClient;
  }

  @Override
  public void create(int documentId, int mobilityId) {
    DataAccess.run(() -> jdbcClient.sql(SQL_INSERT).param(documentId).param(mobilityId).update());
  }

  @Override
  public List<DocumentDto> findAllByMobility(int mobilityId) {
    return DataAccess.call(() -> jdbcClient.sql(SQL_SELECT_BY_MOBILITY).param(mobilityId)
        .query(this::toDto).list());
  }

  @Override
  public void fillInDocument(int document, int mobility) {
    int updated = DataAccess.call(
        () -> jdbcClient.sql(SQL_FILL_IN).param(document).param(mobility).update());
    if (updated == 0) {
      throw new ConcurrentModificationException(
          "Document " + document + " is not linked to mobility " + mobility);
    }
  }

  private DocumentDto toDto(ResultSet rs, int rowNum) throws SQLException {
    DocumentDto document = (DocumentDto) entityFactory.build(DocumentDto.class);
    document.setId(rs.getInt(1));
    document.setName(rs.getString(2));
    document.setCategory(rs.getString(3).charAt(0));
    document.setFilledIn(rs.getBoolean(4));
    return document;
  }
}
