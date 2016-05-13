package ucc.implementations;

import business.dto.CountryDto;
import business.dto.UserDto;
import main.annotations.Inject;
import persistence.CountryDao;
import presentation.annotations.ApiCollection;
import presentation.annotations.PathParameter;
import presentation.annotations.Role;
import presentation.annotations.Route;
import presentation.enums.HttpMethod;
import ucc.CountryUcc;
import ucc.UnitOfWork;
import utils.DataValidationUtils;

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
