package com.dragomitch.ipl.pae.application;

import com.dragomitch.ipl.pae.domain.dto.AddressDto;
import com.dragomitch.ipl.pae.domain.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.domain.dto.UserDto;
import com.dragomitch.ipl.pae.domain.exceptions.BusinessException;
import com.dragomitch.ipl.pae.domain.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.domain.exceptions.InsufficientPermissionException;
import com.dragomitch.ipl.pae.domain.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.domain.model.Role;
import com.dragomitch.ipl.pae.domain.port.in.AddressService;
import com.dragomitch.ipl.pae.domain.port.in.NominatedStudentService;
import com.dragomitch.ipl.pae.domain.port.in.UserService;
import com.dragomitch.ipl.pae.domain.port.out.AddressRepository;
import com.dragomitch.ipl.pae.domain.port.out.CountryRepository;
import com.dragomitch.ipl.pae.domain.port.out.NominatedStudentRepository;
import com.dragomitch.ipl.pae.domain.port.out.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Personal data of the nominated students. Their format is checked by the constraints of {@link
 * NominatedStudentDto}; this class checks the rules that need the database or the requester.
 */
@Service
@Transactional
class NominatedStudentServiceImpl implements NominatedStudentService {

  private final NominatedStudentRepository nominatedStudentRepository;
  private final AddressRepository addressRepository;
  private final AddressService addressService;
  private final CountryRepository countryRepository;
  private final UserRepository userRepository;
  private final UserService userService;

  NominatedStudentServiceImpl(
      NominatedStudentRepository nominatedStudentRepository,
      AddressRepository addressRepository,
      AddressService addressService,
      CountryRepository countryRepository,
      UserRepository userRepository,
      UserService userService) {
    this.nominatedStudentRepository = nominatedStudentRepository;
    this.addressRepository = addressRepository;
    this.addressService = addressService;
    this.countryRepository = countryRepository;
    this.userRepository = userRepository;
    this.userService = userService;
  }

  @Override
  public NominatedStudentDto create(
      NominatedStudentDto nominatedStudent, int userId, Role userRole) {
    if (userRole == Role.STUDENT && userId != nominatedStudent.getId()) {
      throw new InsufficientPermissionException();
    }
    checkCountriesExist(nominatedStudent);
    UserDto user;
    if ((user = userRepository.findById(nominatedStudent.getId())) == null) {
      // user doesn't exist
      throw new BusinessException(ErrorCode.UNKNOWN_USER, nominatedStudent.getId());
    }
    if (nominatedStudentRepository.findById(nominatedStudent.getId()) != null) {
      // student already exists
      throw new BusinessException(ErrorCode.ALREADY_NOMINATED);
    }
    defaultCardHolder(nominatedStudent);
    nominatedStudent.setVersion(user.getVersion());
    nominatedStudent.setAddress(addressRepository.create(nominatedStudent.getAddress()));
    return nominatedStudentRepository.create(nominatedStudent);
  }

  @Override
  @Transactional(readOnly = true)
  public NominatedStudentDto showOne(int id, int userId, Role role) {
    if (role != Role.PROFESSOR && id != userId) {
      throw new InsufficientPermissionException();
    }
    NominatedStudentDto nominatedStudent;
    if ((nominatedStudent = nominatedStudentRepository.findById(id)) == null) {
      throw new ResourceNotFoundException();
    }
    AddressDto address = addressRepository.findById(nominatedStudent.getAddress().getId());
    nominatedStudent.setAddress(address);
    return nominatedStudent;
  }

  @Override
  @Transactional(readOnly = true)
  public List<NominatedStudentDto> showAll() {
    return nominatedStudentRepository.findAll();
  }

  @Override
  public NominatedStudentDto edit(NominatedStudentDto nominatedStudent, int userId, Role userRole) {
    if (userRole == Role.STUDENT && userId != ((UserDto) nominatedStudent).getId()) {
      throw new InsufficientPermissionException();
    }
    checkCountriesExist(nominatedStudent);
    NominatedStudentDto tempStud = nominatedStudentRepository.findById(nominatedStudent.getId());
    if (tempStud == null) {
      throw new ResourceNotFoundException();
    }
    defaultCardHolder(nominatedStudent);
    nominatedStudent.getAddress().setId(tempStud.getAddress().getId());
    nominatedStudent = nominatedStudentRepository.update(nominatedStudent);
    nominatedStudent.setVersion(nominatedStudent.getVersion() - 1);
    userService.edit(nominatedStudent, userId, userRole);
    AddressDto updatedAddress = addressService.edit(nominatedStudent.getAddress());
    nominatedStudent.getAddress().setVersion(updatedAddress.getVersion());
    return nominatedStudent;
  }

  /** The nationality and the country of the address must exist. */
  private void checkCountriesExist(NominatedStudentDto nominatedStudent) {
    for (String countryCode :
        List.of(
            nominatedStudent.getNationality().getCountryCode(),
            nominatedStudent.getAddress().getCountry().getCountryCode())) {
      if (countryRepository.findById(countryCode) == null) {
        throw new BusinessException(ErrorCode.UNKNOWN_COUNTRY, countryCode);
      }
    }
  }

  /** The bank account belongs to the student himself when no card holder is given. */
  private static void defaultCardHolder(NominatedStudentDto nominatedStudent) {
    if (!StringUtils.hasText(nominatedStudent.getCardHolder())) {
      String holder = nominatedStudent.getFirstName() + " " + nominatedStudent.getLastName();
      nominatedStudent.setCardHolder(
          holder.length() > NominatedStudentDto.CARD_HOLDER_MAX_LENGTH
              ? holder.substring(0, NominatedStudentDto.CARD_HOLDER_MAX_LENGTH)
              : holder);
    }
  }
}
