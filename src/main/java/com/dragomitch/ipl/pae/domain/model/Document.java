package com.dragomitch.ipl.pae.domain.model;

/** A document of a mobility, to fill in before the departure or after the return. */
public class Document {

  private int id;
  private String name;
  private DocumentCategory category;
  private boolean filledIn;
  private Programme programme;
  private int version;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public DocumentCategory getCategory() {
    return category;
  }

  public void setCategory(DocumentCategory category) {
    this.category = category;
  }

  public boolean isFilledIn() {
    return filledIn;
  }

  public void setFilledIn(boolean filledIn) {
    this.filledIn = filledIn;
  }

  public Programme getProgramme() {
    return programme;
  }

  public void setProgramme(Programme programme) {
    this.programme = programme;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }
}
