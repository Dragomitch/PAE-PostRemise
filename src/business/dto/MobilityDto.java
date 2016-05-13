package business.dto;

import java.time.LocalDateTime;
import java.util.List;

public interface MobilityDto extends MobilityChoiceDto {

  String STATE_CREATED = "Créée";
  String STATE_IN_PREPARATION = "En préparation";
  String STATE_TO_BE_PAID = "A payer";
  String STATE_IN_PROGRESS = "En cours";
  String STATE_BALANCE_TO_BE_PAID = "Solde à payer";
  String STATE_CLOSED = "Terminée";
  String STATE_CANCELLED = "Annulée";

  String getState();

  void setState(String state);

  String getStateBeforeCancellation();

  void setStateBeforeCancellation(String stateBeforeCanellation);

  List<DocumentDto> getDocuments();

  void setDocuments(List<DocumentDto> documents);

  LocalDateTime getFirstPaymentRequestDate();

  void setFirstPaymentRequestDate(LocalDateTime firstPaymentRequestDate);

  LocalDateTime getSecondPaymentRequestDate();

  void setSecondPaymentRequestDate(LocalDateTime secondPaymentRequestDate);

  boolean isEncodedInProEco();

  void setProEcoEncoding(boolean proEcoEncoding);

  boolean isEncodedInSecondSoftware();

  void setSecondSoftwareEncoding(boolean secondSoftwareEncoding);

  UserDto getProfessorInCharge();

  void setProfessorInCharge(UserDto professorInCharge);

  NominatedStudentDto getNominatedStudent();

  void setNominatedStudent(NominatedStudentDto nominatedStudent);

}
