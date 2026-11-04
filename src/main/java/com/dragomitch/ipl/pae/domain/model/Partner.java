package com.dragomitch.ipl.pae.domain.model;

import java.util.List;

/** A partner institution or company, offering some options to the students. */
public class Partner {

  private int id;
  private String legalName;
  private String businessName;
  private String fullName;
  private String organisationType;
  private int employeeCount;
  private Address address;
  private String email;
  private String website;
  private String phoneNumber;
  private boolean official;
  private boolean archived;
  private boolean archivable;
  private List<PartnerOption> options;
  private Programme programme;
  private int version;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getLegalName() {
    return legalName;
  }

  public void setLegalName(String legalName) {
    this.legalName = legalName;
  }

  public String getBusinessName() {
    return businessName;
  }

  public void setBusinessName(String businessName) {
    this.businessName = businessName;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getOrganisationType() {
    return organisationType;
  }

  public void setOrganisationType(String organisationType) {
    this.organisationType = organisationType;
  }

  public int getEmployeeCount() {
    return employeeCount;
  }

  public void setEmployeeCount(int employeeCount) {
    this.employeeCount = employeeCount;
  }

  public Address getAddress() {
    return address;
  }

  public void setAddress(Address address) {
    this.address = address;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getWebsite() {
    return website;
  }

  public void setWebsite(String website) {
    this.website = website;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  public boolean isOfficial() {
    return official;
  }

  public void setOfficial(boolean official) {
    this.official = official;
  }

  public boolean isArchived() {
    return archived;
  }

  public void setArchived(boolean archived) {
    this.archived = archived;
  }

  public boolean isArchivable() {
    return archivable;
  }

  public void setArchivable(boolean archivable) {
    this.archivable = archivable;
  }

  public List<PartnerOption> getOptions() {
    return options;
  }

  public void setOptions(List<PartnerOption> options) {
    this.options = options;
  }

  public Programme getProgramme() {
    return programme;
  }

  public void setProgramme(Programme programme) {
    this.programme = programme;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }
}
