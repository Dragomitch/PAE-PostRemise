package com.dragomitch.ipl.pae.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

/**
 * A confirmed mobility ({@code mobilities}). It shares its primary key with the mobility choice
 * it confirms ({@code mobility_choice_id}, mapped with {@link MapsId}).
 */
@Entity
@Table(name = "mobilities")
public class MobilityEntity {

  @Id
  @Column(name = "mobility_choice_id")
  private Integer id;

  @MapsId
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "mobility_choice_id")
  private MobilityChoiceEntity choice;

  @Column(name = "submission_date", nullable = false)
  private LocalDateTime submissionDate;

  @Column(name = "state", nullable = false, length = 30)
  private String state;

  @Column(name = "state_before_cancellation", length = 30)
  private String stateBeforeCancellation;

  @Column(name = "first_payment_request_date")
  private LocalDateTime firstPaymentRequestDate;

  @Column(name = "second_payment_request_date")
  private LocalDateTime secondPaymentRequestDate;

  @Column(name = "pro_eco_encoding")
  private Boolean proEcoEncoding;

  @Column(name = "second_software_encoding")
  private Boolean secondSoftwareEncoding;

  @Column(name = "student_cancellation_reason", length = 500)
  private String cancellationReason;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "prof_denial_reason")
  private DenialReasonEntity denialReason;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "professor_in_charge")
  private UserEntity professorInCharge;

  @Version
  @Column(name = "version", nullable = false)
  private Integer version;

  public Integer getId() {
    return id;
  }

  public MobilityChoiceEntity getChoice() {
    return choice;
  }

  public void setChoice(MobilityChoiceEntity choice) {
    this.choice = choice;
  }

  public LocalDateTime getSubmissionDate() {
    return submissionDate;
  }

  public void setSubmissionDate(LocalDateTime submissionDate) {
    this.submissionDate = submissionDate;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public String getStateBeforeCancellation() {
    return stateBeforeCancellation;
  }

  public void setStateBeforeCancellation(String stateBeforeCancellation) {
    this.stateBeforeCancellation = stateBeforeCancellation;
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

  public Boolean getProEcoEncoding() {
    return proEcoEncoding;
  }

  public void setProEcoEncoding(Boolean proEcoEncoding) {
    this.proEcoEncoding = proEcoEncoding;
  }

  public Boolean getSecondSoftwareEncoding() {
    return secondSoftwareEncoding;
  }

  public void setSecondSoftwareEncoding(Boolean secondSoftwareEncoding) {
    this.secondSoftwareEncoding = secondSoftwareEncoding;
  }

  public String getCancellationReason() {
    return cancellationReason;
  }

  public void setCancellationReason(String cancellationReason) {
    this.cancellationReason = cancellationReason;
  }

  public DenialReasonEntity getDenialReason() {
    return denialReason;
  }

  public void setDenialReason(DenialReasonEntity denialReason) {
    this.denialReason = denialReason;
  }

  public UserEntity getProfessorInCharge() {
    return professorInCharge;
  }

  public void setProfessorInCharge(UserEntity professorInCharge) {
    this.professorInCharge = professorInCharge;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }
}
