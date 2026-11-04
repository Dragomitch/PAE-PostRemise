package com.dragomitch.ipl.pae.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dragomitch.ipl.pae.domain.model.Address;
import com.dragomitch.ipl.pae.domain.model.Country;
import com.dragomitch.ipl.pae.domain.model.NominatedStudent;
import com.dragomitch.ipl.pae.domain.model.Role;
import com.dragomitch.ipl.pae.domain.port.out.NominatedStudentRepository;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ConcurrentModificationException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class NominatedStudentRepositoryIT extends AbstractRepositoryIT {

  private static final String STUDENT_BY_ID =
      "SELECT * FROM student_exchange_tools.nominated_students WHERE user_id = ?";

  @Autowired private NominatedStudentRepository nominatedStudentRepository;

  @Override
  protected List<String> fixtures() {
    return List.of(
        "db/fixtures/users.sql", "db/fixtures/partners.sql", "db/fixtures/nominated_students.sql");
  }

  private NominatedStudent newStudent(int userId, String cardHolder) {
    NominatedStudent student = new NominatedStudent();
    student.setId(userId);
    student.setTitle("Mrs");
    student.setBirthdate(LocalDate.of(2002, 2, 28));
    Country nationality = new Country();
    nationality.setCountryCode("CA");
    student.setNationality(nationality);
    Address address = new Address();
    address.setId(2003);
    student.setAddress(address);
    student.setPhoneNumber("+15140000009");
    student.setGender("F");
    student.setNbrPassedYears(1);
    student.setIban("BE71096123456769");
    student.setCardHolder(cardHolder);
    student.setBankName("BNP Paribas Fortis");
    student.setBic("GEBABEBB");
    return student;
  }

  @Test
  void findByIdMapsTheUserAndTheStudentColumns() {
    NominatedStudent alice =
        inTransaction(() -> nominatedStudentRepository.findById(1001).orElse(null));

    // user part
    assertThat(alice.getId()).isEqualTo(1001);
    assertThat(alice.getUsername()).isEqualTo("alice");
    assertThat(alice.getLastName()).isEqualTo("Martin");
    assertThat(alice.getFirstName()).isEqualTo("Alice");
    assertThat(alice.getPassword()).isEqualTo("hash-alice");
    assertThat(alice.getEmail()).isEqualTo("alice@student.test");
    assertThat(alice.getRegistrationDate()).isEqualTo(LocalDateTime.of(2024, 9, 1, 8, 30));
    assertThat(alice.getRole()).isEqualTo(Role.STUDENT);
    assertThat(alice.getOption().getCode()).isEqualTo("BIN");
    assertThat(alice.getOption().getName()).isEqualTo("Bachelier en informatique de gestion");
    // nominated student part
    assertThat(alice.getTitle()).isEqualTo("Ms");
    assertThat(alice.getBirthdate()).isEqualTo(LocalDate.of(2001, 4, 12));
    assertThat(alice.getNationality().getCountryCode()).isEqualTo("BE");
    assertThat(alice.getNationality().getName()).isEqualTo("Belgique");
    assertThat(alice.getPhoneNumber()).isEqualTo("+32470000001");
    assertThat(alice.getGender()).isEqualTo("F");
    assertThat(alice.getNbrPassedYears()).isEqualTo(2);
    assertThat(alice.getIban()).isEqualTo("BE68539007547034");
    assertThat(alice.getCardHolder()).isNull();
    assertThat(alice.getBankName()).isEqualTo("Belfius");
    assertThat(alice.getBic()).isEqualTo("GKCCBEBB");
    assertThat(alice.getAddress().getId()).isEqualTo(2001);
    assertThat(alice.getVersion()).isEqualTo(1);
  }

  @ParameterizedTest
  @ValueSource(ints = {1002, 1003, 0, 99999})
  void findByIdReturnsNullForAUserWhoIsNotNominated(int id) {
    assertThat(inTransaction(() -> nominatedStudentRepository.findById(id))).isEmpty();
  }

  @Test
  void findAllReturnsEveryNominatedStudent() {
    List<NominatedStudent> students = inTransaction(() -> nominatedStudentRepository.findAll());

    assertThat(students).extracting(NominatedStudent::getId).containsExactlyInAnyOrder(1001, 1004);
    assertThat(students)
        .filteredOn(s -> s.getId() == 1004)
        .singleElement()
        .satisfies(david -> {
          assertThat(david.getCardHolder()).isEqualTo("David Petit");
          assertThat(david.getBic()).isEqualTo("PSSTFRPPXXX");
          assertThat(david.getNationality().getName()).isEqualTo("France");
          assertThat(david.getVersion()).isEqualTo(2);
        });
  }

  @Test
  void createInsertsTheStudentRow() {
    runInTransaction(() -> {
      NominatedStudent student = newStudent(1003, null);
      student.setVersion(1);

      NominatedStudent created = nominatedStudentRepository.create(student);

      assertThat(created.getVersion()).isEqualTo(1);
      assertThat(queryForRow(STUDENT_BY_ID, 1003))
          .containsEntry("title", "Mrs")
          .containsEntry("birthdate", Timestamp.valueOf(LocalDate.of(2002, 2, 28).atStartOfDay()))
          .containsEntry("nationality", "CA")
          .containsEntry("address", 2003)
          .containsEntry("phone_number", "+15140000009")
          .containsEntry("gender", "F")
          .containsEntry("passed_years_count", 1)
          .containsEntry("iban", "BE71096123456769")
          .containsEntry("card_holder", null)
          .containsEntry("bank_name", "BNP Paribas Fortis")
          .containsEntry("bic", "GEBABEBB")
          .containsEntry("version", 1);
      NominatedStudent found = nominatedStudentRepository.findById(1003).orElse(null);
      assertThat(found.getFirstName()).isEqualTo("Chloé");
      assertThat(found.getNationality().getName()).isEqualTo("Canada");
    });
  }

  /**
   * A nominated student shares the version of its user (the use case copies the user's version into
   * the DTO): the row stores it and the returned DTO carries the same version.
   */
  @Test
  void createStoresAndReturnsTheVersionCarriedByTheDto() {
    runInTransaction(() -> {
      NominatedStudent student = newStudent(1002, "Bob Dupont");
      student.setVersion(3);

      assertThat(nominatedStudentRepository.create(student).getVersion()).isEqualTo(3);
      assertThat(queryForRow(STUDENT_BY_ID, 1002))
          .containsEntry("version", 3)
          .containsEntry("card_holder", "Bob Dupont");
      assertThat(nominatedStudentRepository.findById(1002).orElseThrow().getVersion()).isEqualTo(3);
    });
  }

  @Test
  void createForAnAlreadyNominatedStudentViolatesThePrimaryKey() {
    runInTransaction(() ->
        assertThatThrownBy(() -> nominatedStudentRepository.create(newStudent(1001, null)))
            .isInstanceOf(DataIntegrityViolationException.class));
  }

  @Test
  void createForAnUnknownUserIsRejectedByTheForeignKey() {
    runInTransaction(() ->
        assertThatThrownBy(() -> nominatedStudentRepository.create(newStudent(9999, null)))
            .isInstanceOf(DataIntegrityViolationException.class));
  }

  @Test
  void updateWritesTheStudentColumnsButNotTheAddressAndIncrementsTheVersion() {
    runInTransaction(() -> {
      NominatedStudent david = nominatedStudentRepository.findById(1004).orElse(null);
      david.setTitle("M");
      david.setBirthdate(LocalDate.of(2000, 1, 1));
      david.getNationality().setCountryCode("DE");
      david.setPhoneNumber("+4915100000");
      david.setGender("O");
      david.setNbrPassedYears(4);
      david.setIban("DE89370400440532013000");
      david.setCardHolder(null);
      david.setBankName("Commerzbank");
      david.setBic("COBADEFFXXX");
      david.getAddress().setId(2003);

      NominatedStudent updated = nominatedStudentRepository.update(david);

      assertThat(updated.getVersion()).isEqualTo(3);
      assertThat(queryForRow(STUDENT_BY_ID, 1004))
          .containsEntry("title", "M")
          .containsEntry("birthdate", Timestamp.valueOf(LocalDateTime.of(2000, 1, 1, 0, 0)))
          .containsEntry("nationality", "DE")
          .containsEntry("phone_number", "+4915100000")
          .containsEntry("gender", "O")
          .containsEntry("passed_years_count", 4)
          .containsEntry("iban", "DE89370400440532013000")
          .containsEntry("card_holder", null)
          .containsEntry("bank_name", "Commerzbank")
          .containsEntry("bic", "COBADEFFXXX")
          .containsEntry("address", 2004)
          .containsEntry("version", 3);
    });
  }

  @Test
  void aStudentWithoutVersionIsFoundWithVersionZeroAndCannotBeUpdated() {
    runInTransaction(() -> {
      execute(
          "UPDATE student_exchange_tools.nominated_students SET version = NULL "
              + "WHERE user_id = 1004");

      NominatedStudent david = nominatedStudentRepository.findById(1004).orElse(null);
      assertThat(david.getVersion()).isZero();
      david.setPhoneNumber("+32 2 000 00 00");
      assertThatThrownBy(() -> nominatedStudentRepository.update(david))
          .isInstanceOf(ConcurrentModificationException.class);
      assertThat(queryForRow(STUDENT_BY_ID, 1004)).containsEntry("version", null);
    });
  }

  @Test
  void updateWithAStaleVersionFailsAndLeavesTheRowUntouched() {
    runInTransaction(() -> {
      NominatedStudent stale = nominatedStudentRepository.findById(1004).orElse(null);
      stale.setVersion(1);
      stale.setBankName("Other");

      assertThatThrownBy(() -> nominatedStudentRepository.update(stale))
          .isInstanceOf(ConcurrentModificationException.class);
      assertThat(queryForRow(STUDENT_BY_ID, 1004))
          .containsEntry("bank_name", "La Banque Postale")
          .containsEntry("version", 2);
    });
  }
}
