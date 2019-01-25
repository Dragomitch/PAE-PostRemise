package com.dragomitch.ipl.pae.persistence.mocks;

import com.dragomitch.ipl.pae.business.dto.AddressDto;
import com.dragomitch.ipl.pae.persistence.AddressDao;

import java.util.ArrayList;
import java.util.List;

public class MockAddressDao implements AddressDao {

  private List<AddressDto> addresses;

  public MockAddressDao() {
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

  public void empty() {
    addresses = new ArrayList<AddressDto>();
  }

}
