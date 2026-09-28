package com.dragomitch.ipl.pae.uccontrollers.implementations;

import static com.dragomitch.ipl.pae.business.exceptions.BusinessExceptionAssert.assertThatBusinessException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.AddressDto;
import com.dragomitch.ipl.pae.business.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.business.dto.MobilityDto;
import com.dragomitch.ipl.pae.business.dto.OptionDto;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.PartnerOptionDto;
import com.dragomitch.ipl.pae.business.dto.PartnerSearch;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.business.implementations.EntityFactories;
import com.dragomitch.ipl.pae.persistence.AddressDao;
import com.dragomitch.ipl.pae.persistence.MobilityChoiceDao;
import com.dragomitch.ipl.pae.persistence.MobilityDao;
import com.dragomitch.ipl.pae.persistence.OptionDao;
import com.dragomitch.ipl.pae.persistence.PartnerDao;
import com.dragomitch.ipl.pae.persistence.PartnerOptionDao;
import com.dragomitch.ipl.pae.persistence.ProgrammeDao;
import com.dragomitch.ipl.pae.persistence.UserDao;
import com.dragomitch.ipl.pae.uccontrollers.MockDtoFactory;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Partners: who may create, edit and restore official or unofficial partners, their options, the
 * archiving rules and the per-role search.
 */
@ExtendWith(MockitoExtension.class)
class PartnerUccImplTest {

  private static final int PARTNER_ID = 5;
  private static final int ADDRESS_ID = 3001;
  private static final int USER_ID = 42;

  @Mock
  private AddressDao addressDao;
  @Mock
  private OptionDao optionDao;
  @Mock
  private PartnerDao partnerDao;
  @Mock
  private PartnerOptionDao partnerOptionDao;
  @Mock
  private MobilityChoiceDao mobilityChoiceDao;
  @Mock
  private MobilityDao mobilityDao;
  @Mock
  private ProgrammeDao programmeDao;
  @Mock
  private UserDao userDao;

  private final EntityFactory entityFactory = EntityFactories.create();
  private final MockDtoFactory dtos = new MockDtoFactory(entityFactory);
  private PartnerUccImpl partnerUcc;

  @BeforeEach
  void setUp() {
    partnerUcc = new PartnerUccImpl(addressDao, optionDao, partnerDao, partnerOptionDao,
        mobilityChoiceDao, mobilityDao, programmeDao, userDao);
  }

  private PartnerOptionDto option(String code) {
    PartnerOptionDto option = (PartnerOptionDto) entityFactory.build(PartnerOptionDto.class);
    option.setCode(code);
    return option;
  }

  private void givenOptionsExist(String... codes) {
    for (String code : codes) {
      when(optionDao.findByCode(code)).thenReturn((OptionDto) entityFactory.build(OptionDto.class));
    }
  }

  /** A partner as {@link PartnerDao#findById} returns it: ids of its address and programme. */
  private PartnerDto givenStoredPartner(boolean official, boolean archived) {
    PartnerDto stored = (PartnerDto) entityFactory.build(PartnerDto.class);
    stored.setId(PARTNER_ID);
    stored.setVersion(6);
    stored.setOfficial(official);
    stored.setArchived(archived);
    AddressDto address = (AddressDto) entityFactory.build(AddressDto.class);
    address.setId(ADDRESS_ID);
    stored.setAddress(address);
    ProgrammeDto programme = (ProgrammeDto) entityFactory.build(ProgrammeDto.class);
    programme.setId(1);
    stored.setProgramme(programme);
    when(partnerDao.findById(PARTNER_ID)).thenReturn(stored);
    return stored;
  }

  @Nested
  class Create {

    private PartnerDto request(boolean official, String... optionCodes) {
      PartnerDto partner = dtos.getPartner();
      partner.setOfficial(official);
      List<PartnerOptionDto> options = new ArrayList<>();
      for (String code : optionCodes) {
        options.add(option(code));
      }
      partner.setOptions(options);
      return partner;
    }

    private void givenCreationSucceeds() {
      when(addressDao.create(any())).thenAnswer(invocation -> {
        AddressDto address = invocation.getArgument(0);
        address.setId(ADDRESS_ID);
        return address;
      });
      when(partnerDao.create(any())).thenAnswer(invocation -> {
        PartnerDto partner = invocation.getArgument(0);
        partner.setId(PARTNER_ID);
        return partner;
      });
    }

    @ParameterizedTest
    @CsvSource({UserDto.ROLE_PROFESSOR + ", true", UserDto.ROLE_PROFESSOR + ", false",
        UserDto.ROLE_STUDENT + ", false"})
    void createsThePartnerItsAddressAndItsOptions(String role, boolean official) {
      PartnerDto request = request(official, "BIN", "BIM");
      givenOptionsExist("BIN", "BIM");
      givenCreationSucceeds();

      PartnerDto created = partnerUcc.create(request, role);

      assertThat(created.getId()).isEqualTo(PARTNER_ID);
      assertThat(created.getAddress().getId()).isEqualTo(ADDRESS_ID);
      // the programme is the one of the country of the partner
      assertThat(created.getProgramme()).isSameAs(request.getAddress().getCountry().getProgramme());
      verify(partnerOptionDao).create(request.getOptions().get(0), PARTNER_ID);
      verify(partnerOptionDao).create(request.getOptions().get(1), PARTNER_ID);
    }

    @Test
    void aStudentCannotCreateAnOfficialPartner() {
      assertThatBusinessException(() -> partnerUcc.create(request(true, "BIN"),
          UserDto.ROLE_STUDENT)).hasErrorCode(ErrorCode.ACCESS_DENIED);
      verifyNoInteractions(addressDao, partnerDao, partnerOptionDao);
    }

    @Test
    void everyOptionMustExist() {
      PartnerDto request = request(false, "BIN", "XYZ");
      givenOptionsExist("BIN");
      givenCreationSucceeds();

      assertThatBusinessException(() -> partnerUcc.create(request, UserDto.ROLE_PROFESSOR))
          .hasErrorCode(ErrorCode.UNKNOWN_OPTION).hasArguments("XYZ");
      verify(partnerOptionDao, never()).create(eq(request.getOptions().get(1)), anyInt());
    }
  }

  @Nested
  class ShowOne {

    @ParameterizedTest
    @CsvSource({"0, true", "2, false"})
    void aPartnerIsArchivableWhenNoActiveChoiceUsesIt(int activeChoices, boolean archivable) {
      givenStoredPartner(true, false);
      AddressDto address = dtos.getAddress();
      when(addressDao.findById(ADDRESS_ID)).thenReturn(address);
      ProgrammeDto programme = dtos.getProgramme();
      when(programmeDao.findById(1)).thenReturn(programme);
      List<PartnerOptionDto> options = List.of(option("BIN"));
      when(partnerOptionDao.findAllOptionsByPartner(PARTNER_ID)).thenReturn(options);
      List<MobilityChoiceDto> choices = new ArrayList<>();
      for (int i = 0; i < activeChoices; i++) {
        choices.add((MobilityChoiceDto) entityFactory.build(MobilityChoiceDto.class));
      }
      when(mobilityChoiceDao.findByActivePartner(PARTNER_ID)).thenReturn(choices);

      PartnerDto shown = partnerUcc.showOne(PARTNER_ID);

      assertThat(shown.getAddress()).isSameAs(address);
      assertThat(shown.getProgramme()).isSameAs(programme);
      assertThat(shown.getOptions()).isEqualTo(options);
      assertThat(shown.isArchivable()).isEqualTo(archivable);
    }

    @Test
    void anUnknownPartnerIsNotFound() {
      assertThatBusinessException(() -> partnerUcc.showOne(PARTNER_ID))
          .hasErrorCode(ErrorCode.RESOURCE_NOT_FOUND);
    }
  }

  @Nested
  class ShowAll {

    private void givenUserWithOption(String code) {
      UserDto user = dtos.getUser(UserDto.ROLE_STUDENT);
      user.getOption().setCode(code);
      when(userDao.findById(USER_ID)).thenReturn(user);
    }

    @ParameterizedTest
    @ValueSource(strings = {UserDto.ROLE_STUDENT, UserDto.ROLE_PROFESSOR})
    void withoutFilterEveryPartnerVisibleToTheRequesterIsListed(String role) {
      givenUserWithOption("BIM");
      List<PartnerDto> partners = List.of(dtos.getPartner());
      when(partnerDao.findAll(PartnerDao.FILTER_ALL_PARTNERS, null, role, "BIM"))
          .thenReturn(partners);

      assertThat(partnerUcc.showAll(PartnerSearch.all(), role, USER_ID)).isSameAs(partners);
    }

    @ParameterizedTest
    @CsvSource({PartnerSearch.FILTER_COUNTRY + ", IE", PartnerSearch.FILTER_ARCHIVED + ", Dublin"})
    void theFilterAndItsValueAreApplied(String filter, String value) {
      givenUserWithOption("BIN");
      List<PartnerDto> partners = List.of(dtos.getPartner());
      when(partnerDao.findAll(filter, value, UserDto.ROLE_STUDENT, "BIN")).thenReturn(partners);

      assertThat(partnerUcc.showAll(new PartnerSearch(filter, value), UserDto.ROLE_STUDENT,
          USER_ID)).isSameAs(partners);
    }

    @Test
    void anUnknownRequesterIsNotFound() {
      assertThatBusinessException(() -> partnerUcc.showAll(PartnerSearch.all(),
          UserDto.ROLE_STUDENT, USER_ID)).hasErrorCode(ErrorCode.RESOURCE_NOT_FOUND);
      verifyNoInteractions(partnerDao);
    }
  }

  @Nested
  class Edit {

    private PartnerDto request(boolean archived, PartnerOptionDto... options) {
      PartnerDto partner = dtos.getPartner();
      partner.setId(0);
      partner.setVersion(0);
      partner.setArchived(archived);
      partner.setOptions(options == null ? null : new ArrayList<>(List.of(options)));
      partner.getAddress().setId(0);
      return partner;
    }

    private void givenUpdatesSucceed() {
      when(addressDao.update(any())).thenAnswer(invocation -> invocation.getArgument(0));
      when(partnerDao.update(any())).thenAnswer(invocation -> {
        PartnerDto partner = invocation.getArgument(0);
        partner.setVersion(partner.getVersion() + 1);
        return partner;
      });
    }

    @Test
    void onlyTheNewOptionsAreAddedOnceTheExistingOnesAreKept() {
      givenStoredPartner(true, false);
      when(partnerOptionDao.findAllOptionsByPartner(PARTNER_ID))
          .thenReturn(List.of(option("BIN")));
      givenOptionsExist("BIM");
      givenUpdatesSucceed();
      PartnerOptionDto bim = option("BIM");
      PartnerDto request = request(false, option("BIN"), bim, option("BIM"));

      PartnerDto edited = partnerUcc.edit(PARTNER_ID, request, UserDto.ROLE_PROFESSOR);

      assertThat(edited.getId()).isEqualTo(PARTNER_ID);
      // the stored row and version, whatever the client sent
      assertThat(edited.getAddress().getId()).isEqualTo(ADDRESS_ID);
      assertThat(edited.getVersion()).isEqualTo(7);
      verify(partnerOptionDao).create(bim, PARTNER_ID);
      verify(partnerOptionDao).create(any(), anyInt());
    }

    @Test
    void anEditWithoutOptionsKeepsTheExistingOnes() {
      givenStoredPartner(true, false);
      when(partnerOptionDao.findAllOptionsByPartner(PARTNER_ID))
          .thenReturn(List.of(option("BIN")));
      givenUpdatesSucceed();

      partnerUcc.edit(PARTNER_ID, request(false, (PartnerOptionDto[]) null),
          UserDto.ROLE_PROFESSOR);

      verify(partnerOptionDao, never()).create(any(), anyInt());
      verify(partnerDao).update(any());
    }

    @Test
    void aPartnerKeepsAtLeastOneOption() {
      givenStoredPartner(true, false);
      when(partnerOptionDao.findAllOptionsByPartner(PARTNER_ID)).thenReturn(List.of());

      assertThatBusinessException(() -> partnerUcc.edit(PARTNER_ID, request(false),
          UserDto.ROLE_PROFESSOR)).hasErrorCode(ErrorCode.PARTNER_OPTION_REQUIRED);
      verify(partnerDao, never()).update(any());
      verifyNoInteractions(addressDao);
    }

    @Test
    void aPartnerWithoutOptionsGetsTheNewOnes() {
      givenStoredPartner(true, false);
      when(partnerOptionDao.findAllOptionsByPartner(PARTNER_ID)).thenReturn(List.of());
      givenOptionsExist("BIN");
      givenUpdatesSucceed();
      PartnerOptionDto bin = option("BIN");

      partnerUcc.edit(PARTNER_ID, request(false, bin), UserDto.ROLE_PROFESSOR);

      verify(partnerOptionDao).create(bin, PARTNER_ID);
    }

    @Test
    void aPartnerWithoutChoicesCanBeArchived() {
      givenStoredPartner(true, false);
      when(mobilityChoiceDao.findByPartner(PARTNER_ID)).thenReturn(List.of());
      when(partnerOptionDao.findAllOptionsByPartner(PARTNER_ID))
          .thenReturn(List.of(option("BIN")));
      givenUpdatesSucceed();

      PartnerDto edited = partnerUcc.edit(PARTNER_ID, request(true), UserDto.ROLE_PROFESSOR);

      assertThat(edited.isArchived()).isTrue();
    }

    @Test
    void aPartnerChosenByStudentsCannotBeArchived() {
      givenStoredPartner(true, false);
      when(mobilityChoiceDao.findByPartner(PARTNER_ID))
          .thenReturn(List.of((MobilityChoiceDto) entityFactory.build(MobilityChoiceDto.class)));

      assertThatBusinessException(() -> partnerUcc.edit(PARTNER_ID, request(true),
          UserDto.ROLE_PROFESSOR)).hasErrorCode(ErrorCode.PARTNER_HAS_MOBILITY_CHOICES);
      verify(partnerDao, never()).update(any());
    }

    @Test
    void aStudentCannotEditAPartner() {
      assertThatBusinessException(() -> partnerUcc.edit(PARTNER_ID, request(false),
          UserDto.ROLE_STUDENT)).hasErrorCode(ErrorCode.ACCESS_DENIED);
      verifyNoInteractions(partnerDao, addressDao, partnerOptionDao);
    }

    @Test
    void anUnknownPartnerIsNotFound() {
      assertThatBusinessException(() -> partnerUcc.edit(PARTNER_ID, request(false),
          UserDto.ROLE_PROFESSOR)).hasErrorCode(ErrorCode.RESOURCE_NOT_FOUND);
      verify(partnerDao, never()).update(any());
    }
  }

  @Nested
  class Restore {

    private void givenUpdateSucceeds() {
      when(partnerDao.update(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @ParameterizedTest
    @CsvSource({UserDto.ROLE_PROFESSOR + ", true", UserDto.ROLE_PROFESSOR + ", false",
        UserDto.ROLE_STUDENT + ", true"})
    void anArchivedPartnerWithOptionsIsRestored(String role, boolean official) {
      givenStoredPartner(official, true);
      when(partnerOptionDao.findAllOptionsByPartner(PARTNER_ID))
          .thenReturn(List.of(option("BIN")));
      givenUpdateSucceeds();

      PartnerDto restored = partnerUcc.restore(PARTNER_ID, role);

      assertThat(restored.isArchived()).isFalse();
      assertThat(restored.isArchivable()).isTrue();
      verify(partnerDao).update(restored);
    }

    @Test
    void aStudentCannotRestoreAnUnofficialPartner() {
      givenStoredPartner(false, true);

      assertThatBusinessException(() -> partnerUcc.restore(PARTNER_ID, UserDto.ROLE_STUDENT))
          .hasErrorCode(ErrorCode.ACCESS_DENIED);
      verify(partnerDao, never()).update(any());
    }

    @Test
    void aPartnerThatIsNotArchivedCannotBeRestored() {
      givenStoredPartner(true, false);

      assertThatBusinessException(() -> partnerUcc.restore(PARTNER_ID, UserDto.ROLE_PROFESSOR))
          .hasErrorCode(ErrorCode.PARTNER_NOT_ARCHIVED);
      verify(partnerDao, never()).update(any());
    }

    @Test
    void aPartnerWithoutOptionsCannotBeRestored() {
      givenStoredPartner(true, true);
      when(partnerOptionDao.findAllOptionsByPartner(PARTNER_ID)).thenReturn(List.of());

      assertThatBusinessException(() -> partnerUcc.restore(PARTNER_ID, UserDto.ROLE_PROFESSOR))
          .hasErrorCode(ErrorCode.PARTNER_OPTION_REQUIRED);
      verify(partnerDao, never()).update(any());
    }

    @Test
    void anUnknownPartnerIsNotFound() {
      assertThatBusinessException(() -> partnerUcc.restore(PARTNER_ID, UserDto.ROLE_PROFESSOR))
          .hasErrorCode(ErrorCode.RESOURCE_NOT_FOUND);
    }
  }

  @Nested
  class Options {

    @Test
    void theOptionsOfAPartnerAreListed() {
      givenStoredPartner(true, false);
      List<PartnerOptionDto> options = List.of(option("BIN"), option("BIM"));
      when(partnerOptionDao.findAllOptionsByPartner(PARTNER_ID)).thenReturn(options);

      assertThat(partnerUcc.findAllPartnerOption(PARTNER_ID)).isSameAs(options);
    }

    @Test
    void theOptionsOfAnUnknownPartnerAreNotFound() {
      assertThatBusinessException(() -> partnerUcc.findAllPartnerOption(PARTNER_ID))
          .hasErrorCode(ErrorCode.RESOURCE_NOT_FOUND);
      verifyNoInteractions(partnerOptionDao);
    }

    @Test
    void aProfessorAddsAnOptionToAnyPartner() {
      givenStoredPartner(true, false);
      givenOptionsExist("BCH");
      PartnerOptionDto option = option("BCH");

      partnerUcc.addOption(PARTNER_ID, option, USER_ID, UserDto.ROLE_PROFESSOR);

      verify(partnerOptionDao).create(option, PARTNER_ID);
      verifyNoInteractions(mobilityChoiceDao, mobilityDao);
    }

    @Test
    void aStudentAddsAnOptionToTheUnofficialPartnerOfHisMobilityChoice() {
      givenStoredPartner(false, false);
      givenOptionsExist("BCH");
      when(mobilityChoiceDao.findByUser(USER_ID)).thenReturn(List.of(choiceWithPartner(PARTNER_ID)));
      PartnerOptionDto option = option("BCH");

      partnerUcc.addOption(PARTNER_ID, option, USER_ID, UserDto.ROLE_STUDENT);

      verify(partnerOptionDao).create(option, PARTNER_ID);
    }

    @Test
    void aStudentAddsAnOptionToTheUnofficialPartnerOfHisMobility() {
      // a choice confirmed with a new partner is a mobility at once
      givenStoredPartner(false, false);
      givenOptionsExist("BCH");
      when(mobilityChoiceDao.findByUser(USER_ID)).thenReturn(List.of(choiceWithPartner(0)));
      when(mobilityDao.findByUser(USER_ID)).thenReturn(List.of(mobilityWithPartner(PARTNER_ID)));
      PartnerOptionDto option = option("BCH");

      partnerUcc.addOption(PARTNER_ID, option, USER_ID, UserDto.ROLE_STUDENT);

      verify(partnerOptionDao).create(option, PARTNER_ID);
    }

    @Test
    void aStudentCannotAddAnOptionToAnOfficialPartner() {
      givenStoredPartner(true, false);

      assertThatBusinessException(() -> partnerUcc.addOption(PARTNER_ID, option("BCH"), USER_ID,
          UserDto.ROLE_STUDENT)).hasErrorCode(ErrorCode.ACCESS_DENIED);
      verifyNoInteractions(partnerOptionDao, optionDao);
    }

    @Test
    void aStudentCannotAddAnOptionToAPartnerHeDoesNotUse() {
      givenStoredPartner(false, false);
      when(mobilityChoiceDao.findByUser(USER_ID)).thenReturn(List.of(choiceWithPartner(0)));
      when(mobilityDao.findByUser(USER_ID)).thenReturn(List.of(mobilityWithPartner(0),
          mobilityWithPartner(PARTNER_ID + 1)));

      assertThatBusinessException(() -> partnerUcc.addOption(PARTNER_ID, option("BCH"), USER_ID,
          UserDto.ROLE_STUDENT)).hasErrorCode(ErrorCode.ACCESS_DENIED);
      verifyNoInteractions(partnerOptionDao, optionDao);
    }

    @Test
    void anOptionOfAnUnknownPartnerIsNotFound() {
      assertThatBusinessException(() -> partnerUcc.addOption(PARTNER_ID, option("BCH"), USER_ID,
          UserDto.ROLE_PROFESSOR)).hasErrorCode(ErrorCode.RESOURCE_NOT_FOUND);
      verifyNoInteractions(partnerOptionDao);
    }

    @Test
    void anUnknownOptionIsNotAdded() {
      givenStoredPartner(true, false);

      assertThatBusinessException(() -> partnerUcc.addOption(PARTNER_ID, option("XYZ"), USER_ID,
          UserDto.ROLE_PROFESSOR)).hasErrorCode(ErrorCode.UNKNOWN_OPTION).hasArguments("XYZ");
      verifyNoInteractions(partnerOptionDao);
    }

    /** A choice of the student with that partner ({@code 0}: no partner). */
    private MobilityChoiceDto choiceWithPartner(int partnerId) {
      MobilityChoiceDto choice = (MobilityChoiceDto) entityFactory.build(MobilityChoiceDto.class);
      if (partnerId > 0) {
        PartnerDto partner = (PartnerDto) entityFactory.build(PartnerDto.class);
        partner.setId(partnerId);
        choice.setPartner(partner);
      }
      return choice;
    }

    /** A mobility of the student with that partner ({@code 0}: no partner). */
    private MobilityDto mobilityWithPartner(int partnerId) {
      MobilityDto mobility = (MobilityDto) entityFactory.build(MobilityDto.class);
      if (partnerId > 0) {
        PartnerDto partner = (PartnerDto) entityFactory.build(PartnerDto.class);
        partner.setId(partnerId);
        mobility.setPartner(partner);
      }
      return mobility;
    }
  }
}
