package com.dragomitch.ipl.pae.application;

import static com.dragomitch.ipl.pae.domain.exceptions.BusinessExceptionAssert.assertThatBusinessException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dragomitch.ipl.pae.domain.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.domain.model.Address;
import com.dragomitch.ipl.pae.domain.model.Country;
import com.dragomitch.ipl.pae.domain.model.NominatedStudent;
import com.dragomitch.ipl.pae.domain.model.Role;
import com.dragomitch.ipl.pae.domain.model.User;
import com.dragomitch.ipl.pae.domain.port.in.AddressService;
import com.dragomitch.ipl.pae.domain.port.in.UserService;
import com.dragomitch.ipl.pae.domain.port.out.AddressRepository;
import com.dragomitch.ipl.pae.domain.port.out.CountryRepository;
import com.dragomitch.ipl.pae.domain.port.out.NominatedStudentRepository;
import com.dragomitch.ipl.pae.domain.port.out.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Personal data of the nominated students: who may record, read and change them, the existing
 * countries, the single record per student, the default card holder. The DAOs are Mockito mocks
 * following the contract of {@code NominatedStudentRepositoryIT} (the student shares the id of the
 * user).
 */
@ExtendWith(MockitoExtension.class)
class NominatedStudentServiceImplTest {

  private static final int STUDENT_ID = 42;
  private static final int PROFESSOR_ID = 2;
  private static final int ADDRESS_ID = 2001;

  @Mock private NominatedStudentRepository nominatedStudentRepository;
  @Mock private AddressRepository addressRepository;
  @Mock private AddressService addressService;
  @Mock private CountryRepository countryRepository;
  @Mock private UserRepository userRepository;
  @Mock private UserService userService;

  private final MockDtoFactory dtos = new MockDtoFactory();
  private NominatedStudentServiceImpl nominatedStudentService;

  @BeforeEach
  void setUp() {
    nominatedStudentService =
        new NominatedStudentServiceImpl(
            nominatedStudentRepository,
            addressRepository,
            addressService,
            countryRepository,
            userRepository,
            userService);
  }

  /** Personal data sent by the client: Irish nationality, address in Great Britain. */
  private NominatedStudent request() {
    NominatedStudent student = dtos.getNominatedStudent();
    student.setId(STUDENT_ID);
    student.setFirstName("Alice");
    student.setLastName("Martin");
    student.setVersion(0);
    return student;
  }

  private void givenCountriesExist(NominatedStudent student) {
    for (String code :
        List.of(
            student.getNationality().getCountryCode(),
            student.getAddress().getCountry().getCountryCode())) {
      when(countryRepository.findById(code)).thenReturn(new Country());
    }
  }

  private User givenUser(int version) {
    User user = dtos.getUser(Role.STUDENT);
    user.setId(STUDENT_ID);
    user.setVersion(version);
    when(userRepository.findById(STUDENT_ID)).thenReturn(user);
    return user;
  }

  private NominatedStudent storedStudent(int version) {
    NominatedStudent stored = dtos.getNominatedStudent();
    stored.setId(STUDENT_ID);
    stored.setVersion(version);
    Address address = new Address();
    address.setId(ADDRESS_ID);
    stored.setAddress(address);
    return stored;
  }

  @Nested
  class Create {

    private void givenCreationSucceeds() {
      when(addressRepository.create(any()))
          .thenAnswer(invocation -> {
            Address address = invocation.getArgument(0);
            address.setId(ADDRESS_ID);
            return address;
          });
      when(nominatedStudentRepository.create(any()))
          .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @ParameterizedTest
    @CsvSource({"STUDENT, 42", "PROFESSOR, 2"})
    void theStudentOrAProfessorRecordsThePersonalData(Role role, int requester) {
      NominatedStudent request = request();
      givenCountriesExist(request);
      givenUser(4);
      givenCreationSucceeds();

      NominatedStudent created = nominatedStudentService.create(request, requester, role);

      assertThat(created.getId()).isEqualTo(STUDENT_ID);
      // the student row starts at the version of the user row
      assertThat(created.getVersion()).isEqualTo(4);
      assertThat(created.getAddress().getId()).isEqualTo(ADDRESS_ID);
      assertThat(created.getCardHolder()).isEqualTo("MyDad");
      verify(addressRepository).create(request.getAddress());
      verify(nominatedStudentRepository).create(request);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void theStudentHoldsTheAccountWhenNoCardHolderIsGiven(String cardHolder) {
      NominatedStudent request = request();
      request.setCardHolder(cardHolder);
      givenCountriesExist(request);
      givenUser(1);
      givenCreationSucceeds();

      NominatedStudent created = nominatedStudentService.create(request, STUDENT_ID, Role.STUDENT);

      assertThat(created.getCardHolder()).isEqualTo("Alice Martin");
    }

    @Test
    void theDefaultCardHolderIsCutToTheColumnLength() {
      NominatedStudent request = request();
      request.setCardHolder(null);
      request.setFirstName("Maria-Magdalena Alexandra");
      request.setLastName("Van den Bossche-Vandermeulen");
      givenCountriesExist(request);
      givenUser(1);
      givenCreationSucceeds();

      NominatedStudent created = nominatedStudentService.create(request, STUDENT_ID, Role.STUDENT);

      assertThat(created.getCardHolder())
          .hasSize(NominatedStudent.CARD_HOLDER_MAX_LENGTH)
          .isEqualTo("Maria-Magdalena Alexandra Van den B");
    }

    @Test
    void aStudentCannotRecordThePersonalDataOfAnotherStudent() {
      assertThatBusinessException(
              () -> nominatedStudentService.create(request(), STUDENT_ID + 1, Role.STUDENT))
          .hasErrorCode(ErrorCode.ACCESS_DENIED);
      verifyNoInteractions(
          countryRepository, userRepository, addressRepository, nominatedStudentRepository);
    }

    @Test
    void theNationalityMustExist() {
      NominatedStudent request = request();
      request.getNationality().setCountryCode("ZZ");

      assertThatBusinessException(
              () -> nominatedStudentService.create(request, STUDENT_ID, Role.STUDENT))
          .hasErrorCode(ErrorCode.UNKNOWN_COUNTRY)
          .hasArguments("ZZ");
      verifyNoInteractions(addressRepository, nominatedStudentRepository);
    }

    @Test
    void theCountryOfTheAddressMustExist() {
      NominatedStudent request = request();
      request.getAddress().getCountry().setCountryCode("ZZ");
      when(countryRepository.findById(request.getNationality().getCountryCode()))
          .thenReturn(new Country());

      assertThatBusinessException(
              () -> nominatedStudentService.create(request, STUDENT_ID, Role.STUDENT))
          .hasErrorCode(ErrorCode.UNKNOWN_COUNTRY)
          .hasArguments("ZZ");
      verifyNoInteractions(addressRepository, nominatedStudentRepository);
    }

    @Test
    void theUserMustExist() {
      NominatedStudent request = request();
      givenCountriesExist(request);

      assertThatBusinessException(
              () -> nominatedStudentService.create(request, PROFESSOR_ID, Role.PROFESSOR))
          .hasErrorCode(ErrorCode.UNKNOWN_USER)
          .hasArguments(STUDENT_ID);
      verifyNoInteractions(addressRepository, nominatedStudentRepository);
    }

    @Test
    void thePersonalDataAreRecordedOnlyOnce() {
      NominatedStudent request = request();
      givenCountriesExist(request);
      givenUser(1);
      when(nominatedStudentRepository.findById(STUDENT_ID)).thenReturn(storedStudent(1));

      assertThatBusinessException(
              () -> nominatedStudentService.create(request, STUDENT_ID, Role.STUDENT))
          .hasErrorCode(ErrorCode.ALREADY_NOMINATED);
      verifyNoInteractions(addressRepository);
      verify(nominatedStudentRepository, never()).create(any());
    }
  }

  @Nested
  class ShowOne {

    @ParameterizedTest
    @CsvSource({"STUDENT, 42", "PROFESSOR, 2"})
    void theStudentOrAProfessorReadsThePersonalDataWithTheFullAddress(Role role, int requester) {
      NominatedStudent stored = storedStudent(1);
      when(nominatedStudentRepository.findById(STUDENT_ID)).thenReturn(stored);
      Address address = dtos.getAddress();
      address.setId(ADDRESS_ID);
      when(addressRepository.findById(ADDRESS_ID)).thenReturn(address);

      NominatedStudent shown = nominatedStudentService.showOne(STUDENT_ID, requester, role);

      assertThat(shown).isSameAs(stored);
      assertThat(shown.getAddress()).isSameAs(address);
      assertThat(shown.getAddress().getCity()).isEqualTo("LiverPool");
    }

    @Test
    void aStudentCannotReadThePersonalDataOfAnotherStudent() {
      assertThatBusinessException(
              () -> nominatedStudentService.showOne(STUDENT_ID, STUDENT_ID + 1, Role.STUDENT))
          .hasErrorCode(ErrorCode.ACCESS_DENIED);
      verifyNoInteractions(nominatedStudentRepository, addressRepository);
    }

    @Test
    void unrecordedPersonalDataAreNotFound() {
      assertThatBusinessException(
              () -> nominatedStudentService.showOne(STUDENT_ID, STUDENT_ID, Role.STUDENT))
          .hasErrorCode(ErrorCode.RESOURCE_NOT_FOUND);
      verifyNoInteractions(addressRepository);
    }
  }

  @Test
  void showAllListsEveryNominatedStudent() {
    List<NominatedStudent> all = List.of(storedStudent(1));
    when(nominatedStudentRepository.findAll()).thenReturn(all);

    assertThat(nominatedStudentService.showAll()).isSameAs(all);
  }

  @Nested
  class Edit {

    @ParameterizedTest
    @CsvSource({"STUDENT, 42", "PROFESSOR, 2"})
    void editsThePersonalDataTheUserAndTheAddress(Role role, int requester) {
      NominatedStudent request = request();
      request.setVersion(5);
      request.getAddress().setId(0);
      givenCountriesExist(request);
      when(nominatedStudentRepository.findById(STUDENT_ID)).thenReturn(storedStudent(5));
      when(nominatedStudentRepository.update(request))
          .thenAnswer(invocation -> {
            NominatedStudent student = invocation.getArgument(0);
            student.setVersion(student.getVersion() + 1);
            return student;
          });
      Address updatedAddress = new Address();
      updatedAddress.setVersion(9);
      when(addressService.edit(request.getAddress())).thenReturn(updatedAddress);

      NominatedStudent edited = nominatedStudentService.edit(request, requester, role);

      // the client cannot move the student to another address row
      assertThat(edited.getAddress().getId()).isEqualTo(ADDRESS_ID);
      assertThat(edited.getAddress().getVersion()).isEqualTo(9);
      // the user row is edited at the version the client read (legacy shared version)
      assertThat(edited.getVersion()).isEqualTo(5);
      verify(userService).edit(request, requester, role);
    }

    @Test
    void theCardHolderDefaultsToTheStudent() {
      NominatedStudent request = request();
      request.setCardHolder("");
      givenCountriesExist(request);
      when(nominatedStudentRepository.findById(STUDENT_ID)).thenReturn(storedStudent(1));
      when(nominatedStudentRepository.update(request)).thenReturn(request);
      when(addressService.edit(any())).thenAnswer(invocation -> invocation.getArgument(0));

      NominatedStudent edited = nominatedStudentService.edit(request, STUDENT_ID, Role.STUDENT);

      assertThat(edited.getCardHolder()).isEqualTo("Alice Martin");
    }

    @Test
    void aStudentCannotEditThePersonalDataOfAnotherStudent() {
      assertThatBusinessException(
              () -> nominatedStudentService.edit(request(), STUDENT_ID + 1, Role.STUDENT))
          .hasErrorCode(ErrorCode.ACCESS_DENIED);
      verifyNoInteractions(
          countryRepository, nominatedStudentRepository, userService, addressService);
    }

    @Test
    void theCountriesMustExist() {
      NominatedStudent request = request();
      request.getNationality().setCountryCode("ZZ");

      assertThatBusinessException(
              () -> nominatedStudentService.edit(request, STUDENT_ID, Role.STUDENT))
          .hasErrorCode(ErrorCode.UNKNOWN_COUNTRY)
          .hasArguments("ZZ");
      verifyNoInteractions(nominatedStudentRepository, userService, addressService);
    }

    @Test
    void unrecordedPersonalDataAreNotFound() {
      NominatedStudent request = request();
      givenCountriesExist(request);

      assertThatBusinessException(
              () -> nominatedStudentService.edit(request, STUDENT_ID, Role.STUDENT))
          .hasErrorCode(ErrorCode.RESOURCE_NOT_FOUND);
      verify(nominatedStudentRepository, never()).update(any());
      verifyNoInteractions(userService, addressService);
      verify(userRepository, never()).findById(anyInt());
    }
  }
}
