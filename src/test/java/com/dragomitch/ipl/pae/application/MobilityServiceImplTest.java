package com.dragomitch.ipl.pae.application;

import static com.dragomitch.ipl.pae.domain.exceptions.BusinessExceptionAssert.assertThatBusinessException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dragomitch.ipl.pae.domain.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.domain.dto.DocumentDto;
import com.dragomitch.ipl.pae.domain.dto.MobilityDto;
import com.dragomitch.ipl.pae.domain.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.domain.dto.PartnerDto;
import com.dragomitch.ipl.pae.domain.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.domain.dto.UserDto;
import com.dragomitch.ipl.pae.domain.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.domain.model.Document;
import com.dragomitch.ipl.pae.domain.model.EntityFactories;
import com.dragomitch.ipl.pae.domain.model.EntityFactory;
import com.dragomitch.ipl.pae.domain.model.Programme;
import com.dragomitch.ipl.pae.domain.model.Violations;
import com.dragomitch.ipl.pae.domain.port.in.PartnerService;
import com.dragomitch.ipl.pae.domain.port.in.ProgrammeService;
import com.dragomitch.ipl.pae.domain.port.out.DenialReasonRepository;
import com.dragomitch.ipl.pae.domain.port.out.MobilityDocumentRepository;
import com.dragomitch.ipl.pae.domain.port.out.MobilityRepository;
import com.dragomitch.ipl.pae.domain.port.out.NominatedStudentRepository;
import com.dragomitch.ipl.pae.domain.port.out.UserRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Business rules of the mobility lifecycle: encodings, payments, documents, cancellation, CSV
 * export. The DAOs are Mockito mocks answering as their contract ({@code MobilityRepositoryIT}...) says:
 * {@code findById} returns the mobility with only the ids of its student, partner, programme and
 * professor, {@code update} increments the version of the DTO it returns.
 */
@ExtendWith(MockitoExtension.class)
class MobilityServiceImplTest {

  /** The instant the use case reads from its clock: the dates it sets are asserted exactly. */
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 15, 10, 15, 30);
  private static final Clock CLOCK = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

  private static final int MOBILITY_ID = 7;
  private static final int VERSION = 3;
  private static final int STUDENT_ID = 42;
  private static final int PROFESSOR_ID = 2;
  private static final int PARTNER_ID = 5;
  private static final int PROGRAMME_ID = 1;

  @Mock
  private MobilityRepository mobilityRepository;
  @Mock
  private NominatedStudentRepository nominatedStudentRepository;
  @Mock
  private DenialReasonRepository denialReasonRepository;
  @Mock
  private MobilityDocumentRepository mobilityDocumentRepository;
  @Mock
  private PartnerService partnerService;
  @Mock
  private ProgrammeService programmeService;
  @Mock
  private UserRepository userRepository;

  private final EntityFactory entityFactory = EntityFactories.create();
  private MobilityServiceImpl mobilityService;

  @BeforeEach
  void setUp() {
    mobilityService = new MobilityServiceImpl(mobilityRepository, nominatedStudentRepository, denialReasonRepository,
        mobilityDocumentRepository, partnerService, programmeService, userRepository, CLOCK);
  }

  /** A mobility as {@link MobilityRepository#findById} returns it. */
  private MobilityDto storedMobility(String state) {
    MobilityDto mobility = (MobilityDto) entityFactory.build(MobilityDto.class);
    mobility.setId(MOBILITY_ID);
    mobility.setVersion(VERSION);
    mobility.setState(state);
    mobility.setMobilityType("SMS");
    mobility.setTerm(2);
    mobility.setAcademicYear(2016);
    mobility.setSubmissionDate(LocalDateTime.of(2016, 3, 1, 10, 0));
    NominatedStudentDto student =
        (NominatedStudentDto) entityFactory.build(NominatedStudentDto.class);
    student.setId(STUDENT_ID);
    student.setFirstName("Alice");
    student.setLastName("Martin");
    mobility.setNominatedStudent(student);
    PartnerDto partner = (PartnerDto) entityFactory.build(PartnerDto.class);
    partner.setId(PARTNER_ID);
    partner.setFullName("University of Dublin");
    mobility.setPartner(partner);
    ProgrammeDto programme = (ProgrammeDto) entityFactory.build(ProgrammeDto.class);
    programme.setId(PROGRAMME_ID);
    programme.setProgrammeName("Erasmus+");
    mobility.setProgramme(programme);
    UserDto professor = (UserDto) entityFactory.build(UserDto.class);
    professor.setId(PROFESSOR_ID);
    mobility.setProfessorInCharge(professor);
    return mobility;
  }

  private MobilityDto givenMobility(String state) {
    MobilityDto mobility = storedMobility(state);
    when(mobilityRepository.findById(MOBILITY_ID)).thenReturn(mobility);
    return mobility;
  }

  /** {@link MobilityRepository#update} checks nothing here and increments the version, as the DAO. */
  private void givenUpdateSucceeds() {
    when(mobilityRepository.update(any())).thenAnswer(invocation -> {
      MobilityDto mobility = invocation.getArgument(0);
      mobility.setVersion(mobility.getVersion() + 1);
      return mobility;
    });
  }

  private DocumentDto document(int id, char category, boolean filledIn, String name) {
    DocumentDto document = (DocumentDto) entityFactory.build(DocumentDto.class);
    document.setId(id);
    document.setCategory(category);
    document.setFilledIn(filledIn);
    document.setName(name);
    return document;
  }

  private DocumentDto document(int id, char category, boolean filledIn) {
    return document(id, category, filledIn, "document " + id);
  }

  private NominatedStudentDto nominatedStudent(String iban, String bankName, String bic) {
    NominatedStudentDto student =
        (NominatedStudentDto) entityFactory.build(NominatedStudentDto.class);
    student.setId(STUDENT_ID);
    student.setIban(iban);
    student.setBankName(bankName);
    student.setBic(bic);
    return student;
  }

  @Nested
  class ShowAll {

    @Test
    void aProfessorSeesEveryMobility() {
      List<MobilityDto> all = List.of(storedMobility(MobilityDto.STATE_CREATED));
      when(mobilityRepository.findAll()).thenReturn(all);

      assertThat(mobilityService.showAll(PROFESSOR_ID, UserDto.ROLE_PROFESSOR)).isSameAs(all);
      verify(mobilityRepository, never()).findByUser(PROFESSOR_ID);
    }

    @Test
    void aStudentSeesOnlyTheirOwnMobilities() {
      List<MobilityDto> own = List.of(storedMobility(MobilityDto.STATE_CREATED));
      when(mobilityRepository.findByUser(STUDENT_ID)).thenReturn(own);

      assertThat(mobilityService.showAll(STUDENT_ID, UserDto.ROLE_STUDENT)).isSameAs(own);
      verify(mobilityRepository, never()).findAll();
    }
  }

  @Nested
  class ShowOne {

    @Test
    void anUnknownMobilityIsNotFound() {
      assertThat(Violations.errorCodeOf(
          () -> mobilityService.showOne(MOBILITY_ID, UserDto.ROLE_PROFESSOR, PROFESSOR_ID)))
          .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void aStudentCannotSeeTheMobilityOfAnotherStudent() {
      givenMobility(MobilityDto.STATE_CREATED);

      assertThat(Violations.errorCodeOf(
          () -> mobilityService.showOne(MOBILITY_ID, UserDto.ROLE_STUDENT, STUDENT_ID + 1)))
          .isEqualTo(ErrorCode.ACCESS_DENIED);
      verifyNoInteractions(mobilityDocumentRepository, partnerService, programmeService, userRepository);
    }

    static Stream<Arguments> allowedRequesters() {
      return Stream.of(Arguments.of(UserDto.ROLE_PROFESSOR, PROFESSOR_ID),
          Arguments.of(UserDto.ROLE_PROFESSOR, STUDENT_ID + 1),
          Arguments.of(UserDto.ROLE_STUDENT, STUDENT_ID));
    }

    @ParameterizedTest
    @MethodSource("allowedRequesters")
    void theOwnerOrAProfessorGetsTheMobilityWithItsDetails(String role, int requester) {
      givenMobility(MobilityDto.STATE_IN_PREPARATION);
      List<DocumentDto> documents = List.of(document(1, DocumentDto.DEPARTURE_DOCUMENT, true));
      when(mobilityDocumentRepository.findAllByMobility(MOBILITY_ID)).thenReturn(documents);
      NominatedStudentDto student = nominatedStudent("BE68539007547034", "Belfius", "GKCCBEBB");
      when(nominatedStudentRepository.findById(STUDENT_ID)).thenReturn(student);
      PartnerDto partner = (PartnerDto) entityFactory.build(PartnerDto.class);
      when(partnerService.showOne(PARTNER_ID)).thenReturn(partner);
      ProgrammeDto programme = (ProgrammeDto) entityFactory.build(ProgrammeDto.class);
      when(programmeService.showOne(PROGRAMME_ID)).thenReturn(programme);
      UserDto professor = (UserDto) entityFactory.build(UserDto.class);
      when(userRepository.findById(PROFESSOR_ID)).thenReturn(professor);

      MobilityDto shown = mobilityService.showOne(MOBILITY_ID, role, requester);

      assertThat(shown.getDocuments()).isEqualTo(documents);
      assertThat(shown.getNominatedStudent()).isSameAs(student);
      assertThat(shown.getPartner()).isSameAs(partner);
      assertThat(shown.getProgramme()).isSameAs(programme);
      assertThat(shown.getProfessorInCharge()).isSameAs(professor);
    }

    @Test
    void aMobilityConfirmedWithANewPartnerHasNoProfessorInCharge() {
      // confirmWithNewPartner creates the mobility without professor (nullable column)
      MobilityDto stored = givenMobility(MobilityDto.STATE_CREATED);
      stored.setProfessorInCharge(null);
      when(mobilityDocumentRepository.findAllByMobility(MOBILITY_ID)).thenReturn(List.of());

      MobilityDto shown = mobilityService.showOne(MOBILITY_ID, UserDto.ROLE_STUDENT, STUDENT_ID);

      assertThat(shown.getProfessorInCharge()).isNull();
      verifyNoInteractions(userRepository);
    }

    @Test
    void theStudentSummaryIsKeptWhenThePersonalDataAreNotRecorded() {
      MobilityDto stored = givenMobility(MobilityDto.STATE_CREATED);
      NominatedStudentDto summary = stored.getNominatedStudent();
      when(mobilityDocumentRepository.findAllByMobility(MOBILITY_ID)).thenReturn(List.of());

      MobilityDto shown = mobilityService.showOne(MOBILITY_ID, UserDto.ROLE_STUDENT, STUDENT_ID);

      assertThat(shown.getNominatedStudent()).isSameAs(summary);
      assertThat(shown.getNominatedStudent().getLastName()).isEqualTo("Martin");
    }
  }

  /** The two external software encodings behave the same way. */
  enum Software {
    PRO_ECO {
      @Override
      void confirm(MobilityServiceImpl ucc, int id, int version) {
        ucc.confirmProEcoEncoding(id, version);
      }

      @Override
      boolean isEncoded(MobilityDto mobility) {
        return mobility.isEncodedInProEco();
      }

      @Override
      void setEncoded(MobilityDto mobility) {
        mobility.setProEcoEncoding(true);
      }
    },
    SECOND {
      @Override
      void confirm(MobilityServiceImpl ucc, int id, int version) {
        ucc.confirmSecondSoftwareEncoding(id, version);
      }

      @Override
      boolean isEncoded(MobilityDto mobility) {
        return mobility.isEncodedInSecondSoftware();
      }

      @Override
      void setEncoded(MobilityDto mobility) {
        mobility.setSecondSoftwareEncoding(true);
      }
    };

    abstract void confirm(MobilityServiceImpl ucc, int id, int version);

    abstract boolean isEncoded(MobilityDto mobility);

    abstract void setEncoded(MobilityDto mobility);

    Software other() {
      return this == PRO_ECO ? SECOND : PRO_ECO;
    }
  }

  @Nested
  class ConfirmEncoding {

    @ParameterizedTest
    @EnumSource(Software.class)
    void confirmsTheEncodingOfThatSoftwareOnly(Software software) {
      MobilityDto mobility = givenMobility(MobilityDto.STATE_IN_PROGRESS);
      givenUpdateSucceeds();

      software.confirm(mobilityService, MOBILITY_ID, VERSION);

      assertThat(software.isEncoded(mobility)).isTrue();
      assertThat(software.other().isEncoded(mobility)).isFalse();
      assertThat(mobility.getVersion()).isEqualTo(VERSION + 1);
      verify(mobilityRepository).update(mobility);
    }

    @ParameterizedTest
    @EnumSource(Software.class)
    void aClosedMobilityCanStillBeEncoded(Software software) {
      MobilityDto mobility = givenMobility(MobilityDto.STATE_CLOSED);
      givenUpdateSucceeds();

      software.confirm(mobilityService, MOBILITY_ID, VERSION);

      assertThat(software.isEncoded(mobility)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(Software.class)
    void confirmingAnEncodingTwiceWritesNothing(Software software) {
      MobilityDto mobility = givenMobility(MobilityDto.STATE_IN_PROGRESS);
      software.setEncoded(mobility);

      software.confirm(mobilityService, MOBILITY_ID, VERSION);

      assertThat(software.isEncoded(mobility)).isTrue();
      verify(mobilityRepository, never()).update(any());
    }

    @ParameterizedTest
    @EnumSource(Software.class)
    void aCancelledMobilityCannotBeEncoded(Software software) {
      MobilityDto mobility = givenMobility(MobilityDto.STATE_CANCELLED);

      assertThat(Violations.errorCodeOf(() -> software.confirm(mobilityService, MOBILITY_ID, VERSION)))
          .isEqualTo(ErrorCode.MOBILITY_CANCELLED);
      assertThat(software.isEncoded(mobility)).isFalse();
      verify(mobilityRepository, never()).update(any());
    }

    @ParameterizedTest
    @EnumSource(Software.class)
    void aStaleVersionIsAConcurrentModification(Software software) {
      givenMobility(MobilityDto.STATE_IN_PROGRESS);

      assertThatThrownBy(() -> software.confirm(mobilityService, MOBILITY_ID, VERSION - 1))
          .isInstanceOf(ConcurrentModificationException.class);
      verify(mobilityRepository, never()).update(any());
    }

    @ParameterizedTest
    @EnumSource(Software.class)
    void anUnknownMobilityIsNotFound(Software software) {
      assertThat(Violations.errorCodeOf(() -> software.confirm(mobilityService, MOBILITY_ID, VERSION)))
          .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }
  }

  @Nested
  class ConfirmPayment {

    @Test
    void theFirstPaymentPutsTheMobilityInProgress() {
      MobilityDto mobility = givenMobility(MobilityDto.STATE_TO_BE_PAID);
      when(nominatedStudentRepository.findById(STUDENT_ID))
          .thenReturn(nominatedStudent("BE68539007547034", "Belfius", "GKCCBEBB"));
      when(mobilityDocumentRepository.findAllByMobility(MOBILITY_ID)).thenReturn(
          List.of(document(1, DocumentDto.DEPARTURE_DOCUMENT, true),
              document(3, DocumentDto.RETURN_DOCUMENT, false)));
      givenUpdateSucceeds();

      MobilityDto paid = mobilityService.confirmPayment(MOBILITY_ID, VERSION);

      assertThat(paid).isSameAs(mobility);
      assertThat(paid.getState()).isEqualTo(MobilityDto.STATE_IN_PROGRESS);
      assertThat(paid.getFirstPaymentRequestDate()).isEqualTo(NOW);
      assertThat(paid.getSecondPaymentRequestDate()).isNull();
      assertThat(paid.getVersion()).isEqualTo(VERSION + 1);
    }

    @Test
    void theBalancePaymentClosesTheMobility() {
      MobilityDto mobility = givenMobility(MobilityDto.STATE_BALANCE_TO_BE_PAID);
      LocalDateTime firstPayment = LocalDateTime.of(2016, 9, 1, 12, 0);
      mobility.setFirstPaymentRequestDate(firstPayment);
      when(nominatedStudentRepository.findById(STUDENT_ID))
          .thenReturn(nominatedStudent("BE68539007547034", "Belfius", "GKCCBEBB"));
      givenUpdateSucceeds();

      MobilityDto paid = mobilityService.confirmPayment(MOBILITY_ID, VERSION);

      assertThat(paid.getState()).isEqualTo(MobilityDto.STATE_CLOSED);
      assertThat(paid.getFirstPaymentRequestDate()).isEqualTo(firstPayment);
      assertThat(paid.getSecondPaymentRequestDate()).isEqualTo(NOW);
      verify(mobilityRepository).update(mobility);
    }

    @Test
    void theFirstPaymentMakesTheBalanceDueWhenTheReturnDocumentsAreAlreadyFilledIn() {
      // the return documents may be ticked while the first payment is pending ("A payer"):
      // "En cours" would then be a dead end, no document being left to fill in
      MobilityDto mobility = givenMobility(MobilityDto.STATE_TO_BE_PAID);
      when(nominatedStudentRepository.findById(STUDENT_ID))
          .thenReturn(nominatedStudent("BE68539007547034", "Belfius", "GKCCBEBB"));
      when(mobilityDocumentRepository.findAllByMobility(MOBILITY_ID)).thenReturn(
          List.of(document(1, DocumentDto.DEPARTURE_DOCUMENT, true),
              document(3, DocumentDto.RETURN_DOCUMENT, true)));
      givenUpdateSucceeds();

      MobilityDto paid = mobilityService.confirmPayment(MOBILITY_ID, VERSION);

      assertThat(paid.getState()).isEqualTo(MobilityDto.STATE_BALANCE_TO_BE_PAID);
      assertThat(paid.getFirstPaymentRequestDate()).isNotNull();
      assertThat(paid.getSecondPaymentRequestDate()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {MobilityDto.STATE_CREATED, MobilityDto.STATE_IN_PREPARATION,
        MobilityDto.STATE_IN_PROGRESS, MobilityDto.STATE_BALANCE_TO_BE_PAID})
    void noFirstPaymentBeforeTheDepartureDocumentsAreFilledIn(String state) {
      // the first payment is only expected in "A payer": before, the departure documents are
      // incomplete; "En cours" and "Solde à payer" without a first payment are inconsistent
      MobilityDto mobility = givenMobility(state);

      assertThat(Violations.errorCodeOf(() -> mobilityService.confirmPayment(MOBILITY_ID, VERSION)))
          .isEqualTo(ErrorCode.PAYMENT_NOT_EXPECTED);
      assertThat(mobility.getState()).isEqualTo(state);
      assertThat(mobility.getFirstPaymentRequestDate()).isNull();
      verifyNoInteractions(nominatedStudentRepository);
      verify(mobilityRepository, never()).update(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {MobilityDto.STATE_IN_PROGRESS, MobilityDto.STATE_TO_BE_PAID,
        MobilityDto.STATE_IN_PREPARATION})
    void noSecondPaymentBeforeTheBalanceIsDue(String state) {
      MobilityDto mobility = givenMobility(state);
      mobility.setFirstPaymentRequestDate(LocalDateTime.of(2016, 9, 1, 12, 0));

      assertThat(Violations.errorCodeOf(() -> mobilityService.confirmPayment(MOBILITY_ID, VERSION)))
          .isEqualTo(ErrorCode.PAYMENT_NOT_EXPECTED);
      verifyNoInteractions(nominatedStudentRepository);
      assertThat(mobility.getState()).isEqualTo(state);
      assertThat(mobility.getSecondPaymentRequestDate()).isNull();
      verify(mobilityRepository, never()).update(any());
    }

    static Stream<Arguments> closedStates() {
      return Stream.of(Arguments.of(MobilityDto.STATE_CANCELLED, ErrorCode.MOBILITY_CANCELLED),
          Arguments.of(MobilityDto.STATE_CLOSED, ErrorCode.MOBILITY_CLOSED));
    }

    @ParameterizedTest
    @MethodSource("closedStates")
    void aCancelledOrClosedMobilityIsNotPaid(String state, ErrorCode expected) {
      givenMobility(state);

      assertThat(Violations.errorCodeOf(() -> mobilityService.confirmPayment(MOBILITY_ID, VERSION)))
          .isEqualTo(expected);
      verifyNoInteractions(nominatedStudentRepository);
      verify(mobilityRepository, never()).update(any());
    }

    @Test
    void aStudentWithoutPersonalDataHasNoBankDetails() {
      givenMobility(MobilityDto.STATE_TO_BE_PAID);

      assertThat(Violations.errorCodeOf(() -> mobilityService.confirmPayment(MOBILITY_ID, VERSION)))
          .isEqualTo(ErrorCode.INCOMPLETE_BANK_DETAILS);
      verify(mobilityRepository, never()).update(any());
    }

    static Stream<Arguments> incompleteBankDetails() {
      return Stream.of(Arguments.of(null, "Belfius", "GKCCBEBB"),
          Arguments.of(" ", "Belfius", "GKCCBEBB"),
          Arguments.of("BE68539007547034", null, "GKCCBEBB"),
          Arguments.of("BE68539007547034", "", "GKCCBEBB"),
          Arguments.of("BE68539007547034", "Belfius", null));
    }

    @ParameterizedTest
    @MethodSource("incompleteBankDetails")
    void theBankDetailsMustBeComplete(String iban, String bankName, String bic) {
      MobilityDto mobility = givenMobility(MobilityDto.STATE_TO_BE_PAID);
      when(nominatedStudentRepository.findById(STUDENT_ID))
          .thenReturn(nominatedStudent(iban, bankName, bic));

      assertThat(Violations.errorCodeOf(() -> mobilityService.confirmPayment(MOBILITY_ID, VERSION)))
          .isEqualTo(ErrorCode.INCOMPLETE_BANK_DETAILS);
      assertThat(mobility.getFirstPaymentRequestDate()).isNull();
      verify(mobilityRepository, never()).update(any());
    }

    @Test
    void aStaleVersionIsAConcurrentModification() {
      givenMobility(MobilityDto.STATE_TO_BE_PAID);

      assertThatThrownBy(() -> mobilityService.confirmPayment(MOBILITY_ID, VERSION + 1))
          .isInstanceOf(ConcurrentModificationException.class);
      verifyNoInteractions(nominatedStudentRepository);
    }

    @Test
    void anUnknownMobilityIsNotFound() {
      assertThat(Violations.errorCodeOf(() -> mobilityService.confirmPayment(MOBILITY_ID, VERSION)))
          .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }
  }

  @Nested
  class ConfirmDocument {

    private static final char D = DocumentDto.DEPARTURE_DOCUMENT;
    private static final char R = DocumentDto.RETURN_DOCUMENT;

    /**
     * State transitions: the mobility, its documents (id 1 and 2 departure, 3 and 4 return,
     * filled-in or not) and the document filled in, the state after.
     */
    static Stream<Arguments> transitions() {
      return Stream.of(
          // the first document starts the preparation...
          Arguments.of(MobilityDto.STATE_CREATED, "----", 1, MobilityDto.STATE_IN_PREPARATION),
          // ...or makes the first payment due at once when it completes the departure documents
          Arguments.of(MobilityDto.STATE_CREATED, "-X--", 1, MobilityDto.STATE_TO_BE_PAID),
          Arguments.of(MobilityDto.STATE_CREATED, "-XX-", 1, MobilityDto.STATE_TO_BE_PAID),
          Arguments.of(MobilityDto.STATE_CREATED, "----", 3, MobilityDto.STATE_IN_PREPARATION),
          // the last departure document makes the first payment due
          Arguments.of(MobilityDto.STATE_IN_PREPARATION, "X---", 2, MobilityDto.STATE_TO_BE_PAID),
          Arguments.of(MobilityDto.STATE_IN_PREPARATION, "----", 1,
              MobilityDto.STATE_IN_PREPARATION),
          Arguments.of(MobilityDto.STATE_IN_PREPARATION, "X---", 3,
              MobilityDto.STATE_IN_PREPARATION),
          // return documents do not change a mobility waiting for its first payment
          Arguments.of(MobilityDto.STATE_TO_BE_PAID, "XX--", 3, MobilityDto.STATE_TO_BE_PAID),
          // the last return document makes the balance due
          Arguments.of(MobilityDto.STATE_IN_PROGRESS, "XXX-", 4,
              MobilityDto.STATE_BALANCE_TO_BE_PAID),
          Arguments.of(MobilityDto.STATE_IN_PROGRESS, "XX--", 3, MobilityDto.STATE_IN_PROGRESS),
          Arguments.of(MobilityDto.STATE_IN_PROGRESS, "-XXX", 1,
              MobilityDto.STATE_BALANCE_TO_BE_PAID),
          Arguments.of(MobilityDto.STATE_IN_PROGRESS, "--XX", 1, MobilityDto.STATE_IN_PROGRESS),
          Arguments.of(MobilityDto.STATE_BALANCE_TO_BE_PAID, "XX-X", 3,
              MobilityDto.STATE_BALANCE_TO_BE_PAID));
    }

    private List<DocumentDto> documents(String filledIn) {
      char[] categories = {D, D, R, R};
      List<DocumentDto> documents = new ArrayList<>();
      for (int i = 0; i < categories.length; i++) {
        documents.add(document(i + 1, categories[i], filledIn.charAt(i) == 'X'));
      }
      return documents;
    }

    @ParameterizedTest(name = "{0} with {1}, fill in {2} -> {3}")
    @MethodSource("transitions")
    void fillingInADocumentMovesTheMobilityThroughItsStates(String state, String filledIn,
        int document, String expectedState) {
      MobilityDto mobility = givenMobility(state);
      List<DocumentDto> documents = documents(filledIn);
      when(mobilityDocumentRepository.findAllByMobility(MOBILITY_ID)).thenReturn(documents);
      givenUpdateSucceeds();

      MobilityDto updated = mobilityService.confirmDocument(MOBILITY_ID, document, VERSION);

      assertThat(updated).isSameAs(mobility);
      assertThat(updated.getState()).isEqualTo(expectedState);
      assertThat(updated.getVersion()).isEqualTo(VERSION + 1);
      assertThat(updated.getDocuments()).filteredOn(d -> d.getId() == document)
          .singleElement().matches(DocumentDto::isFilledIn);
      verify(mobilityDocumentRepository).fillInDocument(document, MOBILITY_ID);
    }

    @Test
    void aDocumentAlreadyFilledInChangesNothing() {
      MobilityDto mobility = givenMobility(MobilityDto.STATE_IN_PREPARATION);
      when(mobilityDocumentRepository.findAllByMobility(MOBILITY_ID)).thenReturn(documents("X---"));

      MobilityDto unchanged = mobilityService.confirmDocument(MOBILITY_ID, 1, VERSION);

      assertThat(unchanged).isSameAs(mobility);
      assertThat(unchanged.getState()).isEqualTo(MobilityDto.STATE_IN_PREPARATION);
      assertThat(unchanged.getVersion()).isEqualTo(VERSION);
      verify(mobilityDocumentRepository, never()).fillInDocument(1, MOBILITY_ID);
      verify(mobilityRepository, never()).update(any());
    }

    @ParameterizedTest
    @MethodSource("com.dragomitch.ipl.pae.application.MobilityServiceImplTest$ConfirmPayment#closedStates")
    void theDocumentsOfACancelledOrClosedMobilityAreFrozen(String state, ErrorCode expected) {
      MobilityDto mobility = givenMobility(state);

      assertThat(Violations.errorCodeOf(() -> mobilityService.confirmDocument(MOBILITY_ID, 1,
          VERSION))).isEqualTo(expected);
      assertThat(mobility.getState()).isEqualTo(state);
      verifyNoInteractions(mobilityDocumentRepository);
      verify(mobilityRepository, never()).update(any());
    }

    @Test
    void aDocumentThatIsNotRequiredByTheMobilityIsUnknown() {
      givenMobility(MobilityDto.STATE_IN_PREPARATION);
      when(mobilityDocumentRepository.findAllByMobility(MOBILITY_ID)).thenReturn(documents("----"));

      assertThat(Violations.errorCodeOf(() -> mobilityService.confirmDocument(MOBILITY_ID, 99,
          VERSION))).isEqualTo(ErrorCode.UNKNOWN_DOCUMENT);
      verify(mobilityRepository, never()).update(any());
    }

    @Test
    void aStaleVersionIsAConcurrentModification() {
      givenMobility(MobilityDto.STATE_IN_PREPARATION);

      assertThatThrownBy(() -> mobilityService.confirmDocument(MOBILITY_ID, 1, VERSION - 1))
          .isInstanceOf(ConcurrentModificationException.class);
      verifyNoInteractions(mobilityDocumentRepository);
    }

    @Test
    void anUnknownMobilityIsNotFound() {
      assertThat(Violations.errorCodeOf(() -> mobilityService.confirmDocument(MOBILITY_ID, 1,
          VERSION))).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }
  }

  @Nested
  class Cancel {

    private DenialReasonDto givenDenialReason(int id) {
      DenialReasonDto denialReason = (DenialReasonDto) entityFactory.build(DenialReasonDto.class);
      denialReason.setId(id);
      denialReason.setReason("Dossier incomplet");
      when(denialReasonRepository.findById(id)).thenReturn(denialReason);
      return denialReason;
    }

    @ParameterizedTest
    @ValueSource(strings = {MobilityDto.STATE_CREATED, MobilityDto.STATE_IN_PREPARATION,
        MobilityDto.STATE_TO_BE_PAID, MobilityDto.STATE_IN_PROGRESS,
        MobilityDto.STATE_BALANCE_TO_BE_PAID})
    void aProfessorCancelsWithADenialReason(String state) {
      givenMobility(state);
      DenialReasonDto denialReason = givenDenialReason(4);
      givenUpdateSucceeds();

      MobilityDto cancelled = mobilityService.cancel(MOBILITY_ID, VERSION, null, 4, PROFESSOR_ID,
          UserDto.ROLE_PROFESSOR);

      assertThat(cancelled.getState()).isEqualTo(MobilityDto.STATE_CANCELLED);
      assertThat(cancelled.getStateBeforeCancellation()).isEqualTo(state);
      assertThat(cancelled.getDenialReason()).isSameAs(denialReason);
      assertThat(cancelled.getCancellationReason()).isNull();
      assertThat(cancelled.getVersion()).isEqualTo(VERSION + 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {MobilityDto.STATE_CREATED, MobilityDto.STATE_IN_PROGRESS})
    void theOwnerCancelsWithAReason(String state) {
      givenMobility(state);
      givenUpdateSucceeds();

      MobilityDto cancelled = mobilityService.cancel(MOBILITY_ID, VERSION, "Raisons médicales", 0,
          STUDENT_ID, UserDto.ROLE_STUDENT);

      assertThat(cancelled.getState()).isEqualTo(MobilityDto.STATE_CANCELLED);
      assertThat(cancelled.getStateBeforeCancellation()).isEqualTo(state);
      assertThat(cancelled.getCancellationReason()).isEqualTo("Raisons médicales");
      assertThat(cancelled.getDenialReason()).isNull();
      verifyNoInteractions(denialReasonRepository);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void aProfessorMustGiveADenialReason(int denialReasonId) {
      MobilityDto mobility = givenMobility(MobilityDto.STATE_CREATED);

      assertThat(Violations.errorCodeOf(() -> mobilityService.cancel(MOBILITY_ID, VERSION,
          "a free-text reason is not enough", denialReasonId, PROFESSOR_ID,
          UserDto.ROLE_PROFESSOR))).isEqualTo(ErrorCode.DENIAL_REASON_REQUIRED);
      assertThat(mobility.getState()).isEqualTo(MobilityDto.STATE_CREATED);
      verify(mobilityRepository, never()).update(any());
    }

    @Test
    void aProfessorMustGiveAnExistingDenialReason() {
      givenMobility(MobilityDto.STATE_CREATED);

      assertThatBusinessException(() -> mobilityService.cancel(MOBILITY_ID,
          VERSION, null, 99, PROFESSOR_ID, UserDto.ROLE_PROFESSOR))
          .hasErrorCode(ErrorCode.UNKNOWN_DENIAL_REASON).hasArguments(99);
      verify(mobilityRepository, never()).update(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void aStudentMustGiveACancellationReason(String reason) {
      MobilityDto mobility = givenMobility(MobilityDto.STATE_CREATED);

      assertThat(Violations.errorCodeOf(() -> mobilityService.cancel(MOBILITY_ID, VERSION, reason, 4,
          STUDENT_ID, UserDto.ROLE_STUDENT)))
          .isEqualTo(ErrorCode.CANCELLATION_REASON_REQUIRED);
      assertThat(mobility.getState()).isEqualTo(MobilityDto.STATE_CREATED);
      verify(mobilityRepository, never()).update(any());
    }

    @Test
    void aStudentCannotCancelTheMobilityOfAnotherStudent() {
      MobilityDto mobility = givenMobility(MobilityDto.STATE_CREATED);

      assertThat(Violations.errorCodeOf(() -> mobilityService.cancel(MOBILITY_ID, VERSION, "reason",
          0, STUDENT_ID + 1, UserDto.ROLE_STUDENT))).isEqualTo(ErrorCode.ACCESS_DENIED);
      assertThat(mobility.getState()).isEqualTo(MobilityDto.STATE_CREATED);
      verify(mobilityRepository, never()).update(any());
    }

    @Test
    void cancellingACancelledMobilityAgainChangesNothing() {
      MobilityDto mobility = givenMobility(MobilityDto.STATE_CANCELLED);
      mobility.setStateBeforeCancellation(MobilityDto.STATE_IN_PROGRESS);
      mobility.setCancellationReason("first reason");

      MobilityDto unchanged = mobilityService.cancel(MOBILITY_ID, VERSION, "second reason", 0,
          STUDENT_ID, UserDto.ROLE_STUDENT);

      assertThat(unchanged).isSameAs(mobility);
      assertThat(unchanged.getCancellationReason()).isEqualTo("first reason");
      assertThat(unchanged.getStateBeforeCancellation()).isEqualTo(MobilityDto.STATE_IN_PROGRESS);
      verify(mobilityRepository, never()).update(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {MobilityDto.STATE_CANCELLED, MobilityDto.STATE_CLOSED})
    void aStudentLearnsNothingAboutTheMobilityOfAnotherStudent(String state) {
      MobilityDto mobility = givenMobility(state);
      mobility.setCancellationReason("personal reason of the owner");

      assertThat(Violations.errorCodeOf(() -> mobilityService.cancel(MOBILITY_ID, VERSION, "reason",
          0, STUDENT_ID + 1, UserDto.ROLE_STUDENT))).isEqualTo(ErrorCode.ACCESS_DENIED);
      verify(mobilityRepository, never()).update(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {UserDto.ROLE_PROFESSOR, UserDto.ROLE_STUDENT})
    void aClosedMobilityCannotBeCancelled(String role) {
      givenMobility(MobilityDto.STATE_CLOSED);

      assertThat(Violations.errorCodeOf(() -> mobilityService.cancel(MOBILITY_ID, VERSION, "reason",
          4, STUDENT_ID, role))).isEqualTo(ErrorCode.MOBILITY_CLOSED);
      verify(mobilityRepository, never()).update(any());
    }

    @Test
    void aStaleVersionIsAConcurrentModification() {
      givenMobility(MobilityDto.STATE_CREATED);

      assertThatThrownBy(() -> mobilityService.cancel(MOBILITY_ID, VERSION - 1, "reason", 0,
          STUDENT_ID, UserDto.ROLE_STUDENT)).isInstanceOf(ConcurrentModificationException.class);
      verify(mobilityRepository, never()).update(any());
    }

    @Test
    void anUnknownMobilityIsNotFound() {
      assertThat(Violations.errorCodeOf(() -> mobilityService.cancel(MOBILITY_ID, VERSION, "reason",
          0, STUDENT_ID, UserDto.ROLE_STUDENT))).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }
  }

  @Nested
  class ExportDocuments {

    private static final String HEADER = "﻿"
        + "Nom;Prénom;Partenaire;Type de mobilité;Programme de mobilité;Semestre;"
        + "Contrat de bourse;Convention de stage / Convention d'études;Charte de l'étudiant;"
        + "Document d'engagement;Preuve du passage des tests linguistiques;Attestation séjour;"
        + "Relevé de notes (SMS) ou certificat de stage (SMP);Rapport final (complété en ligne);"
        + "Preuve du passage des tests linguistiques après la mobilité;\n"
        + "Martin;Alice;University of Dublin;SMS;Erasmus+;2;";

    /** The documents of an Erabel mobility: 4 departure and 3 return documents, some filled in. */
    private List<DocumentDto> erabelDocuments() {
      return List.of(document(1, 'D', true, "Contrat de bourse"),
          document(2, 'D', false, "Convention de stage / Convention d'études"),
          document(3, 'D', true, "Charte de l'étudiant"),
          document(4, 'D', false, "Document d'engagement"),
          document(5, 'R', true, "Attestation séjour"),
          document(6, 'R', false, "Relevé de notes (SMS) ou certificat de stage (SMP)"),
          document(7, 'R', false, "Rapport final (complété en ligne)"));
    }

    @Test
    void withoutFilterEveryDocumentOfTheMobilityIsExportedInTheColumnsOrder() {
      givenMobility(MobilityDto.STATE_IN_PROGRESS);
      when(mobilityDocumentRepository.findAllByMobility(MOBILITY_ID)).thenReturn(erabelDocuments());

      String csv = mobilityService.exportDocuments(MOBILITY_ID, null);

      // the language test proofs are not required by Erabel: empty columns
      assertThat(csv).isEqualTo(HEADER + "Rempli;Non-rempli;Rempli;Non-rempli;;Rempli;"
          + "Non-rempli;Non-rempli;;");
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
        "D  | Rempli;Non-rempli;Rempli;Non-rempli;;;;;;",
        "DF | Rempli;;Rempli;;;;;;;",
        "R  | ;;;;;Rempli;Non-rempli;Non-rempli;;",
        "RF | ;;;;;Rempli;;;;"})
    void theFilterKeepsTheDocumentsOfItsCategoryAndOptionallyOnlyTheFilledInOnes(String filter,
        String documentColumns) {
      givenMobility(MobilityDto.STATE_IN_PROGRESS);
      when(mobilityDocumentRepository.findAllByMobility(MOBILITY_ID)).thenReturn(erabelDocuments());

      assertThat(mobilityService.exportDocuments(MOBILITY_ID, filter))
          .isEqualTo(HEADER + documentColumns);
    }

    @Test
    void aMobilityWithoutDocumentsExportsEmptyColumns() {
      givenMobility(MobilityDto.STATE_CREATED);
      when(mobilityDocumentRepository.findAllByMobility(MOBILITY_ID)).thenReturn(List.of());

      assertThat(mobilityService.exportDocuments(MOBILITY_ID, null)).isEqualTo(HEADER + ";;;;;;;;;");
    }

    @Test
    void anUnknownMobilityIsNotFound() {
      assertThat(Violations.errorCodeOf(() -> mobilityService.exportDocuments(MOBILITY_ID, null)))
          .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
      verifyNoInteractions(mobilityDocumentRepository);
    }
  }
}
