package com.dragomitch.ipl.pae.application;

import static com.dragomitch.ipl.pae.domain.exceptions.BusinessExceptionAssert.assertThatBusinessException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dragomitch.ipl.pae.domain.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.domain.model.DenialReason;
import com.dragomitch.ipl.pae.domain.model.Document;
import com.dragomitch.ipl.pae.domain.model.DocumentCategory;
import com.dragomitch.ipl.pae.domain.model.DocumentFilter;
import com.dragomitch.ipl.pae.domain.model.Mobility;
import com.dragomitch.ipl.pae.domain.model.MobilityState;
import com.dragomitch.ipl.pae.domain.model.MobilityType;
import com.dragomitch.ipl.pae.domain.model.NominatedStudent;
import com.dragomitch.ipl.pae.domain.model.Partner;
import com.dragomitch.ipl.pae.domain.model.Programme;
import com.dragomitch.ipl.pae.domain.model.Role;
import com.dragomitch.ipl.pae.domain.model.User;
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
import java.util.Optional;
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
 * export. The DAOs are Mockito mocks answering as their contract ({@code MobilityRepositoryIT}...)
 * says: {@code findById} returns the mobility with only the ids of its student, partner, programme
 * and professor, {@code update} increments the version of the DTO it returns.
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

  @Mock private MobilityRepository mobilityRepository;
  @Mock private NominatedStudentRepository nominatedStudentRepository;
  @Mock private DenialReasonRepository denialReasonRepository;
  @Mock private MobilityDocumentRepository mobilityDocumentRepository;
  @Mock private PartnerService partnerService;
  @Mock private ProgrammeService programmeService;
  @Mock private UserRepository userRepository;

  private MobilityServiceImpl mobilityService;

  @BeforeEach
  void setUp() {
    mobilityService =
        new MobilityServiceImpl(
            mobilityRepository,
            nominatedStudentRepository,
            denialReasonRepository,
            mobilityDocumentRepository,
            partnerService,
            programmeService,
            userRepository,
            CLOCK);
  }

  /** A mobility as {@link MobilityRepository#findById} returns it. */
  private Mobility storedMobility(MobilityState state) {
    Mobility mobility = new Mobility();
    mobility.setId(MOBILITY_ID);
    mobility.setVersion(VERSION);
    mobility.setState(state);
    mobility.setMobilityType(MobilityType.SMS);
    mobility.setTerm(2);
    mobility.setAcademicYear(2016);
    mobility.setSubmissionDate(LocalDateTime.of(2016, 3, 1, 10, 0));
    NominatedStudent student = new NominatedStudent();
    student.setId(STUDENT_ID);
    student.setFirstName("Alice");
    student.setLastName("Martin");
    mobility.setNominatedStudent(student);
    Partner partner = new Partner();
    partner.setId(PARTNER_ID);
    partner.setFullName("University of Dublin");
    mobility.setPartner(partner);
    Programme programme = new Programme();
    programme.setId(PROGRAMME_ID);
    programme.setProgrammeName("Erasmus+");
    mobility.setProgramme(programme);
    User professor = new User();
    professor.setId(PROFESSOR_ID);
    mobility.setProfessorInCharge(professor);
    return mobility;
  }

  private Mobility givenMobility(MobilityState state) {
    Mobility mobility = storedMobility(state);
    when(mobilityRepository.findById(MOBILITY_ID)).thenReturn(Optional.of(mobility));
    return mobility;
  }

  /**
   * {@link MobilityRepository#update} checks nothing here and increments the version, as the DAO.
   */
  private void givenUpdateSucceeds() {
    when(mobilityRepository.update(any()))
        .thenAnswer(invocation -> {
          Mobility mobility = invocation.getArgument(0);
          mobility.setVersion(mobility.getVersion() + 1);
          return mobility;
        });
  }

  private Document document(int id, DocumentCategory category, boolean filledIn, String name) {
    Document document = new Document();
    document.setId(id);
    document.setCategory(category);
    document.setFilledIn(filledIn);
    document.setName(name);
    return document;
  }

  private Document document(int id, DocumentCategory category, boolean filledIn) {
    return document(id, category, filledIn, "document " + id);
  }

  private NominatedStudent nominatedStudent(String iban, String bankName, String bic) {
    NominatedStudent student = new NominatedStudent();
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
      List<Mobility> all = List.of(storedMobility(MobilityState.CREATED));
      when(mobilityRepository.findAll()).thenReturn(all);

      assertThat(mobilityService.showAll(PROFESSOR_ID, Role.PROFESSOR)).isSameAs(all);
      verify(mobilityRepository, never()).findByUser(PROFESSOR_ID);
    }

    @Test
    void aStudentSeesOnlyTheirOwnMobilities() {
      List<Mobility> own = List.of(storedMobility(MobilityState.CREATED));
      when(mobilityRepository.findByUser(STUDENT_ID)).thenReturn(own);

      assertThat(mobilityService.showAll(STUDENT_ID, Role.STUDENT)).isSameAs(own);
      verify(mobilityRepository, never()).findAll();
    }
  }

  @Nested
  class ShowOne {

    @Test
    void anUnknownMobilityIsNotFound() {
      assertThat(
              Violations.errorCodeOf(
                  () -> mobilityService.showOne(MOBILITY_ID, Role.PROFESSOR, PROFESSOR_ID)))
          .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void aStudentCannotSeeTheMobilityOfAnotherStudent() {
      givenMobility(MobilityState.CREATED);

      assertThat(
              Violations.errorCodeOf(
                  () -> mobilityService.showOne(MOBILITY_ID, Role.STUDENT, STUDENT_ID + 1)))
          .isEqualTo(ErrorCode.ACCESS_DENIED);
      verifyNoInteractions(
          mobilityDocumentRepository, partnerService, programmeService, userRepository);
    }

    static Stream<Arguments> allowedRequesters() {
      return Stream.of(
          Arguments.of(Role.PROFESSOR, PROFESSOR_ID),
          Arguments.of(Role.PROFESSOR, STUDENT_ID + 1),
          Arguments.of(Role.STUDENT, STUDENT_ID));
    }

    @ParameterizedTest
    @MethodSource("allowedRequesters")
    void theOwnerOrAProfessorGetsTheMobilityWithItsDetails(Role role, int requester) {
      givenMobility(MobilityState.IN_PREPARATION);
      List<Document> documents = List.of(document(1, DocumentCategory.DEPARTURE, true));
      when(mobilityDocumentRepository.findAllByMobility(MOBILITY_ID)).thenReturn(documents);
      NominatedStudent student = nominatedStudent("BE68539007547034", "Belfius", "GKCCBEBB");
      when(nominatedStudentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
      Partner partner = new Partner();
      when(partnerService.showOne(PARTNER_ID)).thenReturn(partner);
      Programme programme = new Programme();
      when(programmeService.showOne(PROGRAMME_ID)).thenReturn(programme);
      User professor = new User();
      when(userRepository.findById(PROFESSOR_ID)).thenReturn(Optional.of(professor));

      Mobility shown = mobilityService.showOne(MOBILITY_ID, role, requester);

      assertThat(shown.getDocuments()).isEqualTo(documents);
      assertThat(shown.getNominatedStudent()).isSameAs(student);
      assertThat(shown.getPartner()).isSameAs(partner);
      assertThat(shown.getProgramme()).isSameAs(programme);
      assertThat(shown.getProfessorInCharge()).isSameAs(professor);
    }

    @Test
    void aMobilityConfirmedWithANewPartnerHasNoProfessorInCharge() {
      // confirmWithNewPartner creates the mobility without professor (nullable column)
      Mobility stored = givenMobility(MobilityState.CREATED);
      stored.setProfessorInCharge(null);
      when(mobilityDocumentRepository.findAllByMobility(MOBILITY_ID)).thenReturn(List.of());

      Mobility shown = mobilityService.showOne(MOBILITY_ID, Role.STUDENT, STUDENT_ID);

      assertThat(shown.getProfessorInCharge()).isNull();
      verifyNoInteractions(userRepository);
    }

    @Test
    void theStudentSummaryIsKeptWhenThePersonalDataAreNotRecorded() {
      Mobility stored = givenMobility(MobilityState.CREATED);
      NominatedStudent summary = stored.getNominatedStudent();
      when(mobilityDocumentRepository.findAllByMobility(MOBILITY_ID)).thenReturn(List.of());

      Mobility shown = mobilityService.showOne(MOBILITY_ID, Role.STUDENT, STUDENT_ID);

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
      boolean isEncoded(Mobility mobility) {
        return mobility.isEncodedInProEco();
      }

      @Override
      void setEncoded(Mobility mobility) {
        mobility.setProEcoEncoding(true);
      }
    },
    SECOND {
      @Override
      void confirm(MobilityServiceImpl ucc, int id, int version) {
        ucc.confirmSecondSoftwareEncoding(id, version);
      }

      @Override
      boolean isEncoded(Mobility mobility) {
        return mobility.isEncodedInSecondSoftware();
      }

      @Override
      void setEncoded(Mobility mobility) {
        mobility.setSecondSoftwareEncoding(true);
      }
    };

    abstract void confirm(MobilityServiceImpl ucc, int id, int version);

    abstract boolean isEncoded(Mobility mobility);

    abstract void setEncoded(Mobility mobility);

    Software other() {
      return this == PRO_ECO ? SECOND : PRO_ECO;
    }
  }

  @Nested
  class ConfirmEncoding {

    @ParameterizedTest
    @EnumSource(Software.class)
    void confirmsTheEncodingOfThatSoftwareOnly(Software software) {
      Mobility mobility = givenMobility(MobilityState.IN_PROGRESS);
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
      Mobility mobility = givenMobility(MobilityState.CLOSED);
      givenUpdateSucceeds();

      software.confirm(mobilityService, MOBILITY_ID, VERSION);

      assertThat(software.isEncoded(mobility)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(Software.class)
    void confirmingAnEncodingTwiceWritesNothing(Software software) {
      Mobility mobility = givenMobility(MobilityState.IN_PROGRESS);
      software.setEncoded(mobility);

      software.confirm(mobilityService, MOBILITY_ID, VERSION);

      assertThat(software.isEncoded(mobility)).isTrue();
      verify(mobilityRepository, never()).update(any());
    }

    @ParameterizedTest
    @EnumSource(Software.class)
    void aCancelledMobilityCannotBeEncoded(Software software) {
      Mobility mobility = givenMobility(MobilityState.CANCELLED);

      assertThat(
              Violations.errorCodeOf(() -> software.confirm(mobilityService, MOBILITY_ID, VERSION)))
          .isEqualTo(ErrorCode.MOBILITY_CANCELLED);
      assertThat(software.isEncoded(mobility)).isFalse();
      verify(mobilityRepository, never()).update(any());
    }

    @ParameterizedTest
    @EnumSource(Software.class)
    void aStaleVersionIsAConcurrentModification(Software software) {
      givenMobility(MobilityState.IN_PROGRESS);

      assertThatThrownBy(() -> software.confirm(mobilityService, MOBILITY_ID, VERSION - 1))
          .isInstanceOf(ConcurrentModificationException.class);
      verify(mobilityRepository, never()).update(any());
    }

    @ParameterizedTest
    @EnumSource(Software.class)
    void anUnknownMobilityIsNotFound(Software software) {
      assertThat(
              Violations.errorCodeOf(() -> software.confirm(mobilityService, MOBILITY_ID, VERSION)))
          .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }
  }

  @Nested
  class ConfirmPayment {

    @Test
    void theFirstPaymentPutsTheMobilityInProgress() {
      Mobility mobility = givenMobility(MobilityState.TO_BE_PAID);
      when(nominatedStudentRepository.findById(STUDENT_ID))
          .thenReturn(Optional.of(nominatedStudent("BE68539007547034", "Belfius", "GKCCBEBB")));
      when(mobilityDocumentRepository.findAllByMobility(MOBILITY_ID))
          .thenReturn(
              List.of(
                  document(1, DocumentCategory.DEPARTURE, true),
                  document(3, DocumentCategory.RETURN, false)));
      givenUpdateSucceeds();

      Mobility paid = mobilityService.confirmPayment(MOBILITY_ID, VERSION);

      assertThat(paid).isSameAs(mobility);
      assertThat(paid.getState()).isEqualTo(MobilityState.IN_PROGRESS);
      assertThat(paid.getFirstPaymentRequestDate()).isEqualTo(NOW);
      assertThat(paid.getSecondPaymentRequestDate()).isNull();
      assertThat(paid.getVersion()).isEqualTo(VERSION + 1);
    }

    @Test
    void theBalancePaymentClosesTheMobility() {
      Mobility mobility = givenMobility(MobilityState.BALANCE_TO_BE_PAID);
      LocalDateTime firstPayment = LocalDateTime.of(2016, 9, 1, 12, 0);
      mobility.setFirstPaymentRequestDate(firstPayment);
      when(nominatedStudentRepository.findById(STUDENT_ID))
          .thenReturn(Optional.of(nominatedStudent("BE68539007547034", "Belfius", "GKCCBEBB")));
      givenUpdateSucceeds();

      Mobility paid = mobilityService.confirmPayment(MOBILITY_ID, VERSION);

      assertThat(paid.getState()).isEqualTo(MobilityState.CLOSED);
      assertThat(paid.getFirstPaymentRequestDate()).isEqualTo(firstPayment);
      assertThat(paid.getSecondPaymentRequestDate()).isEqualTo(NOW);
      verify(mobilityRepository).update(mobility);
    }

    @Test
    void theFirstPaymentMakesTheBalanceDueWhenTheReturnDocumentsAreAlreadyFilledIn() {
      // the return documents may be ticked while the first payment is pending ("A payer"):
      // "En cours" would then be a dead end, no document being left to fill in
      Mobility mobility = givenMobility(MobilityState.TO_BE_PAID);
      when(nominatedStudentRepository.findById(STUDENT_ID))
          .thenReturn(Optional.of(nominatedStudent("BE68539007547034", "Belfius", "GKCCBEBB")));
      when(mobilityDocumentRepository.findAllByMobility(MOBILITY_ID))
          .thenReturn(
              List.of(
                  document(1, DocumentCategory.DEPARTURE, true),
                  document(3, DocumentCategory.RETURN, true)));
      givenUpdateSucceeds();

      Mobility paid = mobilityService.confirmPayment(MOBILITY_ID, VERSION);

      assertThat(paid.getState()).isEqualTo(MobilityState.BALANCE_TO_BE_PAID);
      assertThat(paid.getFirstPaymentRequestDate()).isNotNull();
      assertThat(paid.getSecondPaymentRequestDate()).isNull();
    }

    @ParameterizedTest
    @EnumSource(
        value = MobilityState.class,
        names = {"CREATED", "IN_PREPARATION", "IN_PROGRESS", "BALANCE_TO_BE_PAID"})
    void noFirstPaymentBeforeTheDepartureDocumentsAreFilledIn(MobilityState state) {
      // the first payment is only expected in "A payer": before, the departure documents are
      // incomplete; "En cours" and "Solde à payer" without a first payment are inconsistent
      Mobility mobility = givenMobility(state);

      assertThat(Violations.errorCodeOf(() -> mobilityService.confirmPayment(MOBILITY_ID, VERSION)))
          .isEqualTo(ErrorCode.PAYMENT_NOT_EXPECTED);
      assertThat(mobility.getState()).isEqualTo(state);
      assertThat(mobility.getFirstPaymentRequestDate()).isNull();
      verifyNoInteractions(nominatedStudentRepository);
      verify(mobilityRepository, never()).update(any());
    }

    @ParameterizedTest
    @EnumSource(
        value = MobilityState.class,
        names = {"IN_PROGRESS", "TO_BE_PAID", "IN_PREPARATION"})
    void noSecondPaymentBeforeTheBalanceIsDue(MobilityState state) {
      Mobility mobility = givenMobility(state);
      mobility.setFirstPaymentRequestDate(LocalDateTime.of(2016, 9, 1, 12, 0));

      assertThat(Violations.errorCodeOf(() -> mobilityService.confirmPayment(MOBILITY_ID, VERSION)))
          .isEqualTo(ErrorCode.PAYMENT_NOT_EXPECTED);
      verifyNoInteractions(nominatedStudentRepository);
      assertThat(mobility.getState()).isEqualTo(state);
      assertThat(mobility.getSecondPaymentRequestDate()).isNull();
      verify(mobilityRepository, never()).update(any());
    }

    static Stream<Arguments> closedStates() {
      return Stream.of(
          Arguments.of(MobilityState.CANCELLED, ErrorCode.MOBILITY_CANCELLED),
          Arguments.of(MobilityState.CLOSED, ErrorCode.MOBILITY_CLOSED));
    }

    @ParameterizedTest
    @MethodSource("closedStates")
    void aCancelledOrClosedMobilityIsNotPaid(MobilityState state, ErrorCode expected) {
      givenMobility(state);

      assertThat(Violations.errorCodeOf(() -> mobilityService.confirmPayment(MOBILITY_ID, VERSION)))
          .isEqualTo(expected);
      verifyNoInteractions(nominatedStudentRepository);
      verify(mobilityRepository, never()).update(any());
    }

    @Test
    void aStudentWithoutPersonalDataHasNoBankDetails() {
      givenMobility(MobilityState.TO_BE_PAID);

      assertThat(Violations.errorCodeOf(() -> mobilityService.confirmPayment(MOBILITY_ID, VERSION)))
          .isEqualTo(ErrorCode.INCOMPLETE_BANK_DETAILS);
      verify(mobilityRepository, never()).update(any());
    }

    static Stream<Arguments> incompleteBankDetails() {
      return Stream.of(
          Arguments.of(null, "Belfius", "GKCCBEBB"),
          Arguments.of(" ", "Belfius", "GKCCBEBB"),
          Arguments.of("BE68539007547034", null, "GKCCBEBB"),
          Arguments.of("BE68539007547034", "", "GKCCBEBB"),
          Arguments.of("BE68539007547034", "Belfius", null));
    }

    @ParameterizedTest
    @MethodSource("incompleteBankDetails")
    void theBankDetailsMustBeComplete(String iban, String bankName, String bic) {
      Mobility mobility = givenMobility(MobilityState.TO_BE_PAID);
      when(nominatedStudentRepository.findById(STUDENT_ID))
          .thenReturn(Optional.of(nominatedStudent(iban, bankName, bic)));

      assertThat(Violations.errorCodeOf(() -> mobilityService.confirmPayment(MOBILITY_ID, VERSION)))
          .isEqualTo(ErrorCode.INCOMPLETE_BANK_DETAILS);
      assertThat(mobility.getFirstPaymentRequestDate()).isNull();
      verify(mobilityRepository, never()).update(any());
    }

    @Test
    void aStaleVersionIsAConcurrentModification() {
      givenMobility(MobilityState.TO_BE_PAID);

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

    private static final DocumentCategory D = DocumentCategory.DEPARTURE;
    private static final DocumentCategory R = DocumentCategory.RETURN;

    /**
     * State transitions: the mobility, its documents (id 1 and 2 departure, 3 and 4 return,
     * filled-in or not) and the document filled in, the state after.
     */
    static Stream<Arguments> transitions() {
      return Stream.of(
          // the first document starts the preparation...
          Arguments.of(MobilityState.CREATED, "----", 1, MobilityState.IN_PREPARATION),
          // ...or makes the first payment due at once when it completes the departure documents
          Arguments.of(MobilityState.CREATED, "-X--", 1, MobilityState.TO_BE_PAID),
          Arguments.of(MobilityState.CREATED, "-XX-", 1, MobilityState.TO_BE_PAID),
          Arguments.of(MobilityState.CREATED, "----", 3, MobilityState.IN_PREPARATION),
          // the last departure document makes the first payment due
          Arguments.of(MobilityState.IN_PREPARATION, "X---", 2, MobilityState.TO_BE_PAID),
          Arguments.of(MobilityState.IN_PREPARATION, "----", 1, MobilityState.IN_PREPARATION),
          Arguments.of(MobilityState.IN_PREPARATION, "X---", 3, MobilityState.IN_PREPARATION),
          // return documents do not change a mobility waiting for its first payment
          Arguments.of(MobilityState.TO_BE_PAID, "XX--", 3, MobilityState.TO_BE_PAID),
          // the last return document makes the balance due
          Arguments.of(MobilityState.IN_PROGRESS, "XXX-", 4, MobilityState.BALANCE_TO_BE_PAID),
          Arguments.of(MobilityState.IN_PROGRESS, "XX--", 3, MobilityState.IN_PROGRESS),
          Arguments.of(MobilityState.IN_PROGRESS, "-XXX", 1, MobilityState.BALANCE_TO_BE_PAID),
          Arguments.of(MobilityState.IN_PROGRESS, "--XX", 1, MobilityState.IN_PROGRESS),
          Arguments.of(
              MobilityState.BALANCE_TO_BE_PAID, "XX-X", 3, MobilityState.BALANCE_TO_BE_PAID));
    }

    private List<Document> documents(String filledIn) {
      DocumentCategory[] categories = {D, D, R, R};
      List<Document> documents = new ArrayList<>();
      for (int i = 0; i < categories.length; i++) {
        documents.add(document(i + 1, categories[i], filledIn.charAt(i) == 'X'));
      }
      return documents;
    }

    @ParameterizedTest(name = "{0} with {1}, fill in {2} -> {3}")
    @MethodSource("transitions")
    void fillingInADocumentMovesTheMobilityThroughItsStates(
        MobilityState state, String filledIn, int document, MobilityState expectedState) {
      Mobility mobility = givenMobility(state);
      List<Document> documents = documents(filledIn);
      when(mobilityDocumentRepository.findAllByMobility(MOBILITY_ID)).thenReturn(documents);
      givenUpdateSucceeds();

      Mobility updated = mobilityService.confirmDocument(MOBILITY_ID, document, VERSION);

      assertThat(updated).isSameAs(mobility);
      assertThat(updated.getState()).isEqualTo(expectedState);
      assertThat(updated.getVersion()).isEqualTo(VERSION + 1);
      assertThat(updated.getDocuments())
          .filteredOn(d -> d.getId() == document)
          .singleElement()
          .matches(Document::isFilledIn);
      verify(mobilityDocumentRepository).fillInDocument(document, MOBILITY_ID);
    }

    @Test
    void aDocumentAlreadyFilledInChangesNothing() {
      Mobility mobility = givenMobility(MobilityState.IN_PREPARATION);
      when(mobilityDocumentRepository.findAllByMobility(MOBILITY_ID)).thenReturn(documents("X---"));

      Mobility unchanged = mobilityService.confirmDocument(MOBILITY_ID, 1, VERSION);

      assertThat(unchanged).isSameAs(mobility);
      assertThat(unchanged.getState()).isEqualTo(MobilityState.IN_PREPARATION);
      assertThat(unchanged.getVersion()).isEqualTo(VERSION);
      verify(mobilityDocumentRepository, never()).fillInDocument(1, MOBILITY_ID);
      verify(mobilityRepository, never()).update(any());
    }

    @ParameterizedTest
    @MethodSource(
        "com.dragomitch.ipl.pae.application.MobilityServiceImplTest$ConfirmPayment#closedStates")
    void theDocumentsOfACancelledOrClosedMobilityAreFrozen(
        MobilityState state, ErrorCode expected) {
      Mobility mobility = givenMobility(state);

      assertThat(
              Violations.errorCodeOf(
                  () -> mobilityService.confirmDocument(MOBILITY_ID, 1, VERSION)))
          .isEqualTo(expected);
      assertThat(mobility.getState()).isEqualTo(state);
      verifyNoInteractions(mobilityDocumentRepository);
      verify(mobilityRepository, never()).update(any());
    }

    @Test
    void aDocumentThatIsNotRequiredByTheMobilityIsUnknown() {
      givenMobility(MobilityState.IN_PREPARATION);
      when(mobilityDocumentRepository.findAllByMobility(MOBILITY_ID)).thenReturn(documents("----"));

      assertThat(
              Violations.errorCodeOf(
                  () -> mobilityService.confirmDocument(MOBILITY_ID, 99, VERSION)))
          .isEqualTo(ErrorCode.UNKNOWN_DOCUMENT);
      verify(mobilityRepository, never()).update(any());
    }

    @Test
    void aStaleVersionIsAConcurrentModification() {
      givenMobility(MobilityState.IN_PREPARATION);

      assertThatThrownBy(() -> mobilityService.confirmDocument(MOBILITY_ID, 1, VERSION - 1))
          .isInstanceOf(ConcurrentModificationException.class);
      verifyNoInteractions(mobilityDocumentRepository);
    }

    @Test
    void anUnknownMobilityIsNotFound() {
      assertThat(
              Violations.errorCodeOf(
                  () -> mobilityService.confirmDocument(MOBILITY_ID, 1, VERSION)))
          .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }
  }

  @Nested
  class Cancel {

    private DenialReason givenDenialReason(int id) {
      DenialReason denialReason = new DenialReason();
      denialReason.setId(id);
      denialReason.setReason("Dossier incomplet");
      when(denialReasonRepository.findById(id)).thenReturn(Optional.of(denialReason));
      return denialReason;
    }

    @ParameterizedTest
    @EnumSource(
        value = MobilityState.class,
        names = {"CREATED", "IN_PREPARATION", "TO_BE_PAID", "IN_PROGRESS", "BALANCE_TO_BE_PAID"})
    void aProfessorCancelsWithADenialReason(MobilityState state) {
      givenMobility(state);
      DenialReason denialReason = givenDenialReason(4);
      givenUpdateSucceeds();

      Mobility cancelled =
          mobilityService.cancel(MOBILITY_ID, VERSION, null, 4, PROFESSOR_ID, Role.PROFESSOR);

      assertThat(cancelled.getState()).isEqualTo(MobilityState.CANCELLED);
      assertThat(cancelled.getStateBeforeCancellation()).isEqualTo(state);
      assertThat(cancelled.getDenialReason()).isSameAs(denialReason);
      assertThat(cancelled.getCancellationReason()).isNull();
      assertThat(cancelled.getVersion()).isEqualTo(VERSION + 1);
    }

    @ParameterizedTest
    @EnumSource(
        value = MobilityState.class,
        names = {"CREATED", "IN_PROGRESS"})
    void theOwnerCancelsWithAReason(MobilityState state) {
      givenMobility(state);
      givenUpdateSucceeds();

      Mobility cancelled =
          mobilityService.cancel(
              MOBILITY_ID, VERSION, "Raisons médicales", 0, STUDENT_ID, Role.STUDENT);

      assertThat(cancelled.getState()).isEqualTo(MobilityState.CANCELLED);
      assertThat(cancelled.getStateBeforeCancellation()).isEqualTo(state);
      assertThat(cancelled.getCancellationReason()).isEqualTo("Raisons médicales");
      assertThat(cancelled.getDenialReason()).isNull();
      verifyNoInteractions(denialReasonRepository);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void aProfessorMustGiveADenialReason(int denialReasonId) {
      Mobility mobility = givenMobility(MobilityState.CREATED);

      assertThat(
              Violations.errorCodeOf(() ->
                  mobilityService.cancel(
                      MOBILITY_ID,
                      VERSION,
                      "a free-text reason is not enough",
                      denialReasonId,
                      PROFESSOR_ID,
                      Role.PROFESSOR)))
          .isEqualTo(ErrorCode.DENIAL_REASON_REQUIRED);
      assertThat(mobility.getState()).isEqualTo(MobilityState.CREATED);
      verify(mobilityRepository, never()).update(any());
    }

    @Test
    void aProfessorMustGiveAnExistingDenialReason() {
      givenMobility(MobilityState.CREATED);

      assertThatBusinessException(() ->
              mobilityService.cancel(MOBILITY_ID, VERSION, null, 99, PROFESSOR_ID, Role.PROFESSOR))
          .hasErrorCode(ErrorCode.UNKNOWN_DENIAL_REASON)
          .hasArguments(99);
      verify(mobilityRepository, never()).update(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void aStudentMustGiveACancellationReason(String reason) {
      Mobility mobility = givenMobility(MobilityState.CREATED);

      assertThat(
              Violations.errorCodeOf(() ->
                  mobilityService.cancel(
                      MOBILITY_ID, VERSION, reason, 4, STUDENT_ID, Role.STUDENT)))
          .isEqualTo(ErrorCode.CANCELLATION_REASON_REQUIRED);
      assertThat(mobility.getState()).isEqualTo(MobilityState.CREATED);
      verify(mobilityRepository, never()).update(any());
    }

    @Test
    void aStudentCannotCancelTheMobilityOfAnotherStudent() {
      Mobility mobility = givenMobility(MobilityState.CREATED);

      assertThat(
              Violations.errorCodeOf(() ->
                  mobilityService.cancel(
                      MOBILITY_ID, VERSION, "reason", 0, STUDENT_ID + 1, Role.STUDENT)))
          .isEqualTo(ErrorCode.ACCESS_DENIED);
      assertThat(mobility.getState()).isEqualTo(MobilityState.CREATED);
      verify(mobilityRepository, never()).update(any());
    }

    @Test
    void cancellingACancelledMobilityAgainChangesNothing() {
      Mobility mobility = givenMobility(MobilityState.CANCELLED);
      mobility.setStateBeforeCancellation(MobilityState.IN_PROGRESS);
      mobility.setCancellationReason("first reason");

      Mobility unchanged =
          mobilityService.cancel(
              MOBILITY_ID, VERSION, "second reason", 0, STUDENT_ID, Role.STUDENT);

      assertThat(unchanged).isSameAs(mobility);
      assertThat(unchanged.getCancellationReason()).isEqualTo("first reason");
      assertThat(unchanged.getStateBeforeCancellation()).isEqualTo(MobilityState.IN_PROGRESS);
      verify(mobilityRepository, never()).update(any());
    }

    @ParameterizedTest
    @EnumSource(
        value = MobilityState.class,
        names = {"CANCELLED", "CLOSED"})
    void aStudentLearnsNothingAboutTheMobilityOfAnotherStudent(MobilityState state) {
      Mobility mobility = givenMobility(state);
      mobility.setCancellationReason("personal reason of the owner");

      assertThat(
              Violations.errorCodeOf(() ->
                  mobilityService.cancel(
                      MOBILITY_ID, VERSION, "reason", 0, STUDENT_ID + 1, Role.STUDENT)))
          .isEqualTo(ErrorCode.ACCESS_DENIED);
      verify(mobilityRepository, never()).update(any());
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void aClosedMobilityCannotBeCancelled(Role role) {
      givenMobility(MobilityState.CLOSED);

      assertThat(
              Violations.errorCodeOf(() ->
                  mobilityService.cancel(MOBILITY_ID, VERSION, "reason", 4, STUDENT_ID, role)))
          .isEqualTo(ErrorCode.MOBILITY_CLOSED);
      verify(mobilityRepository, never()).update(any());
    }

    @Test
    void aStaleVersionIsAConcurrentModification() {
      givenMobility(MobilityState.CREATED);

      assertThatThrownBy(() ->
              mobilityService.cancel(
                  MOBILITY_ID, VERSION - 1, "reason", 0, STUDENT_ID, Role.STUDENT))
          .isInstanceOf(ConcurrentModificationException.class);
      verify(mobilityRepository, never()).update(any());
    }

    @Test
    void anUnknownMobilityIsNotFound() {
      assertThat(
              Violations.errorCodeOf(() ->
                  mobilityService.cancel(
                      MOBILITY_ID, VERSION, "reason", 0, STUDENT_ID, Role.STUDENT)))
          .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }
  }

  @Nested
  class ExportDocuments {

    private static final String HEADER =
        "﻿"
            + "Nom;Prénom;Partenaire;Type de mobilité;Programme de mobilité;Semestre;"
            + "Contrat de bourse;Convention de stage / Convention d'études;Charte de l'étudiant;"
            + "Document d'engagement;Preuve du passage des tests linguistiques;Attestation séjour;"
            + "Relevé de notes (SMS) ou certificat de stage (SMP);Rapport final (complété en ligne);"
            + "Preuve du passage des tests linguistiques après la mobilité;\n"
            + "Martin;Alice;University of Dublin;SMS;Erasmus+;2;";

    /** The documents of an Erabel mobility: 4 departure and 3 return documents, some filled in. */
    private List<Document> erabelDocuments() {
      return List.of(
          document(1, DocumentCategory.DEPARTURE, true, "Contrat de bourse"),
          document(
              2, DocumentCategory.DEPARTURE, false, "Convention de stage / Convention d'études"),
          document(3, DocumentCategory.DEPARTURE, true, "Charte de l'étudiant"),
          document(4, DocumentCategory.DEPARTURE, false, "Document d'engagement"),
          document(5, DocumentCategory.RETURN, true, "Attestation séjour"),
          document(
              6,
              DocumentCategory.RETURN,
              false,
              "Relevé de notes (SMS) ou certificat de stage (SMP)"),
          document(7, DocumentCategory.RETURN, false, "Rapport final (complété en ligne)"));
    }

    @Test
    void withoutFilterEveryDocumentOfTheMobilityIsExportedInTheColumnsOrder() {
      givenMobility(MobilityState.IN_PROGRESS);
      when(mobilityDocumentRepository.findAllByMobility(MOBILITY_ID)).thenReturn(erabelDocuments());

      String csv = mobilityService.exportDocuments(MOBILITY_ID, null);

      // the language test proofs are not required by Erabel: empty columns
      assertThat(csv)
          .isEqualTo(
              HEADER + "Rempli;Non-rempli;Rempli;Non-rempli;;Rempli;" + "Non-rempli;Non-rempli;;");
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(
        delimiter = '|',
        value = {
          "D  | Rempli;Non-rempli;Rempli;Non-rempli;;;;;;",
          "DF | Rempli;;Rempli;;;;;;;",
          "R  | ;;;;;Rempli;Non-rempli;Non-rempli;;",
          "RF | ;;;;;Rempli;;;;"
        })
    void theFilterKeepsTheDocumentsOfItsCategoryAndOptionallyOnlyTheFilledInOnes(
        DocumentFilter filter, String documentColumns) {
      givenMobility(MobilityState.IN_PROGRESS);
      when(mobilityDocumentRepository.findAllByMobility(MOBILITY_ID)).thenReturn(erabelDocuments());

      assertThat(mobilityService.exportDocuments(MOBILITY_ID, filter))
          .isEqualTo(HEADER + documentColumns);
    }

    @Test
    void aMobilityWithoutDocumentsExportsEmptyColumns() {
      givenMobility(MobilityState.CREATED);
      when(mobilityDocumentRepository.findAllByMobility(MOBILITY_ID)).thenReturn(List.of());

      assertThat(mobilityService.exportDocuments(MOBILITY_ID, null))
          .isEqualTo(HEADER + ";;;;;;;;;");
    }

    @Test
    void anUnknownMobilityIsNotFound() {
      assertThat(Violations.errorCodeOf(() -> mobilityService.exportDocuments(MOBILITY_ID, null)))
          .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
      verifyNoInteractions(mobilityDocumentRepository);
    }
  }
}
