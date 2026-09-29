package com.dragomitch.ipl.pae.business.dto;

import com.dragomitch.ipl.pae.business.validation.FilterValueRequired;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Criteria of the partner list: no filter (every partner visible to the user), the archived
 * partners whose name contains {@code value} ({@code filter=archived}) or the partners of the
 * country {@code value} ({@code filter=country}).
 *
 * @param filter {@value #FILTER_ARCHIVED}, {@value #FILTER_COUNTRY} or {@code null}
 * @param value the value of the filter, required with a filter
 */
@FilterValueRequired
public record PartnerSearch(
    @Pattern(regexp = FILTER_ARCHIVED + "|" + FILTER_COUNTRY,
        message = "{pae.validation.PartnerSearch.filter.message}") String filter,
    @Size(max = 255) String value) {

  public static final String FILTER_ARCHIVED = "archived";
  public static final String FILTER_COUNTRY = "country";

  /** No filter. */
  public static PartnerSearch all() {
    return new PartnerSearch(null, null);
  }
}
