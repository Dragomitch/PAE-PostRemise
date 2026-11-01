package com.dragomitch.ipl.pae.adapter.out.persistence;

import com.dragomitch.ipl.pae.adapter.out.persistence.repository.CountryJpaRepository;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.persistence.CountryDao;

import java.util.List;

import org.springframework.stereotype.Repository;

/** The countries and the programme each one belongs to (reference data). */
@Repository
class CountryPersistenceAdapter implements CountryDao {

  private final CountryJpaRepository countries;
  private final PersistenceMapper mapper;

  CountryPersistenceAdapter(CountryJpaRepository countries, PersistenceMapper mapper) {
    this.countries = countries;
    this.mapper = mapper;
  }

  @Override
  public CountryDto findById(String countryCode) {
    return countries.findWithProgrammeByCode(countryCode).map(mapper::country).orElse(null);
  }

  /** Sorted by name, as the database collates it. */
  @Override
  public List<CountryDto> findAll() {
    return countries.findAllWithProgrammeByOrderByNameAsc().stream().map(mapper::country)
        .toList();
  }
}
