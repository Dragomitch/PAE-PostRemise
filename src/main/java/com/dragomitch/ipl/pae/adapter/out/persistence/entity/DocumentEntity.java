package com.dragomitch.ipl.pae.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A document a programme requires ({@code documents}), filled in before departure
 * ({@code D}) or after return ({@code R}). Reference data, never written by the
 * application: immutable, its {@code version} column is not mapped.
 */
@Entity
@Immutable
@Table(name = "documents")
public class DocumentEntity {

  @Id
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


  public Integer getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getCategory() {
    return category;
  }
}
