package com.dragomitch.ipl.pae.uccontrollers.implementations;

import com.dragomitch.ipl.pae.business.dto.AddressDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.business.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.persistence.AddressDao;
import com.dragomitch.ipl.pae.persistence.CountryDao;
import com.dragomitch.ipl.pae.uccontrollers.AddressUcc;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Addresses. Their format is checked by the constraints of {@link AddressDto}; the country must
 * exist.
 */
@Service
@Transactional
class AddressUccImpl implements AddressUcc {
  private final AddressDao addressDao;
  private final CountryDao countryDao;

  AddressUccImpl(AddressDao addressDao, CountryDao countryDao) {
    this.addressDao = addressDao;
    this.countryDao = countryDao;
  }

  @Override
  public AddressDto create(AddressDto address) {
    checkCountryExists(address);
    addressDao.create(address);
    return address;
  }

  @Override
  public AddressDto edit(AddressDto address) {
    checkCountryExists(address);
    AddressDto addressDb = addressDao.findById(address.getId());
    if (addressDb == null) {
      throw new ResourceNotFoundException();
    }
    address.setVersion(addressDb.getVersion());
    return addressDao.update(address);
  }

  private void checkCountryExists(AddressDto address) {
    String countryCode = address.getCountry().getCountryCode();
    if (countryDao.findById(countryCode) == null) {
      throw new BusinessException(ErrorCode.UNKNOWN_COUNTRY, countryCode);
    }
  }
}
