package com.dragomitch.ipl.pae.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A study option of the school ({@code options}), identified by its three-letter code. */
@Entity
@Table(name = "options")
public class OptionEntity {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "option_code", length = 3)
  private String code;

  @Column(name = "name", nullable = false, length = 50)
  private String name;

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
}
