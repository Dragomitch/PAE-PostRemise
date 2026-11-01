package com.dragomitch.ipl.pae.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.ArrayList;
import java.util.List;

/**
 * A partner institution or company ({@code partners}) that hosts mobilities, with the options it
 * welcomes ({@code partner_options}).
 */
@Entity
@Table(name = "partners")
public class PartnerEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "partner_id")
  private Integer id;

  @Column(name = "legal_name", nullable = false, length = 255)
  private String legalName;

  @Column(name = "business_name", nullable = false, length = 255)
  private String businessName;

  @Column(name = "full_name", nullable = false, length = 255)
  private String fullName;

  @Column(name = "organisation_type", nullable = false, length = 60)
  private String organisationType;

  @Column(name = "employee_count", nullable = false)
  private int employeeCount;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "address", nullable = false)
  private AddressEntity address;

  @Column(name = "email", nullable = false, length = 255)
  private String email;

  @Column(name = "website", nullable = false, length = 255)
  private String website;

  @Column(name = "phone_number", nullable = false, length = 15)
  private String phoneNumber;

  @Column(name = "is_official", nullable = false)
  private boolean official;

  @Column(name = "is_archived", nullable = false)
  private boolean archived;

  @Version
  @Column(name = "version", nullable = false)
  private Integer version;

  @OneToMany(mappedBy = "partner")
  @OrderBy("id.optionCode")
  private List<PartnerOptionEntity> options = new ArrayList<>();

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
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

  public AddressEntity getAddress() {
    return address;
  }

  public void setAddress(AddressEntity address) {
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

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public List<PartnerOptionEntity> getOptions() {
    return options;
  }
}
