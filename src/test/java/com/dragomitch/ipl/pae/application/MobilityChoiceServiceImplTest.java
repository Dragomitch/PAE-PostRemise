package com.dragomitch.ipl.pae.application;

import static com.dragomitch.ipl.pae.domain.exceptions.BusinessExceptionAssert.assertThatBusinessException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dragomitch.ipl.pae.domain.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.domain.model.Country;
import com.dragomitch.ipl.pae.domain.model.DenialReason;
import com.dragomitch.ipl.pae.domain.model.Document;
import com.dragomitch.ipl.pae.domain.model.DocumentCategory;
import com.dragomitch.ipl.pae.domain.model.Mobility;
import com.dragomitch.ipl.pae.domain.model.MobilityChoice;
import com.dragomitch.ipl.pae.domain.model.MobilityChoiceFilter;
import com.dragomitch.ipl.pae.domain.model.MobilityState;
import com.dragomitch.ipl.pae.domain.model.MobilityType;
import com.dragomitch.ipl.pae.domain.model.Partner;
import com.dragomitch.ipl.pae.domain.model.Role;
import com.dragomitch.ipl.pae.domain.model.User;
import com.dragomitch.ipl.pae.domain.port.in.PartnerService;
import com.dragomitch.ipl.pae.domain.port.out.CountryRepository;
import com.dragomitch.ipl.pae.domain.port.out.DenialReasonRepository;
import com.dragomitch.ipl.pae.domain.port.out.DocumentRepository;
import com.dragomitch.ipl.pae.domain.port.out.MobilityChoiceRepository;
import com.dragomitch.ipl.pae.domain.port.out.MobilityDocumentRepository;
import com.dragomitch.ipl.pae.domain.port.out.MobilityRepository;
import com.dragomitch.ipl.pae.domain.port.out.ProgrammeRepository;
import com.dragomitch.ipl.pae.domain.port.out.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
 * same term), CSV export. The DAOs are Mockito mocks answering as {@code
 * MobilityChoiceRepositoryIT} says: a stored choice carries only the id of its denial reason, and a
 * country whose code is null when none was chosen.
 */
@ExtendWith(MockitoExtension.class)
class MobilityChoiceServiceImplTest {

  /** The instant the use case reads from its clock: the dates it sets are asserted exactly. */
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 15, 10, 15, 30);

  private static final Clock CLOCK = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

  private static final int CHOICE_ID = 11;
  private static final int OTHER_CHOICE_ID = 12;
  private static final int STUDENT_ID = 42;
  private static final int PROFESSOR_ID = 2;
  private static final int PARTNER_ID = 5;

  /** The denial reason used when a confirmation rejects the other choices of the student. */
  private static final int AUTOMATIC_DENIAL_REASON = 1;

  @Mock private UserRepository userRepository;
  @Mock private MobilityChoiceRepository mobilityChoiceRepository;
  @Mock private MobilityRepository mobilityRepository;
  @Mock private DenialReasonRepository denialReasonRepository;
  @Mock private DocumentRepository documentRepository;
  @Mock private MobilityDocumentRepository mobilityDocumentRepository;
  @Mock private CountryRepository countryRepository;
  @Mock private ProgrammeRepository programmeRepository;
  @Mock private PartnerService partnerService;

  private final MockDtoFactory dtos = new MockDtoFactory();
  private MobilityChoiceServiceImpl mobilityChoiceService;

  @BeforeEach
  void setUp() {
    mobilityChoiceService =
        new MobilityChoiceServiceImpl(
            userRepository,
            mobilityChoiceRepository,
            mobilityRepository,
            denialReasonRepository,
            documentRepository,
            mobilityDocumentRepository,
            countryRepository,
            programmeRepository,
            partnerService,
            CLOCK);
  }

  private User student() {
    User student = dtos.getUser(Role.STUDENT);
    student.setId(STUDENT_ID);
    return student;
  }

  private User givenUser(int id, Role role) {
    User user = dtos.getUser(role);
    user.setId(id);
    when(userRepository.findById(id)).thenReturn(Optional.of(user));
    return user;
  }

  /** A choice as {@link MobilityChoiceRepository#findById} returns it. */
  private MobilityChoice storedChoice(int id, int academicYear, int term) {
    MobilityChoice choice = new MobilityChoice();
    choice.setId(id);
    choice.setVersion(1);
    choice.setUser(student());
    choice.setAcademicYear(academicYear);
    choice.setTerm(term);
    choice.setPreferenceOrder(1);
    choice.setMobilityType(MobilityType.SMS);
    choice.setProgramme(dtos.getProgramme());
    Country country = new Country();
    country.setCountryCode("IE");
    choice.setCountry(country);
    Partner partner = new Partner();
    partner.setId(PARTNER_ID);
    partner.setFullName("University of Dublin");
    choice.setPartner(partner);
    return choice;
  }

  private MobilityChoice givenChoice(int id, int academicYear, int term) {
    MobilityChoice choice = storedChoice(id, academicYear, term);
    when(mobilityChoiceRepository.findById(id)).thenReturn(Optional.of(choice));
    return choice;
  }

  /** The denial reason of a rejected choice as the DAO reads it: its id only. */
  private DenialReason storedDenialReason(int id) {
    DenialReason denialReason = new DenialReason();
    denialReason.setId(id);
    return denialReason;
  }

  private DenialReason givenDenialReason(int id) {
    DenialReason denialReason = storedDenialReason(id);
    denialReason.setReason("Places épuisées");
    when(denialReasonRepository.findById(id)).thenReturn(Optional.of(denialReason));
    return denialReason;
  }

  /** How a choice was closed before the operation. */
  enum Closed {
    CANCELLED,
    REJECTED
  }

  private void close(MobilityChoice choice, Closed closed) {
    if (closed == Closed.CANCELLED) {
      choice.setCancellationReason("changed my mind");
    } else {
      DenialReason denialReason = storedDenialReason(3);
      denialReason.setReason("Dossier incomplet");
      choice.setDenialReason(denialReason);
    }
  }

  @Nested
  class Create {

    private MobilityChoice request() {
      MobilityChoice request = storedChoice(0, 2016, 1);
      request.setPartner(null);
      return request;
    }

    private void givenReferencesExist(MobilityChoice request) {
      when(countryRepository.findById("IE")).thenReturn(Optional.of(request.getCountry()));
      when(programmeRepository.findById(1)).thenReturn(Optional.of(request.getProgramme()));
    }

    @Test
    void aStudentAppliesForThemself() {
      MobilityChoice request = request();
      givenReferencesExist(request);
      User stored = givenUser(STUDENT_ID, Role.STUDENT);
      when(mobilityChoiceRepository.create(request)).thenReturn(request);

      MobilityChoice created = mobilityChoiceService.create(request, STUDENT_ID, Role.STUDENT);

      assertThat(created.getUser()).isSameAs(stored);
    }

    @Test
    void aProfessorEncodesTheChoiceOfAStudentWithoutCountry() {
      MobilityChoice request = request();
      request.setCountry(null);
      when(programmeRepository.findById(1)).thenReturn(Optional.of(request.getProgramme()));
      givenUser(STUDENT_ID, Role.STUDENT);
      when(mobilityChoiceRepository.create(request)).thenReturn(request);

      assertThat(mobilityChoiceService.create(request, PROFESSOR_ID, Role.PROFESSOR))
          .isSameAs(request);
      verifyNoInteractions(countryRepository);
    }

    @Test
    void aStudentCannotApplyForAnotherStudent() {
      MobilityChoice request = request();
      givenReferencesExist(request);
      givenUser(STUDENT_ID, Role.STUDENT);

      assertThatBusinessException(
              () -> mobilityChoiceService.create(request, STUDENT_ID + 1, Role.STUDENT))
          .hasErrorCode(ErrorCode.ACCESS_DENIED);
      verify(mobilityChoiceRepository, never()).create(any());
    }

    @Test
    void aProfessorCannotApplyForThemself() {
      MobilityChoice request = request();
      givenReferencesExist(request);
      givenUser(STUDENT_ID, Role.PROFESSOR);

      assertThatBusinessException(
              () -> mobilityChoiceService.create(request, STUDENT_ID, Role.PROFESSOR))
          .hasErrorCode(ErrorCode.PROFESSOR_CANNOT_APPLY);
      verify(mobilityChoiceRepository, never()).create(any());
    }

    @Test
    void theCountryMustExist() {
      MobilityChoice request = request();

      assertThatBusinessException(
              () -> mobilityChoiceService.create(request, STUDENT_ID, Role.STUDENT))
          .hasErrorCode(ErrorCode.UNKNOWN_COUNTRY)
          .hasArguments("IE");
      verify(mobilityChoiceRepository, never()).create(any());
    }

    @Test
    void theProgrammeMustExist() {
      MobilityChoice request = request();
      when(countryRepository.findById("IE")).thenReturn(Optional.of(request.getCountry()));

      assertThatBusinessException(
              () -> mobilityChoiceService.create(request, STUDENT_ID, Role.STUDENT))
          .hasErrorCode(ErrorCode.UNKNOWN_PROGRAMME)
          .hasArguments(1);
      verify(mobilityChoiceRepository, never()).create(any());
    }

    @Test
    void theStudentMustExist() {
      MobilityChoice request = request();
      givenReferencesExist(request);

      assertThatBusinessException(
              () -> mobilityChoiceService.create(request, PROFESSOR_ID, Role.PROFESSOR))
          .hasErrorCode(ErrorCode.UNKNOWN_USER)
          .hasArguments(STUDENT_ID);
      verify(mobilityChoiceRepository, never()).create(any());
    }
  }

  @Nested
  class ShowAll {

    @Test
    void aProfessorSeesTheActiveChoicesByDefault() {
      List<MobilityChoice> active = List.of(storedChoice(CHOICE_ID, 2016, 1));
      when(mobilityChoiceRepository.findAll(MobilityChoiceFilter.ACTIVE)).thenReturn(active);

      assertThat(
              mobilityChoiceService.showAll(
                  PROFESSOR_ID, Role.PROFESSOR, MobilityChoiceFilter.ACTIVE))
          .isSameAs(active);
      assertThat(
              mobilityChoiceService.countAll(
                  PROFESSOR_ID, Role.PROFESSOR, MobilityChoiceFilter.ACTIVE))
          .isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(
        value = MobilityChoiceFilter.class,
        names = {"ALL", "CANCELED", "REJECTED", "PASSED"})
    void aProfessorFiltersTheChoices(MobilityChoiceFilter filter) {
      List<MobilityChoice> filtered = List.of();
      when(mobilityChoiceRepository.findAll(filter)).thenReturn(filtered);

      assertThat(mobilityChoiceService.showAll(PROFESSOR_ID, Role.PROFESSOR, filter))
          .isSameAs(filtered);
    }

    @Test
    void aStudentSeesAllTheirOwnChoicesWhateverTheFilter() {
      givenUser(STUDENT_ID, Role.STUDENT);
      List<MobilityChoice> own =
          List.of(storedChoice(CHOICE_ID, 2016, 1), storedChoice(OTHER_CHOICE_ID, 2016, 2));
      when(mobilityChoiceRepository.findByUser(STUDENT_ID)).thenReturn(own);

      assertThat(mobilityChoiceService.showAll(STUDENT_ID, Role.STUDENT, MobilityChoiceFilter.ALL))
          .isSameAs(own);
      verify(mobilityChoiceRepository, never()).findAll(any());
    }

    @Test
    void anUnknownStudentHasNoChoices() {
      assertThatBusinessException(() ->
              mobilityChoiceService.showAll(STUDENT_ID, Role.STUDENT, MobilityChoiceFilter.ACTIVE))
          .hasErrorCode(ErrorCode.UNKNOWN_USER)
          .hasArguments(STUDENT_ID);
    }
  }

  @Nested
  class Cancel {

    @Test
    void theStudentCancelsTheirChoiceWithAReason() {
      MobilityChoice choice = givenChoice(CHOICE_ID, 2016, 1);

      mobilityChoiceService.cancel(CHOICE_ID, STUDENT_ID, "changed my mind");

      assertThat(choice.getCancellationReason()).isEqualTo("changed my mind");
      verify(mobilityChoiceRepository).update(choice);
    }

    @Test
    void onlyTheOwnerCancelsAChoice() {
      MobilityChoice choice = givenChoice(CHOICE_ID, 2016, 1);

      assertThatBusinessException(
              () -> mobilityChoiceService.cancel(CHOICE_ID, PROFESSOR_ID, "reason"))
          .hasErrorCode(ErrorCode.ACCESS_DENIED);
      assertThat(choice.getCancellationReason()).isNull();
      verify(mobilityChoiceRepository, never()).update(any());
    }

    @ParameterizedTest
    @EnumSource(Closed.class)
    void aClosedChoiceCannotBeCancelled(Closed closed) {
      close(givenChoice(CHOICE_ID, 2016, 1), closed);

      assertThatBusinessException(
              () -> mobilityChoiceService.cancel(CHOICE_ID, STUDENT_ID, "reason"))
          .hasErrorCode(ErrorCode.MOBILITY_CHOICE_CLOSED);
      verify(mobilityChoiceRepository, never()).update(any());
    }

    @Test
    void aConfirmedChoiceIsCancelledAsAMobility() {
      givenChoice(CHOICE_ID, 2016, 1);
      when(mobilityRepository.findById(CHOICE_ID)).thenReturn(Optional.of(new Mobility()));

      assertThatBusinessException(
              () -> mobilityChoiceService.cancel(CHOICE_ID, STUDENT_ID, "reason"))
          .hasErrorCode(ErrorCode.MOBILITY_CHOICE_ALREADY_CONFIRMED);
      verify(mobilityChoiceRepository, never()).update(any());
    }

    @Test
    void anUnknownChoiceIsNotFound() {
      assertThatBusinessException(
              () -> mobilityChoiceService.cancel(CHOICE_ID, STUDENT_ID, "reason"))
          .hasErrorCode(ErrorCode.RESOURCE_NOT_FOUND);
    }
  }

  @Nested
  class Reject {

    @Test
    void aProfessorRejectsAChoiceWithADenialReason() {
      MobilityChoice choice = givenChoice(CHOICE_ID, 2016, 1);
      DenialReason denialReason = givenDenialReason(4);

      mobilityChoiceService.reject(CHOICE_ID, 4);

      assertThat(choice.getDenialReason()).isSameAs(denialReason);
      verify(mobilityChoiceRepository).update(choice);
    }

    @Test
    void aCancelledChoiceCannotBeRejected() {
      close(givenChoice(CHOICE_ID, 2016, 1), Closed.CANCELLED);

      assertThatBusinessException(() -> mobilityChoiceService.reject(CHOICE_ID, 4))
          .hasErrorCode(ErrorCode.MOBILITY_CHOICE_CLOSED);
      verify(mobilityChoiceRepository, never()).update(any());
    }

    @Test
    void aChoiceRejectedWithAKnownReasonCannotBeRejectedAgain() {
      close(givenChoice(CHOICE_ID, 2016, 1), Closed.REJECTED);

      assertThatBusinessException(() -> mobilityChoiceService.reject(CHOICE_ID, 4))
          .hasErrorCode(ErrorCode.MOBILITY_CHOICE_CLOSED);
      verify(mobilityChoiceRepository, never()).update(any());
    }

    @Test
    void aRejectedChoiceAsTheRepositoryReadsItCannotBeRejectedAgain() {
      MobilityChoice choice = givenChoice(CHOICE_ID, 2016, 1);
      DenialReason firstReason = storedDenialReason(3);
      choice.setDenialReason(firstReason);
      lenient()
          .when(denialReasonRepository.findById(4))
          .thenReturn(Optional.of(storedDenialReason(4)));

      assertThatBusinessException(() -> mobilityChoiceService.reject(CHOICE_ID, 4))
          .hasErrorCode(ErrorCode.MOBILITY_CHOICE_CLOSED);
      assertThat(choice.getDenialReason()).isSameAs(firstReason);
      verify(mobilityChoiceRepository, never()).update(any());
    }

    @Test
    void theDenialReasonMustExist() {
      givenChoice(CHOICE_ID, 2016, 1);

      assertThatBusinessException(() -> mobilityChoiceService.reject(CHOICE_ID, 99))
          .hasErrorCode(ErrorCode.UNKNOWN_DENIAL_REASON)
          .hasArguments(99);
      verify(mobilityChoiceRepository, never()).update(any());
    }

    @Test
    void aConfirmedChoiceCannotBeRejected() {
      givenChoice(CHOICE_ID, 2016, 1);
      givenDenialReason(4);
      when(mobilityRepository.findById(CHOICE_ID)).thenReturn(Optional.of(new Mobility()));

      assertThatBusinessException(() -> mobilityChoiceService.reject(CHOICE_ID, 4))
          .hasErrorCode(ErrorCode.MOBILITY_CHOICE_ALREADY_CONFIRMED);
      verify(mobilityChoiceRepository, never()).update(any());
    }

    @Test
    void anUnknownChoiceIsNotFound() {
      assertThatBusinessException(() -> mobilityChoiceService.reject(CHOICE_ID, 4))
          .hasErrorCode(ErrorCode.RESOURCE_NOT_FOUND);
    }
  }

  /** Departure documents of the programme of the choices. */
  private List<Document> givenProgrammeDocuments(int... ids) {
    return givenProgrammeDocuments(DocumentCategory.DEPARTURE, ids);
  }

  private List<Document> givenProgrammeDocuments(DocumentCategory category, int... ids) {
    List<Document> documents = new ArrayList<>();
    for (int id : ids) {
      Document document = new Document();
      document.setId(id);
      document.setCategory(category);
      documents.add(document);
    }
    when(documentRepository.findAllByProgramme(1)).thenReturn(documents);
    return documents;
  }

  private Mobility createdMobility() {
    ArgumentCaptor<Mobility> created = ArgumentCaptor.forClass(Mobility.class);
    verify(mobilityRepository).create(created.capture());
    return created.getValue();
  }

  @Nested
  class Confirm {

    @Test
    void theChoiceBecomesAMobilityWithItsDocumentsAndTheOtherChoicesOfTheTermAreRejected() {
      MobilityChoice choice = givenChoice(CHOICE_ID, 2016, 1);
      User professor = givenUser(PROFESSOR_ID, Role.PROFESSOR);
      givenProgrammeDocuments(1, 2, 3);
      MobilityChoice sameTerm = givenChoice(OTHER_CHOICE_ID, 2016, 1);
      MobilityChoice otherTerm = storedChoice(13, 2016, 2);
      MobilityChoice otherYear = storedChoice(14, 2017, 1);
      when(mobilityChoiceRepository.findByUser(STUDENT_ID))
          .thenReturn(List.of(choice, sameTerm, otherTerm, otherYear));
      DenialReason automatic = givenDenialReason(AUTOMATIC_DENIAL_REASON);

      mobilityChoiceService.confirm(CHOICE_ID, PROFESSOR_ID);

      Mobility mobility = createdMobility();
      assertThat(mobility.getId()).isEqualTo(CHOICE_ID);
      assertThat(mobility.getState()).isEqualTo(MobilityState.CREATED);
      assertThat(mobility.getSubmissionDate()).isEqualTo(NOW);
      assertThat(mobility.getProfessorInCharge()).isSameAs(professor);
      verify(mobilityDocumentRepository).create(1, CHOICE_ID);
      verify(mobilityDocumentRepository).create(2, CHOICE_ID);
      verify(mobilityDocumentRepository).create(3, CHOICE_ID);
      assertThat(sameTerm.getDenialReason()).isSameAs(automatic);
      verify(mobilityChoiceRepository).update(sameTerm);
      assertThat(choice.getDenialReason()).isNull();
      assertThat(otherTerm.getDenialReason()).isNull();
      assertThat(otherYear.getDenialReason()).isNull();
      verify(mobilityChoiceRepository, never()).update(choice);
    }

    @Test
    void theClosedChoicesOfTheTermAreLeftAsTheyAre() {
      MobilityChoice choice = givenChoice(CHOICE_ID, 2016, 1);
      givenUser(PROFESSOR_ID, Role.PROFESSOR);
      givenProgrammeDocuments(1);
      MobilityChoice cancelled = storedChoice(OTHER_CHOICE_ID, 2016, 1);
      close(cancelled, Closed.CANCELLED);
      lenient()
          .when(mobilityChoiceRepository.findById(OTHER_CHOICE_ID))
          .thenReturn(Optional.of(cancelled));
      MobilityChoice rejected = storedChoice(13, 2016, 1);
      DenialReason firstReason = storedDenialReason(3);
      rejected.setDenialReason(firstReason);
      lenient().when(mobilityChoiceRepository.findById(13)).thenReturn(Optional.of(rejected));
      lenient()
          .when(denialReasonRepository.findById(AUTOMATIC_DENIAL_REASON))
          .thenReturn(Optional.of(storedDenialReason(AUTOMATIC_DENIAL_REASON)));
      when(mobilityChoiceRepository.findByUser(STUDENT_ID))
          .thenReturn(List.of(choice, cancelled, rejected));

      mobilityChoiceService.confirm(CHOICE_ID, PROFESSOR_ID);

      assertThat(createdMobility().getId()).isEqualTo(CHOICE_ID);
      assertThat(cancelled.getDenialReason()).isNull();
      assertThat(rejected.getDenialReason()).isSameAs(firstReason);
      verify(mobilityChoiceRepository, never()).update(any());
    }

    @Test
    void aChoiceWithoutPartnerCannotBeConfirmed() {
      givenChoice(CHOICE_ID, 2016, 1).setPartner(null);

      assertThatBusinessException(() -> mobilityChoiceService.confirm(CHOICE_ID, PROFESSOR_ID))
          .hasErrorCode(ErrorCode.PARTNER_REQUIRED_TO_CONFIRM);
      verify(mobilityRepository, never()).create(any());
    }

    @ParameterizedTest
    @EnumSource(Closed.class)
    void aClosedChoiceCannotBeConfirmed(Closed closed) {
      close(givenChoice(CHOICE_ID, 2016, 1), closed);

      assertThatBusinessException(() -> mobilityChoiceService.confirm(CHOICE_ID, PROFESSOR_ID))
          .hasErrorCode(ErrorCode.MOBILITY_CHOICE_CLOSED);
      verify(mobilityRepository, never()).create(any());
    }

    @Test
    void aChoiceIsConfirmedOnlyOnce() {
      givenChoice(CHOICE_ID, 2016, 1);
      when(mobilityRepository.findById(CHOICE_ID)).thenReturn(Optional.of(new Mobility()));

      assertThatBusinessException(() -> mobilityChoiceService.confirm(CHOICE_ID, PROFESSOR_ID))
          .hasErrorCode(ErrorCode.MOBILITY_CHOICE_ALREADY_CONFIRMED);
      verify(mobilityRepository, never()).create(any());
    }

    @Test
    void anUnknownChoiceIsNotFound() {
      assertThatBusinessException(() -> mobilityChoiceService.confirm(CHOICE_ID, PROFESSOR_ID))
          .hasErrorCode(ErrorCode.RESOURCE_NOT_FOUND);
    }
  }

  @Nested
  class ConfirmWithNewPartner {

    /** The new partner proposed for the choice, in the country of the choice. */
    private Partner partner(int id, boolean official) {
      Partner partner = dtos.getPartner();
      partner.setId(id);
      partner.setOfficial(official);
      partner.getAddress().getCountry().setCountryCode("IE");
      return partner;
    }

    @Test
    void aStudentConfirmsTheirChoiceWithANewUnofficialPartner() {
      MobilityChoice choice = givenChoice(CHOICE_ID, 2016, 1);
      choice.setPartner(null);
      Partner request = partner(0, false);
      Partner created = partner(PARTNER_ID, false);
      when(partnerService.create(request, Role.STUDENT)).thenReturn(created);
      givenProgrammeDocuments(1, 2);
      givenUser(STUDENT_ID, Role.STUDENT);
      when(mobilityChoiceRepository.findByUser(STUDENT_ID)).thenReturn(List.of(choice));

      mobilityChoiceService.confirmWithNewPartner(CHOICE_ID, request, STUDENT_ID, Role.STUDENT);

      assertThat(choice.getPartner()).isSameAs(created);
      assertThat(choice.getCountry().getCountryCode()).isEqualTo("IE");
      verify(mobilityChoiceRepository).update(choice);
      Mobility mobility = createdMobility();
      assertThat(mobility.getId()).isEqualTo(CHOICE_ID);
      assertThat(mobility.getState()).isEqualTo(MobilityState.CREATED);
      verify(mobilityDocumentRepository).create(1, CHOICE_ID);
      verify(mobilityDocumentRepository).create(2, CHOICE_ID);
    }

    @Test
    void anArchivedPartnerIsRestoredAndTheOpenChoicesOfTheTermAreRejected() {
      MobilityChoice choice = givenChoice(CHOICE_ID, 2016, 1);
      Partner request = partner(PARTNER_ID, true);
      Partner restored = partner(PARTNER_ID, true);
      when(partnerService.restore(PARTNER_ID, Role.PROFESSOR)).thenReturn(restored);
      givenProgrammeDocuments();
      givenUser(PROFESSOR_ID, Role.PROFESSOR);
      MobilityChoice open = givenChoice(OTHER_CHOICE_ID, 2016, 1);
      MobilityChoice cancelled = storedChoice(13, 2016, 1);
      close(cancelled, Closed.CANCELLED);
      MobilityChoice otherTerm = storedChoice(14, 2016, 2);
      when(mobilityChoiceRepository.findByUser(STUDENT_ID))
          .thenReturn(List.of(choice, open, cancelled, otherTerm));
      DenialReason automatic = givenDenialReason(AUTOMATIC_DENIAL_REASON);

      mobilityChoiceService.confirmWithNewPartner(CHOICE_ID, request, PROFESSOR_ID, Role.PROFESSOR);

      assertThat(choice.getPartner()).isSameAs(restored);
      assertThat(open.getDenialReason()).isSameAs(automatic);
      assertThat(cancelled.getDenialReason()).isNull();
      assertThat(otherTerm.getDenialReason()).isNull();
      verify(mobilityChoiceRepository).update(open);
      verify(mobilityChoiceRepository, never()).update(cancelled);
      verify(partnerService, never()).create(any(), any());
      verifyNoInteractions(mobilityDocumentRepository);
    }

    @Test
    void thePartnerMustBeInTheCountryOfTheChoice() {
      MobilityChoice choice = givenChoice(CHOICE_ID, 2016, 1);
      Partner inGreatBritain = partner(0, false);
      inGreatBritain.getAddress().getCountry().setCountryCode("GB");

      assertThatBusinessException(() ->
              mobilityChoiceService.confirmWithNewPartner(
                  CHOICE_ID, inGreatBritain, PROFESSOR_ID, Role.PROFESSOR))
          .hasErrorCode(ErrorCode.COUNTRY_CHANGE_NOT_ALLOWED);
      assertThat(choice.getCountry().getCountryCode()).isEqualTo("IE");
      verifyNoInteractions(partnerService);
      verify(mobilityChoiceRepository, never()).update(any());
      verify(mobilityRepository, never()).create(any());
    }

    @Test
    void aMobilityWithoutDepartureDocumentStartsToBePaid() {
      MobilityChoice choice = givenChoice(CHOICE_ID, 2016, 1);
      Partner request = partner(0, false);
      when(partnerService.create(request, Role.PROFESSOR)).thenReturn(request);
      givenProgrammeDocuments(DocumentCategory.RETURN, 3);
      givenUser(PROFESSOR_ID, Role.PROFESSOR);
      when(mobilityChoiceRepository.findByUser(STUDENT_ID)).thenReturn(List.of(choice));

      mobilityChoiceService.confirmWithNewPartner(CHOICE_ID, request, PROFESSOR_ID, Role.PROFESSOR);

      assertThat(createdMobility().getState()).isEqualTo(MobilityState.TO_BE_PAID);
      verify(mobilityDocumentRepository).create(3, CHOICE_ID);
    }

    @Test
    void aRestoredPartnerIsCheckedAsStoredNotAsSentByTheClient() {
      MobilityChoice choice = givenChoice(CHOICE_ID, 2016, 1);
      Partner request = partner(PARTNER_ID, false); // claims the country of the choice (IE)
      Partner stored = partner(PARTNER_ID, false);
      stored.getAddress().getCountry().setCountryCode("GB");
      when(partnerService.restore(PARTNER_ID, Role.PROFESSOR)).thenReturn(stored);

      assertThatBusinessException(() ->
              mobilityChoiceService.confirmWithNewPartner(
                  CHOICE_ID, request, PROFESSOR_ID, Role.PROFESSOR))
          .hasErrorCode(ErrorCode.COUNTRY_CHANGE_NOT_ALLOWED);
      assertThat(choice.getCountry().getCountryCode()).isEqualTo("IE");
      verify(mobilityChoiceRepository, never()).update(any());
      verify(mobilityRepository, never()).create(any());
    }

    @Test
    void aChoiceWithoutCountryTakesTheCountryOfThePartner() {
      // as MobilityChoiceRepository reads a choice without country: a country without code
      MobilityChoice choice = givenChoice(CHOICE_ID, 2016, 1);
      choice.getCountry().setCountryCode(null);
      Partner inGreatBritain = partner(0, false);
      inGreatBritain.getAddress().getCountry().setCountryCode("GB");
      when(partnerService.create(inGreatBritain, Role.PROFESSOR)).thenReturn(inGreatBritain);
      givenProgrammeDocuments();
      givenUser(PROFESSOR_ID, Role.PROFESSOR);
      when(mobilityChoiceRepository.findByUser(STUDENT_ID)).thenReturn(List.of(choice));

      mobilityChoiceService.confirmWithNewPartner(
          CHOICE_ID, inGreatBritain, PROFESSOR_ID, Role.PROFESSOR);

      assertThat(choice.getCountry().getCountryCode()).isEqualTo("GB");
      verify(mobilityChoiceRepository).update(choice);
    }

    @Test
    void aStudentCannotConfirmTheChoiceOfAnotherStudent() {
      givenChoice(CHOICE_ID, 2016, 1);

      assertThatBusinessException(() ->
              mobilityChoiceService.confirmWithNewPartner(
                  CHOICE_ID, partner(0, false), STUDENT_ID + 1, Role.STUDENT))
          .hasErrorCode(ErrorCode.ACCESS_DENIED);
      verifyNoInteractions(partnerService, mobilityRepository);
    }

    @Test
    void aStudentCannotProposeAnOfficialPartner() {
      givenChoice(CHOICE_ID, 2016, 1);

      assertThatBusinessException(() ->
              mobilityChoiceService.confirmWithNewPartner(
                  CHOICE_ID, partner(0, true), STUDENT_ID, Role.STUDENT))
          .hasErrorCode(ErrorCode.ACCESS_DENIED);
      verifyNoInteractions(partnerService, mobilityRepository);
    }

    @ParameterizedTest
    @EnumSource(Closed.class)
    void aClosedChoiceCannotBeConfirmed(Closed closed) {
      close(givenChoice(CHOICE_ID, 2016, 1), closed);

      assertThatBusinessException(() ->
              mobilityChoiceService.confirmWithNewPartner(
                  CHOICE_ID, partner(0, false), PROFESSOR_ID, Role.PROFESSOR))
          .hasErrorCode(ErrorCode.MOBILITY_CHOICE_CLOSED);
      verifyNoInteractions(partnerService);
    }

    @Test
    void aChoiceIsConfirmedOnlyOnce() {
      givenChoice(CHOICE_ID, 2016, 1);
      when(mobilityRepository.findById(CHOICE_ID)).thenReturn(Optional.of(new Mobility()));

      assertThatBusinessException(() ->
              mobilityChoiceService.confirmWithNewPartner(
                  CHOICE_ID, partner(0, false), PROFESSOR_ID, Role.PROFESSOR))
          .hasErrorCode(ErrorCode.MOBILITY_CHOICE_ALREADY_CONFIRMED);
      verifyNoInteractions(partnerService);
      verify(mobilityRepository, never()).create(any());
    }

    @Test
    void anUnknownChoiceIsNotFound() {
      assertThatBusinessException(() ->
              mobilityChoiceService.confirmWithNewPartner(
                  CHOICE_ID, partner(0, false), PROFESSOR_ID, Role.PROFESSOR))
          .hasErrorCode(ErrorCode.RESOURCE_NOT_FOUND);
    }
  }

  @Nested
  class ExportAll {

    @Test
    void everyChoiceIsALineOfTheCsv() {
      MobilityChoice first = storedChoice(CHOICE_ID, 2016, 1);
      first.getUser().setLastName("Martin");
      first.getUser().setFirstName("Alice");
      MobilityChoice second = storedChoice(OTHER_CHOICE_ID, 2016, 2);
      second.setPreferenceOrder(2);
      second.setMobilityType(MobilityType.SMP);
      when(mobilityChoiceRepository.findAll(MobilityChoiceFilter.ACTIVE))
          .thenReturn(List.of(first, second));

      String csv =
          mobilityChoiceService.exportAll(
              PROFESSOR_ID, Role.PROFESSOR, MobilityChoiceFilter.ACTIVE);

      assertThat(csv)
          .isEqualTo(
              "﻿"
                  + "N° ordre candidature;Nom;Prénom;Option;N° ordre préférence;Programme de mobilité;"
                  + "Type de mobilité;Semestre de départ;Partenaire;\n"
                  + "11;Martin;Alice;Bachelier en informatique de gestion;1;Erasmus+;SMS;1;"
                  + "University of Dublin;\n"
                  + "12;TheReaper;Jack;Bachelier en informatique de gestion;2;Erasmus+;SMP;2;"
                  + "University of Dublin;\n");
    }

    @Test
    void aChoiceWithoutPartnerHasAnEmptyPartnerColumn() {
      MobilityChoice withoutPartner = storedChoice(CHOICE_ID, 2016, 1);
      withoutPartner.setPartner(null);
      when(mobilityChoiceRepository.findAll(MobilityChoiceFilter.ACTIVE))
          .thenReturn(List.of(withoutPartner));

      String csv =
          mobilityChoiceService.exportAll(
              PROFESSOR_ID, Role.PROFESSOR, MobilityChoiceFilter.ACTIVE);

      assertThat(csv.lines().skip(1))
          .containsExactly(
              "11;TheReaper;Jack;Bachelier en informatique de gestion;1;Erasmus+;SMS;1;;");
    }

    @Test
    void withoutChoicesOnlyTheHeaderIsExported() {
      givenUser(STUDENT_ID, Role.STUDENT);
      when(mobilityChoiceRepository.findByUser(STUDENT_ID)).thenReturn(List.of());

      assertThat(
              mobilityChoiceService.exportAll(
                  STUDENT_ID, Role.STUDENT, MobilityChoiceFilter.ACTIVE))
          .hasLineCount(1)
          .startsWith("﻿N° ordre candidature;");
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 2})
  void aPartnerIsFreeWhenNoChoiceUsesIt(int choices) {
    when(mobilityChoiceRepository.findByPartner(PARTNER_ID))
        .thenReturn(Collections.nCopies(choices, storedChoice(CHOICE_ID, 2016, 1)));

    assertThat(mobilityChoiceService.findByPartner(PARTNER_ID)).isEqualTo(choices == 0);
  }
}
