package com.dragomitch.ipl.pae.adapter.out.persistence;

import com.dragomitch.ipl.pae.adapter.out.persistence.entity.AddressEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.CountryEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.DenialReasonEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.OptionEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.ProgrammeEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.UserEntity;
import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.AddressDto;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.business.dto.OptionDto;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;

import org.springframework.stereotype.Component;

/**
 * Maps the JPA entities to the business DTOs handed to the use cases. Each method fills in what
 * the outbound port contracts (asserted by the {@code *DaoIT} tests) promise, and reads only the
 * associations it needs, so a mapping never triggers more lazy loading than its query fetched.
 */
@Component
class PersistenceMapper {

  private final EntityFactory entityFactory;

  PersistenceMapper(EntityFactory entityFactory) {
    this.entityFactory = entityFactory;
  }

  <T> T build(Class<T> type) {
    return type.cast(entityFactory.build(type));
  }

  OptionDto option(OptionEntity entity) {
    OptionDto option = build(OptionDto.class);
    option.setCode(entity.getCode());
    option.setName(entity.getName());
    return option;
  }

  ProgrammeDto programme(ProgrammeEntity entity) {
    ProgrammeDto programme = programmeReference(entity.getId());
    programme.setProgrammeName(entity.getName());
    programme.setExternalSoftName(entity.getExternalSoftwareName());
    return programme;
  }

  /** A programme known by its id only. */
  ProgrammeDto programmeReference(int id) {
    ProgrammeDto programme = build(ProgrammeDto.class);
    programme.setId(id);
    return programme;
  }

  /** A country with its programme (id, name and external software). */
  CountryDto country(CountryEntity entity) {
    CountryDto country = countryNamed(entity);
    country.setProgramme(programme(entity.getProgramme()));
    return country;
  }

  /** A country with its code and name only. */
  CountryDto countryNamed(CountryEntity entity) {
    CountryDto country = build(CountryDto.class);
    country.setCountryCode(entity.getCode());
    country.setName(entity.getName());
    return country;
  }

  DenialReasonDto denialReason(DenialReasonEntity entity) {
    DenialReasonDto reason = build(DenialReasonDto.class);
    reason.setId(entity.getId());
    reason.setReason(entity.getReason());
    return reason;
  }

  /** An address with its country (code and name). */
  AddressDto address(AddressEntity entity) {
    AddressDto address = build(AddressDto.class);
    address.setId(entity.getId());
    address.setStreet(entity.getStreet());
    address.setNumber(entity.getNumber());
    address.setCountry(countryNamed(entity.getCountry()));
    address.setCity(entity.getCity());
    address.setPostalCode(entity.getPostalCode());
    address.setRegion(entity.getRegion());
    address.setVersion(entity.getVersion());
    return address;
  }

  /** A user with every column, its option (code and name) included. */
  UserDto user(UserEntity entity) {
    UserDto user = build(UserDto.class);
    user.setId(entity.getId());
    user.setUsername(entity.getUsername());
    user.setLastName(entity.getLastName());
    user.setFirstName(entity.getFirstName());
    user.setEmail(entity.getEmail());
    user.setPassword(entity.getPassword());
    user.setRole(entity.getRole());
    user.setRegistrationDate(entity.getRegistrationDate());
    user.setVersion(entity.getVersion());
    user.setOption(option(entity.getOption()));
    return user;
  }
}
