package com.dragomitch.ipl.pae.application;

/**
 * Names of the caches of the reference data, one per type, filled by the {@code @Cacheable} use
 * cases of this package and created by {@code config/CacheConfig}. Each cache holds the whole list
 * (key: no argument) and the entries read one at a time (key: the id or the code), as immutable
 * lists and shared domain objects that no caller modifies.
 *
 * <p>Countries, options, programmes and documents never change while the application runs (their
 * entities are {@code @Immutable}, the Flyway migrations are the only writer); the denial reasons
 * do, and {@link DenialReasonServiceImpl} empties their cache on every create and update. An
 * operator empties a cache by restarting the application or with {@code DELETE
 * /actuator/caches/<name>} (signed in).
 */
public final class ReferenceDataCaches {

  /** The countries: the list and each country by its code. */
  public static final String COUNTRIES = "countries";

  /** The options (study programmes of the school): the list. */
  public static final String OPTIONS = "options";

  /** The mobility programmes: the list and each programme by its id. */
  public static final String PROGRAMMES = "programmes";

  /** The documents to fill in, by programme id. */
  public static final String DOCUMENTS = "documents";

  /** The denial reasons: the list, emptied when a reason is created or edited. */
  public static final String DENIAL_REASONS = "denialReasons";

  private ReferenceDataCaches() {}
}
