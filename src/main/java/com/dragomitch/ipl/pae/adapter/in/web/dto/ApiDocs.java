package com.dragomitch.ipl.pae.adapter.in.web.dto;

/** Texts of the OpenAPI document shared by several web DTOs. */
final class ApiDocs {

  /** Description of the {@code version} of a versioned entity. */
  static final String VERSION =
      "Optimistic locking: send back the version that was read; a stale one is "
          + "refused with a 409 CONCURRENT_MODIFICATION. Every update increments it.";

  private ApiDocs() {}
}
