package com.dragomitch.ipl.pae.persistence;

import com.dragomitch.ipl.pae.business.dto.PartnerDto;

import java.util.List;

public interface PartnerDao {

  String TABLE_NAME = "partners";
  String COLUMN_ID = "partner_id";
  String COLUMN_LEGAL_NAME = "legal_name";
  String COLUMN_BUSINESS_NAME = "business_name";
  String COLUMN_FULL_NAME = "full_name";
  String COLUMN_ORGANISATION_TYPE = "organisation_type";
  String COLUMN_EMPLOYEE_COUNT = "employee_count";
  String COLUMN_ADDRESS = "address";
  String COLUMN_EMAIL = "email";
  String COLUMN_WEBSITE = "website";
  String COLUMN_PHONE_NUMBER = "phone_number";
  String COLUMN_STATUS_OFFICIAL = "is_official";
  String COLUMN_ARCHIVE = "is_archived";
  String COLUMN_VERSION = "version";

  String FILTER_ALL_PARTNERS = "all";
  String FILTER_ARCHIVED_PARTNERS = "archived";
  String FILTER_COUNTRY = "country";

  /**
   * Inserts a partner into the database.
   * 
   * @param partner : the partnerDto to be inserted in the database
   * @return the PartnerDto that has just been inserted
   */
  PartnerDto create(PartnerDto partner);

  /**
   * Queries the database for a partner based on it's partner_id.
   * 
   * @param id : the partner_id of the partner we want to find
   * @return the PartnerDto containing the data we were looking for, with all its options (an empty
   *         list if it has none), or null if there is no such partner
   */
  PartnerDto findById(int id);

  /**
   * Queries the database for all partners matching a filter, each with all its options.
   * 
   * @param filter : the filter we want to apply: {@link #FILTER_ALL_PARTNERS},
   *        {@link #FILTER_COUNTRY} (country code {@code value}) or {@link #FILTER_ARCHIVED_PARTNERS}
   *        (archived partners whose full name contains {@code value}, case-insensitively).
   * @param value : the value of the filter.
   * @param userRole : the role of the user; students only get the official, non-archived partners
   *        offering their option (except with the archived filter).
   * @param option : the option of the user.
   * @return a List of all PartnerDto, ordered by id
   * @throws IllegalArgumentException if the filter is unknown (the use cases only pass the filters
   *         allowed by their {@code @Pattern} constraints)
   */
  List<PartnerDto> findAll(String filter, String value, String userRole, String option);

  /**
   * Update a partner in the database.
   * 
   * @param partner : the partner to be updated in the database
   * @return the partnerDto that has just been updated
   */
  PartnerDto update(PartnerDto partner);

}
