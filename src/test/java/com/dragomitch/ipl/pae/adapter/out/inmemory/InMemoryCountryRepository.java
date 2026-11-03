package com.dragomitch.ipl.pae.adapter.out.inmemory;

import com.dragomitch.ipl.pae.domain.dto.CountryDto;
import com.dragomitch.ipl.pae.domain.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.domain.model.EntityFactory;
import com.dragomitch.ipl.pae.domain.port.out.CountryRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryCountryRepository implements CountryRepository {

  private Map<String, CountryDto> countries;
  private EntityFactory entityFactory;

  /**
   * Sole constructor for explicit invocation.
   *
   * @param entityFactory an on-demand object dispenser
   */
  public InMemoryCountryRepository(EntityFactory entityFactory) {
    countries = new HashMap<String, CountryDto>();
    this.entityFactory = entityFactory;
    CountryDto country1 = (CountryDto) this.entityFactory.build(CountryDto.class);
    country1.setCountryCode("GB");
    country1.setName("Angleterre");
    ProgrammeDto programme = (ProgrammeDto) this.entityFactory.build(ProgrammeDto.class);
    programme.setId(1);
    programme.setProgrammeName("Erasmus+");
    country1.setProgramme(programme);
    countries.put(country1.getCountryCode(), country1);
    CountryDto country2 = (CountryDto) this.entityFactory.build(CountryDto.class);
    country2.setCountryCode("IE");
    country2.setName("Ireland");
    country2.setProgramme(programme);
    countries.put(country2.getCountryCode(), country2);
  }

  @Override
  public CountryDto findById(String countryCode) {
    if (countries.containsKey(countryCode)) {
      return countries.get(countryCode);
    }
    return null;
  }

  @Override
  public List<CountryDto> findAll() {
    List<CountryDto> countryList = new ArrayList<CountryDto>();
    for (String key : countries.keySet()) {
      countryList.add(countries.get(key));
    }
    return countryList;
  }
}
