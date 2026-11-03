package com.dragomitch.ipl.pae.domain.model;

import java.util.Arrays;

/**
 * The lists of mobility choices a professor can ask for. The value is the {@code filter} query
 * parameter of the API.
 */
public enum MobilityChoiceFilter {
  /** Every choice. */
  ALL("all"),
  /**
   * The choices of the current academic year that are neither rejected nor cancelled and did not
   * become a mobility (the default).
   */
  ACTIVE("active"),
  /** The choices cancelled by their student. */
  CANCELED("canceled"),
  /** The choices rejected by a professor. */
  REJECTED("rejected"),
  /** The choices of the previous academic years. */
  PASSED("passed");

  private final String value;

  MobilityChoiceFilter(String value) {
    this.value = value;
  }

  /** The value of the query parameter, e.g. {@code active}. */
  public String value() {
    return value;
  }

  /**
   * The filter of a query parameter value.
   *
   * @param value e.g. {@code active} (case-sensitive)
   * @return the filter
   * @throws IllegalArgumentException if no filter has that value
   */
  public static MobilityChoiceFilter fromValue(String value) {
    return Arrays.stream(values())
        .filter(filter -> filter.value.equals(value))
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException("Unknown mobility choice filter: " + value));
  }
}
