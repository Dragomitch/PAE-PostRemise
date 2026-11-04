package com.dragomitch.ipl.pae.adapter.out.inmemory;

import com.dragomitch.ipl.pae.domain.model.Address;
import com.dragomitch.ipl.pae.domain.port.out.AddressRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryAddressRepository implements AddressRepository, ResettableRepository {

  private List<Address> addresses;

  public InMemoryAddressRepository() {
    addresses = new ArrayList<Address>();
  }

  @Override
  public Address create(Address address) {
    address.setId(addresses.size() + 1);
    addresses.add(address);
    return address;
  }

  @Override
  public Optional<Address> findById(int id) {
    if (id > 0 && id <= addresses.size()) {
      return Optional.ofNullable(addresses.get(id - 1));
    }
    return Optional.empty();
  }

  @Override
  public Address update(Address address) {
    address.setVersion(findById(address.getId()).orElseThrow().getVersion() + 1);
    addresses.set(address.getId() - 1, address);
    return address;
  }

  @Override
  public void empty() {
    addresses = new ArrayList<Address>();
  }
}
