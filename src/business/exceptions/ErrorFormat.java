package business.exceptions;

import java.util.List;

/**
 * Class used to format an error which has occured during the process of a request.
 */
public class ErrorFormat {

  public static final int INVALID_INPUT_DATA_110 = 110;
  public static final int CONCURRENT_MODIFICATION_120 = 120;
  public static final int INVALID_PARAMETERS_130 = 130;

  public static final int EXISTENCE_VIOLATION_USER_NULL_132 = 132;
  public static final int EXISTENCE_VIOLATION_MOBILITY_CHOICE_NULL_133 = 133;
  public static final int EXISTENCE_VIOLATION_DENIAL_REASON_NULL_134 = 134;
  public static final int EXISTENCE_VIOLATION_MOBILITY_NULLL_135 = 135;
  public static final int EXISTENCE_VIOLATION_NOMINATED_STUDENT_NULL_136 = 136;
  public static final int EXISTENCE_VIOLATION_PARTNER_NULL_137 = 137;
  public static final int EXISTENCE_VIOLATION_ADDRESS_NULL_138 = 138;
  public static final int EXISTENCE_VIOLATION_COUNTRY_NULL_139 = 139;
  public static final int EXISTENCE_VIOLATION_PROGRAMME_NULL_140 = 140;

  public static final int EXISTENCE_VIOLATION_USER_ID_200 = 200;
  public static final int INVALID_FIRST_NAME_201 = 201;
  public static final int INVALID_LAST_NAME_202 = 202;
  public static final int INVALID_USERNAME_203 = 203;
  public static final int UNICITY_VIOLATION_USERNAME_204 = 204;
  public static final int INVALID_PASSWORD_205 = 205;
  public static final int INVALID_EMAIL_206 = 206;
  public static final int UNICITY_VIOLATION_EMAIL_207 = 207;
  public static final int INVALID_OPTION_208 = 208;
  public static final int INVALID_OPTION_CODE_209 = 209;
  public static final int EXISTENCE_VIOLATION_OPTION_210 = 210;
  public static final int FIRST_NAME_MAX_LENGTH_OVERFLOW_211 = 211;
  public static final int LAST_NAME_MAX_LENGTH_OVERFLOW_212 = 212;
  public static final int USERNAME_MAX_LENGTH_OVERFLOW_213 = 213;
  public static final int PASSWORD_MAX_LENGTH_OVERFLOW_214 = 214;
  public static final int EMAIL_MAX_LENGTH_OVERFLOW_215 = 215;
  public static final int INVALID_OPTION_CODE_LENGTH_216 = 216;

  public static final int EXISTENCE_VIOLATION_MOBILITY_CHOICE_ID_300 = 300;
  public static final int MOBILITY_CHOICE_ALREADY_CONFIRMED_301 = 301;
  public static final int INVALID_PREFERENCE_ORDER_302 = 302;
  public static final int INVALID_PREFERENCE_ORDER_VALUE_303 = 303;
  public static final int INVALID_MOBILITY_TYPE_304 = 304;
  public static final int MAX_LENGTH_MOBILITY_TYPE_OVERFLOW_305 = 305;
  public static final int INVALID_MOBILITY_TYPE_VALUE_306 = 306;
  public static final int INVALID_ACADEMIC_YEAR_307 = 307;
  public static final int INVALID_TERM_308 = 308;
  public static final int INVALID_TERM_VALUE_309 = 309;
  public static final int INVALID_USER_ID_311 = 311;
  public static final int MAX_LENGTH_COUNTRY_CODE_OVERFLOW_314 = 314;
  public static final int INVALID_STATE_MOBILITY_CHOICE_317 = 317;
  public static final int MOBILITY_CHOICE_DONT_EXIST_319 = 319;
  public static final int COUNTRY_CHANGE_NOT_ALLOWED_320 = 320;
  public static final int MOBILITY_CHOICE_ALREADY_CONFIRMED_321 = 321;
  public static final int INVALID_PROFESSOR_MOBILITY_CHOICE_CREATION_322 = 322;
  public static final int INVALID_MOBILITY_CHOICE_FILTER_323 = 323;
  public static final int CONFIRM_WITHOUT_PARTNER_324 = 324;

  public static final int EXISTENCE_VIOLATION_DENIAL_REASON_ID_400 = 400;
  public static final int INVALID_REASON_401 = 401;
  public static final int MAX_LENGTH_REASON_OVERFLOW_402 = 402;

  public static final int INVALID_CANCELED_MOBILITY_STATE_501 = 501;
  public static final int INVALID_CLOSED_MOBILITY_STATE_502 = 502;
  public static final int ALL_DEPARTURE_DOCUMENTS_NOT_FILLED_IN_503 = 503;
  public static final int ALL_RETURN_DOCUMENTS_NOT_FILLED_IN_504 = 504;
  public static final int EXISTENCE_VIOLATION_DOCUMENT_505 = 505;
  public static final int INCOMPLETE_BANK_DETAILS_506 = 506;
  public static final int ALL_DOCUMENTS_NOT_FILLED_IN_507 = 507;
  public static final int INVALID_DOCUMENT_FILTER_508 = 508;

  public static final int INVALID_TITLE_601 = 601;
  public static final int INVALID_TITLE_VALUE_602 = 602;
  public static final int TITLE_MAX_LENGTH_OVERFLOW_603 = 603;
  public static final int INVALID_BIRTHDATE_NULL_604 = 604;
  public static final int INVALID_BIRTHDATE_605 = 605;
  public static final int INVALID_NATIONALITY_NULL_606 = 606;
  public static final int INVALID_ADDRESS_NUL_608 = 608;
  public static final int PHONE_NUMBER_MAX_LENGTH_OVERFLOW_609 = 609;
  public static final int INVALID_GENDER_610 = 610;
  public static final int INVALID_GENDER_VALUE_611 = 611;
  public static final int INVALID_NUMBER_OF_PASSED_YEARS_612 = 612;
  public static final int INVALID_IBAN_613 = 613;
  public static final int CARD_HOLDER_MAX_LENGTH_OVERFLOW_614 = 614;
  public static final int INVALID_BANK_NAME_615 = 615;
  public static final int BANK_NAME_MAX_LENGTH_OVERFLOW_616 = 616;
  public static final int INVALID_BIC_617 = 617;
  public static final int ALREADY_NOMINATED_STUDENT_618 = 618;
  public static final int INVALID_PHONE_NUMBER_619 = 619;

  public static final int EXISTENCE_VIOLATION_PARTNER_ID = 700;
  public static final int INVALID_LEGAL_NAME_701 = 701;
  public static final int INVALID_BUSINESS_NAME_702 = 702;
  public static final int INVALID_FULL_NAME_703 = 703;
  public static final int INVALID_ORGANISATION_TYPE_704 = 704;
  public static final int INVALID_EMPLOYEE_COUNT_705 = 705;
  public static final int INVALID_EMAIL_706 = 706;
  public static final int INVALID_WEBSITE_707 = 707;
  public static final int INVALID_PHONE_NUMBER_708 = 708;
  public static final int INVALID_PARTNER_FILTER_709 = 709;
  public static final int EXISTENCE_VIOLATION_ARCHIVING_710 = 710;
  public static final int PARTNER_NOT_ARCHIVED_711 = 711;


  public static final int EXISTENCE_VIOLATION_ADDRESS_ID_800 = 800;
  public static final int INVALID_STREET_801 = 801;
  public static final int INVALID_STREET_NUMBER_802 = 802;
  public static final int INVALID_CITY_803 = 803;
  public static final int INVALID_POSTAL_CODE_804 = 804;
  public static final int INVALID_REGION_805 = 805;
  public static final int STREET_MAX_LENGTH_OVERFLOW_806 = 806;
  public static final int CITY_MAX_LENGTH_OVERFLOW_807 = 807;
  public static final int POSTAL_CODE_MAX_LENGTH_OVERFLOW_808 = 808;
  public static final int REGION_MAX_LENGTH_OVERFLOW_809 = 809;
  public static final int STREET_NUMBER_MAX_LENGTH_OVERFLOW_810 = 810;


  public static final int EXISTENCE_VIOLATION_COUNTRY_CODE_900 = 900;

  public static final int EXISTENCE_VIOLATION_PROGRAMME_ID_1000 = 1000;

  public static final int TROLL_OVERFLOW_9000 = 9000;// ;)

  private int errorCode;
  private String developerMessage;
  private String userMessage;
  private List<ErrorFormat> details;

  public ErrorFormat() {}

  public ErrorFormat(int errorCode) {
    this.errorCode = errorCode;
  }

  public int getErrorCode() {
    return errorCode;
  }

  public String getDeveloperMessage() {
    return developerMessage;
  }

  public String getUserMessage() {
    return userMessage;
  }

  public List<ErrorFormat> getDetails() {
    return details;
  }

  public void setErrorCode(int errorCode) {
    this.errorCode = errorCode;
  }

  public void setDeveloperMessage(String developerMessage) {
    this.developerMessage = developerMessage;
  }

  public void setUserMessage(String userMessage) {
    this.userMessage = userMessage;
  }

  public void setDetails(List<ErrorFormat> details) {
    this.details = details;
  }

  /**
   * Creates a clone of the calling instance.
   */
  public ErrorFormat buildClone() {
    ErrorFormat error = new ErrorFormat();
    error.setErrorCode(this.errorCode);
    error.setDeveloperMessage(this.developerMessage);
    error.setDetails(this.details);
    error.setUserMessage(this.userMessage);
    return error;
  }

  @Override
  public String toString() {
    return "Error : " + developerMessage;
  }
}
