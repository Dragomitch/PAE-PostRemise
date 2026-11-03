package com.dragomitch.ipl.pae.adapter.out.persistence;

import com.dragomitch.ipl.pae.adapter.out.persistence.repository.DocumentJpaRepository;
import com.dragomitch.ipl.pae.domain.model.Document;
import com.dragomitch.ipl.pae.domain.model.DocumentCategory;
import com.dragomitch.ipl.pae.domain.port.out.DocumentRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

/** The documents each programme requires (reference data). */
@Repository
class JpaDocumentRepositoryAdapter implements DocumentRepository {

  private final DocumentJpaRepository documents;
  private final PersistenceMapper mapper;

  JpaDocumentRepositoryAdapter(DocumentJpaRepository documents, PersistenceMapper mapper) {
    this.documents = documents;
    this.mapper = mapper;
  }

  /** Every document of the programme; each one carries its programme's id only. */
  @Override
  public List<Document> findAllByProgramme(int programmeId) {
    return documents.findByProgrammeIdOrderById(programmeId).stream()
        .map(entity -> {
          Document document = new Document();
          document.setId(entity.getId());
          document.setName(entity.getName());
          document.setCategory(DocumentCategory.fromCode(entity.getCategory().charAt(0)));
          document.setProgramme(mapper.programmeReference(programmeId));
          return document;
        })
        .toList();
  }
}
