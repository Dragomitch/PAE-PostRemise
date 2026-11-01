package com.dragomitch.ipl.pae.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

/**
 * An option a partner welcomes ({@code partner_options}), with the department of the partner
 * that hosts its students. The table has no version column.
 */
@Entity
@Table(name = "partner_options")
public class PartnerOptionEntity {

  @EmbeddedId
  private PartnerOptionId id;

  @MapsId("optionCode")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "option_code")
  private OptionEntity option;

  @MapsId("partnerId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "partner_id")
  private PartnerEntity partner;

  @Column(name = "departement", nullable = false, length = 100)
  private String department;

  public PartnerOptionId getId() {
    return id;
  }

  public void setId(PartnerOptionId id) {
    this.id = id;
  }

  public OptionEntity getOption() {
    return option;
  }

  public void setOption(OptionEntity option) {
    this.option = option;
  }

  public void setPartner(PartnerEntity partner) {
    this.partner = partner;
  }

  public String getDepartment() {
    return department;
  }

  public void setDepartment(String department) {
    this.department = department;
  }
}
