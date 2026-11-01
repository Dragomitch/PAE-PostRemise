package com.dragomitch.ipl.pae.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** A document of a mobility and whether it is filled in ({@code mobility_documents}). */
@Entity
@Table(name = "mobility_documents")
public class MobilityDocumentEntity {

  @EmbeddedId
  private MobilityDocumentId id;

  @MapsId("documentId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "document_id")
  private DocumentEntity document;

  @MapsId("mobilityId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "mobility_id")
  private MobilityEntity mobility;

  @Column(name = "is_filled_in", nullable = false)
  private boolean filledIn;

  @Version
  @Column(name = "version", nullable = false)
  private Integer version;

  public MobilityDocumentId getId() {
    return id;
  }

  public void setId(MobilityDocumentId id) {
    this.id = id;
  }

  public DocumentEntity getDocument() {
    return document;
  }

  public void setDocument(DocumentEntity document) {
    this.document = document;
  }

  public MobilityEntity getMobility() {
    return mobility;
  }

  public void setMobility(MobilityEntity mobility) {
    this.mobility = mobility;
  }

  public boolean isFilledIn() {
    return filledIn;
  }

  public void setFilledIn(boolean filledIn) {
    this.filledIn = filledIn;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }
}
