package com.dragomitch.ipl.pae.persistence.implementations;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.DocumentDto;
import com.dragomitch.ipl.pae.persistence.DocumentDao;
import com.dragomitch.ipl.pae.persistence.jdbc.entity.DocumentEntity;
import com.dragomitch.ipl.pae.persistence.jdbc.repository.DocumentRepository;

import java.util.List;

import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.stereotype.Repository;

/** {@link DocumentDao} on top of the Spring Data {@link DocumentRepository}. */
@Repository
class DocumentDaoImpl implements DocumentDao {

  private final EntityFactory entityFactory;
  private final DocumentRepository documents;

  DocumentDaoImpl(EntityFactory entityFactory, DocumentRepository documents) {
    this.entityFactory = entityFactory;
    this.documents = documents;
  }

  @Override
  public List<DocumentDto> findAllByProgramme(int programmeId) {
    return DataAccess.call(() -> documents.findByProgramme(AggregateReference.to(programmeId))
        .stream().map(this::toDto).toList());
  }

  /** The programme and the filled-in flag are left unset, as they always were. */
  private DocumentDto toDto(DocumentEntity entity) {
    DocumentDto document = (DocumentDto) entityFactory.build(DocumentDto.class);
    document.setId(entity.id());
    document.setName(entity.name());
    document.setCategory(entity.category().charAt(0));
    return document;
  }
}
