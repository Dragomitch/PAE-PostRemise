package com.dragomitch.ipl.pae.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Primary key of {@code partner_options}: (option, partner). */
@Embeddable
public class PartnerOptionId implements Serializable {

  private static final long serialVersionUID = 1L;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "option_code", length = 3)
  private String optionCode;

  @Column(name = "partner_id")
  private Integer partnerId;

  protected PartnerOptionId() {
  }

  public PartnerOptionId(String optionCode, Integer partnerId) {
    this.optionCode = optionCode;
    this.partnerId = partnerId;
  }

  public String getOptionCode() {
    return optionCode;
  }

  public Integer getPartnerId() {
    return partnerId;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof PartnerOptionId id && Objects.equals(optionCode, id.optionCode)
        && Objects.equals(partnerId, id.partnerId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(optionCode, partnerId);
  }
}
