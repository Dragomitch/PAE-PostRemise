package com.dragomitch.ipl.pae.persistence.mocks;

import com.dragomitch.ipl.pae.business.Document;
import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.DocumentDto;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.annotations.Inject;
import com.dragomitch.ipl.pae.persistence.DocumentDao;

import java.util.ArrayList;
import java.util.List;

public class MockDocumentDao implements DocumentDao {
  private List<DocumentDto> documents;
  private EntityFactory entityFactory;

  /**
   * Sole constructor.
   * 
   * @param entityFactory a factory for objects
   */
  @Inject
  public MockDocumentDao(EntityFactory entityFactory) {
    this.entityFactory = entityFactory;
    documents = new ArrayList<DocumentDto>();
    DocumentDto documentDepart1 =
        buildDocument(1, Document.DEPARTURE_DOCUMENT, "Document depart 1");
    DocumentDto documentDepart2 =
        buildDocument(2, Document.DEPARTURE_DOCUMENT, "Document depart 2");
    DocumentDto documentDepart3 =
        buildDocument(3, Document.DEPARTURE_DOCUMENT, "Document depart 3");
    DocumentDto documentDepart4 =
        buildDocument(4, Document.DEPARTURE_DOCUMENT, "Document depart 4");
    DocumentDto documentArrivee1 = buildDocument(5, Document.RETURN_DOCUMENT, "Document arrivee 1");
    DocumentDto documentArrivee2 = buildDocument(6, Document.RETURN_DOCUMENT, "Document arrivee 2");
    DocumentDto documentArrivee3 = buildDocument(7, Document.RETURN_DOCUMENT, "Document arrivee 3");
    DocumentDto documentArrivee4 = buildDocument(8, Document.RETURN_DOCUMENT, "Document arrivee 4");
    documents.add(documentDepart1);
    documents.add(documentDepart2);
    documents.add(documentDepart3);
    documents.add(documentDepart4);
    documents.add(documentArrivee1);
    documents.add(documentArrivee2);
    documents.add(documentArrivee3);
    documents.add(documentArrivee4);
    ProgrammeDto programme = buildProgramme(1, "Erasmus+", "Mobi blabla");
    for (DocumentDto document : documents) {
      document.setProgramme(programme);
    }
  }

  @Override
  public List<DocumentDto> findAllByProgramme(int programmeId) {
    List<DocumentDto> docs = new ArrayList<DocumentDto>();
    for (DocumentDto document : documents) {
      if (document.getProgramme().getId() == programmeId) {
        docs.add(document);
      }
    }
    return docs;
  }

  public List<DocumentDto> findAll() {
    return documents;
  }

  /**
   * Returns the document bearing the given document id.
   * 
   * @param documentId the id to look for
   * @return null if no document found.
   */
  public DocumentDto findById(int documentId) {
    if (documentId <= documents.size()) {
      return documents.get(documentId - 1);
    }
    return null;
  }

  private DocumentDto buildDocument(int id, char category, String name) {
    DocumentDto document = (DocumentDto) entityFactory.build(DocumentDto.class);
    document.setId(id);
    document.setCategory(category);
    document.setName(name);
    document.setVersion(1);
    return document;
  }

  private ProgrammeDto buildProgramme(int id, String programmeName, String softName) {
    ProgrammeDto programme = (ProgrammeDto) entityFactory.build(ProgrammeDto.class);
    programme.setId(1);
    programme.setProgrammeName(programmeName);
    programme.setExternalSoftName(softName);
    programme.setVersion(1);
    return programme;
  }
}
