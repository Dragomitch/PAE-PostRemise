package com.dragomitch.ipl.pae.adapter.out.persistence;

import com.dragomitch.ipl.pae.adapter.out.persistence.repository.CountryJpaRepository;
import com.dragomitch.ipl.pae.domain.dto.CountryDto;
import com.dragomitch.ipl.pae.domain.port.out.CountryRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

/** The countries and the programme each one belongs to (reference data). */
@Repository
class JpaCountryRepositoryAdapter implements CountryRepository {

  private final CountryJpaRepository countries;
  private final PersistenceMapper mapper;

  JpaCountryRepositoryAdapter(CountryJpaRepository countries, PersistenceMapper mapper) {
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
    return countries.findAllWithProgrammeByOrderByNameAsc().stream().map(mapper::country).toList();
  }
}
