package com.dragomitch.ipl.pae.persistence.implementations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dragomitch.ipl.pae.business.dto.AddressDto;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.exceptions.FatalException;
import com.dragomitch.ipl.pae.persistence.NominatedStudentDao;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ConcurrentModificationException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

class NominatedStudentDaoIT extends AbstractDaoIT {

  private static final String STUDENT_BY_ID =
      "SELECT * FROM student_exchange_tools.nominated_students WHERE user_id = ?";

  @Autowired
  private NominatedStudentDao nominatedStudentDao;

  @Override
  protected List<String> fixtures() {
    return List.of("db/fixtures/users.sql", "db/fixtures/partners.sql",
        "db/fixtures/nominated_students.sql");
  }

  private NominatedStudentDto newStudent(int userId, String cardHolder) {
    NominatedStudentDto student = build(NominatedStudentDto.class);
    student.setId(userId);
    student.setTitle("Mrs");
    student.setBirthdate(LocalDate.of(2002, 2, 28));
    CountryDto nationality = build(CountryDto.class);
    nationality.setCountryCode("CA");
    student.setNationality(nationality);
    AddressDto address = build(AddressDto.class);
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
    NominatedStudentDto alice = inTransaction(() -> nominatedStudentDao.findById(1001));

    // user part
    assertThat(alice.getId()).isEqualTo(1001);
    assertThat(alice.getUsername()).isEqualTo("alice");
    assertThat(alice.getLastName()).isEqualTo("Martin");
    assertThat(alice.getFirstName()).isEqualTo("Alice");
    assertThat(alice.getPassword()).isEqualTo("hash-alice");
    assertThat(alice.getEmail()).isEqualTo("alice@student.test");
    assertThat(alice.getRegistrationDate()).isEqualTo(LocalDateTime.of(2024, 9, 1, 8, 30));
    assertThat(alice.getRole()).isEqualTo(UserDto.ROLE_STUDENT);
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
    assertThat(inTransaction(() -> nominatedStudentDao.findById(id))).isNull();
  }

  @Test
  void findAllReturnsEveryNominatedStudent() {
    List<NominatedStudentDto> students = inTransaction(() -> nominatedStudentDao.findAll());

    assertThat(students).extracting(NominatedStudentDto::getId).containsExactlyInAnyOrder(1001,
        1004);
    assertThat(students).filteredOn(s -> s.getId() == 1004).singleElement().satisfies(david -> {
      assertThat(david.getCardHolder()).isEqualTo("David Petit");
      assertThat(david.getBic()).isEqualTo("PSSTFRPPXXX");
      assertThat(david.getNationality().getName()).isEqualTo("France");
      assertThat(david.getVersion()).isEqualTo(2);
    });
  }

  @Test
  void createInsertsTheStudentRow() {
    runInTransaction(() -> {
      NominatedStudentDto student = newStudent(1003, null);
      student.setVersion(1);

      NominatedStudentDto created = nominatedStudentDao.create(student);

      assertThat(created.getVersion()).isEqualTo(1);
      assertThat(queryForRow(STUDENT_BY_ID, 1003)).containsEntry("title", "Mrs")
          .containsEntry("birthdate", Timestamp.valueOf(LocalDate.of(2002, 2, 28).atStartOfDay()))
          .containsEntry("nationality", "CA").containsEntry("address", 2003)
          .containsEntry("phone_number", "+15140000009").containsEntry("gender", "F")
          .containsEntry("passed_years_count", 1).containsEntry("iban", "BE71096123456769")
          .containsEntry("card_holder", null).containsEntry("bank_name", "BNP Paribas Fortis")
          .containsEntry("bic", "GEBABEBB").containsEntry("version", 1);
      NominatedStudentDto found = nominatedStudentDao.findById(1003);
      assertThat(found.getFirstName()).isEqualTo("Chloé");
      assertThat(found.getNationality().getName()).isEqualTo("Canada");
    });
  }

  /**
   * Legacy behaviour: the version column receives the version carried by the DTO (the use case
   * copies the user's version into it) while the returned DTO always says 1.
   */
  @Test
  void createStoresTheVersionCarriedByTheDtoButReturnsVersionOne() {
    runInTransaction(() -> {
      NominatedStudentDto student = newStudent(1002, "Bob Dupont");
      student.setVersion(3);

      assertThat(nominatedStudentDao.create(student).getVersion()).isEqualTo(1);
      assertThat(queryForRow(STUDENT_BY_ID, 1002)).containsEntry("version", 3)
          .containsEntry("card_holder", "Bob Dupont");
    });
  }

  @Test
  void createForAnAlreadyNominatedStudentViolatesThePrimaryKey() {
    runInTransaction(() -> assertThatThrownBy(
        () -> nominatedStudentDao.create(newStudent(1001, null)))
        .isInstanceOf(FatalException.class));
  }

  @Test
  void createForAnUnknownUserIsRejectedByTheForeignKey() {
    runInTransaction(() -> assertThatThrownBy(
        () -> nominatedStudentDao.create(newStudent(9999, null)))
        .isInstanceOf(FatalException.class));
  }

  @Test
  void updateWritesTheStudentColumnsButNotTheAddressAndIncrementsTheVersion() {
    runInTransaction(() -> {
      NominatedStudentDto david = nominatedStudentDao.findById(1004);
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

      NominatedStudentDto updated = nominatedStudentDao.update(david);

      assertThat(updated.getVersion()).isEqualTo(3);
      assertThat(queryForRow(STUDENT_BY_ID, 1004)).containsEntry("title", "M")
          .containsEntry("birthdate", Timestamp.valueOf(LocalDateTime.of(2000, 1, 1, 0, 0)))
          .containsEntry("nationality", "DE").containsEntry("phone_number", "+4915100000")
          .containsEntry("gender", "O").containsEntry("passed_years_count", 4)
          .containsEntry("iban", "DE89370400440532013000").containsEntry("card_holder", null)
          .containsEntry("bank_name", "Commerzbank").containsEntry("bic", "COBADEFFXXX")
          .containsEntry("address", 2004).containsEntry("version", 3);
    });
  }

  @Test
  void updateWithAStaleVersionFailsAndLeavesTheRowUntouched() {
    runInTransaction(() -> {
      NominatedStudentDto stale = nominatedStudentDao.findById(1004);
      stale.setVersion(1);
      stale.setBankName("Other");

      assertThatThrownBy(() -> nominatedStudentDao.update(stale))
          .isInstanceOf(ConcurrentModificationException.class);
      assertThat(queryForRow(STUDENT_BY_ID, 1004)).containsEntry("bank_name", "La Banque Postale")
          .containsEntry("version", 2);
    });
  }
}
