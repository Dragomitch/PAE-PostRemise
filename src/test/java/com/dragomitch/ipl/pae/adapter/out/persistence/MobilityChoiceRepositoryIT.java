package com.dragomitch.ipl.pae.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dragomitch.ipl.pae.domain.model.Country;
import com.dragomitch.ipl.pae.domain.model.DenialReason;
import com.dragomitch.ipl.pae.domain.model.MobilityChoice;
import com.dragomitch.ipl.pae.domain.model.MobilityChoiceFilter;
import com.dragomitch.ipl.pae.domain.model.MobilityType;
import com.dragomitch.ipl.pae.domain.model.Partner;
import com.dragomitch.ipl.pae.domain.model.Programme;
import com.dragomitch.ipl.pae.domain.model.User;
import com.dragomitch.ipl.pae.domain.port.out.MobilityChoiceRepository;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Mobility choices and their filters. The data set is described in
 * db/fixtures/mobility_choices.sql: 5006 and 5007 became mobilities (db/fixtures/mobilities.sql).
 *
 * <p>The "active" and "passed" filters and {@code findByActivePartner} compare the academic year
 * with the current year ({@code LocalDate.now()}); the fixtures are expressed relative to it.
 */
class MobilityChoiceRepositoryIT extends AbstractRepositoryIT {

  private static final String CHOICE_BY_ID =
      "SELECT * FROM student_exchange_tools.mobility_choices WHERE mobility_choice_id = ?";

  @Autowired private MobilityChoiceRepository mobilityChoiceRepository;

  @Override
  protected List<String> fixtures() {
    return List.of(
        "db/fixtures/users.sql",
        "db/fixtures/partners.sql",
        "db/fixtures/mobility_choices.sql",
        "db/fixtures/mobilities.sql");
  }

  private static int currentYear() {
    return LocalDate.now().getYear();
  }

  private static List<Integer> ids(List<MobilityChoice> choices) {
    return choices.stream().map(MobilityChoice::getId).toList();
  }

  private static List<Integer> parseIds(String ids) {
    return ids == null || ids.isEmpty()
        ? List.of()
        : Arrays.stream(ids.split(";")).map(Integer::valueOf).toList();
  }

  private MobilityChoice newChoice(int userId) {
    MobilityChoice choice = new MobilityChoice();
    User user = new User();
    user.setId(userId);
    choice.setUser(user);
    choice.setPreferenceOrder(4);
    choice.setMobilityType(MobilityType.SMP);
    choice.setAcademicYear(2030);
    choice.setTerm(2);
    Programme programme = new Programme();
    programme.setId(3);
    choice.setProgramme(programme);
    return choice;
  }

  private Country country(String code) {
    Country country = new Country();
    country.setCountryCode(code);
    return country;
  }

  private Partner partner(int id) {
    Partner partner = new Partner();
    partner.setId(id);
    return partner;
  }

  private DenialReason denialReason(int id) {
    DenialReason reason = new DenialReason();
    reason.setId(id);
    return reason;
  }

  @Test
  void findByIdMapsEveryFieldAndJoinsUserOptionProgrammeCountryAndPartner() {
    MobilityChoice choice =
        inTransaction(() -> mobilityChoiceRepository.findById(5001).orElse(null));

    assertThat(choice.getId()).isEqualTo(5001);
    assertThat(choice.getUser().getId()).isEqualTo(1001);
    assertThat(choice.getUser().getLastName()).isEqualTo("Martin");
    assertThat(choice.getUser().getFirstName()).isEqualTo("Alice");
    assertThat(choice.getUser().getOption().getCode()).isEqualTo("BIN");
    assertThat(choice.getUser().getOption().getName())
        .isEqualTo("Bachelier en informatique de gestion");
    assertThat(choice.getPreferenceOrder()).isEqualTo(1);
    assertThat(choice.getMobilityType()).isEqualTo(MobilityType.SMS);
    assertThat(choice.getAcademicYear()).isEqualTo(currentYear());
    assertThat(choice.getTerm()).isEqualTo(1);
    assertThat(choice.getProgramme().getId()).isEqualTo(1);
    assertThat(choice.getProgramme().getProgrammeName()).isEqualTo("Erasmus+");
    assertThat(choice.getCountry().getCountryCode()).isEqualTo("FR");
    assertThat(choice.getCountry().getName()).isEqualTo("France");
    assertThat(choice.getSubmissionDate()).isEqualTo(LocalDateTime.of(2025, 1, 5, 10, 0));
    assertThat(choice.getDenialReason()).isNull();
    assertThat(choice.getCancellationReason()).isNull();
    assertThat(choice.getPartner().getId()).isEqualTo(3001);
    assertThat(choice.getPartner().getFullName()).isEqualTo("Université de Lyon");
    assertThat(choice.getVersion()).isEqualTo(1);
  }

  @Test
  void findByIdMapsAbsentPartnerAndCountryAndJoinsTheDenialReason() {
    runInTransaction(() -> {
      assertThat(mobilityChoiceRepository.findById(5002).orElseThrow().getPartner()).isNull();

      MobilityChoice canceled = mobilityChoiceRepository.findById(5003).orElse(null);
      assertThat(canceled.getCountry()).as("no country").isNull();
      assertThat(canceled.getCancellationReason()).isEqualTo("Changed my mind");
      assertThat(canceled.getVersion()).isEqualTo(2);

      MobilityChoice rejected = mobilityChoiceRepository.findById(5004).orElse(null);
      assertThat(rejected.getDenialReason().getId()).isEqualTo(4001);
      assertThat(rejected.getDenialReason().getReason()).isEqualTo("Dossier incomplet");
      assertThat(rejected.getCountry().getName()).isEqualTo("Canada");
      assertThat(rejected.getUser().getOption().getCode()).isEqualTo("BCH");
    });
  }

  @Test
  void findByIdStillFindsAChoiceThatBecameAMobility() {
    assertThat(inTransaction(() -> mobilityChoiceRepository.findById(5006).orElse(null)).getId())
        .isEqualTo(5006);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1, 5000, 99999})
  void findByIdReturnsNullWhenAbsent(int id) {
    assertThat(inTransaction(() -> mobilityChoiceRepository.findById(id))).isEmpty();
  }

  @ParameterizedTest(name = "filter \"{0}\" -> {1}")
  @CsvSource({
    "ALL,       5001;5002;5003;5004;5005;5006;5007",
    "ACTIVE,    5001;5002",
    "CANCELED,  5003",
    "REJECTED,  5004",
    "PASSED,    5005"
  })
  void findAllAppliesTheFilter(MobilityChoiceFilter filter, String expected) {
    List<MobilityChoice> choices = inTransaction(() -> mobilityChoiceRepository.findAll(filter));

    assertThat(ids(choices)).containsExactlyInAnyOrderElementsOf(parseIds(expected));
  }

  @ParameterizedTest(name = "user {0} -> {1}")
  @CsvSource({"1001, 5001;5002;5003", "1003, 5004", "1004, 5005", "1002, ''", "9999, ''"})
  void findByUserExcludesChoicesThatBecameMobilities(int userId, String expected) {
    List<MobilityChoice> choices = inTransaction(() -> mobilityChoiceRepository.findByUser(userId));

    assertThat(ids(choices)).containsExactlyInAnyOrderElementsOf(parseIds(expected));
  }

  @ParameterizedTest(name = "partner {0} -> {1}")
  @CsvSource({"3001, 5001;5005", "3004, 5004", "3002, ''"})
  void findByPartnerExcludesChoicesThatBecameMobilities(int partnerId, String expected) {
    List<MobilityChoice> choices =
        inTransaction(() -> mobilityChoiceRepository.findByPartner(partnerId));

    assertThat(ids(choices)).containsExactlyInAnyOrderElementsOf(parseIds(expected));
  }

  /**
   * Current-year choices of the partner that are neither denied nor cancelled. Choices that became
   * mobilities are kept unless the mobility was cancelled, by the student (cancellation reason) or
   * by a professor (denial reason), like 5007: this is what makes a partner non-archivable.
   */
  @ParameterizedTest(name = "partner {0} -> {1}")
  @CsvSource({"3001, 5001;5006", "3004, ''", "3003, ''"})
  void findByActivePartnerReturnsTheCurrentChoicesOfThePartner(int partnerId, String expected) {
    List<MobilityChoice> choices =
        inTransaction(() -> mobilityChoiceRepository.findByActivePartner(partnerId));

    assertThat(ids(choices)).containsExactlyInAnyOrderElementsOf(parseIds(expected));
  }

  @ParameterizedTest(name = "mobility cancelled with only its {0} set")
  @ValueSource(strings = {"student_cancellation_reason", "prof_denial_reason"})
  void findByActivePartnerExcludesAMobilityCancelledByTheStudentOrByAProfessor(String kept) {
    String cleared =
        kept.equals("prof_denial_reason") ? "student_cancellation_reason" : "prof_denial_reason";
    List<MobilityChoice> choices =
        inTransaction(() -> {
          execute(
              "UPDATE student_exchange_tools.mobilities SET "
                  + cleared
                  + " = NULL "
                  + "WHERE mobility_choice_id = 5007");
          return mobilityChoiceRepository.findByActivePartner(3004);
        });

    assertThat(choices).isEmpty();
  }

  @Test
  void findByActivePartnerKeepsAMobilityThatIsNotCancelled() {
    List<MobilityChoice> choices =
        inTransaction(() -> {
          execute(
              "UPDATE student_exchange_tools.mobilities SET prof_denial_reason = NULL, "
                  + "student_cancellation_reason = NULL WHERE mobility_choice_id = 5007");
          return mobilityChoiceRepository.findByActivePartner(3004);
        });

    assertThat(ids(choices)).containsExactly(5007);
  }

  @Test
  void createInsertsTheChoiceWithTheDatabaseSubmissionTime() {
    LocalDateTime before = LocalDateTime.now().minusMinutes(1);
    runInTransaction(() -> {
      MobilityChoice choice = newChoice(1003);
      choice.setCountry(country("CA"));
      choice.setPartner(partner(3004));

      MobilityChoice created = mobilityChoiceRepository.create(choice);

      assertThat(created.getId()).isGreaterThanOrEqualTo(100000);
      assertThat(created.getVersion()).isEqualTo(1);
      assertThat(created.getSubmissionDate())
          .isAfter(before)
          .isBefore(LocalDateTime.now().plusMinutes(1));
      Map<String, Object> row = queryForRow(CHOICE_BY_ID, created.getId());
      assertThat(row)
          .containsEntry("user_id", 1003)
          .containsEntry("preference_order", 4)
          .containsEntry("mobility_type", "SMP")
          .containsEntry("academic_year", 2030)
          .containsEntry("term", 2)
          .containsEntry("programme", 3)
          .containsEntry("country", "CA")
          .containsEntry("prof_denial_reason", null)
          .containsEntry("student_cancellation_reason", null)
          .containsEntry("partner", 3004)
          .containsEntry("version", 1)
          .containsEntry("submission_date", Timestamp.valueOf(created.getSubmissionDate()));
    });
  }

  @Test
  void createStoresNullsForAbsentCountryAndPartnerAndKeepsDenialAndCancellation() {
    runInTransaction(() -> {
      MobilityChoice choice = newChoice(1001);
      choice.setDenialReason(denialReason(4001));
      choice.setCancellationReason("Plus intéressé");

      MobilityChoice created = mobilityChoiceRepository.create(choice);

      assertThat(queryForRow(CHOICE_BY_ID, created.getId()))
          .containsEntry("country", null)
          .containsEntry("partner", null)
          .containsEntry("prof_denial_reason", 4001)
          .containsEntry("student_cancellation_reason", "Plus intéressé");
    });
  }

  @Test
  void createStoresAnEmptyCancellationReasonAsNullSoThatTheChoiceIsNotCancelled() {
    runInTransaction(() -> {
      MobilityChoice choice = newChoice(1001);
      choice.setCancellationReason("");

      MobilityChoice created = mobilityChoiceRepository.create(choice);

      assertThat(queryForRow(CHOICE_BY_ID, created.getId()))
          .containsEntry("student_cancellation_reason", null);
      assertThat(ids(mobilityChoiceRepository.findAll(MobilityChoiceFilter.CANCELED)))
          .containsExactly(5003);
    });
  }

  @Test
  void createTreatsPartnerIdMinusOneAsNoPartner() {
    runInTransaction(() -> {
      MobilityChoice choice = newChoice(1001);
      choice.setPartner(partner(-1));

      MobilityChoice created = mobilityChoiceRepository.create(choice);

      assertThat(queryForRow(CHOICE_BY_ID, created.getId())).containsEntry("partner", null);
    });
  }

  @Test
  void createWithAnUnknownUserIsRejectedByTheForeignKey() {
    runInTransaction(() ->
        assertThatThrownBy(() -> mobilityChoiceRepository.create(newChoice(9999)))
            .isInstanceOf(DataIntegrityViolationException.class));
  }

  @Test
  void updateWritesEveryUpdatableColumnAndIncrementsTheVersion() {
    runInTransaction(() -> {
      MobilityChoice choice = mobilityChoiceRepository.findById(5001).orElse(null);
      choice.setPreferenceOrder(9);
      choice.setMobilityType(MobilityType.SMP);
      choice.setAcademicYear(2031);
      choice.setTerm(2);
      choice.getProgramme().setId(2);
      choice.getCountry().setCountryCode("BE");
      choice.setSubmissionDate(LocalDateTime.of(2025, 5, 6, 7, 8, 9));
      choice.setDenialReason(denialReason(4001));
      choice.setCancellationReason("Raison");
      choice.setPartner(partner(3003));

      mobilityChoiceRepository.update(choice);

      assertThat(queryForRow(CHOICE_BY_ID, 5001))
          .containsEntry("preference_order", 9)
          .containsEntry("mobility_type", "SMP")
          .containsEntry("academic_year", 2031)
          .containsEntry("term", 2)
          .containsEntry("programme", 2)
          .containsEntry("country", "BE")
          .containsEntry(
              "submission_date", Timestamp.valueOf(LocalDateTime.of(2025, 5, 6, 7, 8, 9)))
          .containsEntry("prof_denial_reason", 4001)
          .containsEntry("student_cancellation_reason", "Raison")
          .containsEntry("partner", 3003)
          .containsEntry("user_id", 1001)
          .containsEntry("version", 2);
    });
  }

  @ParameterizedTest
  @CsvSource(
      value = {"''", "NULL"},
      nullValues = "NULL")
  void updateStoresAnEmptyOrNullCancellationReasonAsNullAndClearsDenialAndPartner(
      String cancellation) {
    runInTransaction(() -> {
      MobilityChoice choice = mobilityChoiceRepository.findById(5003).orElse(null);
      choice.setCancellationReason(cancellation);
      choice.setCountry(country("FR"));

      mobilityChoiceRepository.update(choice);

      assertThat(queryForRow(CHOICE_BY_ID, 5003))
          .containsEntry("student_cancellation_reason", null)
          .containsEntry("prof_denial_reason", null)
          .containsEntry("partner", null)
          .containsEntry("country", "FR")
          .containsEntry("version", 3);
    });
  }

  @Test
  void updateOfAChoiceWithoutCountryKeepsTheCountryNull() {
    runInTransaction(() -> {
      MobilityChoice canceled = mobilityChoiceRepository.findById(5003).orElse(null);
      canceled.setTerm(2);

      mobilityChoiceRepository.update(canceled);

      assertThat(queryForRow(CHOICE_BY_ID, 5003))
          .containsEntry("country", null)
          .containsEntry("term", 2);
    });
  }

  @Test
  void updateSetsTheNewVersionInTheDtoSoThatItCanBeUpdatedAgain() {
    runInTransaction(() -> {
      MobilityChoice choice = mobilityChoiceRepository.findById(5001).orElse(null);
      mobilityChoiceRepository.update(choice);

      assertThat(choice.getVersion()).isEqualTo(2);
      assertThat(mobilityChoiceRepository.findById(5001).orElseThrow().getVersion()).isEqualTo(2);

      choice.setTerm(2);
      mobilityChoiceRepository.update(choice);

      assertThat(choice.getVersion()).isEqualTo(3);
      assertThat(queryForRow(CHOICE_BY_ID, 5001))
          .containsEntry("term", 2)
          .containsEntry("version", 3);
    });
  }

  @Test
  void updateWithAStaleVersionFailsAndLeavesTheRowUntouched() {
    runInTransaction(() -> {
      MobilityChoice stale = mobilityChoiceRepository.findById(5004).orElse(null);
      stale.setVersion(2);
      stale.setTerm(2);

      assertThatThrownBy(() -> mobilityChoiceRepository.update(stale))
          .isInstanceOf(ConcurrentModificationException.class);
      assertThat(queryForRow(CHOICE_BY_ID, 5004))
          .containsEntry("term", 1)
          .containsEntry("version", 3);
    });
  }

  @Test
  void updateOfAnUnknownChoiceFails() {
    runInTransaction(() -> {
      MobilityChoice ghost = mobilityChoiceRepository.findById(5001).orElse(null);
      ghost.setId(424242);
      assertThatThrownBy(() -> mobilityChoiceRepository.update(ghost))
          .isInstanceOf(ConcurrentModificationException.class);
    });
  }
}
