package com.dragomitch.ipl.pae.persistence.implementations;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.persistence.CountryDao;
import com.dragomitch.ipl.pae.persistence.jdbc.entity.CountryEntity;
import com.dragomitch.ipl.pae.persistence.jdbc.entity.ProgrammeEntity;
import com.dragomitch.ipl.pae.persistence.jdbc.repository.CountryRepository;
import com.dragomitch.ipl.pae.persistence.jdbc.repository.ProgrammeRepository;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

/**
 * {@link CountryDao} on top of the Spring Data {@link CountryRepository}. A country references
 * its programme by id; the DTO embeds the whole programme, read from the
 * {@link ProgrammeRepository} (three rows, loaded at once for {@link #findAll()}).
 */
@Repository
class CountryDaoImpl implements CountryDao {

  private final EntityFactory entityFactory;
  private final CountryRepository countries;
  private final ProgrammeRepository programmes;

  CountryDaoImpl(EntityFactory entityFactory, CountryRepository countries,
      ProgrammeRepository programmes) {
    this.entityFactory = entityFactory;
    this.countries = countries;
    this.programmes = programmes;
  }

  @Override
  public CountryDto findById(String countryCode) {
    return DataAccess.call(() -> countries.findById(countryCode)
        .map(country -> toDto(country,
            programmes.findById(country.programme().getId()).orElseThrow()))
        .orElse(null));
  }

  @Override
  public List<CountryDto> findAll() {
    return DataAccess.call(() -> {
      Map<Integer, ProgrammeEntity> programmesById = programmes.findAll().stream()
          .collect(Collectors.toMap(ProgrammeEntity::id, Function.identity()));
      return countries.findAllByOrderByNameAsc().stream()
          .map(country -> toDto(country, programmesById.get(country.programme().getId())))
          .toList();
    });
  }

  private CountryDto toDto(CountryEntity entity, ProgrammeEntity programme) {
    CountryDto country = (CountryDto) entityFactory.build(CountryDto.class);
    country.setCountryCode(entity.code());
    country.setName(entity.name());
    ProgrammeDto programmeDto = ProgrammeDaoImpl.toDto(entityFactory, programme);
    country.setProgramme(programmeDto);
    return country;
  }
}
