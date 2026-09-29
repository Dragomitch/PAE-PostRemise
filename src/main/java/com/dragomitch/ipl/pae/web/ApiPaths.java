package com.dragomitch.ipl.pae.web;

/**
 * Paths shared by the REST controllers.
 */
public final class ApiPaths {

  /** Prefix of every REST endpoint. */
  public static final String BASE = "/api/1.0";

  /** Media type of the CSV exports. */
  public static final String TEXT_CSV_UTF8 = "text/csv;charset=UTF-8";

  /** Role expressions for {@code @PreAuthorize}. */
  public static final String PROFESSOR = "hasRole('PROFESSOR')";
  public static final String STUDENT = "hasRole('STUDENT')";
  public static final String PROFESSOR_OR_STUDENT = "hasAnyRole('PROFESSOR', 'STUDENT')";

  private ApiPaths() {
  }
}
