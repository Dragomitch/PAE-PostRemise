package com.dragomitch.ipl.pae.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A student's wish for a mobility ({@code mobility_choices}): a programme, a term and, possibly,
 * a country and a partner. A professor turns it into a {@link MobilityEntity} (same id) when
 * confirming it, or denies it with a reason; the student can cancel it.
 */
@Entity
@Table(name = "mobility_choices")
public class MobilityChoiceEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "mobility_choice_id")
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, updatable = false)
  private UserEntity user;

  @Column(name = "preference_order", nullable = false)
  private int preferenceOrder;

  /** {@code SMS} (studies) or {@code SMP} (placement). */
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "mobility_type", nullable = false, length = 3)
  private String mobilityType;

  @Column(name = "academic_year", nullable = false)
  private int academicYear;

  /** 1 or 2. */
  @Column(name = "term", nullable = false)
  private int term;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "programme", nullable = false)
  private ProgrammeEntity programme;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "country")
  private CountryEntity country;

  /** The database's time on insert (see the persistence adapter), written by the updates. */
  @Column(name = "submission_date", nullable = false)
  private LocalDateTime submissionDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "prof_denial_reason")
  private DenialReasonEntity denialReason;

  @Column(name = "student_cancellation_reason", length = 500)
  private String cancellationReason;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "partner")
  private PartnerEntity partner;

  @Version
  @Column(name = "version", nullable = false)
  private Integer version;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public UserEntity getUser() {
    return user;
  }

  public void setUser(UserEntity user) {
    this.user = user;
  }

  public int getPreferenceOrder() {
    return preferenceOrder;
  }

  public void setPreferenceOrder(int preferenceOrder) {
    this.preferenceOrder = preferenceOrder;
  }

  public String getMobilityType() {
    return mobilityType;
  }

  public void setMobilityType(String mobilityType) {
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

  public ProgrammeEntity getProgramme() {
    return programme;
  }

  public void setProgramme(ProgrammeEntity programme) {
    this.programme = programme;
  }

  public CountryEntity getCountry() {
    return country;
  }

  public void setCountry(CountryEntity country) {
    this.country = country;
  }

  public LocalDateTime getSubmissionDate() {
    return submissionDate;
  }

  public void setSubmissionDate(LocalDateTime submissionDate) {
    this.submissionDate = submissionDate;
  }

  public DenialReasonEntity getDenialReason() {
    return denialReason;
  }

  public void setDenialReason(DenialReasonEntity denialReason) {
    this.denialReason = denialReason;
  }

  public String getCancellationReason() {
    return cancellationReason;
  }

  public void setCancellationReason(String cancellationReason) {
    this.cancellationReason = cancellationReason;
  }

  public PartnerEntity getPartner() {
    return partner;
  }

  public void setPartner(PartnerEntity partner) {
    this.partner = partner;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }
}
