package com.dragomitch.ipl.pae.uccontrollers.implementations;

import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.annotations.Inject;
import com.dragomitch.ipl.pae.persistence.CountryDao;
import com.dragomitch.ipl.pae.presentation.annotations.ApiCollection;
import com.dragomitch.ipl.pae.presentation.annotations.PathParameter;
import com.dragomitch.ipl.pae.presentation.annotations.Role;
import com.dragomitch.ipl.pae.presentation.annotations.Route;
import com.dragomitch.ipl.pae.presentation.enums.HttpMethod;
import com.dragomitch.ipl.pae.uccontrollers.CountryUcc;
import com.dragomitch.ipl.pae.uccontrollers.UnitOfWork;
import com.dragomitch.ipl.pae.utils.DataValidationUtils;

import java.util.List;

@ApiCollection(name = "Countries", endpoint = "/countries")
class CountryUccImpl implements CountryUcc {

  private CountryDao countryDao;
  private UnitOfWork unitOfWork;

  @Inject
  public CountryUccImpl(CountryDao countryDao, UnitOfWork unitOfWork) {
    this.countryDao = countryDao;
    this.unitOfWork = unitOfWork;
  }

  @Override
  @Role({UserDto.ROLE_PROFESSOR, UserDto.ROLE_STUDENT})
  @Route(method = HttpMethod.GET)
  public List<CountryDto> showAll() {
    try {
      unitOfWork.startTransaction();
      List<CountryDto> countryList = countryDao.findAll();
      unitOfWork.commit();
      return countryList;
    } catch (Exception ex) {
      unitOfWork.rollback();
      throw ex;
    }
  }

  @Override
  @Role({UserDto.ROLE_PROFESSOR, UserDto.ROLE_STUDENT})
  @Route(method = HttpMethod.GET, template = "/{code}")
  public CountryDto showOne(@PathParameter("code") String countryCode) {
    DataValidationUtils.checkString(countryCode);
    try {
      unitOfWork.startTransaction();
      CountryDto country = countryDao.findById(countryCode);
      unitOfWork.commit();
      return country;
    } catch (Exception ex) {
      unitOfWork.rollback();
      throw ex;
    }
  }

}
