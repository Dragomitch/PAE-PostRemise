package com.dragomitch.ipl.pae.application;

import com.dragomitch.ipl.pae.domain.exceptions.BusinessException;
import com.dragomitch.ipl.pae.domain.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.domain.exceptions.InsufficientPermissionException;
import com.dragomitch.ipl.pae.domain.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.domain.model.Address;
import com.dragomitch.ipl.pae.domain.model.NominatedStudent;
import com.dragomitch.ipl.pae.domain.model.Role;
import com.dragomitch.ipl.pae.domain.model.User;
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

/**
 * Personal data of the nominated students. Their format is checked by the constraints of {@link
 * NominatedStudent}; this class checks the rules that need the database or the requester.
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
  public NominatedStudent create(NominatedStudent nominatedStudent, int userId, Role userRole) {
    if (userRole == Role.STUDENT && userId != nominatedStudent.getId()) {
      throw new InsufficientPermissionException();
    }
    checkCountriesExist(nominatedStudent);
    User user =
        userRepository
            .findById(nominatedStudent.getId())
            .orElseThrow(
                () -> new BusinessException(ErrorCode.UNKNOWN_USER, nominatedStudent.getId()));
    if (nominatedStudentRepository.findById(nominatedStudent.getId()).isPresent()) {
      // student already exists
      throw new BusinessException(ErrorCode.ALREADY_NOMINATED);
    }
    nominatedStudent.defaultCardHolder();
    nominatedStudent.setVersion(user.getVersion());
    nominatedStudent.setAddress(addressRepository.create(nominatedStudent.getAddress()));
    return nominatedStudentRepository.create(nominatedStudent);
  }

  @Override
  @Transactional(readOnly = true)
  public NominatedStudent showOne(int id, int userId, Role role) {
    if (role != Role.PROFESSOR && id != userId) {
      throw new InsufficientPermissionException();
    }
    NominatedStudent nominatedStudent =
        nominatedStudentRepository.findById(id).orElseThrow(ResourceNotFoundException::new);
    nominatedStudent.setAddress(
        addressRepository.findById(nominatedStudent.getAddress().getId()).orElse(null));
    return nominatedStudent;
  }

  @Override
  @Transactional(readOnly = true)
  public List<NominatedStudent> showAll() {
    return nominatedStudentRepository.findAll();
  }

  @Override
  public NominatedStudent edit(NominatedStudent nominatedStudent, int userId, Role userRole) {
    if (userRole == Role.STUDENT && userId != ((User) nominatedStudent).getId()) {
      throw new InsufficientPermissionException();
    }
    checkCountriesExist(nominatedStudent);
    NominatedStudent tempStud =
        nominatedStudentRepository
            .findById(nominatedStudent.getId())
            .orElseThrow(ResourceNotFoundException::new);
    nominatedStudent.defaultCardHolder();
    nominatedStudent.getAddress().setId(tempStud.getAddress().getId());
    nominatedStudent = nominatedStudentRepository.update(nominatedStudent);
    nominatedStudent.setVersion(nominatedStudent.getVersion() - 1);
    userService.edit(nominatedStudent, userId, userRole);
    Address updatedAddress = addressService.edit(nominatedStudent.getAddress());
    nominatedStudent.getAddress().setVersion(updatedAddress.getVersion());
    return nominatedStudent;
  }

  /** The nationality and the country of the address must exist. */
  private void checkCountriesExist(NominatedStudent nominatedStudent) {
    for (String countryCode :
        List.of(
            nominatedStudent.getNationality().getCountryCode(),
            nominatedStudent.getAddress().getCountry().getCountryCode())) {
      if (countryRepository.findById(countryCode).isEmpty()) {
        throw new BusinessException(ErrorCode.UNKNOWN_COUNTRY, countryCode);
      }
    }
  }
}
