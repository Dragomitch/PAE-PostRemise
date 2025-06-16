package com.dragomitch.ipl.pae.business.implementations;

import static com.dragomitch.ipl.pae.utils.DataValidationUtils.checkPositive;

import com.dragomitch.ipl.pae.business.Document;
import com.dragomitch.ipl.pae.business.Mobility;
import com.dragomitch.ipl.pae.business.dto.DocumentDto;
import com.dragomitch.ipl.pae.business.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.ErrorFormat;
import com.dragomitch.ipl.pae.exceptions.FatalException; //TODO Check where we use a Fatal and Why is that inside the business layer
import com.dragomitch.ipl.pae.persistence.DaoClass;
import com.dragomitch.ipl.pae.persistence.MobilityDao;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@DaoClass(MobilityDao.class)
class MobilityImpl extends MobilityChoiceImpl implements Mobility, Serializable {

  private static final long serialVersionUID = -4899380265085074048L;

  private LocalDateTime submissionDate;
  private String state;
  private String stateBeforeCancellation;
  private List<DocumentDto> documents;
  private LocalDateTime firstPaymentRequestDate;
  private LocalDateTime secondPaymentRequestDate;
  private boolean proEcoEncoding;
  private boolean secondSoftwareEncoding;
  private UserDto professorInCharge;
  private NominatedStudentDto nominatedStudent;

  @Override
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  public LocalDateTime getSubmissionDate() {
    return submissionDate;
  }

  @Override
  public void setSubmissionDate(LocalDateTime submissionDate) {
    this.submissionDate = submissionDate;
  }

  @Override
  public String getState() {
    return state;
  }

  @Override
  public void setState(String state) {
    this.state = state;
  }

  @Override
  public String getStateBeforeCancellation() {
    return stateBeforeCancellation;
  }

  @Override
  public void setStateBeforeCancellation(String stateBeforeCancellation) {
    this.stateBeforeCancellation = stateBeforeCancellation;
  }

  @Override
  public List<DocumentDto> getDocuments() {
    return documents;
  }

  @Override
  public void setDocuments(List<DocumentDto> documents) {
    this.documents = documents;
  }

  @Override
  public LocalDateTime getFirstPaymentRequestDate() {
    return firstPaymentRequestDate;
  }

  @Override
  public void setFirstPaymentRequestDate(LocalDateTime firstPaymentRequestDate) {
    this.firstPaymentRequestDate = firstPaymentRequestDate;
  }

  @Override
  public LocalDateTime getSecondPaymentRequestDate() {
    return secondPaymentRequestDate;
  }

  @Override
  public void setSecondPaymentRequestDate(LocalDateTime secondPaymentRequestDate) {
    this.secondPaymentRequestDate = secondPaymentRequestDate;
  }

  @Override
  public boolean isEncodedInProEco() {
    return proEcoEncoding;
  }

  @Override
  public void setProEcoEncoding(boolean proEcoEncoding) {
    this.proEcoEncoding = proEcoEncoding;
  }

  @Override
  public boolean isEncodedInSecondSoftware() {
    return secondSoftwareEncoding;
  }

  @Override
  public void setSecondSoftwareEncoding(boolean secondSoftwareEncoding) {
    this.secondSoftwareEncoding = secondSoftwareEncoding;
  }

  @Override
  public UserDto getProfessorInCharge() {
    return professorInCharge;
  }

  @Override
  public void setProfessorInCharge(UserDto professorInCharge) {
    this.professorInCharge = professorInCharge;
  }

  @Override
  public NominatedStudentDto getNominatedStudent() {
    return nominatedStudent;
  }

  @Override
  public void setNominatedStudent(NominatedStudentDto nominatedStudent) {
    this.nominatedStudent = nominatedStudent;
  }

  @Override
  public boolean fillInDocument(int documentId) {
    checkPositive(documentId);
    if (documents == null) {
      throw new FatalException(FatalException.LAZY_LOADING_ERROR_MSG);
    }
    for (DocumentDto document : documents) {
      if (document.getId() == documentId) {
        if (document.isFilledIn()) {
          return false;
        } else {
          document.setFilledIn(true);
          return true;
        }
      }
    }
    throw new BusinessException(ErrorFormat.EXISTENCE_VIOLATION_DOCUMENT_505);
  }

  @Override
  public boolean allDocumentsFilledIn() {
    if (documents == null) {
      throw new FatalException(FatalException.LAZY_LOADING_ERROR_MSG);
    }
    for (DocumentDto document : documents) {
      if (!document.isFilledIn()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean allDepartureDocumentsFilledIn() {
    if (documents == null) {
      throw new FatalException(FatalException.LAZY_LOADING_ERROR_MSG);
    }
    for (DocumentDto document : documents) {
      if (document.getCategory() == Document.DEPARTURE_DOCUMENT && !document.isFilledIn()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean allReturnDocumentsFilledIn() {
    if (documents == null) {
      throw new FatalException(FatalException.LAZY_LOADING_ERROR_MSG);
    }
    for (DocumentDto document : documents) {
      if (document.getCategory() == Document.RETURN_DOCUMENT && !document.isFilledIn()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void checkAllDocumentsFilledIn() {
    if (!allDocumentsFilledIn()) {
      throw new BusinessException(ErrorFormat.ALL_DOCUMENTS_NOT_FILLED_IN_507);
    }
  }

  @Override
  public void checkAllDepartureDocumentsFilledIn() {
    if (!allDepartureDocumentsFilledIn()) {
      throw new BusinessException(ErrorFormat.ALL_DEPARTURE_DOCUMENTS_NOT_FILLED_IN_503);
    }
  }

  @Override
  public void checkAllReturnDocumentsFilledIn() {
    if (!allReturnDocumentsFilledIn()) {
      throw new BusinessException(ErrorFormat.ALL_RETURN_DOCUMENTS_NOT_FILLED_IN_504);
    }
  }

  @Override
  public void checkNotCancelled() {
    if (this.state.equals(STATE_CANCELLED)) {
      throw new BusinessException(ErrorFormat.INVALID_CANCELED_MOBILITY_STATE_501);
    }
  }

  @Override
  public void checkNotClosed() {
    if (this.state.equals(STATE_CLOSED)) {
      throw new BusinessException(ErrorFormat.INVALID_CLOSED_MOBILITY_STATE_502);
    }
  }

  @Override
  public void checkNotCancelledAndNotClosed() {
    checkNotCancelled();
    checkNotClosed();
  }
}
