package com.dragomitch.ipl.pae.persistence.jdbc.repository;

import com.dragomitch.ipl.pae.persistence.jdbc.entity.DocumentEntity;
import com.dragomitch.ipl.pae.persistence.jdbc.entity.ProgrammeEntity;

import java.util.List;

import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.repository.ListCrudRepository;

public interface DocumentRepository extends ListCrudRepository<DocumentEntity, Integer> {

  /**
   * The documents required by a programme.
   *
   * @param programme the programme
   * @return its documents, in no particular order
   */
  List<DocumentEntity> findByProgramme(AggregateReference<ProgrammeEntity, Integer> programme);
}
