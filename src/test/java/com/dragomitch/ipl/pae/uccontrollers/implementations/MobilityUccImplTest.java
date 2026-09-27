package com.dragomitch.ipl.pae.uccontrollers.implementations;

import static com.dragomitch.ipl.pae.business.exceptions.BusinessExceptionAssert.assertThatBusinessException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.Violations;
import com.dragomitch.ipl.pae.business.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.business.dto.DocumentDto;
import com.dragomitch.ipl.pae.business.dto.MobilityDto;
import com.dragomitch.ipl.pae.business.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.business.implementations.EntityFactories;
import com.dragomitch.ipl.pae.persistence.DenialReasonDao;
import com.dragomitch.ipl.pae.persistence.MobilityDao;
import com.dragomitch.ipl.pae.persistence.MobilityDocumentDao;
import com.dragomitch.ipl.pae.persistence.NominatedStudentDao;
import com.dragomitch.ipl.pae.persistence.UserDao;
import com.dragomitch.ipl.pae.uccontrollers.PartnerUcc;
import com.dragomitch.ipl.pae.uccontrollers.ProgrammeUcc;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Business rules of the mobility lifecycle: encodings, payments, documents, cancellation, CSV
 * export. The DAOs are Mockito mocks answering as their contract ({@code MobilityDaoIT}...) says:
 * {@code findById} returns the mobility with only the ids of its student, partner, programme and
 * professor, {@code update} increments the version of the DTO it returns.
 */
@ExtendWith(MockitoExtension.class)
class MobilityUccImplTest {

  private static final int MOBILITY_ID = 7;
  private static final int VERSION = 3;
  private static final int STUDENT_ID = 42;
  private static final int PROFESSOR_ID = 2;
  private static final int PARTNER_ID = 5;
  private static final int PROGRAMME_ID = 1;

  @Mock
  private MobilityDao mobilityDao;
  @Mock
  private NominatedStudentDao nominatedStudentDao;
  @Mock
  private DenialReasonDao denialReasonDao;
  @Mock
  private MobilityDocumentDao mobilityDocumentDao;
  @Mock
  private PartnerUcc partnerUcc;
  @Mock
  private ProgrammeUcc programmeUcc;
  @Mock
  private UserDao userDao;

  private final EntityFactory entityFactory = EntityFactories.create();
  private MobilityUccImpl mobilityUcc;

  @BeforeEach
  void setUp() {
    mobilityUcc = new MobilityUccImpl(mobilityDao, nominatedStudentDao, denialReasonDao,
        mobilityDocumentDao, partnerUcc, programmeUcc, userDao);
  }

  /** A mobility as {@link MobilityDao#findById} returns it. */
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
    when(mobilityDao.findById(MOBILITY_ID)).thenReturn(mobility);
    return mobility;
  }

  /** {@link MobilityDao#update} checks nothing here and increments the version, as the DAO. */
  private void givenUpdateSucceeds() {
    when(mobilityDao.update(any())).thenAnswer(invocation -> {
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
      when(mobilityDao.findAll()).thenReturn(all);

      assertThat(mobilityUcc.showAll(PROFESSOR_ID, UserDto.ROLE_PROFESSOR)).isSameAs(all);
      verify(mobilityDao, never()).findByUser(PROFESSOR_ID);
    }

    @Test
    void aStudentSeesOnlyTheirOwnMobilities() {
      List<MobilityDto> own = List.of(storedMobility(MobilityDto.STATE_CREATED));
      when(mobilityDao.findByUser(STUDENT_ID)).thenReturn(own);

      assertThat(mobilityUcc.showAll(STUDENT_ID, UserDto.ROLE_STUDENT)).isSameAs(own);
      verify(mobilityDao, never()).findAll();
    }
  }

  @Nested
  class ShowOne {

    @Test
    void anUnknownMobilityIsNotFound() {
      assertThat(Violations.errorCodeOf(
          () -> mobilityUcc.showOne(MOBILITY_ID, UserDto.ROLE_PROFESSOR, PROFESSOR_ID)))
          .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void aStudentCannotSeeTheMobilityOfAnotherStudent() {
      givenMobility(MobilityDto.STATE_CREATED);

      assertThat(Violations.errorCodeOf(
          () -> mobilityUcc.showOne(MOBILITY_ID, UserDto.ROLE_STUDENT, STUDENT_ID + 1)))
          .isEqualTo(ErrorCode.ACCESS_DENIED);
      verifyNoInteractions(mobilityDocumentDao, partnerUcc, programmeUcc, userDao);
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
      when(mobilityDocumentDao.findAllByMobility(MOBILITY_ID)).thenReturn(documents);
      NominatedStudentDto student = nominatedStudent("BE68539007547034", "Belfius", "GKCCBEBB");
      when(nominatedStudentDao.findById(STUDENT_ID)).thenReturn(student);
      PartnerDto partner = (PartnerDto) entityFactory.build(PartnerDto.class);
      when(partnerUcc.showOne(PARTNER_ID)).thenReturn(partner);
      ProgrammeDto programme = (ProgrammeDto) entityFactory.build(ProgrammeDto.class);
      when(programmeUcc.showOne(PROGRAMME_ID)).thenReturn(programme);
      UserDto professor = (UserDto) entityFactory.build(UserDto.class);
      when(userDao.findById(PROFESSOR_ID)).thenReturn(professor);

      MobilityDto shown = mobilityUcc.showOne(MOBILITY_ID, role, requester);

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
      when(mobilityDocumentDao.findAllByMobility(MOBILITY_ID)).thenReturn(List.of());

      MobilityDto shown = mobilityUcc.showOne(MOBILITY_ID, UserDto.ROLE_STUDENT, STUDENT_ID);

      assertThat(shown.getProfessorInCharge()).isNull();
      verifyNoInteractions(userDao);
    }

    @Test
    void theStudentSummaryIsKeptWhenThePersonalDataAreNotRecorded() {
      MobilityDto stored = givenMobility(MobilityDto.STATE_CREATED);
      NominatedStudentDto summary = stored.getNominatedStudent();
      when(mobilityDocumentDao.findAllByMobility(MOBILITY_ID)).thenReturn(List.of());

      MobilityDto shown = mobilityUcc.showOne(MOBILITY_ID, UserDto.ROLE_STUDENT, STUDENT_ID);

      assertThat(shown.getNominatedStudent()).isSameAs(summary);
      assertThat(shown.getNominatedStudent().getLastName()).isEqualTo("Martin");
    }
  }

  /** The two external software encodings behave the same way. */
  enum Software {
    PRO_ECO {
      @Override
      void confirm(MobilityUccImpl ucc, int id, int version) {
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
      void confirm(MobilityUccImpl ucc, int id, int version) {
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

    abstract void confirm(MobilityUccImpl ucc, int id, int version);

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

      software.confirm(mobilityUcc, MOBILITY_ID, VERSION);

      assertThat(software.isEncoded(mobility)).isTrue();
      assertThat(software.other().isEncoded(mobility)).isFalse();
      assertThat(mobility.getVersion()).isEqualTo(VERSION + 1);
      verify(mobilityDao).update(mobility);
    }

    @ParameterizedTest
    @EnumSource(Software.class)
    void aClosedMobilityCanStillBeEncoded(Software software) {
      MobilityDto mobility = givenMobility(MobilityDto.STATE_CLOSED);
      givenUpdateSucceeds();

      software.confirm(mobilityUcc, MOBILITY_ID, VERSION);

      assertThat(software.isEncoded(mobility)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(Software.class)
    void confirmingAnEncodingTwiceWritesNothing(Software software) {
      MobilityDto mobility = givenMobility(MobilityDto.STATE_IN_PROGRESS);
      software.setEncoded(mobility);

      software.confirm(mobilityUcc, MOBILITY_ID, VERSION);

      assertThat(software.isEncoded(mobility)).isTrue();
      verify(mobilityDao, never()).update(any());
    }

    @ParameterizedTest
    @EnumSource(Software.class)
    void aCancelledMobilityCannotBeEncoded(Software software) {
      MobilityDto mobility = givenMobility(MobilityDto.STATE_CANCELLED);

      assertThat(Violations.errorCodeOf(() -> software.confirm(mobilityUcc, MOBILITY_ID, VERSION)))
          .isEqualTo(ErrorCode.MOBILITY_CANCELLED);
      assertThat(software.isEncoded(mobility)).isFalse();
      verify(mobilityDao, never()).update(any());
    }

    @ParameterizedTest
    @EnumSource(Software.class)
    void aStaleVersionIsAConcurrentModification(Software software) {
      givenMobility(MobilityDto.STATE_IN_PROGRESS);

      assertThatThrownBy(() -> software.confirm(mobilityUcc, MOBILITY_ID, VERSION - 1))
          .isInstanceOf(ConcurrentModificationException.class);
      verify(mobilityDao, never()).update(any());
    }

    @ParameterizedTest
    @EnumSource(Software.class)
    void anUnknownMobilityIsNotFound(Software software) {
      assertThat(Violations.errorCodeOf(() -> software.confirm(mobilityUcc, MOBILITY_ID, VERSION)))
          .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }
  }

  @Nested
  class ConfirmPayment {

    @Test
    void theFirstPaymentPutsTheMobilityInProgress() {
      MobilityDto mobility = givenMobility(MobilityDto.STATE_TO_BE_PAID);
      when(nominatedStudentDao.findById(STUDENT_ID))
          .thenReturn(nominatedStudent("BE68539007547034", "Belfius", "GKCCBEBB"));
      givenUpdateSucceeds();

      MobilityDto paid = mobilityUcc.confirmPayment(MOBILITY_ID, VERSION);

      assertThat(paid).isSameAs(mobility);
      assertThat(paid.getState()).isEqualTo(MobilityDto.STATE_IN_PROGRESS);
      assertThat(paid.getFirstPaymentRequestDate())
          .isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES));
      assertThat(paid.getSecondPaymentRequestDate()).isNull();
      assertThat(paid.getVersion()).isEqualTo(VERSION + 1);
    }

    @Test
    void theBalancePaymentClosesTheMobility() {
      MobilityDto mobility = givenMobility(MobilityDto.STATE_BALANCE_TO_BE_PAID);
      LocalDateTime firstPayment = LocalDateTime.of(2016, 9, 1, 12, 0);
      mobility.setFirstPaymentRequestDate(firstPayment);
      when(nominatedStudentDao.findById(STUDENT_ID))
          .thenReturn(nominatedStudent("BE68539007547034", "Belfius", "GKCCBEBB"));
      givenUpdateSucceeds();

      MobilityDto paid = mobilityUcc.confirmPayment(MOBILITY_ID, VERSION);

      assertThat(paid.getState()).isEqualTo(MobilityDto.STATE_CLOSED);
      assertThat(paid.getFirstPaymentRequestDate()).isEqualTo(firstPayment);
      assertThat(paid.getSecondPaymentRequestDate())
          .isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES));
      verify(mobilityDao).update(mobility);
    }

    @ParameterizedTest
    @ValueSource(strings = {MobilityDto.STATE_IN_PROGRESS, MobilityDto.STATE_TO_BE_PAID,
        MobilityDto.STATE_IN_PREPARATION})
    void noSecondPaymentBeforeTheBalanceIsDue(String state) {
      MobilityDto mobility = givenMobility(state);
      mobility.setFirstPaymentRequestDate(LocalDateTime.of(2016, 9, 1, 12, 0));
      when(nominatedStudentDao.findById(STUDENT_ID))
          .thenReturn(nominatedStudent("BE68539007547034", "Belfius", "GKCCBEBB"));

      assertThat(Violations.errorCodeOf(() -> mobilityUcc.confirmPayment(MOBILITY_ID, VERSION)))
          .isEqualTo(ErrorCode.PAYMENT_NOT_EXPECTED);
      assertThat(mobility.getState()).isEqualTo(state);
      assertThat(mobility.getSecondPaymentRequestDate()).isNull();
      verify(mobilityDao, never()).update(any());
    }

    static Stream<Arguments> closedStates() {
      return Stream.of(Arguments.of(MobilityDto.STATE_CANCELLED, ErrorCode.MOBILITY_CANCELLED),
          Arguments.of(MobilityDto.STATE_CLOSED, ErrorCode.MOBILITY_CLOSED));
    }

    @ParameterizedTest
    @MethodSource("closedStates")
    void aCancelledOrClosedMobilityIsNotPaid(String state, ErrorCode expected) {
      givenMobility(state);

      assertThat(Violations.errorCodeOf(() -> mobilityUcc.confirmPayment(MOBILITY_ID, VERSION)))
          .isEqualTo(expected);
      verifyNoInteractions(nominatedStudentDao);
      verify(mobilityDao, never()).update(any());
    }

    @Test
    void aStudentWithoutPersonalDataHasNoBankDetails() {
      givenMobility(MobilityDto.STATE_TO_BE_PAID);

      assertThat(Violations.errorCodeOf(() -> mobilityUcc.confirmPayment(MOBILITY_ID, VERSION)))
          .isEqualTo(ErrorCode.INCOMPLETE_BANK_DETAILS);
      verify(mobilityDao, never()).update(any());
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
      when(nominatedStudentDao.findById(STUDENT_ID))
          .thenReturn(nominatedStudent(iban, bankName, bic));

      assertThat(Violations.errorCodeOf(() -> mobilityUcc.confirmPayment(MOBILITY_ID, VERSION)))
          .isEqualTo(ErrorCode.INCOMPLETE_BANK_DETAILS);
      assertThat(mobility.getFirstPaymentRequestDate()).isNull();
      verify(mobilityDao, never()).update(any());
    }

    @Test
    void aStaleVersionIsAConcurrentModification() {
      givenMobility(MobilityDto.STATE_TO_BE_PAID);

      assertThatThrownBy(() -> mobilityUcc.confirmPayment(MOBILITY_ID, VERSION + 1))
          .isInstanceOf(ConcurrentModificationException.class);
      verifyNoInteractions(nominatedStudentDao);
    }

    @Test
    void anUnknownMobilityIsNotFound() {
      assertThat(Violations.errorCodeOf(() -> mobilityUcc.confirmPayment(MOBILITY_ID, VERSION)))
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
          // the first document starts the preparation (legacy: even if it completes it)
          Arguments.of(MobilityDto.STATE_CREATED, "----", 1, MobilityDto.STATE_IN_PREPARATION),
          Arguments.of(MobilityDto.STATE_CREATED, "-X--", 1, MobilityDto.STATE_IN_PREPARATION),
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
      when(mobilityDocumentDao.findAllByMobility(MOBILITY_ID)).thenReturn(documents);
      givenUpdateSucceeds();

      MobilityDto updated = mobilityUcc.confirmDocument(MOBILITY_ID, document, VERSION);

      assertThat(updated).isSameAs(mobility);
      assertThat(updated.getState()).isEqualTo(expectedState);
      assertThat(updated.getVersion()).isEqualTo(VERSION + 1);
      assertThat(updated.getDocuments()).filteredOn(d -> d.getId() == document)
          .singleElement().matches(DocumentDto::isFilledIn);
      verify(mobilityDocumentDao).fillInDocument(document, MOBILITY_ID);
    }

    @Test
    void aDocumentAlreadyFilledInChangesNothing() {
      MobilityDto mobility = givenMobility(MobilityDto.STATE_IN_PREPARATION);
      when(mobilityDocumentDao.findAllByMobility(MOBILITY_ID)).thenReturn(documents("X---"));

      MobilityDto unchanged = mobilityUcc.confirmDocument(MOBILITY_ID, 1, VERSION);

      assertThat(unchanged).isSameAs(mobility);
      assertThat(unchanged.getState()).isEqualTo(MobilityDto.STATE_IN_PREPARATION);
      assertThat(unchanged.getVersion()).isEqualTo(VERSION);
      verify(mobilityDocumentDao, never()).fillInDocument(1, MOBILITY_ID);
      verify(mobilityDao, never()).update(any());
    }

    @Test
    void aDocumentThatIsNotRequiredByTheMobilityIsUnknown() {
      givenMobility(MobilityDto.STATE_IN_PREPARATION);
      when(mobilityDocumentDao.findAllByMobility(MOBILITY_ID)).thenReturn(documents("----"));

      assertThat(Violations.errorCodeOf(() -> mobilityUcc.confirmDocument(MOBILITY_ID, 99,
          VERSION))).isEqualTo(ErrorCode.UNKNOWN_DOCUMENT);
      verify(mobilityDao, never()).update(any());
    }

    @Test
    void aStaleVersionIsAConcurrentModification() {
      givenMobility(MobilityDto.STATE_IN_PREPARATION);

      assertThatThrownBy(() -> mobilityUcc.confirmDocument(MOBILITY_ID, 1, VERSION - 1))
          .isInstanceOf(ConcurrentModificationException.class);
      verifyNoInteractions(mobilityDocumentDao);
    }

    @Test
    void anUnknownMobilityIsNotFound() {
      assertThat(Violations.errorCodeOf(() -> mobilityUcc.confirmDocument(MOBILITY_ID, 1,
          VERSION))).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }
  }

  @Nested
  class Cancel {

    private DenialReasonDto givenDenialReason(int id) {
      DenialReasonDto denialReason = (DenialReasonDto) entityFactory.build(DenialReasonDto.class);
      denialReason.setId(id);
      denialReason.setReason("Dossier incomplet");
      when(denialReasonDao.findById(id)).thenReturn(denialReason);
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

      MobilityDto cancelled = mobilityUcc.cancel(MOBILITY_ID, VERSION, null, 4, PROFESSOR_ID,
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

      MobilityDto cancelled = mobilityUcc.cancel(MOBILITY_ID, VERSION, "Raisons médicales", 0,
          STUDENT_ID, UserDto.ROLE_STUDENT);

      assertThat(cancelled.getState()).isEqualTo(MobilityDto.STATE_CANCELLED);
      assertThat(cancelled.getStateBeforeCancellation()).isEqualTo(state);
      assertThat(cancelled.getCancellationReason()).isEqualTo("Raisons médicales");
      assertThat(cancelled.getDenialReason()).isNull();
      verifyNoInteractions(denialReasonDao);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void aProfessorMustGiveADenialReason(int denialReasonId) {
      MobilityDto mobility = givenMobility(MobilityDto.STATE_CREATED);

      assertThat(Violations.errorCodeOf(() -> mobilityUcc.cancel(MOBILITY_ID, VERSION,
          "a free-text reason is not enough", denialReasonId, PROFESSOR_ID,
          UserDto.ROLE_PROFESSOR))).isEqualTo(ErrorCode.DENIAL_REASON_REQUIRED);
      assertThat(mobility.getState()).isEqualTo(MobilityDto.STATE_CREATED);
      verify(mobilityDao, never()).update(any());
    }

    @Test
    void aProfessorMustGiveAnExistingDenialReason() {
      givenMobility(MobilityDto.STATE_CREATED);

      assertThatBusinessException(() -> mobilityUcc.cancel(MOBILITY_ID,
          VERSION, null, 99, PROFESSOR_ID, UserDto.ROLE_PROFESSOR))
          .hasErrorCode(ErrorCode.UNKNOWN_DENIAL_REASON).hasArguments(99);
      verify(mobilityDao, never()).update(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void aStudentMustGiveACancellationReason(String reason) {
      MobilityDto mobility = givenMobility(MobilityDto.STATE_CREATED);

      assertThat(Violations.errorCodeOf(() -> mobilityUcc.cancel(MOBILITY_ID, VERSION, reason, 4,
          STUDENT_ID, UserDto.ROLE_STUDENT)))
          .isEqualTo(ErrorCode.CANCELLATION_REASON_REQUIRED);
      assertThat(mobility.getState()).isEqualTo(MobilityDto.STATE_CREATED);
      verify(mobilityDao, never()).update(any());
    }

    @Test
    void aStudentCannotCancelTheMobilityOfAnotherStudent() {
      MobilityDto mobility = givenMobility(MobilityDto.STATE_CREATED);

      assertThat(Violations.errorCodeOf(() -> mobilityUcc.cancel(MOBILITY_ID, VERSION, "reason",
          0, STUDENT_ID + 1, UserDto.ROLE_STUDENT))).isEqualTo(ErrorCode.ACCESS_DENIED);
      assertThat(mobility.getState()).isEqualTo(MobilityDto.STATE_CREATED);
      verify(mobilityDao, never()).update(any());
    }

    @Test
    void cancellingACancelledMobilityAgainChangesNothing() {
      MobilityDto mobility = givenMobility(MobilityDto.STATE_CANCELLED);
      mobility.setStateBeforeCancellation(MobilityDto.STATE_IN_PROGRESS);
      mobility.setCancellationReason("first reason");

      MobilityDto unchanged = mobilityUcc.cancel(MOBILITY_ID, VERSION, "second reason", 0,
          STUDENT_ID, UserDto.ROLE_STUDENT);

      assertThat(unchanged).isSameAs(mobility);
      assertThat(unchanged.getCancellationReason()).isEqualTo("first reason");
      assertThat(unchanged.getStateBeforeCancellation()).isEqualTo(MobilityDto.STATE_IN_PROGRESS);
      verify(mobilityDao, never()).update(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {MobilityDto.STATE_CANCELLED, MobilityDto.STATE_CLOSED})
    void aStudentLearnsNothingAboutTheMobilityOfAnotherStudent(String state) {
      MobilityDto mobility = givenMobility(state);
      mobility.setCancellationReason("personal reason of the owner");

      assertThat(Violations.errorCodeOf(() -> mobilityUcc.cancel(MOBILITY_ID, VERSION, "reason",
          0, STUDENT_ID + 1, UserDto.ROLE_STUDENT))).isEqualTo(ErrorCode.ACCESS_DENIED);
      verify(mobilityDao, never()).update(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {UserDto.ROLE_PROFESSOR, UserDto.ROLE_STUDENT})
    void aClosedMobilityCannotBeCancelled(String role) {
      givenMobility(MobilityDto.STATE_CLOSED);

      assertThat(Violations.errorCodeOf(() -> mobilityUcc.cancel(MOBILITY_ID, VERSION, "reason",
          4, STUDENT_ID, role))).isEqualTo(ErrorCode.MOBILITY_CLOSED);
      verify(mobilityDao, never()).update(any());
    }

    @Test
    void aStaleVersionIsAConcurrentModification() {
      givenMobility(MobilityDto.STATE_CREATED);

      assertThatThrownBy(() -> mobilityUcc.cancel(MOBILITY_ID, VERSION - 1, "reason", 0,
          STUDENT_ID, UserDto.ROLE_STUDENT)).isInstanceOf(ConcurrentModificationException.class);
      verify(mobilityDao, never()).update(any());
    }

    @Test
    void anUnknownMobilityIsNotFound() {
      assertThat(Violations.errorCodeOf(() -> mobilityUcc.cancel(MOBILITY_ID, VERSION, "reason",
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
      when(mobilityDocumentDao.findAllByMobility(MOBILITY_ID)).thenReturn(erabelDocuments());

      String csv = mobilityUcc.exportDocuments(MOBILITY_ID, null);

      // the language test proofs are not required by Erabel: empty columns
      assertThat(csv).isEqualTo(HEADER + "Rempli;Non-rempli;Rempli;Non-rempli;;Rempli;"
          + "Non-rempli;Non-rempli;;");
    }

    @Test
    void aMobilityWithoutDocumentsExportsEmptyColumns() {
      givenMobility(MobilityDto.STATE_CREATED);
      when(mobilityDocumentDao.findAllByMobility(MOBILITY_ID)).thenReturn(List.of());

      assertThat(mobilityUcc.exportDocuments(MOBILITY_ID, null)).isEqualTo(HEADER + ";;;;;;;;;");
    }

    @Test
    void anUnknownMobilityIsNotFound() {
      assertThat(Violations.errorCodeOf(() -> mobilityUcc.exportDocuments(MOBILITY_ID, null)))
          .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
      verifyNoInteractions(mobilityDocumentDao);
    }
  }
}
