package ucc.implementations;

import business.dto.ProgrammeDto;
import business.dto.UserDto;
import main.annotations.Inject;
import persistence.DalServices;
import persistence.ProgrammeDao;
import presentation.annotations.PathParameter;
import presentation.annotations.Role;
import presentation.annotations.Route;
import presentation.enums.HttpMethod;
import ucc.ProgrammeUcc;
import utils.DataValidationUtils;

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
