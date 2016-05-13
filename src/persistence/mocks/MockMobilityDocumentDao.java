package persistence.mocks;

import business.dto.DocumentDto;
import main.annotations.Inject;
import persistence.DocumentDao;
import persistence.MobilityDocumentDao;

import java.util.ArrayList;
import java.util.List;

public class MockMobilityDocumentDao implements MobilityDocumentDao {

  private List<MobilityDocument> mobilityDocuments;
  private DocumentDao documentDao;

  @Inject
  public MockMobilityDocumentDao(DocumentDao documentDao) {
    mobilityDocuments = new ArrayList<MobilityDocument>();
    this.documentDao = documentDao;
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
        docs.add(((MockDocumentDao) documentDao).findById(mobilityDocument.getDocumentId()));
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
      }
    }
  }

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
