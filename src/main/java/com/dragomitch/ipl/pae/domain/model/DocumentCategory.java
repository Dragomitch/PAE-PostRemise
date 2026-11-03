package com.dragomitch.ipl.pae.domain.model;

import java.util.Arrays;

/**
 * When a document of a mobility is filled in: before the departure or after the return. The code is
 * the value stored in {@code documents.category} and sent in the JSON.
 */
public enum DocumentCategory {
  /** A document to fill in before the departure ({@code D}). */
  DEPARTURE('D'),
  /** A document to fill in after the return ({@code R}). */
  RETURN('R');

  private final char code;

  DocumentCategory(char code) {
    this.code = code;
  }

  /** The stored and serialized value: {@code D} or {@code R}. */
  public char code() {
    return code;
  }

  /**
   * The category of a code.
   *
   * @param code {@code D} or {@code R}
   * @return the category
   * @throws IllegalArgumentException if no category has that code
   */
  public static DocumentCategory fromCode(char code) {
    return Arrays.stream(values())
        .filter(category -> category.code == code)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown document category: " + code));
  }
}
