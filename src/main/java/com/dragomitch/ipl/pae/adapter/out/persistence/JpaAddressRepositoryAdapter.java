package com.dragomitch.ipl.pae.adapter.out.persistence;

import com.dragomitch.ipl.pae.adapter.out.persistence.entity.AddressEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.CountryEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.repository.AddressJpaRepository;
import com.dragomitch.ipl.pae.domain.model.Address;
import com.dragomitch.ipl.pae.domain.port.out.AddressRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ConcurrentModificationException;
import org.springframework.stereotype.Repository;

/** The postal addresses of the partners and of the nominated students. */
@Repository
class JpaAddressRepositoryAdapter implements AddressRepository {

  private final AddressJpaRepository addresses;
  private final PersistenceMapper mapper;

  @PersistenceContext private EntityManager entityManager;

  JpaAddressRepositoryAdapter(AddressJpaRepository addresses, PersistenceMapper mapper) {
    this.addresses = addresses;
    this.mapper = mapper;
  }

  /** Stores the address at version 1 and sets its generated id and version on the DTO. */
  @Override
  public Address create(Address address) {
    AddressEntity entity = new AddressEntity();
    copy(address, entity);
    entity.setVersion(1);
    entityManager.persist(entity);
    address.setId(entity.getId());
    address.setVersion(entity.getVersion());
    return address;
  }

  @Override
  public Address findById(int id) {
    return addresses.findWithCountryById(id).map(mapper::address).orElse(null);
  }

  /**
   * {@inheritDoc}
   *
   * @throws ConcurrentModificationException if the address does not exist or its version is not the
   *     DTO's (it was changed since it was read)
   */
  @Override
  public Address update(Address address) {
    AddressEntity entity =
        addresses
            .findById(address.getId())
            .filter(found -> found.getVersion() == address.getVersion())
            .orElseThrow(ConcurrentModificationException::new);
    copy(address, entity);
    Versions.flushIncrementingVersion(
        entityManager, entity, entity::getVersion, address.getVersion());
    address.setVersion(entity.getVersion());
    return address;
  }

  private void copy(Address address, AddressEntity entity) {
    entity.setStreet(address.getStreet());
    entity.setNumber(address.getNumber());
    entity.setCountry(
        entityManager.getReference(CountryEntity.class, address.getCountry().getCountryCode()));
    entity.setCity(address.getCity());
    entity.setPostalCode(address.getPostalCode());
    entity.setRegion(address.getRegion());
  }
}
