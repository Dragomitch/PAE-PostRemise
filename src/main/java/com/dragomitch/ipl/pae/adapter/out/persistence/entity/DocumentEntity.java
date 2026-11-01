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

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A document a programme requires ({@code documents}), filled in before departure
 * ({@code D}) or after return ({@code R}). Reference data, never written by the application.
 */
@Entity
@Table(name = "documents")
public class DocumentEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "document_id")
  private Integer id;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "category", nullable = false, length = 1)
  private String category;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "programme_id", nullable = false)
  private ProgrammeEntity programme;

  @Column(name = "version", nullable = false)
  private Integer version;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
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
