package ucc;

import business.dto.CountryDto;

import java.util.List;

public interface CountryUcc {

  /**
   * Ask the countryDao to get all countries in the database.
   * 
   * @return a list of all the countryDto found in the database
   */
  List<CountryDto> showAll();

  /**
   * Ask the countryDao to get a countryDto existing in the database.
   * 
   * @param countryCode : the country code of the country we want to find
   * @return the countryDto found in the database
   */
  CountryDto showOne(String countryCode);
}
