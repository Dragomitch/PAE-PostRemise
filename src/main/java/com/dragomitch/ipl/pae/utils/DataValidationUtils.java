package com.dragomitch.ipl.pae.utils;

/**
 * What remains of the former hand-written validation helpers. The checks of the input data are
 * now Bean Validation constraints (on the DTO getters and on the use-case methods); only
 * {@link #isAValidString(String)} is left because the JDBC {@code MobilityChoiceDaoImpl} still
 * uses it. Delete this class when the DAOs no longer need it.
 */
public final class DataValidationUtils {

  private DataValidationUtils() {
    throw new UnsupportedOperationException();
  }

  /**
   * Checks if a string is valid.
   *
   * @param str the String to check, may be null
   * @return true if the String is non-null and not empty
   */
  public static boolean isAValidString(String str) {
    return str != null && !"".equals(str);
  }
}
