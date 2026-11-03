package com.dragomitch.ipl.pae.domain.model;

import java.util.Arrays;

/**
 * The lists of partners: every partner visible to the user, the archived partners whose name
 * contains a value, or the partners of a country. The value is the {@code filter} query parameter
 * of the API (absent for {@link #ALL}).
 */
public enum PartnerFilter {
  /** Every partner visible to the user. */
  ALL("all"),
  /** The archived partners whose full name contains the value, case-insensitively. */
  ARCHIVED("archived"),
  /** The partners of the country whose code is the value. */
  COUNTRY("country");

  private final String value;

  PartnerFilter(String value) {
    this.value = value;
  }

  /** The value of the query parameter, e.g. {@code archived}. */
  public String value() {
    return value;
  }

  /**
   * The filter of a query parameter value.
   *
   * @param value e.g. {@code country} (case-sensitive)
   * @return the filter
   * @throws IllegalArgumentException if no filter has that value
   */
  public static PartnerFilter fromValue(String value) {
    return Arrays.stream(values())
        .filter(filter -> filter.value.equals(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown partner filter: " + value));
  }
}
