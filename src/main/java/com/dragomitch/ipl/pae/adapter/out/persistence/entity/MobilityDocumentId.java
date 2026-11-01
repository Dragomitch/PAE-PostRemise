package com.dragomitch.ipl.pae.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/** Primary key of {@code mobility_documents}: (document, mobility). */
@Embeddable
public class MobilityDocumentId implements Serializable {

  private static final long serialVersionUID = 1L;

  @Column(name = "document_id")
  private Integer documentId;

  @Column(name = "mobility_id")
  private Integer mobilityId;

  protected MobilityDocumentId() {
  }

  public MobilityDocumentId(Integer documentId, Integer mobilityId) {
    this.documentId = documentId;
    this.mobilityId = mobilityId;
  }

  public Integer getDocumentId() {
    return documentId;
  }

  public Integer getMobilityId() {
    return mobilityId;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof MobilityDocumentId id && Objects.equals(documentId, id.documentId)
        && Objects.equals(mobilityId, id.mobilityId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(documentId, mobilityId);
  }
}
