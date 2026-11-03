package com.dragomitch.ipl.pae.adapter.out.inmemory;

import com.dragomitch.ipl.pae.domain.dto.AddressDto;
import com.dragomitch.ipl.pae.domain.port.out.AddressRepository;
import java.util.ArrayList;
import java.util.List;

public class InMemoryAddressRepository implements AddressRepository, ResettableRepository {

  private List<AddressDto> addresses;

  public InMemoryAddressRepository() {
    addresses = new ArrayList<AddressDto>();
  }

  @Override
  public AddressDto create(AddressDto address) {
    address.setId(addresses.size() + 1);
    addresses.add(address);
    return address;
  }

  @Override
  public AddressDto findById(int id) {
    if (id > 0 && id <= addresses.size()) {
      return addresses.get(id - 1);
    }
    return null;
  }

  @Override
  public AddressDto update(AddressDto address) {
    address.setVersion((findById(address.getId()).getVersion()) + 1);
    addresses.set(address.getId() - 1, address);
    return address;
  }

  @Override
  public void empty() {
    addresses = new ArrayList<AddressDto>();
  }
}
