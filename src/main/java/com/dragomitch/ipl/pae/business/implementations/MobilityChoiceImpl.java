package com.dragomitch.ipl.pae.business.implementations;

import com.dragomitch.ipl.pae.business.MobilityChoice;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;

import java.io.Serializable;
import java.time.LocalDateTime;

class MobilityChoiceImpl implements MobilityChoice, Serializable {

  private static final long serialVersionUID = 1L;

  private int id;
  private UserDto user;
  private int preferenceOrder;
  private String mobilityType;
  private int academicYear;
  private int term;
  private ProgrammeDto programme;
  private CountryDto country;
  private LocalDateTime submissionDate;
  private DenialReasonDto denialReason;
  private String cancellationReason;
  private PartnerDto partner;
  private int version;

  @Override
  public int getId() {
    return id;
  }

  @Override
  public void setId(int id) {
    this.id = id;
  }

  @Override
  public UserDto getUser() {
    return user;
  }

  @Override
  public void setUser(UserDto user) {
    this.user = user;
  }

  @Override
  public int getPreferenceOrder() {
    return preferenceOrder;
  }

  @Override
  public void setPreferenceOrder(int preferenceOrder) {
    this.preferenceOrder = preferenceOrder;
  }

  @Override
  public String getMobilityType() {
    return mobilityType;
  }

  @Override
  public void setMobilityType(String mobilityType) {
    this.mobilityType = mobilityType;
  }

  @Override
  public int getAcademicYear() {
    return academicYear;
  }

  @Override
  public void setAcademicYear(int academicYear) {
    this.academicYear = academicYear;
  }

  @Override
  public int getTerm() {
    return term;
  }

  @Override
  public void setTerm(int term) {
    this.term = term;
  }

  @Override
  public ProgrammeDto getProgramme() {
    return programme;
  }

  @Override
  public void setProgramme(ProgrammeDto programme) {
    this.programme = programme;
  }

  @Override
  public CountryDto getCountry() {
    return country;
  }

  @Override
  public void setCountry(CountryDto country) {
    this.country = country;
  }

  @Override
  public LocalDateTime getSubmissionDate() {
    return submissionDate;
  }

  @Override
  public void setSubmissionDate(LocalDateTime submissionDate) {
    this.submissionDate = submissionDate;
  }

  @Override
  public DenialReasonDto getDenialReason() {
    return denialReason;
  }

  @Override
  public void setDenialReason(DenialReasonDto denialReason) {
    this.denialReason = denialReason;
  }

  @Override
  public String getCancellationReason() {
    return cancellationReason;
  }

  @Override
  public void setCancellationReason(String cancellationReason) {
    this.cancellationReason = cancellationReason;
  }

  @Override
  public PartnerDto getPartner() {
    return partner;
  }

  @Override
  public void setPartner(PartnerDto partner) {
    this.partner = partner;
  }

  @Override
  public int getVersion() {
    return version;
  }

  @Override
  public void setVersion(int version) {
    this.version = version;
  }

}
