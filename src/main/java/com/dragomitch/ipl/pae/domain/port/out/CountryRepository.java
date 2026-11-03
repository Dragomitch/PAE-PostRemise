package com.dragomitch.ipl.pae.domain.port.out;

import com.dragomitch.ipl.pae.domain.model.Country;
import java.util.List;

public interface CountryRepository {

  String TABLE_NAME = "countries";
  String COLUMN_CODE = "country_code";
  String COLUMN_NAME = "name";
  String COLUMN_PROGRAMME_ID = "programme_id";

  int CODE_LENGTH = 2;

  /**
   * Queries the database for a country identified by a countryCode.
   *
   * @param countryCode the country_code of the country we want to find
   * @return the countryDto corresponding to the countryCode
   */
  Country findById(String countryCode);

  /**
   * Queries the database for all countries.
   *
   * @return all countries
   */
  List<Country> findAll();
}
