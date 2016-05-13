package business.dto;

import java.time.LocalDateTime;

public interface MobilityChoiceDto extends Entity {

  UserDto getUser();

  void setUser(UserDto user);

  int getPreferenceOrder();

  void setPreferenceOrder(int preferenceOrder);

  String getMobilityType();

  void setMobilityType(String mobilityType);

  int getAcademicYear();

  void setAcademicYear(int academicYear);

  int getTerm();

  void setTerm(int term);

  ProgrammeDto getProgramme();

  void setProgramme(ProgrammeDto programme);

  CountryDto getCountry();

  void setCountry(CountryDto country);

  LocalDateTime getSubmissionDate();

  void setSubmissionDate(LocalDateTime submissionDate);

  DenialReasonDto getDenialReason();

  void setDenialReason(DenialReasonDto denialReason);

  String getCancellationReason();

  void setCancellationReason(String cancellationReason);

  PartnerDto getPartner();

  void setPartner(PartnerDto partner);

}
