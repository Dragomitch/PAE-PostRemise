package com.dragomitch.ipl.pae.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A country ({@code countries}), identified by its ISO 3166-1 alpha-2 code, with the programme
 * its mobilities belong to. Reference data: the application never writes it, so its
 * {@code version} column is not used for optimistic locking.
 */
@Entity
@Table(name = "countries")
public class CountryEntity {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "country_code", length = 2)
  private String code;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "programme_id", nullable = false)
  private ProgrammeEntity programme;

  @Column(name = "version")
  private Integer version;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ProgrammeEntity getProgramme() {
    return programme;
  }

  public void setProgramme(ProgrammeEntity programme) {
    this.programme = programme;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }
}
