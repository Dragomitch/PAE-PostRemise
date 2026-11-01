package com.dragomitch.ipl.pae.adapter.out.persistence;

import com.dragomitch.ipl.pae.adapter.out.persistence.repository.MobilityDocumentJpaRepository;
import com.dragomitch.ipl.pae.business.dto.DocumentDto;
import com.dragomitch.ipl.pae.persistence.MobilityDocumentDao;

import java.util.ConcurrentModificationException;
import java.util.List;

import org.springframework.stereotype.Repository;

/** The documents each mobility requires, and whether they are filled in. */
@Repository
class MobilityDocumentPersistenceAdapter implements MobilityDocumentDao {

  private final MobilityDocumentJpaRepository mobilityDocuments;
  private final PersistenceMapper mapper;

  MobilityDocumentPersistenceAdapter(MobilityDocumentJpaRepository mobilityDocuments,
      PersistenceMapper mapper) {
    this.mobilityDocuments = mobilityDocuments;
    this.mapper = mapper;
  }

  /** Links the document to the mobility, not filled in, at version 1. */
  @Override
  public void create(int documentId, int mobilityId) {
    mobilityDocuments.insert(documentId, mobilityId);
  }

  /** The documents of the mobility, by id: id, name, category and filled-in flag. */
  @Override
  public List<DocumentDto> findAllByMobility(int mobilityId) {
    return mobilityDocuments.findByIdMobilityIdOrderByIdDocumentId(mobilityId).stream()
        .map(entity -> {
          DocumentDto document = mapper.build(DocumentDto.class);
          document.setId(entity.getDocument().getId());
          document.setName(entity.getDocument().getName());
          document.setCategory(entity.getDocument().getCategory().charAt(0));
          document.setFilledIn(entity.isFilledIn());
          return document;
        }).toList();
  }

  @Override
  public void fillInDocument(int document, int mobility) {
    if (mobilityDocuments.fillIn(document, mobility) == 0) {
      throw new ConcurrentModificationException(
          "Document " + document + " is not linked to mobility " + mobility);
    }
  }
}
