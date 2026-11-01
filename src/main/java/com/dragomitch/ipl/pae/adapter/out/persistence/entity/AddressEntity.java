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

/** A postal address ({@code addresses}) of a partner or of a nominated student. */
@Entity
@Table(name = "addresses")
public class AddressEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "address_id")
  private Integer id;

  @Column(name = "street", nullable = false, length = 80)
  private String street;

  @Column(name = "number", nullable = false, length = 10)
  private String number;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "country", nullable = false)
  private CountryEntity country;

  @Column(name = "city", nullable = false, length = 60)
  private String city;

  @Column(name = "postal_code", nullable = false, length = 10)
  private String postalCode;

  @Column(name = "region", length = 60)
  private String region;

  @Version
  @Column(name = "version", nullable = false)
  private Integer version;

  public Integer getId() {
    return id;
  }

  public String getStreet() {
    return street;
  }

  public void setStreet(String street) {
    this.street = street;
  }

  public String getNumber() {
    return number;
  }

  public void setNumber(String number) {
    this.number = number;
  }

  public CountryEntity getCountry() {
    return country;
  }

  public void setCountry(CountryEntity country) {
    this.country = country;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getPostalCode() {
    return postalCode;
  }

  public void setPostalCode(String postalCode) {
    this.postalCode = postalCode;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }
}
