package com.dragomitch.ipl.pae.uccontrollers.implementations;

import static com.dragomitch.ipl.pae.business.exceptions.BusinessExceptionAssert.assertThatBusinessException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.business.dto.DocumentDto;
import com.dragomitch.ipl.pae.business.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.business.dto.MobilityDto;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.business.implementations.EntityFactories;
import com.dragomitch.ipl.pae.persistence.CountryDao;
import com.dragomitch.ipl.pae.persistence.DenialReasonDao;
import com.dragomitch.ipl.pae.persistence.DocumentDao;
import com.dragomitch.ipl.pae.persistence.MobilityChoiceDao;
import com.dragomitch.ipl.pae.persistence.MobilityDao;
import com.dragomitch.ipl.pae.persistence.MobilityDocumentDao;
import com.dragomitch.ipl.pae.persistence.ProgrammeDao;
import com.dragomitch.ipl.pae.persistence.UserDao;
import com.dragomitch.ipl.pae.uccontrollers.MockDtoFactory;
import com.dragomitch.ipl.pae.uccontrollers.PartnerUcc;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Mobility choices: application, cancellation by the student, rejection and confirmation by a
 * professor (which turns the choice into a mobility and rejects the student's other choices for the
 * same term), CSV export. The DAOs are Mockito mocks answering as {@code MobilityChoiceDaoIT}
 * says: a stored choice carries only the id of its denial reason, and a country whose code is null
 * when none was chosen.
 */
@ExtendWith(MockitoExtension.class)
class MobilityChoiceUccImplTest {

  private static final int CHOICE_ID = 11;
  private static final int OTHER_CHOICE_ID = 12;
  private static final int STUDENT_ID = 42;
  private static final int PROFESSOR_ID = 2;
  private static final int PARTNER_ID = 5;
  /** The denial reason used when a confirmation rejects the other choices of the student. */
  private static final int AUTOMATIC_DENIAL_REASON = 1;

  @Mock
  private UserDao userDao;
  @Mock
  private MobilityChoiceDao mobilityChoiceDao;
  @Mock
  private MobilityDao mobilityDao;
  @Mock
  private DenialReasonDao denialReasonDao;
  @Mock
  private DocumentDao documentDao;
  @Mock
  private MobilityDocumentDao mobilityDocumentDao;
  @Mock
  private CountryDao countryDao;
  @Mock
  private ProgrammeDao programmeDao;
  @Mock
  private PartnerUcc partnerUcc;

  private final EntityFactory entityFactory = EntityFactories.create();
  private final MockDtoFactory dtos = new MockDtoFactory(entityFactory);
  private MobilityChoiceUccImpl mobilityChoiceUcc;

  @BeforeEach
  void setUp() {
    mobilityChoiceUcc = new MobilityChoiceUccImpl(userDao, mobilityChoiceDao, mobilityDao,
        denialReasonDao, documentDao, mobilityDocumentDao, entityFactory, countryDao,
        programmeDao, partnerUcc);
  }

  private UserDto student() {
    UserDto student = dtos.getUser(UserDto.ROLE_STUDENT);
    student.setId(STUDENT_ID);
    return student;
  }

  private UserDto givenUser(int id, String role) {
    UserDto user = dtos.getUser(role);
    user.setId(id);
    when(userDao.findById(id)).thenReturn(user);
    return user;
  }

  /** A choice as {@link MobilityChoiceDao#findById} returns it. */
  private MobilityChoiceDto storedChoice(int id, int academicYear, int term) {
    MobilityChoiceDto choice = (MobilityChoiceDto) entityFactory.build(MobilityChoiceDto.class);
    choice.setId(id);
    choice.setVersion(1);
    choice.setUser(student());
    choice.setAcademicYear(academicYear);
    choice.setTerm(term);
    choice.setPreferenceOrder(1);
    choice.setMobilityType("SMS");
    choice.setProgramme(dtos.getProgramme());
    CountryDto country = (CountryDto) entityFactory.build(CountryDto.class);
    country.setCountryCode("IE");
    choice.setCountry(country);
    PartnerDto partner = (PartnerDto) entityFactory.build(PartnerDto.class);
    partner.setId(PARTNER_ID);
    partner.setFullName("University of Dublin");
    choice.setPartner(partner);
    return choice;
  }

  private MobilityChoiceDto givenChoice(int id, int academicYear, int term) {
    MobilityChoiceDto choice = storedChoice(id, academicYear, term);
    when(mobilityChoiceDao.findById(id)).thenReturn(choice);
    return choice;
  }

  /** The denial reason of a rejected choice as the DAO reads it: its id only. */
  private DenialReasonDto storedDenialReason(int id) {
    DenialReasonDto denialReason = (DenialReasonDto) entityFactory.build(DenialReasonDto.class);
    denialReason.setId(id);
    return denialReason;
  }

  private DenialReasonDto givenDenialReason(int id) {
    DenialReasonDto denialReason = storedDenialReason(id);
    denialReason.setReason("Places épuisées");
    when(denialReasonDao.findById(id)).thenReturn(denialReason);
    return denialReason;
  }

  /** How a choice was closed before the operation. */
  enum Closed {
    CANCELLED, REJECTED
  }

  private void close(MobilityChoiceDto choice, Closed closed) {
    if (closed == Closed.CANCELLED) {
      choice.setCancellationReason("changed my mind");
    } else {
      DenialReasonDto denialReason = storedDenialReason(3);
      denialReason.setReason("Dossier incomplet");
      choice.setDenialReason(denialReason);
    }
  }

  @Nested
  class Create {

    private MobilityChoiceDto request() {
      MobilityChoiceDto request = storedChoice(0, 2016, 1);
      request.setPartner(null);
      return request;
    }

    private void givenReferencesExist(MobilityChoiceDto request) {
      when(countryDao.findById("IE")).thenReturn(request.getCountry());
      when(programmeDao.findById(1)).thenReturn(request.getProgramme());
    }

    @Test
    void aStudentAppliesForThemself() {
      MobilityChoiceDto request = request();
      givenReferencesExist(request);
      UserDto stored = givenUser(STUDENT_ID, UserDto.ROLE_STUDENT);
      when(mobilityChoiceDao.create(request)).thenReturn(request);

      MobilityChoiceDto created =
          mobilityChoiceUcc.create(request, STUDENT_ID, UserDto.ROLE_STUDENT);

      assertThat(created.getUser()).isSameAs(stored);
    }

    @Test
    void aProfessorEncodesTheChoiceOfAStudentWithoutCountry() {
      MobilityChoiceDto request = request();
      request.setCountry(null);
      when(programmeDao.findById(1)).thenReturn(request.getProgramme());
      givenUser(STUDENT_ID, UserDto.ROLE_STUDENT);
      when(mobilityChoiceDao.create(request)).thenReturn(request);

      assertThat(mobilityChoiceUcc.create(request, PROFESSOR_ID, UserDto.ROLE_PROFESSOR))
          .isSameAs(request);
      verifyNoInteractions(countryDao);
    }

    @Test
    void aStudentCannotApplyForAnotherStudent() {
      MobilityChoiceDto request = request();
      givenReferencesExist(request);
      givenUser(STUDENT_ID, UserDto.ROLE_STUDENT);

      assertThatBusinessException(() -> mobilityChoiceUcc.create(request, STUDENT_ID + 1,
          UserDto.ROLE_STUDENT)).hasErrorCode(ErrorCode.ACCESS_DENIED);
      verify(mobilityChoiceDao, never()).create(any());
    }

    @Test
    void aProfessorCannotApplyForThemself() {
      MobilityChoiceDto request = request();
      givenReferencesExist(request);
      givenUser(STUDENT_ID, UserDto.ROLE_PROFESSOR);

      assertThatBusinessException(() -> mobilityChoiceUcc.create(request, STUDENT_ID,
          UserDto.ROLE_PROFESSOR)).hasErrorCode(ErrorCode.PROFESSOR_CANNOT_APPLY);
      verify(mobilityChoiceDao, never()).create(any());
    }

    @Test
    void theCountryMustExist() {
      MobilityChoiceDto request = request();

      assertThatBusinessException(() -> mobilityChoiceUcc.create(request, STUDENT_ID,
          UserDto.ROLE_STUDENT)).hasErrorCode(ErrorCode.UNKNOWN_COUNTRY).hasArguments("IE");
      verify(mobilityChoiceDao, never()).create(any());
    }

    @Test
    void theProgrammeMustExist() {
      MobilityChoiceDto request = request();
      when(countryDao.findById("IE")).thenReturn(request.getCountry());

      assertThatBusinessException(() -> mobilityChoiceUcc.create(request, STUDENT_ID,
          UserDto.ROLE_STUDENT)).hasErrorCode(ErrorCode.UNKNOWN_PROGRAMME).hasArguments(1);
      verify(mobilityChoiceDao, never()).create(any());
    }

    @Test
    void theStudentMustExist() {
      MobilityChoiceDto request = request();
      givenReferencesExist(request);

      assertThatBusinessException(() -> mobilityChoiceUcc.create(request, PROFESSOR_ID,
          UserDto.ROLE_PROFESSOR)).hasErrorCode(ErrorCode.UNKNOWN_USER).hasArguments(STUDENT_ID);
      verify(mobilityChoiceDao, never()).create(any());
    }
  }

  @Nested
  class ShowAll {

    @Test
    void aProfessorSeesTheActiveChoicesByDefault() {
      List<MobilityChoiceDto> active = List.of(storedChoice(CHOICE_ID, 2016, 1));
      when(mobilityChoiceDao.findAll(MobilityChoiceDao.FILTER_ACTIVE_MOBILITIES_CHOICES))
          .thenReturn(active);

      assertThat(mobilityChoiceUcc.showAll(PROFESSOR_ID, UserDto.ROLE_PROFESSOR, null))
          .isSameAs(active);
      assertThat(mobilityChoiceUcc.countAll(PROFESSOR_ID, UserDto.ROLE_PROFESSOR, null))
          .isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {MobilityChoiceDao.FILTER_ALL_MOBILITIES_CHOICES,
        MobilityChoiceDao.FILTER_CANCELED_MOBILITIES_CHOICES,
        MobilityChoiceDao.FILTER_REJECTED_MOBILITIES_CHOICES,
        MobilityChoiceDao.FILTER_PASSED_MOBILITIES_CHOICES})
    void aProfessorFiltersTheChoices(String filter) {
      List<MobilityChoiceDto> filtered = List.of();
      when(mobilityChoiceDao.findAll(filter)).thenReturn(filtered);

      assertThat(mobilityChoiceUcc.showAll(PROFESSOR_ID, UserDto.ROLE_PROFESSOR, filter))
          .isSameAs(filtered);
    }

    @Test
    void aStudentSeesAllTheirOwnChoicesWhateverTheFilter() {
      givenUser(STUDENT_ID, UserDto.ROLE_STUDENT);
      List<MobilityChoiceDto> own = List.of(storedChoice(CHOICE_ID, 2016, 1),
          storedChoice(OTHER_CHOICE_ID, 2016, 2));
      when(mobilityChoiceDao.findByUser(STUDENT_ID)).thenReturn(own);

      assertThat(mobilityChoiceUcc.showAll(STUDENT_ID, UserDto.ROLE_STUDENT,
          MobilityChoiceDao.FILTER_ALL_MOBILITIES_CHOICES)).isSameAs(own);
      verify(mobilityChoiceDao, never()).findAll(any());
    }

    @Test
    void anUnknownStudentHasNoChoices() {
      assertThatBusinessException(() -> mobilityChoiceUcc.showAll(STUDENT_ID,
          UserDto.ROLE_STUDENT, null)).hasErrorCode(ErrorCode.UNKNOWN_USER)
          .hasArguments(STUDENT_ID);
    }
  }

  @Nested
  class Cancel {

    @Test
    void theStudentCancelsTheirChoiceWithAReason() {
      MobilityChoiceDto choice = givenChoice(CHOICE_ID, 2016, 1);

      mobilityChoiceUcc.cancel(CHOICE_ID, STUDENT_ID, "changed my mind");

      assertThat(choice.getCancellationReason()).isEqualTo("changed my mind");
      verify(mobilityChoiceDao).update(choice);
    }

    @Test
    void onlyTheOwnerCancelsAChoice() {
      MobilityChoiceDto choice = givenChoice(CHOICE_ID, 2016, 1);

      assertThatBusinessException(() -> mobilityChoiceUcc.cancel(CHOICE_ID, PROFESSOR_ID,
          "reason")).hasErrorCode(ErrorCode.ACCESS_DENIED);
      assertThat(choice.getCancellationReason()).isNull();
      verify(mobilityChoiceDao, never()).update(any());
    }

    @ParameterizedTest
    @EnumSource(Closed.class)
    void aClosedChoiceCannotBeCancelled(Closed closed) {
      close(givenChoice(CHOICE_ID, 2016, 1), closed);

      assertThatBusinessException(() -> mobilityChoiceUcc.cancel(CHOICE_ID, STUDENT_ID,
          "reason")).hasErrorCode(ErrorCode.MOBILITY_CHOICE_CLOSED);
      verify(mobilityChoiceDao, never()).update(any());
    }

    @Test
    void aConfirmedChoiceIsCancelledAsAMobility() {
      givenChoice(CHOICE_ID, 2016, 1);
      when(mobilityDao.findById(CHOICE_ID))
          .thenReturn((MobilityDto) entityFactory.build(MobilityDto.class));

      assertThatBusinessException(() -> mobilityChoiceUcc.cancel(CHOICE_ID, STUDENT_ID,
          "reason")).hasErrorCode(ErrorCode.MOBILITY_CHOICE_ALREADY_CONFIRMED);
      verify(mobilityChoiceDao, never()).update(any());
    }

    @Test
    void anUnknownChoiceIsNotFound() {
      assertThatBusinessException(() -> mobilityChoiceUcc.cancel(CHOICE_ID, STUDENT_ID,
          "reason")).hasErrorCode(ErrorCode.RESOURCE_NOT_FOUND);
    }
  }

  @Nested
  class Reject {

    @Test
    void aProfessorRejectsAChoiceWithADenialReason() {
      MobilityChoiceDto choice = givenChoice(CHOICE_ID, 2016, 1);
      DenialReasonDto denialReason = givenDenialReason(4);

      mobilityChoiceUcc.reject(CHOICE_ID, 4);

      assertThat(choice.getDenialReason()).isSameAs(denialReason);
      verify(mobilityChoiceDao).update(choice);
    }

    @Test
    void aCancelledChoiceCannotBeRejected() {
      close(givenChoice(CHOICE_ID, 2016, 1), Closed.CANCELLED);

      assertThatBusinessException(() -> mobilityChoiceUcc.reject(CHOICE_ID, 4))
          .hasErrorCode(ErrorCode.MOBILITY_CHOICE_CLOSED);
      verify(mobilityChoiceDao, never()).update(any());
    }

    @Test
    void aChoiceRejectedWithAKnownReasonCannotBeRejectedAgain() {
      close(givenChoice(CHOICE_ID, 2016, 1), Closed.REJECTED);

      assertThatBusinessException(() -> mobilityChoiceUcc.reject(CHOICE_ID, 4))
          .hasErrorCode(ErrorCode.MOBILITY_CHOICE_CLOSED);
      verify(mobilityChoiceDao, never()).update(any());
    }

    @Test
    void theDenialReasonMustExist() {
      givenChoice(CHOICE_ID, 2016, 1);

      assertThatBusinessException(() -> mobilityChoiceUcc.reject(CHOICE_ID, 99))
          .hasErrorCode(ErrorCode.UNKNOWN_DENIAL_REASON).hasArguments(99);
      verify(mobilityChoiceDao, never()).update(any());
    }

    @Test
    void aConfirmedChoiceCannotBeRejected() {
      givenChoice(CHOICE_ID, 2016, 1);
      givenDenialReason(4);
      when(mobilityDao.findById(CHOICE_ID))
          .thenReturn((MobilityDto) entityFactory.build(MobilityDto.class));

      assertThatBusinessException(() -> mobilityChoiceUcc.reject(CHOICE_ID, 4))
          .hasErrorCode(ErrorCode.MOBILITY_CHOICE_ALREADY_CONFIRMED);
      verify(mobilityChoiceDao, never()).update(any());
    }

    @Test
    void anUnknownChoiceIsNotFound() {
      assertThatBusinessException(() -> mobilityChoiceUcc.reject(CHOICE_ID, 4))
          .hasErrorCode(ErrorCode.RESOURCE_NOT_FOUND);
    }
  }

  private List<DocumentDto> givenProgrammeDocuments(int... ids) {
    List<DocumentDto> documents = new ArrayList<>();
    for (int id : ids) {
      DocumentDto document = (DocumentDto) entityFactory.build(DocumentDto.class);
      document.setId(id);
      documents.add(document);
    }
    when(documentDao.findAllByProgramme(1)).thenReturn(documents);
    return documents;
  }

  private MobilityDto createdMobility() {
    ArgumentCaptor<MobilityDto> created = ArgumentCaptor.forClass(MobilityDto.class);
    verify(mobilityDao).create(created.capture());
    return created.getValue();
  }

  @Nested
  class Confirm {

    @Test
    void theChoiceBecomesAMobilityWithItsDocumentsAndTheOtherChoicesOfTheTermAreRejected() {
      MobilityChoiceDto choice = givenChoice(CHOICE_ID, 2016, 1);
      UserDto professor = givenUser(PROFESSOR_ID, UserDto.ROLE_PROFESSOR);
      givenProgrammeDocuments(1, 2, 3);
      MobilityChoiceDto sameTerm = givenChoice(OTHER_CHOICE_ID, 2016, 1);
      MobilityChoiceDto otherTerm = storedChoice(13, 2016, 2);
      MobilityChoiceDto otherYear = storedChoice(14, 2017, 1);
      when(mobilityChoiceDao.findByUser(STUDENT_ID))
          .thenReturn(List.of(choice, sameTerm, otherTerm, otherYear));
      DenialReasonDto automatic = givenDenialReason(AUTOMATIC_DENIAL_REASON);

      mobilityChoiceUcc.confirm(CHOICE_ID, PROFESSOR_ID);

      MobilityDto mobility = createdMobility();
      assertThat(mobility.getId()).isEqualTo(CHOICE_ID);
      assertThat(mobility.getState()).isEqualTo(MobilityDto.STATE_CREATED);
      assertThat(mobility.getSubmissionDate())
          .isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES));
      assertThat(mobility.getProfessorInCharge()).isSameAs(professor);
      verify(mobilityDocumentDao).create(1, CHOICE_ID);
      verify(mobilityDocumentDao).create(2, CHOICE_ID);
      verify(mobilityDocumentDao).create(3, CHOICE_ID);
      assertThat(sameTerm.getDenialReason()).isSameAs(automatic);
      verify(mobilityChoiceDao).update(sameTerm);
      assertThat(choice.getDenialReason()).isNull();
      assertThat(otherTerm.getDenialReason()).isNull();
      assertThat(otherYear.getDenialReason()).isNull();
      verify(mobilityChoiceDao, never()).update(choice);
    }

    @Test
    void aChoiceWithoutPartnerCannotBeConfirmed() {
      givenChoice(CHOICE_ID, 2016, 1).setPartner(null);

      assertThatBusinessException(() -> mobilityChoiceUcc.confirm(CHOICE_ID, PROFESSOR_ID))
          .hasErrorCode(ErrorCode.PARTNER_REQUIRED_TO_CONFIRM);
      verify(mobilityDao, never()).create(any());
    }

    @ParameterizedTest
    @EnumSource(Closed.class)
    void aClosedChoiceCannotBeConfirmed(Closed closed) {
      close(givenChoice(CHOICE_ID, 2016, 1), closed);

      assertThatBusinessException(() -> mobilityChoiceUcc.confirm(CHOICE_ID, PROFESSOR_ID))
          .hasErrorCode(ErrorCode.MOBILITY_CHOICE_CLOSED);
      verify(mobilityDao, never()).create(any());
    }

    @Test
    void aChoiceIsConfirmedOnlyOnce() {
      givenChoice(CHOICE_ID, 2016, 1);
      when(mobilityDao.findById(CHOICE_ID))
          .thenReturn((MobilityDto) entityFactory.build(MobilityDto.class));

      assertThatBusinessException(() -> mobilityChoiceUcc.confirm(CHOICE_ID, PROFESSOR_ID))
          .hasErrorCode(ErrorCode.MOBILITY_CHOICE_ALREADY_CONFIRMED);
      verify(mobilityDao, never()).create(any());
    }

    @Test
    void anUnknownChoiceIsNotFound() {
      assertThatBusinessException(() -> mobilityChoiceUcc.confirm(CHOICE_ID, PROFESSOR_ID))
          .hasErrorCode(ErrorCode.RESOURCE_NOT_FOUND);
    }
  }

  @Nested
  class ConfirmWithNewPartner {

    /** The new partner proposed for the choice, in the country of the choice. */
    private PartnerDto partner(int id, boolean official) {
      PartnerDto partner = dtos.getPartner();
      partner.setId(id);
      partner.setOfficial(official);
      partner.getAddress().getCountry().setCountryCode("IE");
      return partner;
    }

    @Test
    void aStudentConfirmsTheirChoiceWithANewUnofficialPartner() {
      MobilityChoiceDto choice = givenChoice(CHOICE_ID, 2016, 1);
      choice.setPartner(null);
      PartnerDto request = partner(0, false);
      PartnerDto created = partner(PARTNER_ID, false);
      when(partnerUcc.create(request, UserDto.ROLE_STUDENT)).thenReturn(created);
      givenProgrammeDocuments(1, 2);
      givenUser(STUDENT_ID, UserDto.ROLE_STUDENT);
      when(mobilityChoiceDao.findByUser(STUDENT_ID)).thenReturn(List.of(choice));

      mobilityChoiceUcc.confirmWithNewPartner(CHOICE_ID, request, STUDENT_ID,
          UserDto.ROLE_STUDENT);

      assertThat(choice.getPartner()).isSameAs(created);
      assertThat(choice.getCountry().getCountryCode()).isEqualTo("IE");
      verify(mobilityChoiceDao).update(choice);
      MobilityDto mobility = createdMobility();
      assertThat(mobility.getId()).isEqualTo(CHOICE_ID);
      assertThat(mobility.getState()).isEqualTo(MobilityDto.STATE_CREATED);
      verify(mobilityDocumentDao).create(1, CHOICE_ID);
      verify(mobilityDocumentDao).create(2, CHOICE_ID);
    }

    @Test
    void anArchivedPartnerIsRestoredAndTheOpenChoicesOfTheTermAreRejected() {
      MobilityChoiceDto choice = givenChoice(CHOICE_ID, 2016, 1);
      PartnerDto request = partner(PARTNER_ID, true);
      PartnerDto restored = partner(PARTNER_ID, true);
      when(partnerUcc.restore(PARTNER_ID, UserDto.ROLE_PROFESSOR)).thenReturn(restored);
      givenProgrammeDocuments();
      givenUser(PROFESSOR_ID, UserDto.ROLE_PROFESSOR);
      MobilityChoiceDto open = givenChoice(OTHER_CHOICE_ID, 2016, 1);
      MobilityChoiceDto cancelled = storedChoice(13, 2016, 1);
      close(cancelled, Closed.CANCELLED);
      MobilityChoiceDto otherTerm = storedChoice(14, 2016, 2);
      when(mobilityChoiceDao.findByUser(STUDENT_ID))
          .thenReturn(List.of(choice, open, cancelled, otherTerm));
      DenialReasonDto automatic = givenDenialReason(AUTOMATIC_DENIAL_REASON);

      mobilityChoiceUcc.confirmWithNewPartner(CHOICE_ID, request, PROFESSOR_ID,
          UserDto.ROLE_PROFESSOR);

      assertThat(choice.getPartner()).isSameAs(restored);
      assertThat(open.getDenialReason()).isSameAs(automatic);
      assertThat(cancelled.getDenialReason()).isNull();
      assertThat(otherTerm.getDenialReason()).isNull();
      verify(mobilityChoiceDao).update(open);
      verify(mobilityChoiceDao, never()).update(cancelled);
      verify(partnerUcc, never()).create(any(), any());
      verifyNoInteractions(mobilityDocumentDao);
    }

    @Test
    void aStudentCannotConfirmTheChoiceOfAnotherStudent() {
      givenChoice(CHOICE_ID, 2016, 1);

      assertThatBusinessException(() -> mobilityChoiceUcc.confirmWithNewPartner(CHOICE_ID,
          partner(0, false), STUDENT_ID + 1, UserDto.ROLE_STUDENT))
          .hasErrorCode(ErrorCode.ACCESS_DENIED);
      verifyNoInteractions(partnerUcc, mobilityDao);
    }

    @Test
    void aStudentCannotProposeAnOfficialPartner() {
      givenChoice(CHOICE_ID, 2016, 1);

      assertThatBusinessException(() -> mobilityChoiceUcc.confirmWithNewPartner(CHOICE_ID,
          partner(0, true), STUDENT_ID, UserDto.ROLE_STUDENT))
          .hasErrorCode(ErrorCode.ACCESS_DENIED);
      verifyNoInteractions(partnerUcc, mobilityDao);
    }

    @ParameterizedTest
    @EnumSource(Closed.class)
    void aClosedChoiceCannotBeConfirmed(Closed closed) {
      close(givenChoice(CHOICE_ID, 2016, 1), closed);

      assertThatBusinessException(() -> mobilityChoiceUcc.confirmWithNewPartner(CHOICE_ID,
          partner(0, false), PROFESSOR_ID, UserDto.ROLE_PROFESSOR))
          .hasErrorCode(ErrorCode.MOBILITY_CHOICE_CLOSED);
      verifyNoInteractions(partnerUcc);
    }

    @Test
    void aChoiceIsConfirmedOnlyOnce() {
      givenChoice(CHOICE_ID, 2016, 1);
      when(mobilityDao.findById(CHOICE_ID))
          .thenReturn((MobilityDto) entityFactory.build(MobilityDto.class));

      assertThatBusinessException(() -> mobilityChoiceUcc.confirmWithNewPartner(CHOICE_ID,
          partner(0, false), PROFESSOR_ID, UserDto.ROLE_PROFESSOR))
          .hasErrorCode(ErrorCode.MOBILITY_CHOICE_ALREADY_CONFIRMED);
      verifyNoInteractions(partnerUcc);
      verify(mobilityDao, never()).create(any());
    }

    @Test
    void anUnknownChoiceIsNotFound() {
      assertThatBusinessException(() -> mobilityChoiceUcc.confirmWithNewPartner(CHOICE_ID,
          partner(0, false), PROFESSOR_ID, UserDto.ROLE_PROFESSOR))
          .hasErrorCode(ErrorCode.RESOURCE_NOT_FOUND);
    }
  }

  @Nested
  class ExportAll {

    @Test
    void everyChoiceIsALineOfTheCsv() {
      MobilityChoiceDto first = storedChoice(CHOICE_ID, 2016, 1);
      first.getUser().setLastName("Martin");
      first.getUser().setFirstName("Alice");
      MobilityChoiceDto second = storedChoice(OTHER_CHOICE_ID, 2016, 2);
      second.setPreferenceOrder(2);
      second.setMobilityType("SMP");
      when(mobilityChoiceDao.findAll(MobilityChoiceDao.FILTER_ACTIVE_MOBILITIES_CHOICES))
          .thenReturn(List.of(first, second));

      String csv = mobilityChoiceUcc.exportAll(PROFESSOR_ID, UserDto.ROLE_PROFESSOR, null);

      assertThat(csv).isEqualTo("﻿"
          + "N° ordre candidature;Nom;Prénom;Option;N° ordre préférence;Programme de mobilité;"
          + "Type de mobilité;Semestre de départ;Partenaire;\n"
          + "11;Martin;Alice;Bachelier en informatique de gestion;1;Erasmus+;SMS;1;"
          + "University of Dublin;\n"
          + "12;TheReaper;Jack;Bachelier en informatique de gestion;2;Erasmus+;SMP;2;"
          + "University of Dublin;\n");
    }

    @Test
    void withoutChoicesOnlyTheHeaderIsExported() {
      givenUser(STUDENT_ID, UserDto.ROLE_STUDENT);
      when(mobilityChoiceDao.findByUser(STUDENT_ID)).thenReturn(List.of());

      assertThat(mobilityChoiceUcc.exportAll(STUDENT_ID, UserDto.ROLE_STUDENT, null))
          .hasLineCount(1).startsWith("﻿N° ordre candidature;");
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 2})
  void aPartnerIsFreeWhenNoChoiceUsesIt(int choices) {
    when(mobilityChoiceDao.findByPartner(PARTNER_ID)).thenReturn(
        Collections.nCopies(choices, storedChoice(CHOICE_ID, 2016, 1)));

    assertThat(mobilityChoiceUcc.findByPartner(PARTNER_ID)).isEqualTo(choices == 0);
  }
}
