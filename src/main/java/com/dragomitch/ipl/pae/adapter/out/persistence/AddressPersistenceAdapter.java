package com.dragomitch.ipl.pae.adapter.out.persistence;

import com.dragomitch.ipl.pae.adapter.out.persistence.entity.AddressEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.CountryEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.repository.AddressRepository;
import com.dragomitch.ipl.pae.business.dto.AddressDto;
import com.dragomitch.ipl.pae.persistence.AddressDao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.ConcurrentModificationException;

import org.springframework.stereotype.Repository;

/** The postal addresses of the partners and of the nominated students. */
@Repository
class AddressPersistenceAdapter implements AddressDao {

  private final AddressRepository addresses;
  private final PersistenceMapper mapper;

  @PersistenceContext
  private EntityManager entityManager;

  AddressPersistenceAdapter(AddressRepository addresses, PersistenceMapper mapper) {
    this.addresses = addresses;
    this.mapper = mapper;
  }

  /** Stores the address at version 1 and sets its generated id and version on the DTO. */
  @Override
  public AddressDto create(AddressDto address) {
    AddressEntity entity = new AddressEntity();
    copy(address, entity);
    entity.setVersion(1);
    entityManager.persist(entity);
    address.setId(entity.getId());
    address.setVersion(entity.getVersion());
    return address;
  }

  @Override
  public AddressDto findById(int id) {
    return addresses.findWithCountryById(id).map(mapper::address).orElse(null);
  }

  /**
   * {@inheritDoc}
   *
   * @throws ConcurrentModificationException if the address does not exist or its version is not
   *         the DTO's (it was changed since it was read)
   */
  @Override
  public AddressDto update(AddressDto address) {
    AddressEntity entity = addresses.findById(address.getId())
        .filter(found -> found.getVersion() == address.getVersion())
        .orElseThrow(ConcurrentModificationException::new);
    copy(address, entity);
    addresses.flush();
    address.setVersion(entity.getVersion());
    return address;
  }

  private void copy(AddressDto address, AddressEntity entity) {
    entity.setStreet(address.getStreet());
    entity.setNumber(address.getNumber());
    entity.setCountry(entityManager.getReference(CountryEntity.class,
        address.getCountry().getCountryCode()));
    entity.setCity(address.getCity());
    entity.setPostalCode(address.getPostalCode());
    entity.setRegion(address.getRegion());
  }
}
