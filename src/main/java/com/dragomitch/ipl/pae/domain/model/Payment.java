package com.dragomitch.ipl.pae.domain.model;

import java.time.LocalDateTime;

/** A payment of a mobility: one per payment request ({@code D} first, {@code R} second). */
public class Payment {

  private int mobilityChoiceId;
  private User user;
  private NominatedStudent nominatedStudent;
  private MobilityType mobilityType;
  private String academicYear;
  private int term;
  private Programme programme;
  private Country country;
  private Partner partner;
  private LocalDateTime paymentDate;
  private String paymentType;

  public int getMobilityChoiceId() {
    return mobilityChoiceId;
  }

  public void setMobilityChoiceId(int mobilityChoiceId) {
    this.mobilityChoiceId = mobilityChoiceId;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public NominatedStudent getNominatedStudent() {
    return nominatedStudent;
  }

  public void setNominatedStudent(NominatedStudent nominatedStudent) {
    this.nominatedStudent = nominatedStudent;
  }

  public MobilityType getMobilityType() {
    return mobilityType;
  }

  public void setMobilityType(MobilityType mobilityType) {
    this.mobilityType = mobilityType;
  }

  public String getAcademicYear() {
    return academicYear;
  }

  public void setAcademicYear(String academicYear) {
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

  public Partner getPartner() {
    return partner;
  }

  public void setPartner(Partner partner) {
    this.partner = partner;
  }

  public LocalDateTime getPaymentDate() {
    return paymentDate;
  }

  public void setPaymentDate(LocalDateTime paymentDate) {
    this.paymentDate = paymentDate;
  }

  public String getPaymentType() {
    return paymentType;
  }

  public void setPaymentType(String paymentType) {
    this.paymentType = paymentType;
  }
}
