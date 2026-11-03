package com.dragomitch.ipl.pae.domain.port.in;

import com.dragomitch.ipl.pae.domain.model.Country;
import java.util.List;

public interface CountryService {

  /**
   * Ask the countryRepository to get all countries in the database.
   *
   * @return a list of all the countryDto found in the database
   */
  List<Country> showAll();

  /**
   * Ask the countryRepository to get a countryDto existing in the database.
   *
   * @param countryCode : the country code of the country we want to find
   * @return the countryDto found in the database
   * @throws com.dragomitch.ipl.pae.domain.exceptions.ResourceNotFoundException if no country has
   *     this code
   */
  Country showOne(String countryCode);
}
