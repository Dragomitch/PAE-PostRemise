package com.dragomitch.ipl.pae.domain.port.out;

import com.dragomitch.ipl.pae.domain.dto.PartnerDto;
import com.dragomitch.ipl.pae.domain.model.PartnerFilter;
import com.dragomitch.ipl.pae.domain.model.Role;
import java.util.List;

public interface PartnerRepository {

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

  /**
   * Inserts a partner into the database. A partner without website ({@code null}) is stored with an
   * empty one, as the website is optional; the same holds for {@link #update}.
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
   *     list if it has none), or null if there is no such partner
   */
  PartnerDto findById(int id);

  /**
   * Queries the database for all partners matching a filter, each with all its options.
   *
   * @param filter : the filter we want to apply: {@link PartnerFilter#ALL}, {@link
   *     PartnerFilter#COUNTRY} (country code {@code value}) or {@link PartnerFilter#ARCHIVED}
   *     (archived partners whose full name contains {@code value}, case-insensitively).
   * @param value : the value of the filter.
   * @param userRole : the role of the user; students only get the official, non-archived partners
   *     offering their option (except with the archived filter).
   * @param option : the option of the user.
   * @return a List of all PartnerDto, ordered by id
   */
  List<PartnerDto> findAll(PartnerFilter filter, String value, Role userRole, String option);

  /**
   * Update a partner in the database.
   *
   * @param partner : the partner to be updated in the database
   * @return the partnerDto that has just been updated
   */
  PartnerDto update(PartnerDto partner);
}
