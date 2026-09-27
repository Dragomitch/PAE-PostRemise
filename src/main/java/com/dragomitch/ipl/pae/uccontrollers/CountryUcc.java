package com.dragomitch.ipl.pae.uccontrollers;

import com.dragomitch.ipl.pae.business.dto.CountryDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.validation.annotation.Validated;

@Validated
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
  CountryDto showOne(
      @NotBlank @Size(min = CountryDto.CODE_LENGTH, max = CountryDto.CODE_LENGTH)
      String countryCode);
}
