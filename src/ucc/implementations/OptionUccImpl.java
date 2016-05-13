package ucc.implementations;

import static utils.DataValidationUtils.checkString;

import business.dto.OptionDto;
import business.dto.PartnerDto;
import business.dto.UserDto;
import business.exceptions.RessourceNotFoundException;
import main.annotations.Inject;
import persistence.DalServices;
import persistence.OptionDao;
import persistence.PartnerOptionDao;
import presentation.annotations.PathParameter;
import presentation.annotations.Role;
import presentation.annotations.Route;
import presentation.enums.HttpMethod;
import ucc.OptionUcc;

import java.util.List;

class OptionUccImpl implements OptionUcc {

  private final OptionDao optionDao;
  private final PartnerOptionDao partnerOptionDao;
  private final DalServices dalServices;

  @Inject
  public OptionUccImpl(OptionDao optionDao, PartnerOptionDao partnerOptionDao,
      DalServices dalServices) {
    this.optionDao = optionDao;
    this.partnerOptionDao = partnerOptionDao;
    this.dalServices = dalServices;
  }

  @Override
  @Route(method = HttpMethod.GET, template = "/options")
  public List<OptionDto> showAll() {
    dalServices.openConnection();
    List<OptionDto> optionList = optionDao.findAll();
    dalServices.closeConnection();
    return optionList;
  }

  @Override
  @Role({UserDto.ROLE_PROFESSOR, UserDto.ROLE_STUDENT})
  @Route(method = HttpMethod.GET, template = "/options/{optionCode}")
  public List<PartnerDto> findAllPartnersByOption(@PathParameter("optionCode") String optionCode) {
    checkString(optionCode);
    dalServices.openConnection();
    if (optionDao.findByCode(optionCode) == null) {
      dalServices.closeConnection();
      throw new RessourceNotFoundException();
    }
    List<PartnerDto> partner = null;
    partner = partnerOptionDao.findAllPartnersByOption(optionCode);
    dalServices.closeConnection();
    return partner;
  }

}
