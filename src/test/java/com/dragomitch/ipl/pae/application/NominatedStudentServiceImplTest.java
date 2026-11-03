package com.dragomitch.ipl.pae.application;

import static com.dragomitch.ipl.pae.domain.exceptions.BusinessExceptionAssert.assertThatBusinessException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dragomitch.ipl.pae.domain.dto.AddressDto;
import com.dragomitch.ipl.pae.domain.dto.CountryDto;
import com.dragomitch.ipl.pae.domain.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.domain.dto.UserDto;
import com.dragomitch.ipl.pae.domain.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.domain.model.EntityFactories;
import com.dragomitch.ipl.pae.domain.model.EntityFactory;
import com.dragomitch.ipl.pae.domain.model.Role;
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

  private final EntityFactory entityFactory = EntityFactories.create();
  private final MockDtoFactory dtos = new MockDtoFactory(entityFactory);
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
  private NominatedStudentDto request() {
    NominatedStudentDto student = dtos.getNominatedStudent();
    student.setId(STUDENT_ID);
    student.setFirstName("Alice");
    student.setLastName("Martin");
    student.setVersion(0);
    return student;
  }

  private void givenCountriesExist(NominatedStudentDto student) {
    for (String code :
        List.of(
            student.getNationality().getCountryCode(),
            student.getAddress().getCountry().getCountryCode())) {
      when(countryRepository.findById(code))
          .thenReturn((CountryDto) entityFactory.build(CountryDto.class));
    }
  }

  private UserDto givenUser(int version) {
    UserDto user = dtos.getUser(UserDto.ROLE_STUDENT);
    user.setId(STUDENT_ID);
    user.setVersion(version);
    when(userRepository.findById(STUDENT_ID)).thenReturn(user);
    return user;
  }

  private NominatedStudentDto storedStudent(int version) {
    NominatedStudentDto stored = dtos.getNominatedStudent();
    stored.setId(STUDENT_ID);
    stored.setVersion(version);
    AddressDto address = (AddressDto) entityFactory.build(AddressDto.class);
    address.setId(ADDRESS_ID);
    stored.setAddress(address);
    return stored;
  }

  @Nested
  class Create {

    private void givenCreationSucceeds() {
      when(addressRepository.create(any()))
          .thenAnswer(invocation -> {
            AddressDto address = invocation.getArgument(0);
            address.setId(ADDRESS_ID);
            return address;
          });
      when(nominatedStudentRepository.create(any()))
          .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @ParameterizedTest
    @CsvSource({"Student, 42", "Professor, 2"})
    void theStudentOrAProfessorRecordsThePersonalData(String role, int requester) {
      NominatedStudentDto request = request();
      givenCountriesExist(request);
      givenUser(4);
      givenCreationSucceeds();

      NominatedStudentDto created =
          nominatedStudentService.create(request, requester, Role.fromLabel(role));

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
      NominatedStudentDto request = request();
      request.setCardHolder(cardHolder);
      givenCountriesExist(request);
      givenUser(1);
      givenCreationSucceeds();

      NominatedStudentDto created =
          nominatedStudentService.create(request, STUDENT_ID, Role.STUDENT);

      assertThat(created.getCardHolder()).isEqualTo("Alice Martin");
    }

    @Test
    void theDefaultCardHolderIsCutToTheColumnLength() {
      NominatedStudentDto request = request();
      request.setCardHolder(null);
      request.setFirstName("Maria-Magdalena Alexandra");
      request.setLastName("Van den Bossche-Vandermeulen");
      givenCountriesExist(request);
      givenUser(1);
      givenCreationSucceeds();

      NominatedStudentDto created =
          nominatedStudentService.create(request, STUDENT_ID, Role.STUDENT);

      assertThat(created.getCardHolder())
          .hasSize(NominatedStudentDto.CARD_HOLDER_MAX_LENGTH)
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
      NominatedStudentDto request = request();
      request.getNationality().setCountryCode("ZZ");

      assertThatBusinessException(
              () -> nominatedStudentService.create(request, STUDENT_ID, Role.STUDENT))
          .hasErrorCode(ErrorCode.UNKNOWN_COUNTRY)
          .hasArguments("ZZ");
      verifyNoInteractions(addressRepository, nominatedStudentRepository);
    }

    @Test
    void theCountryOfTheAddressMustExist() {
      NominatedStudentDto request = request();
      request.getAddress().getCountry().setCountryCode("ZZ");
      when(countryRepository.findById(request.getNationality().getCountryCode()))
          .thenReturn((CountryDto) entityFactory.build(CountryDto.class));

      assertThatBusinessException(
              () -> nominatedStudentService.create(request, STUDENT_ID, Role.STUDENT))
          .hasErrorCode(ErrorCode.UNKNOWN_COUNTRY)
          .hasArguments("ZZ");
      verifyNoInteractions(addressRepository, nominatedStudentRepository);
    }

    @Test
    void theUserMustExist() {
      NominatedStudentDto request = request();
      givenCountriesExist(request);

      assertThatBusinessException(
              () -> nominatedStudentService.create(request, PROFESSOR_ID, Role.PROFESSOR))
          .hasErrorCode(ErrorCode.UNKNOWN_USER)
          .hasArguments(STUDENT_ID);
      verifyNoInteractions(addressRepository, nominatedStudentRepository);
    }

    @Test
    void thePersonalDataAreRecordedOnlyOnce() {
      NominatedStudentDto request = request();
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
    @CsvSource({"Student, 42", "Professor, 2"})
    void theStudentOrAProfessorReadsThePersonalDataWithTheFullAddress(String role, int requester) {
      NominatedStudentDto stored = storedStudent(1);
      when(nominatedStudentRepository.findById(STUDENT_ID)).thenReturn(stored);
      AddressDto address = dtos.getAddress();
      address.setId(ADDRESS_ID);
      when(addressRepository.findById(ADDRESS_ID)).thenReturn(address);

      NominatedStudentDto shown =
          nominatedStudentService.showOne(STUDENT_ID, requester, Role.fromLabel(role));

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
    List<NominatedStudentDto> all = List.of(storedStudent(1));
    when(nominatedStudentRepository.findAll()).thenReturn(all);

    assertThat(nominatedStudentService.showAll()).isSameAs(all);
  }

  @Nested
  class Edit {

    @ParameterizedTest
    @CsvSource({"Student, 42", "Professor, 2"})
    void editsThePersonalDataTheUserAndTheAddress(String role, int requester) {
      NominatedStudentDto request = request();
      request.setVersion(5);
      request.getAddress().setId(0);
      givenCountriesExist(request);
      when(nominatedStudentRepository.findById(STUDENT_ID)).thenReturn(storedStudent(5));
      when(nominatedStudentRepository.update(request))
          .thenAnswer(invocation -> {
            NominatedStudentDto student = invocation.getArgument(0);
            student.setVersion(student.getVersion() + 1);
            return student;
          });
      AddressDto updatedAddress = (AddressDto) entityFactory.build(AddressDto.class);
      updatedAddress.setVersion(9);
      when(addressService.edit(request.getAddress())).thenReturn(updatedAddress);

      NominatedStudentDto edited =
          nominatedStudentService.edit(request, requester, Role.fromLabel(role));

      // the client cannot move the student to another address row
      assertThat(edited.getAddress().getId()).isEqualTo(ADDRESS_ID);
      assertThat(edited.getAddress().getVersion()).isEqualTo(9);
      // the user row is edited at the version the client read (legacy shared version)
      assertThat(edited.getVersion()).isEqualTo(5);
      verify(userService).edit(request, requester, Role.fromLabel(role));
    }

    @Test
    void theCardHolderDefaultsToTheStudent() {
      NominatedStudentDto request = request();
      request.setCardHolder("");
      givenCountriesExist(request);
      when(nominatedStudentRepository.findById(STUDENT_ID)).thenReturn(storedStudent(1));
      when(nominatedStudentRepository.update(request)).thenReturn(request);
      when(addressService.edit(any())).thenAnswer(invocation -> invocation.getArgument(0));

      NominatedStudentDto edited = nominatedStudentService.edit(request, STUDENT_ID, Role.STUDENT);

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
      NominatedStudentDto request = request();
      request.getNationality().setCountryCode("ZZ");

      assertThatBusinessException(
              () -> nominatedStudentService.edit(request, STUDENT_ID, Role.STUDENT))
          .hasErrorCode(ErrorCode.UNKNOWN_COUNTRY)
          .hasArguments("ZZ");
      verifyNoInteractions(nominatedStudentRepository, userService, addressService);
    }

    @Test
    void unrecordedPersonalDataAreNotFound() {
      NominatedStudentDto request = request();
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
