package com.dragomitch.ipl.pae.application;

import com.dragomitch.ipl.pae.domain.model.Document;
import com.dragomitch.ipl.pae.domain.port.out.DocumentRepository;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The documents a mobility of each programme must fill in, read through the {@link
 * ReferenceDataCaches#DOCUMENTS} cache. No driving port lists them: the confirmation of a mobility
 * choice ({@link MobilityChoiceServiceImpl}) reads them to create the documents of the new
 * mobility, and goes through this bean so that the cache proxy applies (a call from inside the
 * service would bypass it).
 */
@Component
@Transactional(readOnly = true)
class ProgrammeDocuments {

  private final DocumentRepository documentRepository;

  ProgrammeDocuments(DocumentRepository documentRepository) {
    this.documentRepository = documentRepository;
  }

  /**
   * Lists the documents of a programme.
   *
   * @param programmeId the id of the programme
   * @return its documents, as an unmodifiable list (shared by every caller)
   */
  @Cacheable(ReferenceDataCaches.DOCUMENTS)
  public List<Document> findAllByProgramme(int programmeId) {
    return List.copyOf(documentRepository.findAllByProgramme(programmeId));
  }
}
