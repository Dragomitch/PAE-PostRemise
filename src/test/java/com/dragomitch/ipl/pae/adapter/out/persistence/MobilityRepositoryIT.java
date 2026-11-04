package com.dragomitch.ipl.pae.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dragomitch.ipl.pae.domain.model.DenialReason;
import com.dragomitch.ipl.pae.domain.model.Mobility;
import com.dragomitch.ipl.pae.domain.model.MobilityState;
import com.dragomitch.ipl.pae.domain.model.MobilityType;
import com.dragomitch.ipl.pae.domain.model.User;
import com.dragomitch.ipl.pae.domain.port.out.MobilityRepository;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ConcurrentModificationException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Mobilities (see db/fixtures/mobilities.sql). A mobility shares its id with the mobility choice it
 * comes from; the choice's partner and country are optional (LEFT-joined, null when absent).
 */
class MobilityRepositoryIT extends AbstractRepositoryIT {

  private static final String MOBILITY_BY_ID =
      "SELECT * FROM student_exchange_tools.mobilities WHERE mobility_choice_id = ?";

  @Autowired private MobilityRepository mobilityRepository;

  @Override
  protected List<String> fixtures() {
    return List.of(
        "db/fixtures/users.sql",
        "db/fixtures/partners.sql",
        "db/fixtures/mobility_choices.sql",
        "db/fixtures/mobilities.sql");
  }

  private Mobility newMobility(int choiceId) {
    Mobility mobility = new Mobility();
    mobility.setId(choiceId);
    mobility.setSubmissionDate(LocalDateTime.of(2025, 4, 1, 12, 0, 30));
    mobility.setState(MobilityState.CREATED);
    mobility.setProEcoEncoding(false);
    mobility.setSecondSoftwareEncoding(false);
    return mobility;
  }

  private DenialReason denialReason(int id) {
    DenialReason reason = new DenialReason();
    reason.setId(id);
    return reason;
  }

  private User user(int id) {
    User user = new User();
    user.setId(id);
    return user;
  }

  @Test
  void findByIdMapsEveryFieldAndJoinsStudentPartnerCountryAndProgramme() {
    Mobility mobility = inTransaction(() -> mobilityRepository.findById(5006).orElse(null));

    assertThat(mobility.getId()).isEqualTo(5006);
    assertThat(mobility.getMobilityType()).isEqualTo(MobilityType.SMP);
    assertThat(mobility.getAcademicYear()).isEqualTo(LocalDate.now().getYear());
    assertThat(mobility.getTerm()).isEqualTo(2);
    assertThat(mobility.getSubmissionDate())
        .as("the mobility's own submission date")
        .isEqualTo(LocalDateTime.of(2025, 1, 10, 10, 0));
    assertThat(mobility.getState()).isEqualTo(MobilityState.IN_PREPARATION);
    assertThat(mobility.getStateBeforeCancellation()).isNull();
    assertThat(mobility.getFirstPaymentRequestDate()).isEqualTo(LocalDateTime.of(2025, 2, 1, 9, 0));
    assertThat(mobility.getSecondPaymentRequestDate()).isNull();
    assertThat(mobility.isEncodedInProEco()).isTrue();
    assertThat(mobility.isEncodedInSecondSoftware()).isFalse();
    assertThat(mobility.getCancellationReason()).isNull();
    assertThat(mobility.getDenialReason()).isNull();
    assertThat(mobility.getProfessorInCharge().getId()).isEqualTo(1002);
    assertThat(mobility.getVersion()).isEqualTo(2);
    assertThat(mobility.getNominatedStudent().getId()).isEqualTo(1004);
    assertThat(mobility.getNominatedStudent().getFirstName()).isEqualTo("David");
    assertThat(mobility.getNominatedStudent().getLastName()).isEqualTo("Petit");
    assertThat(mobility.getNominatedStudent().getOption().getCode()).isEqualTo("BIN");
    assertThat(mobility.getPartner().getId()).isEqualTo(3001);
    assertThat(mobility.getPartner().getFullName()).isEqualTo("Université de Lyon");
    assertThat(mobility.getCountry().getCountryCode()).isEqualTo("FR");
    assertThat(mobility.getCountry().getName()).isEqualTo("France");
    assertThat(mobility.getProgramme().getId()).isEqualTo(1);
    assertThat(mobility.getProgramme().getProgrammeName()).isEqualTo("Erasmus+");
  }

  @Test
  void findByIdMapsACancelledAndDeniedMobility() {
    Mobility mobility = inTransaction(() -> mobilityRepository.findById(5007).orElse(null));

    assertThat(mobility.getState()).isEqualTo(MobilityState.CANCELLED);
    assertThat(mobility.getStateBeforeCancellation()).isEqualTo(MobilityState.CREATED);
    assertThat(mobility.getFirstPaymentRequestDate()).isEqualTo(LocalDateTime.of(2025, 3, 1, 8, 0));
    assertThat(mobility.getSecondPaymentRequestDate())
        .isEqualTo(LocalDateTime.of(2025, 6, 30, 16, 30));
    assertThat(mobility.isEncodedInProEco()).isFalse();
    assertThat(mobility.isEncodedInSecondSoftware()).isTrue();
    assertThat(mobility.getCancellationReason()).isEqualTo("Sick");
    assertThat(mobility.getDenialReason().getId()).isEqualTo(4001);
    assertThat(mobility.getDenialReason().getReason()).isEqualTo("Dossier incomplet");
    assertThat(mobility.getProfessorInCharge()).isNull();
    assertThat(mobility.getCountry().getName()).isEqualTo("Canada");
    assertThat(mobility.getProgramme().getProgrammeName()).isEqualTo("FAME");
    assertThat(mobility.getNominatedStudent().getOption().getCode()).isEqualTo("BCH");
  }

  @ParameterizedTest
  @ValueSource(ints = {5001, 5005, 0, 99999})
  void findByIdReturnsNullForAChoiceThatIsNotAMobility(int id) {
    assertThat(inTransaction(() -> mobilityRepository.findById(id))).isEmpty();
  }

  @Test
  void findAllReturnsEveryMobility() {
    List<Mobility> mobilities = inTransaction(() -> mobilityRepository.findAll());

    assertThat(mobilities).extracting(Mobility::getId).containsExactlyInAnyOrder(5006, 5007);
  }

  @ParameterizedTest(name = "user {0} -> {1}")
  @CsvSource({"1004, 5006", "1003, 5007", "1001, ''", "1002, ''"})
  void findByUserReturnsTheMobilitiesOfThatStudent(int userId, String expected) {
    List<Mobility> mobilities = inTransaction(() -> mobilityRepository.findByUser(userId));

    assertThat(mobilities)
        .extracting(Mobility::getId)
        .containsExactlyElementsOf(
            expected.isEmpty() ? List.of() : List.of(Integer.valueOf(expected)));
  }

  @Test
  void createInsertsTheMobilityWithVersionOne() {
    runInTransaction(() -> {
      Mobility mobility = newMobility(5001);
      mobility.setProfessorInCharge(user(1002));
      mobility.setProEcoEncoding(true);

      Mobility created = mobilityRepository.create(mobility);

      assertThat(created.getVersion()).isEqualTo(1);
      assertThat(queryForRow(MOBILITY_BY_ID, 5001))
          .containsEntry(
              "submission_date", Timestamp.valueOf(LocalDateTime.of(2025, 4, 1, 12, 0, 30)))
          .containsEntry("state", "Créée")
          .containsEntry("state_before_cancellation", null)
          .containsEntry("first_payment_request_date", null)
          .containsEntry("second_payment_request_date", null)
          .containsEntry("pro_eco_encoding", true)
          .containsEntry("second_software_encoding", false)
          .containsEntry("student_cancellation_reason", null)
          .containsEntry("prof_denial_reason", null)
          .containsEntry("professor_in_charge", 1002)
          .containsEntry("version", 1);
      Mobility found = mobilityRepository.findById(5001).orElse(null);
      assertThat(found.getPartner().getId()).isEqualTo(3001);
      assertThat(found.getNominatedStudent().getId()).isEqualTo(1001);
    });
  }

  @Test
  void createStoresPaymentDatesDenialAndCancellation() {
    runInTransaction(() -> {
      Mobility mobility = newMobility(5004);
      mobility.setState(MobilityState.CANCELLED);
      mobility.setStateBeforeCancellation(MobilityState.TO_BE_PAID);
      mobility.setFirstPaymentRequestDate(LocalDateTime.of(2025, 5, 1, 0, 0));
      mobility.setSecondPaymentRequestDate(LocalDateTime.of(2025, 9, 1, 0, 0));
      mobility.setCancellationReason("Abandon");
      mobility.setDenialReason(denialReason(4001));

      mobilityRepository.create(mobility);

      assertThat(queryForRow(MOBILITY_BY_ID, 5004))
          .containsEntry("state_before_cancellation", "A payer")
          .containsEntry(
              "first_payment_request_date", Timestamp.valueOf(LocalDateTime.of(2025, 5, 1, 0, 0)))
          .containsEntry(
              "second_payment_request_date", Timestamp.valueOf(LocalDateTime.of(2025, 9, 1, 0, 0)))
          .containsEntry("student_cancellation_reason", "Abandon")
          .containsEntry("prof_denial_reason", 4001)
          .containsEntry("professor_in_charge", null);
    });
  }

  @Test
  void aMobilityWhoseChoiceHasNoPartnerIsFoundWithANullPartner() {
    runInTransaction(() -> {
      mobilityRepository.create(newMobility(5002));

      Mobility found = mobilityRepository.findById(5002).orElse(null);
      assertThat(found.getPartner()).isNull();
      assertThat(found.getCountry().getCountryCode()).isEqualTo("BE");
      assertThat(found.getCountry().getName()).isEqualTo("Belgique");
      assertThat(found.getNominatedStudent().getId()).isEqualTo(1001);
      assertThat(mobilityRepository.findByUser(1001))
          .extracting(Mobility::getId)
          .containsExactly(5002);
      assertThat(mobilityRepository.findAll())
          .extracting(Mobility::getId)
          .containsExactlyInAnyOrder(5002, 5006, 5007);
    });
  }

  @Test
  void aMobilityWhoseChoiceHasNeitherPartnerNorCountryIsFoundWithNullsForBoth() {
    runInTransaction(() -> {
      mobilityRepository.create(newMobility(5003));

      Mobility found = mobilityRepository.findById(5003).orElse(null);
      assertThat(found.getPartner()).isNull();
      assertThat(found.getCountry()).isNull();
      assertThat(found.getProgramme().getProgrammeName()).isEqualTo("Erasmus+");
    });
  }

  @ParameterizedTest
  @ValueSource(ints = {5006, 9999})
  void createForAnExistingMobilityOrAnUnknownChoiceFails(int id) {
    runInTransaction(() ->
        assertThatThrownBy(() -> mobilityRepository.create(newMobility(id)))
            .isInstanceOf(DataIntegrityViolationException.class));
  }

  @Test
  void updateWritesEveryUpdatableColumnAndIncrementsTheVersion() {
    runInTransaction(() -> {
      Mobility mobility = mobilityRepository.findById(5006).orElse(null);
      mobility.setSubmissionDate(LocalDateTime.of(2025, 1, 12, 13, 14, 15));
      mobility.setState(MobilityState.CANCELLED);
      mobility.setStateBeforeCancellation(MobilityState.IN_PREPARATION);
      mobility.setFirstPaymentRequestDate(null);
      mobility.setSecondPaymentRequestDate(LocalDateTime.of(2025, 7, 7, 7, 7));
      mobility.setProEcoEncoding(false);
      mobility.setSecondSoftwareEncoding(true);
      mobility.setCancellationReason("Covid");
      mobility.setDenialReason(denialReason(4001));
      mobility.setProfessorInCharge(null);

      Mobility updated = mobilityRepository.update(mobility);

      assertThat(updated.getVersion()).isEqualTo(3);
      assertThat(queryForRow(MOBILITY_BY_ID, 5006))
          .containsEntry(
              "submission_date", Timestamp.valueOf(LocalDateTime.of(2025, 1, 12, 13, 14, 15)))
          .containsEntry("state", "Annulée")
          .containsEntry("state_before_cancellation", "En préparation")
          .containsEntry("first_payment_request_date", null)
          .containsEntry(
              "second_payment_request_date", Timestamp.valueOf(LocalDateTime.of(2025, 7, 7, 7, 7)))
          .containsEntry("pro_eco_encoding", false)
          .containsEntry("second_software_encoding", true)
          .containsEntry("student_cancellation_reason", "Covid")
          .containsEntry("prof_denial_reason", 4001)
          .containsEntry("professor_in_charge", null)
          .containsEntry("version", 3);
    });
  }

  @Test
  void updateWithAStaleVersionFailsAndLeavesTheRowUntouched() {
    runInTransaction(() -> {
      Mobility stale = mobilityRepository.findById(5006).orElse(null);
      stale.setVersion(1);
      stale.setState(MobilityState.CLOSED);

      assertThatThrownBy(() -> mobilityRepository.update(stale))
          .isInstanceOf(ConcurrentModificationException.class);
      assertThat(queryForRow(MOBILITY_BY_ID, 5006))
          .containsEntry("state", "En préparation")
          .containsEntry("version", 2);
    });
  }
}
