package com.dragomitch.ipl.pae.persistence;

import com.dragomitch.ipl.pae.business.dto.PaymentDto;

import java.util.List;

public interface PaymentDao {
  String TABLE_MOBILITY_CHOICES_NAME = "mobility_choices";
  String TABLE_USERS_NAME = "users";
  String TABLE_MOBILITIES_NAME = "mobilities";
  String TABLE_PROGRAMMES_NAME = "programmes";
  String TABLE_COUNTRIES_NAME = "countries";
  String TABLE_PARTNERS_NAME = "partners";

  String COLUMN_MOBILITY_CHOICE_ID = "mobility_choice_id";
  String COLUMN_MOBILITY_TYPE = "mobility_type";
  String COLUMN_ACADEMIC_YEAR = "academic_year";
  String COLUMN_TERM = "term";
  String COLUMN_PROGRAMME = "programme";
  String COLUMN_COUNTRY = "country";
  String COLUMN_PARTNER = "partner";
  String COLUMN_PARTNER_ID = "partner_id";


  String COLUMN_PROGRAMME_ID = "programme_id";
  String COLUMN_NAME = "name";
  String COLUMN_COUNTRY_CODE = "country_code";

  String COLUMN_USER_ID = "user_id";
  String COLUMN_FIRST_NAME = "first_name";
  String COLUMN_LAST_NAME = "last_name";

  String COLUMN_FULL_NAME = "full_name";

  String COLUMN_FIRST_PAYMENT_REQUEST_DATE = "first_payment_request_date";
  String COLUMN_SECOND_PAYMENT_REQUEST_DATE = "second_payment_request_date";

  /**
   * Queries the database for all payments.
   * 
   * @return all payments
   */
  List<PaymentDto> findAll();

}
