package com.dragomitch.ipl.pae.utils;

import com.dragomitch.ipl.pae.persistence.NominatedStudentDao;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DataValidationUtils {

  private static final String REGEX_EMAIL = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
      + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
  private static final String REGEX_PHONE_NUMBER = "^\\+?[0-9\\.\\/]+$";
  private static final String REGEX_IBAN =
      "^[a-zA-Z]{2}[0-9]{2}[a-zA-Z0-9]{4}[0-9]{7}([a-zA-Z0-9]?){0,16}$";
  private static final String REGEX_BIC =
      "^([a-zA-Z]{4}[a-zA-Z]{2}[a-zA-Z0-9]{2}([a-zA-Z0-9]{3})?)$";

  private DataValidationUtils() {
    throw new UnsupportedOperationException();
  }

  /**
   * Checks if a string is valid.
   * 
   * @param str the String to check, may be null
   * @return true if the String contains more than one character and is non-null
   */
  public static boolean isAValidString(String str) {
    return str != null && !"".equals(str);
  }

  /**
   * Checks if the given String is shorter or equals than a given length.
   * 
   * @param str the String to check
   * @param maximalLength the maximal length that the String should have
   * @return true if the String shorter or equals than a given length
   */
  public static boolean isShortherOrEqualsThan(String str, int maximalLength) {
    if (str == null) {
      return false;
    }
    if (maximalLength < 0) {
      return false;
    }
    return str.length() <= maximalLength;
  }

  /**
   * Checks if the given iban respects the correct format.
   * 
   * @param iban the iban String to check, may be null
   * @return true if the String is a valid iban
   */
  public static boolean isAValidIban(String iban) {
    if (!isAValidString(iban)) {
      return false;
    }
    return stringMatchesRegex(iban, REGEX_IBAN)
        && (iban.length() <= NominatedStudentDao.IBAN_MAX_LENGTH);
  }

  /**
   * Checks if the given bic respects the correct format.
   * 
   * @param bic the bic String to check, may be null
   * @return true if the String is a valid bic
   */
  public static boolean isAValidBic(String bic) {
    if (!isAValidString(bic)) {
      return false;
    }
    return (stringMatchesRegex(bic, REGEX_BIC) && (bic.length() == NominatedStudentDao.BIC_LENGTH_1
        || bic.length() == NominatedStudentDao.BIC_LENGTH_2));
  }


  /**
   * Checks if the given email address respects the correct format.
   * 
   * @param email the email address String to check, may be null
   * @return true if the String is a valid email address
   */
  public static boolean isAValidEmail(String email) {
    if (!isAValidString(email)) {
      return false;
    }
    return stringMatchesRegex(email, REGEX_EMAIL);
  }

  /**
   * Checks if the given phone number respects the correct format.
   * 
   * @param phoneNumber the String to check, may be null
   * @return true if the String is a valid phone number, false otherwise
   */
  public static boolean isAValidPhoneNumber(String phoneNumber) {
    if (!isAValidString(phoneNumber)) {
      return false;
    }
    return stringMatchesRegex(phoneNumber, REGEX_PHONE_NUMBER);
  }

  /**
   * Checks if a String matches the given regex e.
   * 
   * @param str the String to check
   * @param regex the expression to use to verify the String
   * @return true if the String matches the regex
   */
  private static boolean stringMatchesRegex(String str, String regex) {
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(str);
    return matcher.matches();
  }

  /**
   * Checks if an Object is non-null.
   * 
   * @param object the String to check, may be null
   * @return true if the Object is non-null
   */
  public static boolean isAValidObject(Object object) {
    return object != null;
  }

  /**
   * Checks if an Integer is strictly positive.
   * 
   * @param number the Integer to check
   * @return true if the Integer is strictly positive
   */
  public static boolean isPositive(int number) {
    return number > 0;
  }

  /**
   * Checks if an Integer is positive or zero.
   * 
   * @param number the Integer to check
   * @return true if the Integer is positive or zero
   */
  public static boolean isPositiveOrZero(int number) {
    return number >= 0;
  }

  /**
   * Checks if a string is valid.
   * 
   * @param str the String to check, may be null
   * @throws IllegalArgumentException if the String doesn't contain more than one character or is
   *         null
   */
  public static void checkString(String str) {
    if (!isAValidString(str)) {
      throw new IllegalArgumentException("Invalid String argument.");
    }
  }

  /**
   * Checks if an Object is non-null.
   * 
   * @param object object the String to check, may be null
   * @throws IllegalArgumentException if the Object is null
   */
  public static void checkObject(Object object) {
    if (!isAValidObject(object)) {
      throw new IllegalArgumentException("Argument must not be null.");
    }
  }

  /**
   * Checks if an Integer is strictly positive.
   * 
   * @param number the Integer to check
   * @throws IllegalArgumentException if the Integer is not strictly positive
   */
  public static void checkPositive(int number) {
    if (!isPositive(number)) {
      throw new IllegalArgumentException("Argument must be positive.");
    }
  }

  /**
   * Checks if an Integer is positive or zero.
   * 
   * @param number the Integer to check
   * @throws IllegalArgumentException if the Integer is not positive or zero
   */
  public static void checkPositiveOrZero(int number) {
    if (!isPositiveOrZero(number)) {
      throw new IllegalArgumentException("Argument must be positive.");
    }
  }

}
