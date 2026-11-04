package com.dragomitch.ipl.pae.domain.port.out;

import com.dragomitch.ipl.pae.domain.model.Document;
import java.util.List;

public interface MobilityDocumentRepository {

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
   *     Database.
   * @return All documents concerned by the mobility.
   */
  List<Document> findAllByMobility(int mobilityId);

  /**
   * Marks a document of a mobility as filled in (and increments the version of the link).
   *
   * @param document the id of the document that has been filled.
   * @param mobility the id of the mobility for which the document has been filled.
   * @throws java.util.ConcurrentModificationException if that document is not linked to that
   *     mobility (0 rows updated, as for the other repository updates)
   */
  void fillInDocument(int document, int mobility);
}
