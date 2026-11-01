package com.dragomitch.ipl.pae.adapter.out.persistence;

import com.dragomitch.ipl.pae.adapter.out.persistence.repository.DocumentJpaRepository;
import com.dragomitch.ipl.pae.business.dto.DocumentDto;
import com.dragomitch.ipl.pae.persistence.DocumentDao;

import java.util.List;

import org.springframework.stereotype.Repository;

/** The documents each programme requires (reference data). */
@Repository
class DocumentPersistenceAdapter implements DocumentDao {

  private final DocumentJpaRepository documents;
  private final PersistenceMapper mapper;

  DocumentPersistenceAdapter(DocumentJpaRepository documents, PersistenceMapper mapper) {
    this.documents = documents;
    this.mapper = mapper;
  }

  /** Every document of the programme; each one carries its programme's id only. */
  @Override
  public List<DocumentDto> findAllByProgramme(int programmeId) {
    return documents.findByProgrammeIdOrderById(programmeId).stream().map(entity -> {
      DocumentDto document = mapper.build(DocumentDto.class);
      document.setId(entity.getId());
      document.setName(entity.getName());
      document.setCategory(entity.getCategory().charAt(0));
      document.setProgramme(mapper.programmeReference(programmeId));
      return document;
    }).toList();
  }
}
