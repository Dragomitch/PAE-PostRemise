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
import com.dragomitch.ipl.pae.persistence.AddressDao;
import com.dragomitch.ipl.pae.persistence.CountryDao;
import com.dragomitch.ipl.pae.persistence.NominatedStudentDao;
import com.dragomitch.ipl.pae.persistence.UserDao;
import com.dragomitch.ipl.pae.business.exceptions.InsufficientPermissionException;
import com.dragomitch.ipl.pae.uccontrollers.AddressUcc;
import com.dragomitch.ipl.pae.uccontrollers.NominatedStudentUcc;
import com.dragomitch.ipl.pae.uccontrollers.UserUcc;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class NominatedStudentUccImpl implements NominatedStudentUcc {

  private final NominatedStudentDao nominatedStudentDao;
  private final AddressDao addressDao;
  private final AddressUcc addressUcc;
  private final CountryDao countryDao;
  private final UserDao userDao;
  private final UserUcc userUcc;

  NominatedStudentUccImpl(NominatedStudentDao nominatedStudentDao, AddressDao addressDao,
      AddressUcc addressUcc, CountryDao countryDao, UserDao userDao, UserUcc userUcc) {
    this.nominatedStudentDao = nominatedStudentDao;
    this.addressDao = addressDao;
    this.addressUcc = addressUcc;
    this.countryDao = countryDao;
    this.userDao = userDao;
    this.userUcc = userUcc;
  }

  @Override
  public NominatedStudentDto create(NominatedStudentDto nominatedStudent, int userId,
      String userRole) {
    checkObject(nominatedStudent);
    if (userRole.equals(UserDto.ROLE_STUDENT) && userId != nominatedStudent.getId()) {
      throw new InsufficientPermissionException();
    }
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
    return nominatedStudentDao.create(nominatedStudent);
  }

  @Override
  @Transactional(readOnly = true)
  public NominatedStudentDto showOne(int id, int userId, String role) {
    checkPositiveOrZero(id);
    if (!role.equals(UserDto.ROLE_PROFESSOR) && id != userId) {
      throw new InsufficientPermissionException();
    }
    NominatedStudentDto nominatedStudent;
    if ((nominatedStudent = nominatedStudentDao.findById(id)) == null) {
      throw new RessourceNotFoundException();
    }
    AddressDto address = addressDao.findById(nominatedStudent.getAddress().getId());
    nominatedStudent.setAddress(address);
    return nominatedStudent;
  }

  @Override
  @Transactional(readOnly = true)
  public List<NominatedStudentDto> showAll() {
    return nominatedStudentDao.findAll();
  }

  @Override
  public NominatedStudentDto edit(NominatedStudentDto nominatedStudent, int userId,
      String userRole) {
    checkObject(nominatedStudent);
    if (userRole.equals(UserDto.ROLE_STUDENT) && userId != ((UserDto) nominatedStudent).getId()) {
      throw new InsufficientPermissionException();
    }
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
    return nominatedStudent;
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
