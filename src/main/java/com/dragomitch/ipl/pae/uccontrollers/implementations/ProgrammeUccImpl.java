package com.dragomitch.ipl.pae.uccontrollers.implementations;

import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.annotations.Inject;
import com.dragomitch.ipl.pae.persistence.DalServices;
import com.dragomitch.ipl.pae.persistence.ProgrammeDao;
import com.dragomitch.ipl.pae.presentation.annotations.PathParameter;
import com.dragomitch.ipl.pae.presentation.annotations.Role;
import com.dragomitch.ipl.pae.presentation.annotations.Route;
import com.dragomitch.ipl.pae.presentation.enums.HttpMethod;
import com.dragomitch.ipl.pae.uccontrollers.ProgrammeUcc;
import com.dragomitch.ipl.pae.utils.DataValidationUtils;

import java.util.List;

public class ProgrammeUccImpl implements ProgrammeUcc {

  private ProgrammeDao programmeDao;
  private DalServices dalServices;

  @Inject
  public ProgrammeUccImpl(ProgrammeDao programmeDao, DalServices dalServices) {
    this.programmeDao = programmeDao;
    this.dalServices = dalServices;
  }

  @Override
  @Role({UserDto.ROLE_PROFESSOR, UserDto.ROLE_STUDENT})
  @Route(method = HttpMethod.GET, template = "/programmes/{id}")
  public ProgrammeDto showOne(@PathParameter("id") int id) {
    DataValidationUtils.checkPositive(id);
    dalServices.openConnection();
    ProgrammeDto programme = programmeDao.findById(id);
    dalServices.closeConnection();
    return programme;
  }

  @Override
  @Role({UserDto.ROLE_PROFESSOR, UserDto.ROLE_STUDENT})
  @Route(method = HttpMethod.GET, template = "/programmes")
  public List<ProgrammeDto> showAll() {
    dalServices.openConnection();
    List<ProgrammeDto> programmes = programmeDao.findAll();
    dalServices.closeConnection();
    return programmes;
  }

}
