package com.dragomitch.ipl.pae.uccontrollers.implementations;

import static com.dragomitch.ipl.pae.business.exceptions.BusinessExceptionAssert.assertThatBusinessException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.AddressDto;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.business.implementations.EntityFactories;
import com.dragomitch.ipl.pae.persistence.AddressDao;
import com.dragomitch.ipl.pae.persistence.CountryDao;
import com.dragomitch.ipl.pae.persistence.NominatedStudentDao;
import com.dragomitch.ipl.pae.persistence.UserDao;
import com.dragomitch.ipl.pae.uccontrollers.AddressUcc;
import com.dragomitch.ipl.pae.uccontrollers.MockDtoFactory;
import com.dragomitch.ipl.pae.uccontrollers.UserUcc;

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
 * following the contract of {@code NominatedStudentDaoIT} (the student shares the id of the user).
 */
@ExtendWith(MockitoExtension.class)
class NominatedStudentUccImplTest {

  private static final int STUDENT_ID = 42;
  private static final int PROFESSOR_ID = 2;
  private static final int ADDRESS_ID = 2001;

  @Mock
  private NominatedStudentDao nominatedStudentDao;
  @Mock
  private AddressDao addressDao;
  @Mock
  private AddressUcc addressUcc;
  @Mock
  private CountryDao countryDao;
  @Mock
  private UserDao userDao;
  @Mock
  private UserUcc userUcc;

  private final EntityFactory entityFactory = EntityFactories.create();
  private final MockDtoFactory dtos = new MockDtoFactory(entityFactory);
  private NominatedStudentUccImpl nominatedStudentUcc;

  @BeforeEach
  void setUp() {
    nominatedStudentUcc = new NominatedStudentUccImpl(nominatedStudentDao, addressDao, addressUcc,
        countryDao, userDao, userUcc);
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
    for (String code : List.of(student.getNationality().getCountryCode(),
        student.getAddress().getCountry().getCountryCode())) {
      when(countryDao.findById(code)).thenReturn((CountryDto) entityFactory.build(CountryDto.class));
    }
  }

  private UserDto givenUser(int version) {
    UserDto user = dtos.getUser(UserDto.ROLE_STUDENT);
    user.setId(STUDENT_ID);
    user.setVersion(version);
    when(userDao.findById(STUDENT_ID)).thenReturn(user);
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
      when(addressDao.create(any())).thenAnswer(invocation -> {
        AddressDto address = invocation.getArgument(0);
        address.setId(ADDRESS_ID);
        return address;
      });
      when(nominatedStudentDao.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @ParameterizedTest
    @CsvSource({"Student, 42", "Professor, 2"})
    void theStudentOrAProfessorRecordsThePersonalData(String role, int requester) {
      NominatedStudentDto request = request();
      givenCountriesExist(request);
      givenUser(4);
      givenCreationSucceeds();

      NominatedStudentDto created = nominatedStudentUcc.create(request, requester, role);

      assertThat(created.getId()).isEqualTo(STUDENT_ID);
      // the student row starts at the version of the user row
      assertThat(created.getVersion()).isEqualTo(4);
      assertThat(created.getAddress().getId()).isEqualTo(ADDRESS_ID);
      assertThat(created.getCardHolder()).isEqualTo("MyDad");
      verify(addressDao).create(request.getAddress());
      verify(nominatedStudentDao).create(request);
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
          nominatedStudentUcc.create(request, STUDENT_ID, UserDto.ROLE_STUDENT);

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
          nominatedStudentUcc.create(request, STUDENT_ID, UserDto.ROLE_STUDENT);

      assertThat(created.getCardHolder())
          .hasSize(NominatedStudentDto.CARD_HOLDER_MAX_LENGTH)
          .isEqualTo("Maria-Magdalena Alexandra Van den B");
    }

    @Test
    void aStudentCannotRecordThePersonalDataOfAnotherStudent() {
      assertThatBusinessException(() -> nominatedStudentUcc.create(request(), STUDENT_ID + 1,
          UserDto.ROLE_STUDENT)).hasErrorCode(ErrorCode.ACCESS_DENIED);
      verifyNoInteractions(countryDao, userDao, addressDao, nominatedStudentDao);
    }

    @Test
    void theNationalityMustExist() {
      NominatedStudentDto request = request();
      request.getNationality().setCountryCode("ZZ");

      assertThatBusinessException(() -> nominatedStudentUcc.create(request, STUDENT_ID,
          UserDto.ROLE_STUDENT)).hasErrorCode(ErrorCode.UNKNOWN_COUNTRY).hasArguments("ZZ");
      verifyNoInteractions(addressDao, nominatedStudentDao);
    }

    @Test
    void theCountryOfTheAddressMustExist() {
      NominatedStudentDto request = request();
      request.getAddress().getCountry().setCountryCode("ZZ");
      when(countryDao.findById(request.getNationality().getCountryCode()))
          .thenReturn((CountryDto) entityFactory.build(CountryDto.class));

      assertThatBusinessException(() -> nominatedStudentUcc.create(request, STUDENT_ID,
          UserDto.ROLE_STUDENT)).hasErrorCode(ErrorCode.UNKNOWN_COUNTRY).hasArguments("ZZ");
      verifyNoInteractions(addressDao, nominatedStudentDao);
    }

    @Test
    void theUserMustExist() {
      NominatedStudentDto request = request();
      givenCountriesExist(request);

      assertThatBusinessException(() -> nominatedStudentUcc.create(request, PROFESSOR_ID,
          UserDto.ROLE_PROFESSOR)).hasErrorCode(ErrorCode.UNKNOWN_USER).hasArguments(STUDENT_ID);
      verifyNoInteractions(addressDao, nominatedStudentDao);
    }

    @Test
    void thePersonalDataAreRecordedOnlyOnce() {
      NominatedStudentDto request = request();
      givenCountriesExist(request);
      givenUser(1);
      when(nominatedStudentDao.findById(STUDENT_ID)).thenReturn(storedStudent(1));

      assertThatBusinessException(() -> nominatedStudentUcc.create(request, STUDENT_ID,
          UserDto.ROLE_STUDENT)).hasErrorCode(ErrorCode.ALREADY_NOMINATED);
      verifyNoInteractions(addressDao);
      verify(nominatedStudentDao, never()).create(any());
    }
  }

  @Nested
  class ShowOne {

    @ParameterizedTest
    @CsvSource({"Student, 42", "Professor, 2"})
    void theStudentOrAProfessorReadsThePersonalDataWithTheFullAddress(String role,
        int requester) {
      NominatedStudentDto stored = storedStudent(1);
      when(nominatedStudentDao.findById(STUDENT_ID)).thenReturn(stored);
      AddressDto address = dtos.getAddress();
      address.setId(ADDRESS_ID);
      when(addressDao.findById(ADDRESS_ID)).thenReturn(address);

      NominatedStudentDto shown = nominatedStudentUcc.showOne(STUDENT_ID, requester, role);

      assertThat(shown).isSameAs(stored);
      assertThat(shown.getAddress()).isSameAs(address);
      assertThat(shown.getAddress().getCity()).isEqualTo("LiverPool");
    }

    @Test
    void aStudentCannotReadThePersonalDataOfAnotherStudent() {
      assertThatBusinessException(() -> nominatedStudentUcc.showOne(STUDENT_ID, STUDENT_ID + 1,
          UserDto.ROLE_STUDENT)).hasErrorCode(ErrorCode.ACCESS_DENIED);
      verifyNoInteractions(nominatedStudentDao, addressDao);
    }

    @Test
    void unrecordedPersonalDataAreNotFound() {
      assertThatBusinessException(() -> nominatedStudentUcc.showOne(STUDENT_ID, STUDENT_ID,
          UserDto.ROLE_STUDENT)).hasErrorCode(ErrorCode.RESOURCE_NOT_FOUND);
      verifyNoInteractions(addressDao);
    }
  }

  @Test
  void showAllListsEveryNominatedStudent() {
    List<NominatedStudentDto> all = List.of(storedStudent(1));
    when(nominatedStudentDao.findAll()).thenReturn(all);

    assertThat(nominatedStudentUcc.showAll()).isSameAs(all);
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
      when(nominatedStudentDao.findById(STUDENT_ID)).thenReturn(storedStudent(5));
      when(nominatedStudentDao.update(request)).thenAnswer(invocation -> {
        NominatedStudentDto student = invocation.getArgument(0);
        student.setVersion(student.getVersion() + 1);
        return student;
      });
      AddressDto updatedAddress = (AddressDto) entityFactory.build(AddressDto.class);
      updatedAddress.setVersion(9);
      when(addressUcc.edit(request.getAddress())).thenReturn(updatedAddress);

      NominatedStudentDto edited = nominatedStudentUcc.edit(request, requester, role);

      // the client cannot move the student to another address row
      assertThat(edited.getAddress().getId()).isEqualTo(ADDRESS_ID);
      assertThat(edited.getAddress().getVersion()).isEqualTo(9);
      // the user row is edited at the version the client read (legacy shared version)
      assertThat(edited.getVersion()).isEqualTo(5);
      verify(userUcc).edit(request, requester, role);
    }

    @Test
    void theCardHolderDefaultsToTheStudent() {
      NominatedStudentDto request = request();
      request.setCardHolder("");
      givenCountriesExist(request);
      when(nominatedStudentDao.findById(STUDENT_ID)).thenReturn(storedStudent(1));
      when(nominatedStudentDao.update(request)).thenReturn(request);
      when(addressUcc.edit(any())).thenAnswer(invocation -> invocation.getArgument(0));

      NominatedStudentDto edited =
          nominatedStudentUcc.edit(request, STUDENT_ID, UserDto.ROLE_STUDENT);

      assertThat(edited.getCardHolder()).isEqualTo("Alice Martin");
    }

    @Test
    void aStudentCannotEditThePersonalDataOfAnotherStudent() {
      assertThatBusinessException(() -> nominatedStudentUcc.edit(request(), STUDENT_ID + 1,
          UserDto.ROLE_STUDENT)).hasErrorCode(ErrorCode.ACCESS_DENIED);
      verifyNoInteractions(countryDao, nominatedStudentDao, userUcc, addressUcc);
    }

    @Test
    void theCountriesMustExist() {
      NominatedStudentDto request = request();
      request.getNationality().setCountryCode("ZZ");

      assertThatBusinessException(() -> nominatedStudentUcc.edit(request, STUDENT_ID,
          UserDto.ROLE_STUDENT)).hasErrorCode(ErrorCode.UNKNOWN_COUNTRY).hasArguments("ZZ");
      verifyNoInteractions(nominatedStudentDao, userUcc, addressUcc);
    }

    @Test
    void unrecordedPersonalDataAreNotFound() {
      NominatedStudentDto request = request();
      givenCountriesExist(request);

      assertThatBusinessException(() -> nominatedStudentUcc.edit(request, STUDENT_ID,
          UserDto.ROLE_STUDENT)).hasErrorCode(ErrorCode.RESOURCE_NOT_FOUND);
      verify(nominatedStudentDao, never()).update(any());
      verifyNoInteractions(userUcc, addressUcc);
      verify(userDao, never()).findById(anyInt());
    }
  }
}
