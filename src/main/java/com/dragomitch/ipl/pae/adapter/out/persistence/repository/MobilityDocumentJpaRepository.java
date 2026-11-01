package com.dragomitch.ipl.pae.adapter.out.persistence.repository;

import com.dragomitch.ipl.pae.adapter.out.persistence.entity.MobilityDocumentEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.MobilityDocumentId;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MobilityDocumentJpaRepository
    extends JpaRepository<MobilityDocumentEntity, MobilityDocumentId> {

  /**
   * Links the document to the mobility, not filled in, at version 1.
   *
   * <p>An insert statement rather than a persist: Hibernate takes a new entity with an assigned
   * composite key and a version for a detached one.
   */
  @Modifying(flushAutomatically = true)
  @Query("INSERT INTO MobilityDocumentEntity (id.documentId, id.mobilityId, filledIn, version)"
      + " VALUES (:documentId, :mobilityId, FALSE, 1)")
  void insert(@Param("documentId") int documentId, @Param("mobilityId") int mobilityId);

  /** The documents of the mobility with their names and categories, in one query. */
  @EntityGraph(attributePaths = "document")
  List<MobilityDocumentEntity> findByIdMobilityIdOrderByIdDocumentId(int mobilityId);

  /**
   * Marks the document of the mobility as filled in and increments the version of the link, even
   * when it was already filled in.
   *
   * @return the number of links updated: 0 if the document is not linked to the mobility
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("UPDATE MobilityDocumentEntity md SET md.filledIn = TRUE, md.version = md.version + 1"
      + " WHERE md.id.documentId = :documentId AND md.id.mobilityId = :mobilityId")
  int fillIn(@Param("documentId") int documentId, @Param("mobilityId") int mobilityId);
}
