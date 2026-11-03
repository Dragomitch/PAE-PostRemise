package com.dragomitch.ipl.pae.adapter.out.persistence;

import com.dragomitch.ipl.pae.adapter.out.persistence.entity.AddressEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.CountryEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.DenialReasonEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.OptionEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.ProgrammeEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.UserEntity;
import com.dragomitch.ipl.pae.domain.model.Address;
import com.dragomitch.ipl.pae.domain.model.Country;
import com.dragomitch.ipl.pae.domain.model.DenialReason;
import com.dragomitch.ipl.pae.domain.model.Option;
import com.dragomitch.ipl.pae.domain.model.Programme;
import com.dragomitch.ipl.pae.domain.model.Role;
import com.dragomitch.ipl.pae.domain.model.User;
import org.springframework.stereotype.Component;

/**
 * Maps the JPA entities to the business DTOs handed to the use cases. Each method fills in what the
 * outbound port contracts (asserted by the {@code *RepositoryIT} tests) promise, and reads only the
 * associations it needs, so a mapping never triggers more lazy loading than its query fetched.
 */
@Component
class PersistenceMapper {

  Option option(OptionEntity entity) {
    Option option = new Option();
    option.setCode(entity.getCode());
    option.setName(entity.getName());
    return option;
  }

  Programme programme(ProgrammeEntity entity) {
    Programme programme = programmeReference(entity.getId());
    programme.setProgrammeName(entity.getName());
    programme.setExternalSoftName(entity.getExternalSoftwareName());
    return programme;
  }

  /** A programme known by its id only. */
  Programme programmeReference(int id) {
    Programme programme = new Programme();
    programme.setId(id);
    return programme;
  }

  /** A country with its programme (id, name and external software). */
  Country country(CountryEntity entity) {
    Country country = countryNamed(entity);
    country.setProgramme(programme(entity.getProgramme()));
    return country;
  }

  /** A country with its code and name only. */
  Country countryNamed(CountryEntity entity) {
    Country country = new Country();
    country.setCountryCode(entity.getCode());
    country.setName(entity.getName());
    return country;
  }

  DenialReason denialReason(DenialReasonEntity entity) {
    DenialReason reason = new DenialReason();
    reason.setId(entity.getId());
    reason.setReason(entity.getReason());
    return reason;
  }

  /** An address with its country (code and name). */
  Address address(AddressEntity entity) {
    Address address = new Address();
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
  User user(UserEntity entity) {
    User user = new User();
    user.setId(entity.getId());
    user.setUsername(entity.getUsername());
    user.setLastName(entity.getLastName());
    user.setFirstName(entity.getFirstName());
    user.setEmail(entity.getEmail());
    user.setPassword(entity.getPassword());
    user.setRole(Role.fromLabel(entity.getRole()));
    user.setRegistrationDate(entity.getRegistrationDate());
    user.setVersion(entity.getVersion());
    user.setOption(option(entity.getOption()));
    return user;
  }
}
