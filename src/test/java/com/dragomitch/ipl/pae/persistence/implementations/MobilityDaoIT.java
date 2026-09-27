package com.dragomitch.ipl.pae.persistence.implementations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dragomitch.ipl.pae.business.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.business.dto.MobilityDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.exceptions.FatalException;
import com.dragomitch.ipl.pae.persistence.MobilityDao;

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

/**
 * Mobilities (see db/fixtures/mobilities.sql). A mobility shares its id with the mobility choice
 * it comes from; the queries inner-join the choice's partner and country.
 */
class MobilityDaoIT extends AbstractDaoIT {

  private static final String MOBILITY_BY_ID =
      "SELECT * FROM student_exchange_tools.mobilities WHERE mobility_choice_id = ?";

  @Autowired
  private MobilityDao mobilityDao;

  @Override
  protected List<String> fixtures() {
    return List.of("db/fixtures/users.sql", "db/fixtures/partners.sql",
        "db/fixtures/mobility_choices.sql", "db/fixtures/mobilities.sql");
  }

  private MobilityDto newMobility(int choiceId) {
    MobilityDto mobility = build(MobilityDto.class);
    mobility.setId(choiceId);
    mobility.setSubmissionDate(LocalDateTime.of(2025, 4, 1, 12, 0, 30));
    mobility.setState(MobilityDto.STATE_CREATED);
    mobility.setProEcoEncoding(false);
    mobility.setSecondSoftwareEncoding(false);
    return mobility;
  }

  private DenialReasonDto denialReason(int id) {
    DenialReasonDto reason = build(DenialReasonDto.class);
    reason.setId(id);
    return reason;
  }

  private UserDto user(int id) {
    UserDto user = build(UserDto.class);
    user.setId(id);
    return user;
  }

  @Test
  void findByIdMapsEveryFieldAndJoinsStudentPartnerCountryAndProgramme() {
    MobilityDto mobility = inTransaction(() -> mobilityDao.findById(5006));

    assertThat(mobility.getId()).isEqualTo(5006);
    assertThat(mobility.getMobilityType()).isEqualTo("SMP");
    assertThat(mobility.getAcademicYear()).isEqualTo(LocalDate.now().getYear());
    assertThat(mobility.getTerm()).isEqualTo(2);
    assertThat(mobility.getSubmissionDate()).as("the mobility's own submission date")
        .isEqualTo(LocalDateTime.of(2025, 1, 10, 10, 0));
    assertThat(mobility.getState()).isEqualTo(MobilityDto.STATE_IN_PREPARATION);
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
    MobilityDto mobility = inTransaction(() -> mobilityDao.findById(5007));

    assertThat(mobility.getState()).isEqualTo(MobilityDto.STATE_CANCELLED);
    assertThat(mobility.getStateBeforeCancellation()).isEqualTo(MobilityDto.STATE_CREATED);
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
    assertThat(inTransaction(() -> mobilityDao.findById(id))).isNull();
  }

  @Test
  void findAllReturnsEveryMobility() {
    List<MobilityDto> mobilities = inTransaction(() -> mobilityDao.findAll());

    assertThat(mobilities).extracting(MobilityDto::getId).containsExactlyInAnyOrder(5006, 5007);
  }

  @ParameterizedTest(name = "user {0} -> {1}")
  @CsvSource({"1004, 5006", "1003, 5007", "1001, ''", "1002, ''"})
  void findByUserReturnsTheMobilitiesOfThatStudent(int userId, String expected) {
    List<MobilityDto> mobilities = inTransaction(() -> mobilityDao.findByUser(userId));

    assertThat(mobilities).extracting(MobilityDto::getId)
        .containsExactlyElementsOf(expected.isEmpty() ? List.of() : List.of(
            Integer.valueOf(expected)));
  }

  @Test
  void createInsertsTheMobilityWithVersionOne() {
    runInTransaction(() -> {
      MobilityDto mobility = newMobility(5001);
      mobility.setProfessorInCharge(user(1002));
      mobility.setProEcoEncoding(true);

      MobilityDto created = mobilityDao.create(mobility);

      assertThat(created.getVersion()).isEqualTo(1);
      assertThat(queryForRow(MOBILITY_BY_ID, 5001))
          .containsEntry("submission_date", Timestamp.valueOf(LocalDateTime.of(2025, 4, 1, 12, 0,
              30)))
          .containsEntry("state", "Créée").containsEntry("state_before_cancellation", null)
          .containsEntry("first_payment_request_date", null)
          .containsEntry("second_payment_request_date", null)
          .containsEntry("pro_eco_encoding", true).containsEntry("second_software_encoding", false)
          .containsEntry("student_cancellation_reason", null)
          .containsEntry("prof_denial_reason", null).containsEntry("professor_in_charge", 1002)
          .containsEntry("version", 1);
      MobilityDto found = mobilityDao.findById(5001);
      assertThat(found.getPartner().getId()).isEqualTo(3001);
      assertThat(found.getNominatedStudent().getId()).isEqualTo(1001);
    });
  }

  @Test
  void createStoresPaymentDatesDenialAndCancellation() {
    runInTransaction(() -> {
      MobilityDto mobility = newMobility(5004);
      mobility.setState(MobilityDto.STATE_CANCELLED);
      mobility.setStateBeforeCancellation(MobilityDto.STATE_TO_BE_PAID);
      mobility.setFirstPaymentRequestDate(LocalDateTime.of(2025, 5, 1, 0, 0));
      mobility.setSecondPaymentRequestDate(LocalDateTime.of(2025, 9, 1, 0, 0));
      mobility.setCancellationReason("Abandon");
      mobility.setDenialReason(denialReason(4001));

      mobilityDao.create(mobility);

      assertThat(queryForRow(MOBILITY_BY_ID, 5004))
          .containsEntry("state_before_cancellation", "A payer")
          .containsEntry("first_payment_request_date",
              Timestamp.valueOf(LocalDateTime.of(2025, 5, 1, 0, 0)))
          .containsEntry("second_payment_request_date",
              Timestamp.valueOf(LocalDateTime.of(2025, 9, 1, 0, 0)))
          .containsEntry("student_cancellation_reason", "Abandon")
          .containsEntry("prof_denial_reason", 4001).containsEntry("professor_in_charge", null);
    });
  }

  @Test
  void aMobilityWhoseChoiceHasNoPartnerIsStoredButInvisibleToTheQueries() {
    runInTransaction(() -> {
      mobilityDao.create(newMobility(5002));

      assertThat(query(MOBILITY_BY_ID, 5002)).hasSize(1);
      assertThat(mobilityDao.findById(5002)).isNull();
      assertThat(mobilityDao.findByUser(1001)).isEmpty();
    });
  }

  @ParameterizedTest
  @ValueSource(ints = {5006, 9999})
  void createForAnExistingMobilityOrAnUnknownChoiceFails(int id) {
    runInTransaction(() -> assertThatThrownBy(() -> mobilityDao.create(newMobility(id)))
        .isInstanceOf(FatalException.class));
  }

  @Test
  void updateWritesEveryUpdatableColumnAndIncrementsTheVersion() {
    runInTransaction(() -> {
      MobilityDto mobility = mobilityDao.findById(5006);
      mobility.setSubmissionDate(LocalDateTime.of(2025, 1, 12, 13, 14, 15));
      mobility.setState(MobilityDto.STATE_CANCELLED);
      mobility.setStateBeforeCancellation(MobilityDto.STATE_IN_PREPARATION);
      mobility.setFirstPaymentRequestDate(null);
      mobility.setSecondPaymentRequestDate(LocalDateTime.of(2025, 7, 7, 7, 7));
      mobility.setProEcoEncoding(false);
      mobility.setSecondSoftwareEncoding(true);
      mobility.setCancellationReason("Covid");
      mobility.setDenialReason(denialReason(4001));
      mobility.setProfessorInCharge(null);

      MobilityDto updated = mobilityDao.update(mobility);

      assertThat(updated.getVersion()).isEqualTo(3);
      assertThat(queryForRow(MOBILITY_BY_ID, 5006))
          .containsEntry("submission_date",
              Timestamp.valueOf(LocalDateTime.of(2025, 1, 12, 13, 14, 15)))
          .containsEntry("state", "Annulée")
          .containsEntry("state_before_cancellation", "En préparation")
          .containsEntry("first_payment_request_date", null)
          .containsEntry("second_payment_request_date",
              Timestamp.valueOf(LocalDateTime.of(2025, 7, 7, 7, 7)))
          .containsEntry("pro_eco_encoding", false).containsEntry("second_software_encoding", true)
          .containsEntry("student_cancellation_reason", "Covid")
          .containsEntry("prof_denial_reason", 4001).containsEntry("professor_in_charge", null)
          .containsEntry("version", 3);
    });
  }

  @Test
  void updateWithAStaleVersionFailsAndLeavesTheRowUntouched() {
    runInTransaction(() -> {
      MobilityDto stale = mobilityDao.findById(5006);
      stale.setVersion(1);
      stale.setState(MobilityDto.STATE_CLOSED);

      assertThatThrownBy(() -> mobilityDao.update(stale))
          .isInstanceOf(ConcurrentModificationException.class);
      assertThat(queryForRow(MOBILITY_BY_ID, 5006)).containsEntry("state", "En préparation")
          .containsEntry("version", 2);
    });
  }
}
