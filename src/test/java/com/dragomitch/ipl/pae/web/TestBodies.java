package com.dragomitch.ipl.pae.web;

/**
 * Valid JSON request bodies for the controller tests: the constraints of the DTOs are checked
 * before the (mocked) use case is called, so a body must satisfy them to reach it.
 */
final class TestBodies {

  static final String USER = "{\"id\":1,\"username\":\"u\",\"password\":\"p\",\"firstName\":\"F\","
      + "\"lastName\":\"L\",\"email\":\"u@example.test\",\"option\":{\"code\":\"BIN\"}}";

  static final String ADDRESS = "{\"street\":\"Rue de la Loi\",\"number\":\"16\","
      + "\"city\":\"Bruxelles\",\"postalCode\":\"1000\",\"region\":\"\","
      + "\"country\":{\"countryCode\":\"BE\"}}";

  /** A nominated student: a user with his personal data. */
  static final String NOMINATED_STUDENT = USER.substring(0, USER.length() - 1)
      + ",\"title\":\"Mr\",\"birthdate\":\"2000-12-31\",\"nationality\":{\"countryCode\":\"BE\"},"
      + "\"address\":" + ADDRESS + ",\"phoneNumber\":\"+32470000001\",\"gender\":\"M\","
      + "\"nbrPassedYears\":1,\"iban\":\"BE68539007547034\",\"bankName\":\"Belfius\","
      + "\"bic\":\"GKCCBEBB\"}";

  static final String MOBILITY_CHOICE = "{\"user\":{\"id\":2},\"preferenceOrder\":1,"
      + "\"mobilityType\":\"SMS\",\"academicYear\":2016,\"term\":1,\"programme\":{\"id\":1}}";

  static final String PARTNER_OPTION = "{\"code\":\"BIN\",\"departement\":\"IT\"}";

  static final String PARTNER = "{\"legalName\":\"ACME SA\",\"businessName\":\"ACME\","
      + "\"fullName\":\"ACME\",\"official\":false,\"organisationType\":\"PME\","
      + "\"employeeCount\":\"12\",\"address\":" + ADDRESS + ",\"email\":\"acme@example.test\","
      + "\"phoneNumber\":\"+3221234567\",\"options\":[" + PARTNER_OPTION + "]}";

  private TestBodies() {
  }
}
