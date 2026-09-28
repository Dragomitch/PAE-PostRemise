package com.dragomitch.ipl.pae.uccontrollers.implementations;

import static com.dragomitch.ipl.pae.utils.DataValidationUtils.checkString;

import com.dragomitch.ipl.pae.business.dto.OptionDto;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.exceptions.RessourceNotFoundException;
import com.dragomitch.ipl.pae.persistence.DalServices;
import com.dragomitch.ipl.pae.persistence.OptionDao;
import com.dragomitch.ipl.pae.persistence.PartnerOptionDao;
import com.dragomitch.ipl.pae.presentation.annotations.PathParameter;
import com.dragomitch.ipl.pae.presentation.annotations.Role;
import com.dragomitch.ipl.pae.presentation.annotations.Route;
import com.dragomitch.ipl.pae.presentation.enums.HttpMethod;
import com.dragomitch.ipl.pae.uccontrollers.OptionUcc;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
class OptionUccImpl implements OptionUcc {

  private final OptionDao optionDao;
  private final PartnerOptionDao partnerOptionDao;
  private final DalServices dalServices;

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
    try {
      return optionDao.findAll();
    } finally {
      dalServices.closeConnection();
    }
  }

  @Override
  @Role({UserDto.ROLE_PROFESSOR, UserDto.ROLE_STUDENT})
  @Route(method = HttpMethod.GET, template = "/options/{optionCode}")
  public List<PartnerDto> findAllPartnersByOption(@PathParameter("optionCode") String optionCode) {
    checkString(optionCode);
    dalServices.openConnection();
    try {
      if (optionDao.findByCode(optionCode) == null) {
        throw new RessourceNotFoundException();
      }
      return partnerOptionDao.findAllPartnersByOption(optionCode);
    } finally {
      dalServices.closeConnection();
    }
  }

}
