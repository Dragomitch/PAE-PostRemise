package com.dragomitch.ipl.pae.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

/**
 * A mobility programme ({@code programmes}): Erasmus+, Erabel, FAME. Reference data, never written
 * by the application.
 */
@Entity
@Immutable
@Table(name = "programmes")
public class ProgrammeEntity {

  @Id
  @Column(name = "programme_id")
  private Integer id;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "external_software_name", nullable = false, length = 100)
  private String externalSoftwareName;

  public Integer getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getExternalSoftwareName() {
    return externalSoftwareName;
  }
}
