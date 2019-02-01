package com.dragomitch.ipl.pae.business.dto;

import java.time.LocalDateTime;

public interface PaymentDto {

  int getMobilityChoiceId();

  void setMobilityChoiceId(int mobilityChoiceId);

  UserDto getUser();

  void setUser(UserDto user);

  NominatedStudentDto getNominatedStudent();

  void setNominatedStudent(NominatedStudentDto nominatedStudent);

  String getMobilityType();

  void setMobilityType(String mobilityType);

  String getAcademicYear();

  void setAcademicYear(String academicYear);

  int getTerm();

  void setTerm(int term);

  ProgrammeDto getProgramme();

  void setProgramme(ProgrammeDto programme);

  CountryDto getCountry();

  void setCountry(CountryDto country);

  PartnerDto getPartner();

  void setPartner(PartnerDto partner);

  LocalDateTime getPaymentDate();

  void setPaymentDate(LocalDateTime paymentDate);

  String getPaymentType();

  void setPaymentType(String paymentType);

  int getVersion();

  void setVersion(int version);

}
