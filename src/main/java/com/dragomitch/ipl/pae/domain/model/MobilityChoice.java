package com.dragomitch.ipl.pae.domain.model;

import java.time.LocalDateTime;

/** An application of a student for a mobility, in his order of preference. */
public class MobilityChoice {

  private int id;
  private User user;
  private int preferenceOrder;
  private MobilityType mobilityType;
  private int academicYear;
  private int term;
  private Programme programme;
  private Country country;
  private LocalDateTime submissionDate;
  private DenialReason denialReason;
  private String cancellationReason;
  private Partner partner;
  private int version;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public int getPreferenceOrder() {
    return preferenceOrder;
  }

  public void setPreferenceOrder(int preferenceOrder) {
    this.preferenceOrder = preferenceOrder;
  }

  public MobilityType getMobilityType() {
    return mobilityType;
  }

  public void setMobilityType(MobilityType mobilityType) {
    this.mobilityType = mobilityType;
  }

  public int getAcademicYear() {
    return academicYear;
  }

  public void setAcademicYear(int academicYear) {
    this.academicYear = academicYear;
  }

  public int getTerm() {
    return term;
  }

  public void setTerm(int term) {
    this.term = term;
  }

  public Programme getProgramme() {
    return programme;
  }

  public void setProgramme(Programme programme) {
    this.programme = programme;
  }

  public Country getCountry() {
    return country;
  }

  public void setCountry(Country country) {
    this.country = country;
  }

  public LocalDateTime getSubmissionDate() {
    return submissionDate;
  }

  public void setSubmissionDate(LocalDateTime submissionDate) {
    this.submissionDate = submissionDate;
  }

  public DenialReason getDenialReason() {
    return denialReason;
  }

  public void setDenialReason(DenialReason denialReason) {
    this.denialReason = denialReason;
  }

  public String getCancellationReason() {
    return cancellationReason;
  }

  public void setCancellationReason(String cancellationReason) {
    this.cancellationReason = cancellationReason;
  }

  public Partner getPartner() {
    return partner;
  }

  public void setPartner(Partner partner) {
    this.partner = partner;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  /** Tells whether the choice was cancelled by the student or rejected by a professor. */
  public boolean isClosed() {
    return cancellationReason != null || denialReason != null;
  }
}
