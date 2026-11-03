package com.dragomitch.ipl.pae.adapter.out.inmemory;

import com.dragomitch.ipl.pae.domain.model.Country;
import com.dragomitch.ipl.pae.domain.model.Programme;
import com.dragomitch.ipl.pae.domain.port.out.CountryRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryCountryRepository implements CountryRepository {

  private Map<String, Country> countries;

  /** Sole constructor for explicit invocation. */
  public InMemoryCountryRepository() {
    countries = new HashMap<String, Country>();
    Country country1 = new Country();
    country1.setCountryCode("GB");
    country1.setName("Angleterre");
    Programme programme = new Programme();
    programme.setId(1);
    programme.setProgrammeName("Erasmus+");
    country1.setProgramme(programme);
    countries.put(country1.getCountryCode(), country1);
    Country country2 = new Country();
    country2.setCountryCode("IE");
    country2.setName("Ireland");
    country2.setProgramme(programme);
    countries.put(country2.getCountryCode(), country2);
  }

  @Override
  public Country findById(String countryCode) {
    if (countries.containsKey(countryCode)) {
      return countries.get(countryCode);
    }
    return null;
  }

  @Override
  public List<Country> findAll() {
    List<Country> countryList = new ArrayList<Country>();
    for (String key : countries.keySet()) {
      countryList.add(countries.get(key));
    }
    return countryList;
  }
}
