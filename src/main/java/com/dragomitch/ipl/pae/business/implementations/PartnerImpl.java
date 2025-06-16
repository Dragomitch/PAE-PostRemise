package com.dragomitch.ipl.pae.business.implementations;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Scope;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

import com.dragomitch.ipl.pae.business.Partner;
import com.dragomitch.ipl.pae.business.dto.AddressDto;
import com.dragomitch.ipl.pae.business.dto.PartnerOptionDto;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.persistence.DaoClass;
import com.dragomitch.ipl.pae.persistence.PartnerDao;

import java.io.Serializable;
import java.util.List;

@DaoClass(PartnerDao.class)
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PartnerImpl implements Partner, Serializable {

  private static final long serialVersionUID = 1L;

  private int id;
  private String legalName;
  private String businessName;
  private String fullName;
  private String organisationType;
  private int employeeCount;
  private AddressDto address;
  private String phoneNumber;
  private String email;
  private String website;
  private boolean archived;
  private boolean archivable;
  private boolean status;
  private List<PartnerOptionDto> options;
  private ProgrammeDto programme;
  private int version;

  @Override
  public int getId() {
    return this.id;
  }

  @Override
  public void setId(int id) {
    this.id = id;
  }

  @Override
  public String getLegalName() {
    return this.legalName;
  }

  @Override
  public void setLegalName(String legalName) {
    this.legalName = legalName;
  }

  @Override
  public String getBusinessName() {
    return this.businessName;
  }

  @Override
  public void setBusinessName(String businessName) {
    this.businessName = businessName;
  }

  @Override
  public String getFullName() {
    return this.fullName;
  }

  @Override
  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  @Override
  public String getOrganisationType() {
    return this.organisationType;
  }

  @Override
  public void setOrganisationType(String organisationType) {
    this.organisationType = organisationType;
  }

  @Override
  public int getEmployeeCount() {
    return this.employeeCount;

  }

  @Override
  public void setEmployeeCount(int employeeCount) {
    this.employeeCount = employeeCount;
  }

  @Override
  public AddressDto getAddress() {
    return this.address;
  }

  @Override
  public void setAddress(AddressDto address) {
    this.address = address;
  }

  @Override
  public String getEmail() {
    return this.email;
  }

  @Override
  public void setEmail(String email) {
    this.email = email;
  }

  @Override
  public String getWebsite() {
    return website;
  }

  @Override
  public void setWebsite(String website) {
    this.website = website;
  }

  @Override
  public boolean isOfficial() {
    return status;
  }

  @Override
  public void setStatus(boolean status) {
    this.status = status;
  }

  @Override
  public void setOfficial(boolean status) {
    this.status = status;
  }

  @Override
  public boolean isArchived() {
    return archived;
  }

  @Override
  public void setArchived(boolean archived) {
    this.archived = archived;
  }

  @Override
  public List<PartnerOptionDto> getOptions() {
    return options;
  }

  @Override
  public void setOptions(List<PartnerOptionDto> options) {
    this.options = options;
  }

  @Override
  public String getPhoneNumber() {
    return this.phoneNumber;
  }

  @Override
  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  @Override
  public ProgrammeDto getProgramme() {
    return this.programme;
  }

  @Override
  public void setProgramme(ProgrammeDto programme) {
    this.programme = programme;
  }

  @Override
  public int getVersion() {
    return version;
  }

  @Override
  public void setVersion(int version) {
    this.version = version;
  }

  @Override
  public boolean isArchivable() {
    return archivable;
  }

  @Override
  public void setArchivable(boolean archivable) {
    this.archivable = archivable;
  }

}
