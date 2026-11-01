package com.dragomitch.ipl.pae.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A mobility programme ({@code programmes}): Erasmus+, Erabel, FAME. */
@Entity
@Table(name = "programmes")
public class ProgrammeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "programme_id")
  private Integer id;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "external_software_name", nullable = false, length = 100)
  private String externalSoftwareName;

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

  public String getExternalSoftwareName() {
    return externalSoftwareName;
  }

  public void setExternalSoftwareName(String externalSoftwareName) {
    this.externalSoftwareName = externalSoftwareName;
  }
}
