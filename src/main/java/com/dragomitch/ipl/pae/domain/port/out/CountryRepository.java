package com.dragomitch.ipl.pae.domain.port.out;

import com.dragomitch.ipl.pae.domain.model.Country;
import java.util.List;
import java.util.Optional;

public interface CountryRepository {

  /**
   * Queries the database for a country identified by a countryCode.
   *
   * @param countryCode the country_code of the country we want to find
   * @return the country corresponding to the countryCode, or empty if there is none
   */
  Optional<Country> findById(String countryCode);

  /**
   * Queries the database for all countries.
   *
   * @return all countries
   */
  List<Country> findAll();
}
