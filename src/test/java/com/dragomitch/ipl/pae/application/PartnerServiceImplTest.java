package com.dragomitch.ipl.pae.application;

import static com.dragomitch.ipl.pae.domain.exceptions.BusinessExceptionAssert.assertThatBusinessException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dragomitch.ipl.pae.domain.dto.AddressDto;
import com.dragomitch.ipl.pae.domain.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.domain.dto.MobilityDto;
import com.dragomitch.ipl.pae.domain.dto.OptionDto;
import com.dragomitch.ipl.pae.domain.dto.PartnerDto;
import com.dragomitch.ipl.pae.domain.dto.PartnerOptionDto;
import com.dragomitch.ipl.pae.domain.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.domain.dto.UserDto;
import com.dragomitch.ipl.pae.domain.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.domain.model.EntityFactories;
import com.dragomitch.ipl.pae.domain.model.EntityFactory;
import com.dragomitch.ipl.pae.domain.model.PartnerFilter;
import com.dragomitch.ipl.pae.domain.model.Role;
import com.dragomitch.ipl.pae.domain.port.out.AddressRepository;
import com.dragomitch.ipl.pae.domain.port.out.MobilityChoiceRepository;
import com.dragomitch.ipl.pae.domain.port.out.MobilityRepository;
import com.dragomitch.ipl.pae.domain.port.out.OptionRepository;
import com.dragomitch.ipl.pae.domain.port.out.PartnerOptionRepository;
import com.dragomitch.ipl.pae.domain.port.out.PartnerRepository;
import com.dragomitch.ipl.pae.domain.port.out.ProgrammeRepository;
import com.dragomitch.ipl.pae.domain.port.out.UserRepository;
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
class PartnerServiceImplTest {

  private static final int PARTNER_ID = 5;
  private static final int ADDRESS_ID = 3001;
  private static final int USER_ID = 42;

  @Mock private AddressRepository addressRepository;
  @Mock private OptionRepository optionRepository;
  @Mock private PartnerRepository partnerRepository;
  @Mock private PartnerOptionRepository partnerOptionRepository;
  @Mock private MobilityChoiceRepository mobilityChoiceRepository;
  @Mock private MobilityRepository mobilityRepository;
  @Mock private ProgrammeRepository programmeRepository;
  @Mock private UserRepository userRepository;

  private final EntityFactory entityFactory = EntityFactories.create();
  private final MockDtoFactory dtos = new MockDtoFactory(entityFactory);
  private PartnerServiceImpl partnerService;

  @BeforeEach
  void setUp() {
    partnerService =
        new PartnerServiceImpl(
            addressRepository,
            optionRepository,
            partnerRepository,
            partnerOptionRepository,
            mobilityChoiceRepository,
            mobilityRepository,
            programmeRepository,
            userRepository);
  }

  private PartnerOptionDto option(String code) {
    PartnerOptionDto option = (PartnerOptionDto) entityFactory.build(PartnerOptionDto.class);
    option.setCode(code);
    return option;
  }

  private void givenOptionsExist(String... codes) {
    for (String code : codes) {
      when(optionRepository.findByCode(code))
          .thenReturn((OptionDto) entityFactory.build(OptionDto.class));
    }
  }

  /**
   * A partner as {@link PartnerRepository#findById} returns it: ids of its address and programme.
   */
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
    when(partnerRepository.findById(PARTNER_ID)).thenReturn(stored);
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
      when(addressRepository.create(any()))
          .thenAnswer(invocation -> {
            AddressDto address = invocation.getArgument(0);
            address.setId(ADDRESS_ID);
            return address;
          });
      when(partnerRepository.create(any()))
          .thenAnswer(invocation -> {
            PartnerDto partner = invocation.getArgument(0);
            partner.setId(PARTNER_ID);
            return partner;
          });
    }

    @ParameterizedTest
    @CsvSource({
      UserDto.ROLE_PROFESSOR + ", true",
      UserDto.ROLE_PROFESSOR + ", false",
      UserDto.ROLE_STUDENT + ", false"
    })
    void createsThePartnerItsAddressAndItsOptions(String role, boolean official) {
      PartnerDto request = request(official, "BIN", "BIM");
      givenOptionsExist("BIN", "BIM");
      givenCreationSucceeds();

      PartnerDto created = partnerService.create(request, Role.fromLabel(role));

      assertThat(created.getId()).isEqualTo(PARTNER_ID);
      assertThat(created.getAddress().getId()).isEqualTo(ADDRESS_ID);
      // the programme is the one of the country of the partner
      assertThat(created.getProgramme()).isSameAs(request.getAddress().getCountry().getProgramme());
      verify(partnerOptionRepository).create(request.getOptions().get(0), PARTNER_ID);
      verify(partnerOptionRepository).create(request.getOptions().get(1), PARTNER_ID);
    }

    @Test
    void aStudentCannotCreateAnOfficialPartner() {
      assertThatBusinessException(() -> partnerService.create(request(true, "BIN"), Role.STUDENT))
          .hasErrorCode(ErrorCode.ACCESS_DENIED);
      verifyNoInteractions(addressRepository, partnerRepository, partnerOptionRepository);
    }

    @Test
    void everyOptionMustExist() {
      PartnerDto request = request(false, "BIN", "XYZ");
      givenOptionsExist("BIN");
      givenCreationSucceeds();

      assertThatBusinessException(() -> partnerService.create(request, Role.PROFESSOR))
          .hasErrorCode(ErrorCode.UNKNOWN_OPTION)
          .hasArguments("XYZ");
      verify(partnerOptionRepository, never()).create(eq(request.getOptions().get(1)), anyInt());
    }
  }

  @Nested
  class ShowOne {

    @ParameterizedTest
    @CsvSource({"0, true", "2, false"})
    void aPartnerIsArchivableWhenNoActiveChoiceUsesIt(int activeChoices, boolean archivable) {
      givenStoredPartner(true, false);
      AddressDto address = dtos.getAddress();
      when(addressRepository.findById(ADDRESS_ID)).thenReturn(address);
      ProgrammeDto programme = dtos.getProgramme();
      when(programmeRepository.findById(1)).thenReturn(programme);
      List<PartnerOptionDto> options = List.of(option("BIN"));
      when(partnerOptionRepository.findAllOptionsByPartner(PARTNER_ID)).thenReturn(options);
      List<MobilityChoiceDto> choices = new ArrayList<>();
      for (int i = 0; i < activeChoices; i++) {
        choices.add((MobilityChoiceDto) entityFactory.build(MobilityChoiceDto.class));
      }
      when(mobilityChoiceRepository.findByActivePartner(PARTNER_ID)).thenReturn(choices);

      PartnerDto shown = partnerService.showOne(PARTNER_ID);

      assertThat(shown.getAddress()).isSameAs(address);
      assertThat(shown.getProgramme()).isSameAs(programme);
      assertThat(shown.getOptions()).isEqualTo(options);
      assertThat(shown.isArchivable()).isEqualTo(archivable);
    }

    @Test
    void anUnknownPartnerIsNotFound() {
      assertThatBusinessException(() -> partnerService.showOne(PARTNER_ID))
          .hasErrorCode(ErrorCode.RESOURCE_NOT_FOUND);
    }
  }

  @Nested
  class ShowAll {

    private void givenUserWithOption(String code) {
      UserDto user = dtos.getUser(UserDto.ROLE_STUDENT);
      user.getOption().setCode(code);
      when(userRepository.findById(USER_ID)).thenReturn(user);
    }

    @ParameterizedTest
    @ValueSource(strings = {UserDto.ROLE_STUDENT, UserDto.ROLE_PROFESSOR})
    void withoutFilterEveryPartnerVisibleToTheRequesterIsListed(String role) {
      givenUserWithOption("BIM");
      List<PartnerDto> partners = List.of(dtos.getPartner());
      when(partnerRepository.findAll(PartnerFilter.ALL, null, Role.fromLabel(role), "BIM"))
          .thenReturn(partners);

      assertThat(partnerService.showAll(PartnerFilter.ALL, null, Role.fromLabel(role), USER_ID))
          .isSameAs(partners);
    }

    @ParameterizedTest
    @CsvSource({"COUNTRY, IE", "ARCHIVED, Dublin"})
    void theFilterAndItsValueAreApplied(PartnerFilter filter, String value) {
      givenUserWithOption("BIN");
      List<PartnerDto> partners = List.of(dtos.getPartner());
      when(partnerRepository.findAll(filter, value, Role.STUDENT, "BIN")).thenReturn(partners);

      assertThat(partnerService.showAll(filter, value, Role.STUDENT, USER_ID)).isSameAs(partners);
    }

    @Test
    void anUnknownRequesterIsNotFound() {
      assertThatBusinessException(
              () -> partnerService.showAll(PartnerFilter.ALL, null, Role.STUDENT, USER_ID))
          .hasErrorCode(ErrorCode.RESOURCE_NOT_FOUND);
      verifyNoInteractions(partnerRepository);
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
      when(addressRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));
      when(partnerRepository.update(any()))
          .thenAnswer(invocation -> {
            PartnerDto partner = invocation.getArgument(0);
            partner.setVersion(partner.getVersion() + 1);
            return partner;
          });
    }

    @Test
    void onlyTheNewOptionsAreAddedOnceTheExistingOnesAreKept() {
      givenStoredPartner(true, false);
      when(partnerOptionRepository.findAllOptionsByPartner(PARTNER_ID))
          .thenReturn(List.of(option("BIN")));
      givenOptionsExist("BIM");
      givenUpdatesSucceed();
      PartnerOptionDto bim = option("BIM");
      PartnerDto request = request(false, option("BIN"), bim, option("BIM"));

      PartnerDto edited = partnerService.edit(PARTNER_ID, request, Role.PROFESSOR);

      assertThat(edited.getId()).isEqualTo(PARTNER_ID);
      // the stored row and version, whatever the client sent
      assertThat(edited.getAddress().getId()).isEqualTo(ADDRESS_ID);
      assertThat(edited.getVersion()).isEqualTo(7);
      verify(partnerOptionRepository).create(bim, PARTNER_ID);
      verify(partnerOptionRepository).create(any(), anyInt());
    }

    @Test
    void anEditWithoutOptionsKeepsTheExistingOnes() {
      givenStoredPartner(true, false);
      when(partnerOptionRepository.findAllOptionsByPartner(PARTNER_ID))
          .thenReturn(List.of(option("BIN")));
      givenUpdatesSucceed();

      partnerService.edit(PARTNER_ID, request(false, (PartnerOptionDto[]) null), Role.PROFESSOR);

      verify(partnerOptionRepository, never()).create(any(), anyInt());
      verify(partnerRepository).update(any());
    }

    @Test
    void aPartnerKeepsAtLeastOneOption() {
      givenStoredPartner(true, false);
      when(partnerOptionRepository.findAllOptionsByPartner(PARTNER_ID)).thenReturn(List.of());

      assertThatBusinessException(
              () -> partnerService.edit(PARTNER_ID, request(false), Role.PROFESSOR))
          .hasErrorCode(ErrorCode.PARTNER_OPTION_REQUIRED);
      verify(partnerRepository, never()).update(any());
      verifyNoInteractions(addressRepository);
    }

    @Test
    void aPartnerWithoutOptionsGetsTheNewOnes() {
      givenStoredPartner(true, false);
      when(partnerOptionRepository.findAllOptionsByPartner(PARTNER_ID)).thenReturn(List.of());
      givenOptionsExist("BIN");
      givenUpdatesSucceed();
      PartnerOptionDto bin = option("BIN");

      partnerService.edit(PARTNER_ID, request(false, bin), Role.PROFESSOR);

      verify(partnerOptionRepository).create(bin, PARTNER_ID);
    }

    @Test
    void aPartnerWithoutChoicesCanBeArchived() {
      givenStoredPartner(true, false);
      when(mobilityChoiceRepository.findByPartner(PARTNER_ID)).thenReturn(List.of());
      when(partnerOptionRepository.findAllOptionsByPartner(PARTNER_ID))
          .thenReturn(List.of(option("BIN")));
      givenUpdatesSucceed();

      PartnerDto edited = partnerService.edit(PARTNER_ID, request(true), Role.PROFESSOR);

      assertThat(edited.isArchived()).isTrue();
    }

    @Test
    void aPartnerChosenByStudentsCannotBeArchived() {
      givenStoredPartner(true, false);
      when(mobilityChoiceRepository.findByPartner(PARTNER_ID))
          .thenReturn(List.of((MobilityChoiceDto) entityFactory.build(MobilityChoiceDto.class)));

      assertThatBusinessException(
              () -> partnerService.edit(PARTNER_ID, request(true), Role.PROFESSOR))
          .hasErrorCode(ErrorCode.PARTNER_HAS_MOBILITY_CHOICES);
      verify(partnerRepository, never()).update(any());
    }

    @Test
    void aStudentCannotEditAPartner() {
      assertThatBusinessException(
              () -> partnerService.edit(PARTNER_ID, request(false), Role.STUDENT))
          .hasErrorCode(ErrorCode.ACCESS_DENIED);
      verifyNoInteractions(partnerRepository, addressRepository, partnerOptionRepository);
    }

    @Test
    void anUnknownPartnerIsNotFound() {
      assertThatBusinessException(
              () -> partnerService.edit(PARTNER_ID, request(false), Role.PROFESSOR))
          .hasErrorCode(ErrorCode.RESOURCE_NOT_FOUND);
      verify(partnerRepository, never()).update(any());
    }
  }

  @Nested
  class Restore {

    private void givenUpdateSucceeds() {
      when(partnerRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @ParameterizedTest
    @CsvSource({
      UserDto.ROLE_PROFESSOR + ", true",
      UserDto.ROLE_PROFESSOR + ", false",
      UserDto.ROLE_STUDENT + ", true"
    })
    void anArchivedPartnerWithOptionsIsRestored(String role, boolean official) {
      givenStoredPartner(official, true);
      when(partnerOptionRepository.findAllOptionsByPartner(PARTNER_ID))
          .thenReturn(List.of(option("BIN")));
      givenUpdateSucceeds();

      PartnerDto restored = partnerService.restore(PARTNER_ID, Role.fromLabel(role));

      assertThat(restored.isArchived()).isFalse();
      assertThat(restored.isArchivable()).isTrue();
      verify(partnerRepository).update(restored);
    }

    @Test
    void aStudentCannotRestoreAnUnofficialPartner() {
      givenStoredPartner(false, true);

      assertThatBusinessException(() -> partnerService.restore(PARTNER_ID, Role.STUDENT))
          .hasErrorCode(ErrorCode.ACCESS_DENIED);
      verify(partnerRepository, never()).update(any());
    }

    @Test
    void aPartnerThatIsNotArchivedCannotBeRestored() {
      givenStoredPartner(true, false);

      assertThatBusinessException(() -> partnerService.restore(PARTNER_ID, Role.PROFESSOR))
          .hasErrorCode(ErrorCode.PARTNER_NOT_ARCHIVED);
      verify(partnerRepository, never()).update(any());
    }

    @Test
    void aPartnerWithoutOptionsCannotBeRestored() {
      givenStoredPartner(true, true);
      when(partnerOptionRepository.findAllOptionsByPartner(PARTNER_ID)).thenReturn(List.of());

      assertThatBusinessException(() -> partnerService.restore(PARTNER_ID, Role.PROFESSOR))
          .hasErrorCode(ErrorCode.PARTNER_OPTION_REQUIRED);
      verify(partnerRepository, never()).update(any());
    }

    @Test
    void anUnknownPartnerIsNotFound() {
      assertThatBusinessException(() -> partnerService.restore(PARTNER_ID, Role.PROFESSOR))
          .hasErrorCode(ErrorCode.RESOURCE_NOT_FOUND);
    }
  }

  @Nested
  class Options {

    @Test
    void theOptionsOfAPartnerAreListed() {
      givenStoredPartner(true, false);
      List<PartnerOptionDto> options = List.of(option("BIN"), option("BIM"));
      when(partnerOptionRepository.findAllOptionsByPartner(PARTNER_ID)).thenReturn(options);

      assertThat(partnerService.findAllPartnerOption(PARTNER_ID)).isSameAs(options);
    }

    @Test
    void theOptionsOfAnUnknownPartnerAreNotFound() {
      assertThatBusinessException(() -> partnerService.findAllPartnerOption(PARTNER_ID))
          .hasErrorCode(ErrorCode.RESOURCE_NOT_FOUND);
      verifyNoInteractions(partnerOptionRepository);
    }

    @Test
    void aProfessorAddsAnOptionToAnyPartner() {
      givenStoredPartner(true, false);
      givenOptionsExist("BCH");
      PartnerOptionDto option = option("BCH");

      partnerService.addOption(PARTNER_ID, option, USER_ID, Role.PROFESSOR);

      verify(partnerOptionRepository).create(option, PARTNER_ID);
      verifyNoInteractions(mobilityChoiceRepository, mobilityRepository);
    }

    @Test
    void aStudentAddsAnOptionToTheUnofficialPartnerOfHisMobilityChoice() {
      givenStoredPartner(false, false);
      givenOptionsExist("BCH");
      when(mobilityChoiceRepository.findByUser(USER_ID))
          .thenReturn(List.of(choiceWithPartner(PARTNER_ID)));
      PartnerOptionDto option = option("BCH");

      partnerService.addOption(PARTNER_ID, option, USER_ID, Role.STUDENT);

      verify(partnerOptionRepository).create(option, PARTNER_ID);
    }

    @Test
    void aStudentAddsAnOptionToTheUnofficialPartnerOfHisMobility() {
      // a choice confirmed with a new partner is a mobility at once
      givenStoredPartner(false, false);
      givenOptionsExist("BCH");
      when(mobilityChoiceRepository.findByUser(USER_ID)).thenReturn(List.of(choiceWithPartner(0)));
      when(mobilityRepository.findByUser(USER_ID))
          .thenReturn(List.of(mobilityWithPartner(PARTNER_ID)));
      PartnerOptionDto option = option("BCH");

      partnerService.addOption(PARTNER_ID, option, USER_ID, Role.STUDENT);

      verify(partnerOptionRepository).create(option, PARTNER_ID);
    }

    @Test
    void aStudentCannotAddAnOptionToAnOfficialPartner() {
      givenStoredPartner(true, false);

      assertThatBusinessException(
              () -> partnerService.addOption(PARTNER_ID, option("BCH"), USER_ID, Role.STUDENT))
          .hasErrorCode(ErrorCode.ACCESS_DENIED);
      verifyNoInteractions(partnerOptionRepository, optionRepository);
    }

    @Test
    void aStudentCannotAddAnOptionToAPartnerHeDoesNotUse() {
      givenStoredPartner(false, false);
      when(mobilityChoiceRepository.findByUser(USER_ID)).thenReturn(List.of(choiceWithPartner(0)));
      when(mobilityRepository.findByUser(USER_ID))
          .thenReturn(List.of(mobilityWithPartner(0), mobilityWithPartner(PARTNER_ID + 1)));

      assertThatBusinessException(
              () -> partnerService.addOption(PARTNER_ID, option("BCH"), USER_ID, Role.STUDENT))
          .hasErrorCode(ErrorCode.ACCESS_DENIED);
      verifyNoInteractions(partnerOptionRepository, optionRepository);
    }

    @Test
    void anOptionOfAnUnknownPartnerIsNotFound() {
      assertThatBusinessException(
              () -> partnerService.addOption(PARTNER_ID, option("BCH"), USER_ID, Role.PROFESSOR))
          .hasErrorCode(ErrorCode.RESOURCE_NOT_FOUND);
      verifyNoInteractions(partnerOptionRepository);
    }

    @Test
    void anUnknownOptionIsNotAdded() {
      givenStoredPartner(true, false);

      assertThatBusinessException(
              () -> partnerService.addOption(PARTNER_ID, option("XYZ"), USER_ID, Role.PROFESSOR))
          .hasErrorCode(ErrorCode.UNKNOWN_OPTION)
          .hasArguments("XYZ");
      verifyNoInteractions(partnerOptionRepository);
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
