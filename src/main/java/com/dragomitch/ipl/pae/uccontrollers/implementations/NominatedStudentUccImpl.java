package com.dragomitch.ipl.pae.uccontrollers.implementations;

import static com.dragomitch.ipl.pae.utils.DataValidationUtils.checkObject;
import static com.dragomitch.ipl.pae.utils.DataValidationUtils.checkPositiveOrZero;
import static com.dragomitch.ipl.pae.utils.DataValidationUtils.isAValidObject;

import com.dragomitch.ipl.pae.business.NominatedStudent;
import com.dragomitch.ipl.pae.business.dto.AddressDto;
import com.dragomitch.ipl.pae.business.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.ErrorFormat;
import com.dragomitch.ipl.pae.business.exceptions.RessourceNotFoundException;
import java.util.LinkedList;
import java.util.List;
import com.dragomitch.ipl.pae.annotations.Inject;
import com.dragomitch.ipl.pae.persistence.AddressDao;
import com.dragomitch.ipl.pae.persistence.CountryDao;
import com.dragomitch.ipl.pae.persistence.NominatedStudentDao;
import com.dragomitch.ipl.pae.persistence.UserDao;
import com.dragomitch.ipl.pae.presentation.annotations.ApiCollection;
import com.dragomitch.ipl.pae.presentation.annotations.HttpParameter;
import com.dragomitch.ipl.pae.presentation.annotations.PathParameter;
import com.dragomitch.ipl.pae.presentation.annotations.Role;
import com.dragomitch.ipl.pae.presentation.annotations.Route;
import com.dragomitch.ipl.pae.presentation.annotations.SessionParameter;
import com.dragomitch.ipl.pae.presentation.enums.HttpMethod;
import com.dragomitch.ipl.pae.presentation.exceptions.InsufficientPermissionException;
import com.dragomitch.ipl.pae.uccontrollers.AddressUcc;
import com.dragomitch.ipl.pae.uccontrollers.NominatedStudentUcc;
import com.dragomitch.ipl.pae.uccontrollers.UnitOfWork;
import com.dragomitch.ipl.pae.uccontrollers.UserUcc;

@ApiCollection(name = "Nominated students", endpoint = "/nominatedStudents")
public class NominatedStudentUccImpl implements NominatedStudentUcc {

  private NominatedStudentDao nominatedStudentDao;
  private AddressDao addressDao;
  private AddressUcc addressUcc;
  private CountryDao countryDao;
  private UserDao userDao;
  private UserUcc userUcc;
  private UnitOfWork unitOfWork;

  @Inject
  NominatedStudentUccImpl(NominatedStudentDao nominatedStudentDao, AddressDao addressDao,
      AddressUcc addressUcc, CountryDao countryDao, UserDao userDao, UserUcc userUcc,
      UnitOfWork unitOfWork) {
    this.nominatedStudentDao = nominatedStudentDao;
    this.addressDao = addressDao;
    this.addressUcc = addressUcc;
    this.countryDao = countryDao;
    this.userDao = userDao;
    this.userUcc = userUcc;
    this.unitOfWork = unitOfWork;
  }

  @Override
  @Role({UserDto.ROLE_PROFESSOR, UserDto.ROLE_STUDENT})
  @Route(method = HttpMethod.POST)
  public NominatedStudentDto create(@HttpParameter("data") NominatedStudentDto nominatedStudent,
      @SessionParameter("userId") int userId, @SessionParameter("userRole") String userRole) {
    checkObject(nominatedStudent);
    if (userRole.equals(UserDto.ROLE_STUDENT) && userId != nominatedStudent.getId()) {
      throw new InsufficientPermissionException();
    }
    try {
      unitOfWork.startTransaction();
      checkDataIntegrity(nominatedStudent);
      UserDto user;
      if ((user = userDao.findById(nominatedStudent.getId())) == null) {
        // user doesn't exist
        throw new BusinessException(ErrorFormat.EXISTENCE_VIOLATION_USER_ID_200);
      }
      if (nominatedStudentDao.findById(nominatedStudent.getId()) != null) {
        // student already exists
        throw new BusinessException(ErrorFormat.ALREADY_NOMINATED_STUDENT_618);
      }
      nominatedStudent.setVersion(user.getVersion());
      nominatedStudent.setAddress(addressDao.create(nominatedStudent.getAddress()));
      nominatedStudent = nominatedStudentDao.create(nominatedStudent);
      unitOfWork.commit();
      return nominatedStudent;
    } catch (Exception ex) {
      unitOfWork.rollback();
      throw ex;
    }

  }

  @Override
  @Role({UserDto.ROLE_STUDENT, UserDto.ROLE_PROFESSOR})
  @Route(method = HttpMethod.GET, template = "/{id}")
  public NominatedStudentDto showOne(@PathParameter("id") int id,
      @SessionParameter("userId") int userId, @SessionParameter("userRole") String role) {
    checkPositiveOrZero(id);
    if (!role.equals(UserDto.ROLE_PROFESSOR) && id != userId) {
      throw new InsufficientPermissionException();
    }
    try {
      unitOfWork.startTransaction();
      NominatedStudentDto nominatedStudent;
      if ((nominatedStudent = nominatedStudentDao.findById(id)) == null) {
        throw new RessourceNotFoundException();
      }
      AddressDto address = addressDao.findById(nominatedStudent.getAddress().getId());
      nominatedStudent.setAddress(address);
      unitOfWork.commit();
      return nominatedStudent;
    } catch (Exception ex) {
      unitOfWork.rollback();
      throw ex;
    }
  }

  @Override
  @Role({UserDto.ROLE_PROFESSOR})
  @Route(method = HttpMethod.GET)
  public List<NominatedStudentDto> showAll() {
    try {
      unitOfWork.startTransaction();
      List<NominatedStudentDto> nominatedStudList = nominatedStudentDao.findAll();
      unitOfWork.commit();
      return nominatedStudList;
    } catch (Exception ex) {
      unitOfWork.rollback();
      throw ex;
    }
  }

  @Override
  @Role({UserDto.ROLE_PROFESSOR, UserDto.ROLE_STUDENT})
  @Route(method = HttpMethod.PUT, template = "/{id}")
  public NominatedStudentDto edit(@HttpParameter("data") NominatedStudentDto nominatedStudent,
      @SessionParameter("userId") int userId, @SessionParameter("userRole") String userRole) {
    checkObject(nominatedStudent);
    if (userRole.equals(UserDto.ROLE_STUDENT) && userId != ((UserDto) nominatedStudent).getId()) {
      throw new InsufficientPermissionException();
    }
    try {
      unitOfWork.startTransaction();
      checkDataIntegrity(nominatedStudent);

      NominatedStudentDto tempStud = nominatedStudentDao.findById(nominatedStudent.getId());
      if (tempStud == null) {
        throw new RessourceNotFoundException();
      }
      nominatedStudent.getAddress().setId(tempStud.getAddress().getId());
      nominatedStudent = nominatedStudentDao.update(nominatedStudent);
      nominatedStudent.setVersion(nominatedStudent.getVersion() - 1);
      userUcc.edit(nominatedStudent, userId, userRole);
      AddressDto updatedAddress = addressUcc.edit(nominatedStudent.getAddress());
      nominatedStudent.getAddress().setVersion(updatedAddress.getVersion());
      unitOfWork.commit();
      return nominatedStudent;
    } catch (Exception ex) {
      unitOfWork.rollback();
      throw ex;
    }
  }

  private void checkDataIntegrity(NominatedStudentDto nominatedStudent) {
    List<Integer> violations = new LinkedList<Integer>();
    try {
      ((NominatedStudent) nominatedStudent).checkDataIntegrity();
    } catch (BusinessException ex) {
      List<ErrorFormat> errors = ex.getError().getDetails();
      for (ErrorFormat oneError : errors) {
        violations.add(oneError.getErrorCode());
      }
    }
    if (isAValidObject(nominatedStudent.getNationality())
        && countryDao.findById(nominatedStudent.getNationality().getCountryCode()) == null) {
      violations.add(ErrorFormat.EXISTENCE_VIOLATION_COUNTRY_CODE_900);
    }
    if (violations.size() > 0) {
      throw new BusinessException(ErrorFormat.INVALID_INPUT_DATA_110, violations);
    }
  }
}
