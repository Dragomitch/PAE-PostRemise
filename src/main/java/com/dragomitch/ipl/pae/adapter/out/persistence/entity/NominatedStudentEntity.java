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

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * The personal data of a student nominated for a mobility ({@code nominated_students}). It
 * shares its primary key with the user ({@code user_id}, mapped with {@link MapsId}).
 */
@Entity
@Table(name = "nominated_students")
public class NominatedStudentEntity {

  @Id
  @Column(name = "user_id")
  private Integer id;

  @MapsId
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private UserEntity user;

  /** {@code Mr}, {@code M}, {@code Mrs} or {@code Ms}. */
  @Column(name = "title", nullable = false, length = 3)
  private String title;

  /** Stored as a {@code TIMESTAMP} at midnight. */
  @Column(name = "birthdate", nullable = false)
  private LocalDateTime birthdate;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "nationality", nullable = false)
  private CountryEntity nationality;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "address", nullable = false, updatable = false)
  private AddressEntity address;

  @Column(name = "phone_number", nullable = false, length = 15)
  private String phoneNumber;

  /** {@code M}, {@code F} or {@code O}. */
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "gender", nullable = false, length = 1)
  private String gender;

  @Column(name = "passed_years_count", nullable = false)
  private int passedYearsCount;

  @Column(name = "iban", nullable = false, length = 31)
  private String iban;

  @Column(name = "card_holder", length = 35)
  private String cardHolder;

  @Column(name = "bank_name", nullable = false, length = 60)
  private String bankName;

  @Column(name = "bic", nullable = false, length = 12)
  private String bic;

  @Version
  @Column(name = "version")
  private Integer version;

  public Integer getId() {
    return id;
  }

  public UserEntity getUser() {
    return user;
  }

  public void setUser(UserEntity user) {
    this.user = user;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public LocalDateTime getBirthdate() {
    return birthdate;
  }

  public void setBirthdate(LocalDateTime birthdate) {
    this.birthdate = birthdate;
  }

  public CountryEntity getNationality() {
    return nationality;
  }

  public void setNationality(CountryEntity nationality) {
    this.nationality = nationality;
  }

  public AddressEntity getAddress() {
    return address;
  }

  public void setAddress(AddressEntity address) {
    this.address = address;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  public String getGender() {
    return gender;
  }

  public void setGender(String gender) {
    this.gender = gender;
  }

  public int getPassedYearsCount() {
    return passedYearsCount;
  }

  public void setPassedYearsCount(int passedYearsCount) {
    this.passedYearsCount = passedYearsCount;
  }

  public String getIban() {
    return iban;
  }

  public void setIban(String iban) {
    this.iban = iban;
  }

  public String getCardHolder() {
    return cardHolder;
  }

  public void setCardHolder(String cardHolder) {
    this.cardHolder = cardHolder;
  }

  public String getBankName() {
    return bankName;
  }

  public void setBankName(String bankName) {
    this.bankName = bankName;
  }

  public String getBic() {
    return bic;
  }

  public void setBic(String bic) {
    this.bic = bic;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }
}
