package com.dragomitch.ipl.pae.uccontrollers.implementations;

import static com.dragomitch.ipl.pae.utils.DataValidationUtils.checkObject;
import static com.dragomitch.ipl.pae.utils.DataValidationUtils.isAValidObject;
import static com.dragomitch.ipl.pae.utils.DataValidationUtils.isAValidString;

import com.dragomitch.ipl.pae.business.Address;
import com.dragomitch.ipl.pae.business.dto.AddressDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.ErrorFormat;
import com.dragomitch.ipl.pae.business.exceptions.RessourceNotFoundException;
import com.dragomitch.ipl.pae.persistence.AddressDao;
import com.dragomitch.ipl.pae.persistence.CountryDao;
import com.dragomitch.ipl.pae.uccontrollers.AddressUcc;
import com.dragomitch.ipl.pae.uccontrollers.UnitOfWork;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;

@Service
public class AddressUccImpl implements AddressUcc {
  AddressDao addressDao;
  CountryDao countryDao;
  UnitOfWork unitOfWork;

  AddressUccImpl(AddressDao addressDao, CountryDao countryDao, UnitOfWork unitOfWork) {
    this.addressDao = addressDao;
    this.countryDao = countryDao;
    this.unitOfWork = unitOfWork;
  }

  @Override
  public AddressDto create(AddressDto address) {
    checkObject(address);
    checkDataIntegrity(address);
    try {
      unitOfWork.startTransaction();
      if ((countryDao.findById(address.getCountry().getCountryCode())) == null) {
        throw new RessourceNotFoundException("Unknown countryCode for creation of the new Addres");
      }
      addressDao.create(address);
      unitOfWork.commit();
      return address;
    } catch (Exception ex) {
      unitOfWork.rollback();
      throw ex;
    }
  }

  @Override
  public AddressDto edit(AddressDto address) {
    checkObject(address);
    checkDataIntegrity(address);
    try {
      unitOfWork.startTransaction();
      if ((countryDao.findById(address.getCountry().getCountryCode())) == null) {
        throw new RessourceNotFoundException();
      }
      AddressDto addressDb = addressDao.findById(address.getId());
      if (addressDb == null) {
        throw new RessourceNotFoundException();
      }
      address.setVersion(addressDb.getVersion());
      AddressDto newAddress = addressDao.update(address);
      unitOfWork.commit();
      return newAddress;
    } catch (Exception ex) {
      unitOfWork.rollback();
      throw ex;
    }
  }

  private void checkDataIntegrity(AddressDto address) {
    List<Integer> violations = new LinkedList<Integer>();
    if (address == null) {
      throw new BusinessException(ErrorFormat.EXISTENCE_VIOLATION_ADDRESS_NULL_138);
    }
    try {
      ((Address) address).checkDataIntegrity();
    } catch (BusinessException ex) {
      List<ErrorFormat> errors = ex.getError().getDetails();
      for (ErrorFormat oneError : errors) {
        violations.add(oneError.getErrorCode());
      }
    }
    if (isAValidString(address.getStreet())
        && address.getStreet().length() > AddressDao.MAX_LENGTH_STREET) {
      violations.add(ErrorFormat.STREET_MAX_LENGTH_OVERFLOW_806);
    }
    if (isAValidString(address.getNumber())
        && address.getNumber().length() > AddressDao.MAX_LENGTH_NUMBER) {
      violations.add(ErrorFormat.STREET_NUMBER_MAX_LENGTH_OVERFLOW_810);
    }
    if (isAValidObject(address.getCountry())
        && isAValidObject(address.getCountry().getCountryCode())
        && address.getCountry().getCountryCode().length() > CountryDao.CODE_LENGTH) {
      violations.add(ErrorFormat.MAX_LENGTH_COUNTRY_CODE_OVERFLOW_314);
    }
    if (isAValidString(address.getCity())
        && address.getCity().length() > AddressDao.MAX_LENGTH_CITY) {
      violations.add(ErrorFormat.CITY_MAX_LENGTH_OVERFLOW_807);
    }
    if (isAValidString(address.getPostalCode())
        && address.getPostalCode().length() > AddressDao.MAX_LENGTH_POSTAL_CODE) {
      violations.add(ErrorFormat.POSTAL_CODE_MAX_LENGTH_OVERFLOW_808);
    }
    if (isAValidObject(address.getRegion())
        && address.getRegion().length() > AddressDao.MAX_LENGTH_REGION) {
      violations.add(ErrorFormat.REGION_MAX_LENGTH_OVERFLOW_809);
    }

    if (violations.size() > 0) {
      throw new BusinessException(ErrorFormat.INVALID_INPUT_DATA_110, violations);
    }
  }


}
