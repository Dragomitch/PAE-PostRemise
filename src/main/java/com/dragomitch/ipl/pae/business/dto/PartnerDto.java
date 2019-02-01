package com.dragomitch.ipl.pae.business.dto;

import java.util.List;

public interface PartnerDto extends Entity {

  String getLegalName();

  void setLegalName(String legalName);

  String getBusinessName();

  void setBusinessName(String businessName);

  String getFullName();

  void setFullName(String fullName);

  String getOrganisationType();

  void setOrganisationType(String organisationType);

  int getEmployeeCount();

  void setEmployeeCount(int employeeCount);

  AddressDto getAddress();

  void setAddress(AddressDto address);

  String getEmail();

  void setEmail(String email);

  String getWebsite();

  void setWebsite(String website);

  String getPhoneNumber();

  void setPhoneNumber(String phoneNumber);

  boolean isOfficial();

  void setStatus(boolean status);

  void setOfficial(boolean status);

  boolean isArchived();

  void setArchived(boolean archived);

  boolean isArchivable();

  void setArchivable(boolean archivable);

  List<PartnerOptionDto> getOptions();

  void setOptions(List<PartnerOptionDto> options);

  ProgrammeDto getProgramme();

  void setProgramme(ProgrammeDto programme);

}
