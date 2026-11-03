package com.dragomitch.ipl.pae.domain.model;

/**
 * The documents a CSV export of a mobility keeps: those of a category, possibly only the filled-in
 * ones. The name is the {@code filter} query parameter of the API; without a filter, the export
 * keeps every document.
 */
public enum DocumentFilter {
  /** The departure documents. */
  D(DocumentCategory.DEPARTURE, false),
  /** The filled-in departure documents. */
  DF(DocumentCategory.DEPARTURE, true),
  /** The return documents. */
  R(DocumentCategory.RETURN, false),
  /** The filled-in return documents. */
  RF(DocumentCategory.RETURN, true);

  private final DocumentCategory category;
  private final boolean filledInOnly;

  DocumentFilter(DocumentCategory category, boolean filledInOnly) {
    this.category = category;
    this.filledInOnly = filledInOnly;
  }

  /** The category of the documents kept. */
  public DocumentCategory category() {
    return category;
  }

  /** True if only the filled-in documents are kept. */
  public boolean filledInOnly() {
    return filledInOnly;
  }

  /**
   * Tells whether the export keeps a document.
   *
   * @param category the category of the document
   * @param filledIn whether the document is filled in
   * @return true if the document is of the category of the filter and, for {@link #DF} and {@link
   *     #RF}, filled in
   */
  public boolean keeps(DocumentCategory category, boolean filledIn) {
    return this.category == category && (filledIn || !filledInOnly);
  }
}
