package com.dragomitch.ipl.pae.adapter.out.inmemory;

import com.dragomitch.ipl.pae.domain.dto.DocumentDto;
import com.dragomitch.ipl.pae.domain.port.out.DocumentRepository;
import com.dragomitch.ipl.pae.domain.port.out.MobilityDocumentRepository;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

public class InMemoryMobilityDocumentRepository
    implements MobilityDocumentRepository, ResettableRepository {

  private List<MobilityDocument> mobilityDocuments;
  private DocumentRepository documentRepository;

  public InMemoryMobilityDocumentRepository(DocumentRepository documentRepository) {
    mobilityDocuments = new ArrayList<MobilityDocument>();
    this.documentRepository = documentRepository;
  }

  @Override
  public void create(int documentId, int mobilityId) {
    for (MobilityDocument mobilityDocument : mobilityDocuments) {
      if (mobilityDocument.getDocumentId() == documentId
          && mobilityDocument.getMobilityId() == mobilityId) {
        return; // or throw FatalException ?
      }
    }
    mobilityDocuments.add(new MobilityDocument(documentId, mobilityId));
  }

  @Override
  public List<DocumentDto> findAllByMobility(int mobilityId) {
    List<DocumentDto> docs = new ArrayList<DocumentDto>();
    for (MobilityDocument mobilityDocument : mobilityDocuments) {
      if (mobilityDocument.getMobilityId() == mobilityId) {
        docs.add(
            ((InMemoryDocumentRepository) documentRepository)
                .findById(mobilityDocument.getDocumentId()));
      }
    }
    return docs;
  }

  @Override
  public void fillInDocument(int documentId, int mobilityId) {
    for (MobilityDocument mobilityDocument : mobilityDocuments) {
      if (mobilityDocument.getDocumentId() == documentId
          && mobilityDocument.getMobilityId() == mobilityId) {
        mobilityDocument.fillIn();
        return;
      }
    }
    throw new ConcurrentModificationException();
  }

  @Override
  public void empty() {
    mobilityDocuments = new ArrayList<MobilityDocument>();
  }

  private static class MobilityDocument {
    private int mobilityId;
    private int documentId;
    private boolean filledIn;

    private MobilityDocument(int documentId, int mobilityId) {
      this.documentId = documentId;
      this.mobilityId = mobilityId;
    }

    public int getMobilityId() {
      return mobilityId;
    }

    public int getDocumentId() {
      return documentId;
    }

    @SuppressWarnings("unused")
    public boolean isFilledIn() {
      return filledIn;
    }

    public void fillIn() {
      filledIn = true;
    }
  }
}
