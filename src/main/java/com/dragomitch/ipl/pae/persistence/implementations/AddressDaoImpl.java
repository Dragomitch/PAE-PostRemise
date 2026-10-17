package com.dragomitch.ipl.pae.persistence.implementations;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.AddressDto;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.persistence.AddressDao;
import com.dragomitch.ipl.pae.persistence.jdbc.entity.AddressEntity;
import com.dragomitch.ipl.pae.persistence.jdbc.entity.CountryEntity;
import com.dragomitch.ipl.pae.persistence.jdbc.repository.AddressRepository;
import com.dragomitch.ipl.pae.persistence.jdbc.repository.CountryRepository;

import org.springframework.data.jdbc.core.JdbcAggregateOperations;
import org.springframework.stereotype.Repository;

/**
 * {@link AddressDao} on top of the Spring Data {@link AddressRepository}. The optimistic lock is
 * the {@code @Version} of {@link AddressEntity}; the DTO gets the version Spring Data wrote. The
 * country of the DTO carries its code and name (not its programme).
 */
@Repository
class AddressDaoImpl implements AddressDao {

  private final EntityFactory entityFactory;
  private final AddressRepository addresses;
  private final CountryRepository countries;
  private final JdbcAggregateOperations aggregates;

  AddressDaoImpl(EntityFactory entityFactory, AddressRepository addresses,
      CountryRepository countries, JdbcAggregateOperations aggregates) {
    this.entityFactory = entityFactory;
    this.addresses = addresses;
    this.countries = countries;
    this.aggregates = aggregates;
  }

  @Override
  public AddressDto create(AddressDto address) {
    AddressEntity created = DataAccess.call(() -> aggregates.insert(toEntity(address, null, 0)));
    address.setId(created.id());
    address.setVersion(created.version());
    return address;
  }

  @Override
  public AddressDto findById(int id) {
    return DataAccess.call(() -> addresses.findById(id).map(this::toDto).orElse(null));
  }

  @Override
  public AddressDto update(AddressDto address) {
    AddressEntity updated = DataAccess.call(
        () -> aggregates.update(toEntity(address, address.getId(), address.getVersion())));
    address.setVersion(updated.version());
    return address;
  }

  private static AddressEntity toEntity(AddressDto address, Integer id, int version) {
    return new AddressEntity(id, address.getStreet(), address.getNumber(),
        DataAccess.reference(address.getCountry().getCountryCode()), address.getCity(),
        address.getPostalCode(), address.getRegion(), version);
  }

  private AddressDto toDto(AddressEntity entity) {
    AddressDto address = (AddressDto) entityFactory.build(AddressDto.class);
    address.setId(entity.id());
    address.setStreet(entity.street());
    address.setNumber(entity.number());
    CountryDto country = (CountryDto) entityFactory.build(CountryDto.class);
    country.setCountryCode(entity.country().getId());
    country.setName(countries.findById(entity.country().getId()).map(CountryEntity::name)
        .orElseThrow());
    address.setCountry(country);
    address.setCity(entity.city());
    address.setPostalCode(entity.postalCode());
    address.setRegion(entity.region());
    address.setVersion(entity.version());
    return address;
  }
}
