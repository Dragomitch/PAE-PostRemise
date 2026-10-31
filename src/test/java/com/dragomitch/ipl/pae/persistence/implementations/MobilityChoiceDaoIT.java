package com.dragomitch.ipl.pae.persistence.implementations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dragomitch.ipl.pae.business.MobilityChoice;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.business.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.exceptions.FatalException;
import com.dragomitch.ipl.pae.persistence.MobilityChoiceDao;

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
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Mobility choices and their filters. The data set is described in
 * db/fixtures/mobility_choices.sql: 5006 and 5007 became mobilities (db/fixtures/mobilities.sql).
 *
 * <p>The "active" and "passed" filters and {@code findByActivePartner} compare the academic year
 * with the current year ({@code LocalDate.now()}); the fixtures are expressed relative to it.
 */
class MobilityChoiceDaoIT extends AbstractDaoIT {

  private static final String CHOICE_BY_ID =
      "SELECT * FROM student_exchange_tools.mobility_choices WHERE mobility_choice_id = ?";

  @Autowired
  private MobilityChoiceDao mobilityChoiceDao;

  @Override
  protected List<String> fixtures() {
    return List.of("db/fixtures/users.sql", "db/fixtures/partners.sql",
        "db/fixtures/mobility_choices.sql", "db/fixtures/mobilities.sql");
  }

  private static int currentYear() {
    return LocalDate.now().getYear();
  }

  private static List<Integer> ids(List<MobilityChoiceDto> choices) {
    return choices.stream().map(MobilityChoiceDto::getId).toList();
  }

  private static List<Integer> parseIds(String ids) {
    return ids == null || ids.isEmpty() ? List.of()
        : Arrays.stream(ids.split(";")).map(Integer::valueOf).toList();
  }

  private MobilityChoiceDto newChoice(int userId) {
    MobilityChoiceDto choice = build(MobilityChoiceDto.class);
    UserDto user = build(UserDto.class);
    user.setId(userId);
    choice.setUser(user);
    choice.setPreferenceOrder(4);
    choice.setMobilityType(MobilityChoice.MOBILITY_TYPE_SMP);
    choice.setAcademicYear(2030);
    choice.setTerm(2);
    ProgrammeDto programme = build(ProgrammeDto.class);
    programme.setId(3);
    choice.setProgramme(programme);
    return choice;
  }

  private CountryDto country(String code) {
    CountryDto country = build(CountryDto.class);
    country.setCountryCode(code);
    return country;
  }

  private PartnerDto partner(int id) {
    PartnerDto partner = build(PartnerDto.class);
    partner.setId(id);
    return partner;
  }

  private DenialReasonDto denialReason(int id) {
    DenialReasonDto reason = build(DenialReasonDto.class);
    reason.setId(id);
    return reason;
  }

  @Test
  void findByIdMapsEveryFieldAndJoinsUserOptionProgrammeCountryAndPartner() {
    MobilityChoiceDto choice = inTransaction(() -> mobilityChoiceDao.findById(5001));

    assertThat(choice.getId()).isEqualTo(5001);
    assertThat(choice.getUser().getId()).isEqualTo(1001);
    assertThat(choice.getUser().getLastName()).isEqualTo("Martin");
    assertThat(choice.getUser().getFirstName()).isEqualTo("Alice");
    assertThat(choice.getUser().getOption().getCode()).isEqualTo("BIN");
    assertThat(choice.getUser().getOption().getName())
        .isEqualTo("Bachelier en informatique de gestion");
    assertThat(choice.getPreferenceOrder()).isEqualTo(1);
    assertThat(choice.getMobilityType()).isEqualTo("SMS");
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
      assertThat(mobilityChoiceDao.findById(5002).getPartner()).isNull();

      MobilityChoiceDto canceled = mobilityChoiceDao.findById(5003);
      assertThat(canceled.getCountry()).as("no country").isNull();
      assertThat(canceled.getCancellationReason()).isEqualTo("Changed my mind");
      assertThat(canceled.getVersion()).isEqualTo(2);

      MobilityChoiceDto rejected = mobilityChoiceDao.findById(5004);
      assertThat(rejected.getDenialReason().getId()).isEqualTo(4001);
      assertThat(rejected.getDenialReason().getReason()).isEqualTo("Dossier incomplet");
      assertThat(rejected.getCountry().getName()).isEqualTo("Canada");
      assertThat(rejected.getUser().getOption().getCode()).isEqualTo("BCH");
    });
  }

  @Test
  void findByIdStillFindsAChoiceThatBecameAMobility() {
    assertThat(inTransaction(() -> mobilityChoiceDao.findById(5006)).getId()).isEqualTo(5006);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1, 5000, 99999})
  void findByIdReturnsNullWhenAbsent(int id) {
    assertThat(inTransaction(() -> mobilityChoiceDao.findById(id))).isNull();
  }

  @ParameterizedTest(name = "filter \"{0}\" -> {1}")
  @CsvSource({
      "all,       5001;5002;5003;5004;5005;5006;5007",
      "active,    5001;5002",
      "canceled,  5003",
      "rejected,  5004",
      "passed,    5005"})
  void findAllAppliesTheFilter(String filter, String expected) {
    List<MobilityChoiceDto> choices = inTransaction(() -> mobilityChoiceDao.findAll(filter));

    assertThat(ids(choices)).containsExactlyInAnyOrderElementsOf(parseIds(expected));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"whatever", "ALL", "active "})
  void findAllRejectsAnUnknownFilter(String filter) {
    runInTransaction(() -> assertThatThrownBy(() -> mobilityChoiceDao.findAll(filter))
        .isInstanceOf(IllegalArgumentException.class));
  }

  @ParameterizedTest(name = "user {0} -> {1}")
  @CsvSource({"1001, 5001;5002;5003", "1003, 5004", "1004, 5005", "1002, ''", "9999, ''"})
  void findByUserExcludesChoicesThatBecameMobilities(int userId, String expected) {
    List<MobilityChoiceDto> choices = inTransaction(() -> mobilityChoiceDao.findByUser(userId));

    assertThat(ids(choices)).containsExactlyInAnyOrderElementsOf(parseIds(expected));
  }

  @ParameterizedTest(name = "partner {0} -> {1}")
  @CsvSource({"3001, 5001;5005", "3004, 5004", "3002, ''"})
  void findByPartnerExcludesChoicesThatBecameMobilities(int partnerId, String expected) {
    List<MobilityChoiceDto> choices =
        inTransaction(() -> mobilityChoiceDao.findByPartner(partnerId));

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
    List<MobilityChoiceDto> choices =
        inTransaction(() -> mobilityChoiceDao.findByActivePartner(partnerId));

    assertThat(ids(choices)).containsExactlyInAnyOrderElementsOf(parseIds(expected));
  }

  @ParameterizedTest(name = "mobility cancelled with only its {0} set")
  @ValueSource(strings = {"student_cancellation_reason", "prof_denial_reason"})
  void findByActivePartnerExcludesAMobilityCancelledByTheStudentOrByAProfessor(String kept) {
    String cleared = kept.equals("prof_denial_reason") ? "student_cancellation_reason"
        : "prof_denial_reason";
    List<MobilityChoiceDto> choices = inTransaction(() -> {
      execute("UPDATE student_exchange_tools.mobilities SET " + cleared + " = NULL "
          + "WHERE mobility_choice_id = 5007");
      return mobilityChoiceDao.findByActivePartner(3004);
    });

    assertThat(choices).isEmpty();
  }

  @Test
  void findByActivePartnerKeepsAMobilityThatIsNotCancelled() {
    List<MobilityChoiceDto> choices = inTransaction(() -> {
      execute("UPDATE student_exchange_tools.mobilities SET prof_denial_reason = NULL, "
          + "student_cancellation_reason = NULL WHERE mobility_choice_id = 5007");
      return mobilityChoiceDao.findByActivePartner(3004);
    });

    assertThat(ids(choices)).containsExactly(5007);
  }

  @Test
  void createInsertsTheChoiceWithTheDatabaseSubmissionTime() {
    LocalDateTime before = LocalDateTime.now().minusMinutes(1);
    runInTransaction(() -> {
      MobilityChoiceDto choice = newChoice(1003);
      choice.setCountry(country("CA"));
      choice.setPartner(partner(3004));

      MobilityChoiceDto created = mobilityChoiceDao.create(choice);

      assertThat(created.getId()).isGreaterThanOrEqualTo(100000);
      assertThat(created.getVersion()).isEqualTo(1);
      assertThat(created.getSubmissionDate()).isAfter(before)
          .isBefore(LocalDateTime.now().plusMinutes(1));
      Map<String, Object> row = queryForRow(CHOICE_BY_ID, created.getId());
      assertThat(row).containsEntry("user_id", 1003).containsEntry("preference_order", 4)
          .containsEntry("mobility_type", "SMP").containsEntry("academic_year", 2030)
          .containsEntry("term", 2).containsEntry("programme", 3).containsEntry("country", "CA")
          .containsEntry("prof_denial_reason", null)
          .containsEntry("student_cancellation_reason", null).containsEntry("partner", 3004)
          .containsEntry("version", 1)
          .containsEntry("submission_date", Timestamp.valueOf(created.getSubmissionDate()));
    });
  }

  @Test
  void createStoresNullsForAbsentCountryAndPartnerAndKeepsDenialAndCancellation() {
    runInTransaction(() -> {
      MobilityChoiceDto choice = newChoice(1001);
      choice.setDenialReason(denialReason(4001));
      choice.setCancellationReason("Plus intéressé");

      MobilityChoiceDto created = mobilityChoiceDao.create(choice);

      assertThat(queryForRow(CHOICE_BY_ID, created.getId())).containsEntry("country", null)
          .containsEntry("partner", null).containsEntry("prof_denial_reason", 4001)
          .containsEntry("student_cancellation_reason", "Plus intéressé");
    });
  }

  @Test
  void createStoresAnEmptyCancellationReasonAsNullSoThatTheChoiceIsNotCancelled() {
    runInTransaction(() -> {
      MobilityChoiceDto choice = newChoice(1001);
      choice.setCancellationReason("");

      MobilityChoiceDto created = mobilityChoiceDao.create(choice);

      assertThat(queryForRow(CHOICE_BY_ID, created.getId()))
          .containsEntry("student_cancellation_reason", null);
      assertThat(ids(mobilityChoiceDao.findAll(MobilityChoiceDao.FILTER_CANCELED_MOBILITIES_CHOICES)))
          .containsExactly(5003);
    });
  }

  @Test
  void createTreatsPartnerIdMinusOneAsNoPartner() {
    runInTransaction(() -> {
      MobilityChoiceDto choice = newChoice(1001);
      choice.setPartner(partner(-1));

      MobilityChoiceDto created = mobilityChoiceDao.create(choice);

      assertThat(queryForRow(CHOICE_BY_ID, created.getId())).containsEntry("partner", null);
    });
  }

  @Test
  void createWithAnUnknownUserIsRejectedByTheForeignKey() {
    runInTransaction(() -> assertThatThrownBy(() -> mobilityChoiceDao.create(newChoice(9999)))
        .isInstanceOf(FatalException.class));
  }

  @Test
  void updateWritesEveryUpdatableColumnAndIncrementsTheVersion() {
    runInTransaction(() -> {
      MobilityChoiceDto choice = mobilityChoiceDao.findById(5001);
      choice.setPreferenceOrder(9);
      choice.setMobilityType("SMP");
      choice.setAcademicYear(2031);
      choice.setTerm(2);
      choice.getProgramme().setId(2);
      choice.getCountry().setCountryCode("BE");
      choice.setSubmissionDate(LocalDateTime.of(2025, 5, 6, 7, 8, 9));
      choice.setDenialReason(denialReason(4001));
      choice.setCancellationReason("Raison");
      choice.setPartner(partner(3003));

      mobilityChoiceDao.update(choice);

      assertThat(queryForRow(CHOICE_BY_ID, 5001)).containsEntry("preference_order", 9)
          .containsEntry("mobility_type", "SMP").containsEntry("academic_year", 2031)
          .containsEntry("term", 2).containsEntry("programme", 2).containsEntry("country", "BE")
          .containsEntry("submission_date",
              Timestamp.valueOf(LocalDateTime.of(2025, 5, 6, 7, 8, 9)))
          .containsEntry("prof_denial_reason", 4001)
          .containsEntry("student_cancellation_reason", "Raison").containsEntry("partner", 3003)
          .containsEntry("user_id", 1001).containsEntry("version", 2);
    });
  }

  @ParameterizedTest
  @CsvSource(value = {"''", "NULL"}, nullValues = "NULL")
  void updateStoresAnEmptyOrNullCancellationReasonAsNullAndClearsDenialAndPartner(
      String cancellation) {
    runInTransaction(() -> {
      MobilityChoiceDto choice = mobilityChoiceDao.findById(5003);
      choice.setCancellationReason(cancellation);
      choice.setCountry(country("FR"));

      mobilityChoiceDao.update(choice);

      assertThat(queryForRow(CHOICE_BY_ID, 5003)).containsEntry("student_cancellation_reason", null)
          .containsEntry("prof_denial_reason", null).containsEntry("partner", null)
          .containsEntry("country", "FR").containsEntry("version", 3);
    });
  }

  @Test
  void updateOfAChoiceWithoutCountryKeepsTheCountryNull() {
    runInTransaction(() -> {
      MobilityChoiceDto canceled = mobilityChoiceDao.findById(5003);
      canceled.setTerm(2);

      mobilityChoiceDao.update(canceled);

      assertThat(queryForRow(CHOICE_BY_ID, 5003)).containsEntry("country", null)
          .containsEntry("term", 2);
    });
  }

  @Test
  void updateSetsTheNewVersionInTheDtoSoThatItCanBeUpdatedAgain() {
    runInTransaction(() -> {
      MobilityChoiceDto choice = mobilityChoiceDao.findById(5001);
      mobilityChoiceDao.update(choice);

      assertThat(choice.getVersion()).isEqualTo(2);
      assertThat(mobilityChoiceDao.findById(5001).getVersion()).isEqualTo(2);

      choice.setTerm(2);
      mobilityChoiceDao.update(choice);

      assertThat(choice.getVersion()).isEqualTo(3);
      assertThat(queryForRow(CHOICE_BY_ID, 5001)).containsEntry("term", 2)
          .containsEntry("version", 3);
    });
  }

  @Test
  void updateWithAStaleVersionFailsAndLeavesTheRowUntouched() {
    runInTransaction(() -> {
      MobilityChoiceDto stale = mobilityChoiceDao.findById(5004);
      stale.setVersion(2);
      stale.setTerm(2);

      assertThatThrownBy(() -> mobilityChoiceDao.update(stale))
          .isInstanceOf(ConcurrentModificationException.class);
      assertThat(queryForRow(CHOICE_BY_ID, 5004)).containsEntry("term", 1)
          .containsEntry("version", 3);
    });
  }

  @Test
  void updateOfAnUnknownChoiceFails() {
    runInTransaction(() -> {
      MobilityChoiceDto ghost = mobilityChoiceDao.findById(5001);
      ghost.setId(424242);
      assertThatThrownBy(() -> mobilityChoiceDao.update(ghost))
          .isInstanceOf(ConcurrentModificationException.class);
    });
  }
}
