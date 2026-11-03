package com.dragomitch.ipl.pae.domain.model;

import com.dragomitch.ipl.pae.domain.exceptions.BusinessException;
import com.dragomitch.ipl.pae.domain.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.domain.exceptions.FatalException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A confirmed mobility choice: its state ({@link MobilityState}), its documents, its payments and
 * its encodings in the external software. Its submission date is the date of the confirmation.
 */
public class Mobility extends MobilityChoice {

  private MobilityState state;
  private MobilityState stateBeforeCancellation;
  private List<Document> documents;
  private LocalDateTime firstPaymentRequestDate;
  private LocalDateTime secondPaymentRequestDate;
  private boolean proEcoEncoding;
  private boolean secondSoftwareEncoding;
  private User professorInCharge;
  private NominatedStudent nominatedStudent;

  public MobilityState getState() {
    return state;
  }

  /** Sets the state as stored or as it starts; {@link #moveTo} checks a transition. */
  public void setState(MobilityState state) {
    this.state = state;
  }

  /**
   * Moves the mobility to a state, which must be one of the {@linkplain MobilityState#nextStates()
   * next states} of the current one (or the current one, which changes nothing).
   *
   * @param next the new state
   * @throws IllegalStateException if the state machine does not allow the transition
   */
  public void moveTo(MobilityState next) {
    if (next != state && !state.canMoveTo(next)) {
      throw new IllegalStateException("A mobility cannot move from " + state + " to " + next);
    }
    state = next;
  }

  public MobilityState getStateBeforeCancellation() {
    return stateBeforeCancellation;
  }

  public void setStateBeforeCancellation(MobilityState stateBeforeCancellation) {
    this.stateBeforeCancellation = stateBeforeCancellation;
  }

  public List<Document> getDocuments() {
    return documents;
  }

  public void setDocuments(List<Document> documents) {
    this.documents = documents;
  }

  public LocalDateTime getFirstPaymentRequestDate() {
    return firstPaymentRequestDate;
  }

  public void setFirstPaymentRequestDate(LocalDateTime firstPaymentRequestDate) {
    this.firstPaymentRequestDate = firstPaymentRequestDate;
  }

  public LocalDateTime getSecondPaymentRequestDate() {
    return secondPaymentRequestDate;
  }

  public void setSecondPaymentRequestDate(LocalDateTime secondPaymentRequestDate) {
    this.secondPaymentRequestDate = secondPaymentRequestDate;
  }

  public boolean isEncodedInProEco() {
    return proEcoEncoding;
  }

  public void setProEcoEncoding(boolean proEcoEncoding) {
    this.proEcoEncoding = proEcoEncoding;
  }

  public boolean isEncodedInSecondSoftware() {
    return secondSoftwareEncoding;
  }

  public void setSecondSoftwareEncoding(boolean secondSoftwareEncoding) {
    this.secondSoftwareEncoding = secondSoftwareEncoding;
  }

  public User getProfessorInCharge() {
    return professorInCharge;
  }

  public void setProfessorInCharge(User professorInCharge) {
    this.professorInCharge = professorInCharge;
  }

  public NominatedStudent getNominatedStudent() {
    return nominatedStudent;
  }

  public void setNominatedStudent(NominatedStudent nominatedStudent) {
    this.nominatedStudent = nominatedStudent;
  }

  /**
   * Indicates that a document has been filled in.
   *
   * @param documentId the document id
   * @return true if the document was not filled in yet, false if it already was
   * @throws BusinessException {@code UNKNOWN_DOCUMENT} if the document is not one of the mobility
   * @throws FatalException if the documents haven't been loaded
   */
  public boolean fillInDocument(int documentId) {
    for (Document document : loadedDocuments()) {
      if (document.getId() == documentId) {
        if (document.isFilledIn()) {
          return false;
        }
        document.setFilledIn(true);
        return true;
      }
    }
    throw new BusinessException(ErrorCode.UNKNOWN_DOCUMENT);
  }

  /**
   * Checks if all documents are filled in.
   *
   * @throws FatalException if the documents haven't been loaded
   */
  public boolean allDocumentsFilledIn() {
    return loadedDocuments().stream().allMatch(Document::isFilledIn);
  }

  /**
   * Checks if all departure documents are filled in.
   *
   * @throws FatalException if the documents haven't been loaded
   */
  public boolean allDepartureDocumentsFilledIn() {
    return allFilledIn(DocumentCategory.DEPARTURE);
  }

  /**
   * Checks if all return documents are filled in.
   *
   * @throws FatalException if the documents haven't been loaded
   */
  public boolean allReturnDocumentsFilledIn() {
    return allFilledIn(DocumentCategory.RETURN);
  }

  private boolean allFilledIn(DocumentCategory category) {
    return loadedDocuments().stream()
        .noneMatch(document -> document.getCategory() == category && !document.isFilledIn());
  }

  private List<Document> loadedDocuments() {
    if (documents == null) {
      throw new FatalException(FatalException.LAZY_LOADING_ERROR_MSG);
    }
    return documents;
  }

  /**
   * Checks if all documents are filled in.
   *
   * @throws BusinessException {@code DOCUMENTS_INCOMPLETE} if a document is not filled in
   */
  public void checkAllDocumentsFilledIn() {
    if (!allDocumentsFilledIn()) {
      throw new BusinessException(ErrorCode.DOCUMENTS_INCOMPLETE);
    }
  }

  /**
   * Checks if all departure documents are filled in.
   *
   * @throws BusinessException {@code DEPARTURE_DOCUMENTS_INCOMPLETE} otherwise
   */
  public void checkAllDepartureDocumentsFilledIn() {
    if (!allDepartureDocumentsFilledIn()) {
      throw new BusinessException(ErrorCode.DEPARTURE_DOCUMENTS_INCOMPLETE);
    }
  }

  /**
   * Checks if all return documents are filled in.
   *
   * @throws BusinessException {@code RETURN_DOCUMENTS_INCOMPLETE} otherwise
   */
  public void checkAllReturnDocumentsFilledIn() {
    if (!allReturnDocumentsFilledIn()) {
      throw new BusinessException(ErrorCode.RETURN_DOCUMENTS_INCOMPLETE);
    }
  }

  /**
   * Checks that the mobility is not cancelled.
   *
   * @throws BusinessException {@code MOBILITY_CANCELLED} if it is
   */
  public void checkNotCancelled() {
    if (state == MobilityState.CANCELLED) {
      throw new BusinessException(ErrorCode.MOBILITY_CANCELLED);
    }
  }

  /**
   * Checks that the mobility is not closed.
   *
   * @throws BusinessException {@code MOBILITY_CLOSED} if it is
   */
  public void checkNotClosed() {
    if (state == MobilityState.CLOSED) {
      throw new BusinessException(ErrorCode.MOBILITY_CLOSED);
    }
  }

  /**
   * Checks that the mobility is neither cancelled nor closed.
   *
   * @throws BusinessException {@code MOBILITY_CANCELLED} or {@code MOBILITY_CLOSED}
   */
  public void checkNotCancelledAndNotClosed() {
    checkNotCancelled();
    checkNotClosed();
  }
}
