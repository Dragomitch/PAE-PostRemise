package com.dragomitch.ipl.pae.application;

import com.dragomitch.ipl.pae.domain.exceptions.BusinessException;
import com.dragomitch.ipl.pae.domain.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.domain.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.domain.model.Address;
import com.dragomitch.ipl.pae.domain.port.in.AddressService;
import com.dragomitch.ipl.pae.domain.port.out.AddressRepository;
import com.dragomitch.ipl.pae.domain.port.out.CountryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Addresses. Their format is checked by the constraints of {@link Address}; the country must exist.
 */
@Service
@Transactional
class AddressServiceImpl implements AddressService {
  private final AddressRepository addressRepository;
  private final CountryRepository countryRepository;

  AddressServiceImpl(AddressRepository addressRepository, CountryRepository countryRepository) {
    this.addressRepository = addressRepository;
    this.countryRepository = countryRepository;
  }

  @Override
  public Address create(Address address) {
    checkCountryExists(address);
    addressRepository.create(address);
    return address;
  }

  @Override
  public Address edit(Address address) {
    checkCountryExists(address);
    Address addressDb = addressRepository.findById(address.getId());
    if (addressDb == null) {
      throw new ResourceNotFoundException();
    }
    address.setVersion(addressDb.getVersion());
    return addressRepository.update(address);
  }

  private void checkCountryExists(Address address) {
    String countryCode = address.getCountry().getCountryCode();
    if (countryRepository.findById(countryCode) == null) {
      throw new BusinessException(ErrorCode.UNKNOWN_COUNTRY, countryCode);
    }
  }
}
