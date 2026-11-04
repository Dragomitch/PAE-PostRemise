package com.dragomitch.ipl.pae.adapter.out.inmemory;

import com.dragomitch.ipl.pae.domain.model.Document;
import com.dragomitch.ipl.pae.domain.model.DocumentCategory;
import com.dragomitch.ipl.pae.domain.model.Programme;
import com.dragomitch.ipl.pae.domain.port.out.DocumentRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryDocumentRepository implements DocumentRepository {
  private List<Document> documents;

  /** Sole constructor. */
  public InMemoryDocumentRepository() {
    documents = new ArrayList<Document>();
    Document documentDepart1 = buildDocument(1, DocumentCategory.DEPARTURE, "Document depart 1");
    Document documentDepart2 = buildDocument(2, DocumentCategory.DEPARTURE, "Document depart 2");
    Document documentDepart3 = buildDocument(3, DocumentCategory.DEPARTURE, "Document depart 3");
    Document documentDepart4 = buildDocument(4, DocumentCategory.DEPARTURE, "Document depart 4");
    Document documentArrivee1 = buildDocument(5, DocumentCategory.RETURN, "Document arrivee 1");
    Document documentArrivee2 = buildDocument(6, DocumentCategory.RETURN, "Document arrivee 2");
    Document documentArrivee3 = buildDocument(7, DocumentCategory.RETURN, "Document arrivee 3");
    Document documentArrivee4 = buildDocument(8, DocumentCategory.RETURN, "Document arrivee 4");
    documents.add(documentDepart1);
    documents.add(documentDepart2);
    documents.add(documentDepart3);
    documents.add(documentDepart4);
    documents.add(documentArrivee1);
    documents.add(documentArrivee2);
    documents.add(documentArrivee3);
    documents.add(documentArrivee4);
    Programme programme = buildProgramme(1, "Erasmus+", "Mobi blabla");
    for (Document document : documents) {
      document.setProgramme(programme);
    }
  }

  @Override
  public List<Document> findAllByProgramme(int programmeId) {
    List<Document> docs = new ArrayList<Document>();
    for (Document document : documents) {
      if (document.getProgramme().getId() == programmeId) {
        docs.add(document);
      }
    }
    return docs;
  }

  public List<Document> findAll() {
    return documents;
  }

  /**
   * Returns the document bearing the given document id.
   *
   * @param documentId the id to look for
   * @return null if no document found.
   */
  public Optional<Document> findById(int documentId) {
    if (documentId <= documents.size()) {
      return Optional.ofNullable(documents.get(documentId - 1));
    }
    return Optional.empty();
  }

  private Document buildDocument(int id, DocumentCategory category, String name) {
    Document document = new Document();
    document.setId(id);
    document.setCategory(category);
    document.setName(name);
    document.setVersion(1);
    return document;
  }

  private Programme buildProgramme(int id, String programmeName, String softName) {
    Programme programme = new Programme();
    programme.setId(1);
    programme.setProgrammeName(programmeName);
    programme.setExternalSoftName(softName);
    programme.setVersion(1);
    return programme;
  }
}
