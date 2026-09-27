package com.dragomitch.ipl.pae.uccontrollers.implementations;

import com.dragomitch.ipl.pae.business.dto.AddressDto;
import com.dragomitch.ipl.pae.business.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.business.exceptions.InsufficientPermissionException;
import com.dragomitch.ipl.pae.business.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.persistence.AddressDao;
import com.dragomitch.ipl.pae.persistence.CountryDao;
import com.dragomitch.ipl.pae.persistence.NominatedStudentDao;
import com.dragomitch.ipl.pae.persistence.UserDao;
import com.dragomitch.ipl.pae.uccontrollers.AddressUcc;
import com.dragomitch.ipl.pae.uccontrollers.NominatedStudentUcc;
import com.dragomitch.ipl.pae.uccontrollers.UserUcc;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Personal data of the nominated students. Their format is checked by the constraints of
 * {@link NominatedStudentDto}; this class checks the rules that need the database or the
 * requester.
 */
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
    if (userRole.equals(UserDto.ROLE_STUDENT) && userId != nominatedStudent.getId()) {
      throw new InsufficientPermissionException();
    }
    checkCountriesExist(nominatedStudent);
    UserDto user;
    if ((user = userDao.findById(nominatedStudent.getId())) == null) {
      // user doesn't exist
      throw new BusinessException(ErrorCode.UNKNOWN_USER, nominatedStudent.getId());
    }
    if (nominatedStudentDao.findById(nominatedStudent.getId()) != null) {
      // student already exists
      throw new BusinessException(ErrorCode.ALREADY_NOMINATED);
    }
    defaultCardHolder(nominatedStudent);
    nominatedStudent.setVersion(user.getVersion());
    nominatedStudent.setAddress(addressDao.create(nominatedStudent.getAddress()));
    return nominatedStudentDao.create(nominatedStudent);
  }

  @Override
  @Transactional(readOnly = true)
  public NominatedStudentDto showOne(int id, int userId, String role) {
    if (!role.equals(UserDto.ROLE_PROFESSOR) && id != userId) {
      throw new InsufficientPermissionException();
    }
    NominatedStudentDto nominatedStudent;
    if ((nominatedStudent = nominatedStudentDao.findById(id)) == null) {
      throw new ResourceNotFoundException();
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
    if (userRole.equals(UserDto.ROLE_STUDENT) && userId != ((UserDto) nominatedStudent).getId()) {
      throw new InsufficientPermissionException();
    }
    checkCountriesExist(nominatedStudent);
    NominatedStudentDto tempStud = nominatedStudentDao.findById(nominatedStudent.getId());
    if (tempStud == null) {
      throw new ResourceNotFoundException();
    }
    defaultCardHolder(nominatedStudent);
    nominatedStudent.getAddress().setId(tempStud.getAddress().getId());
    nominatedStudent = nominatedStudentDao.update(nominatedStudent);
    nominatedStudent.setVersion(nominatedStudent.getVersion() - 1);
    userUcc.edit(nominatedStudent, userId, userRole);
    AddressDto updatedAddress = addressUcc.edit(nominatedStudent.getAddress());
    nominatedStudent.getAddress().setVersion(updatedAddress.getVersion());
    return nominatedStudent;
  }

  /** The nationality and the country of the address must exist. */
  private void checkCountriesExist(NominatedStudentDto nominatedStudent) {
    for (String countryCode : List.of(nominatedStudent.getNationality().getCountryCode(),
        nominatedStudent.getAddress().getCountry().getCountryCode())) {
      if (countryDao.findById(countryCode) == null) {
        throw new BusinessException(ErrorCode.UNKNOWN_COUNTRY, countryCode);
      }
    }
  }

  /** The bank account belongs to the student himself when no card holder is given. */
  private static void defaultCardHolder(NominatedStudentDto nominatedStudent) {
    if (!StringUtils.hasText(nominatedStudent.getCardHolder())) {
      String holder = nominatedStudent.getFirstName() + " " + nominatedStudent.getLastName();
      nominatedStudent.setCardHolder(holder.length() > NominatedStudentDto.CARD_HOLDER_MAX_LENGTH
          ? holder.substring(0, NominatedStudentDto.CARD_HOLDER_MAX_LENGTH) : holder);
    }
  }
}
