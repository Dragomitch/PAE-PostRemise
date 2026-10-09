package com.dragomitch.ipl.pae.persistence.implementations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dragomitch.ipl.pae.business.dto.AddressDto;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.business.dto.OptionDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.exceptions.FatalException;
import com.dragomitch.ipl.pae.persistence.AddressDao;
import com.dragomitch.ipl.pae.persistence.DenialReasonDao;
import com.dragomitch.ipl.pae.persistence.NominatedStudentDao;
import com.dragomitch.ipl.pae.persistence.UserDao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

/**
 * The DAOs migrated to Spring Data JDBC and the hand-written JDBC DAOs work on the same
 * connection and in the same transaction, the one opened by the caller: what one writes, the other
 * sees before the commit, and a rollback undoes both.
 */
class SharedTransactionIT extends AbstractDaoIT {

  private static final String COUNT_TEST_ROWS = "SELECT"
      + " (SELECT count(*) FROM student_exchange_tools.users WHERE username = 'shared') AS users,"
      + " (SELECT count(*) FROM student_exchange_tools.addresses WHERE street = 'Rue Partagée')"
      + " AS addresses,"
      + " (SELECT count(*) FROM student_exchange_tools.nominated_students n"
      + "    JOIN student_exchange_tools.users u ON u.user_id = n.user_id"
      + "   WHERE u.username = 'shared') AS students";

  @Autowired
  private UserDao userDao;

  @Autowired
  private AddressDao addressDao;

  @Autowired
  private DenialReasonDao denialReasonDao;

  @Autowired
  private NominatedStudentDao nominatedStudentDao;

  @Autowired
  private NamedParameterJdbcOperations springDataJdbcOperations;

  @Test
  void springDataAndTheJdbcDaosRunOnTheConnectionOfTheCurrentTransaction() {
    runInTransaction(() -> {
      Object transactionOfTheJdbcDaos = queryForRow("SELECT txid_current() AS id").get("id");
      Long transactionOfSpringData = springDataJdbcOperations
          .queryForObject("SELECT txid_current()", Map.of(), Long.class);

      assertThat(transactionOfSpringData).isEqualTo(transactionOfTheJdbcDaos);
    });
  }

  @Test
  void aJdbcDaoSeesWhatSpringDataWroteAndTheRollbackUndoesBoth() {
    int[] addressId = new int[1];
    NominatedStudentDto found = inTransaction(() -> {
      // Spring Data JDBC (UserDao, AddressDao)...
      UserDto user = userDao.create(newUser());
      AddressDto address = addressDao.create(newAddress());
      addressId[0] = address.getId();
      // ...then the hand-written JDBC DAO, whose foreign keys need the uncommitted rows
      nominatedStudentDao.create(newStudent(user.getId(), address.getId()));
      assertThat(queryForRow(COUNT_TEST_ROWS)).containsEntry("users", 1L)
          .containsEntry("addresses", 1L).containsEntry("students", 1L);
      return nominatedStudentDao.findById(user.getId());
    });

    assertThat(found.getUsername()).isEqualTo("shared");
    assertThat(found.getAddress().getId()).isEqualTo(addressId[0]);
    // AbstractDaoIT always rolls back: nothing of the above was committed
    assertThat(inTransaction(() -> queryForRow(COUNT_TEST_ROWS))).containsEntry("users", 0L)
        .containsEntry("addresses", 0L).containsEntry("students", 0L);
  }

  @Test
  void springDataSeesWhatPlainJdbcWroteInTheSameTransaction() {
    runInTransaction(() -> {
      execute("INSERT INTO student_exchange_tools.denial_reasons (reason_id, reason) "
          + "VALUES (4242, 'Écrit en JDBC')");

      assertThat(denialReasonDao.findById(4242).getReason()).isEqualTo("Écrit en JDBC");
    });
  }

  @Test
  void aMigratedDaoRefusesToRunOutsideATransaction() {
    assertThatThrownBy(() -> denialReasonDao.findAll())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("outside a transaction");
  }

  @Test
  void userDaoFindByRejectsAColumnThatIsNotOfTheUsersTable() {
    // the column name is resolved against the mapping, never concatenated into the SQL
    runInTransaction(() -> assertThatThrownBy(
        () -> userDao.findBy("username = username OR 1", "1"))
        .isInstanceOf(FatalException.class)
        .hasRootCauseInstanceOf(IllegalArgumentException.class));
  }

  private UserDto newUser() {
    UserDto user = build(UserDto.class);
    user.setUsername("shared");
    user.setLastName("Lambert");
    user.setFirstName("Emma");
    user.setEmail("shared@student.test");
    user.setPassword("hash");
    OptionDto option = build(OptionDto.class);
    option.setCode("BIN");
    user.setOption(option);
    user.setRole(UserDto.ROLE_STUDENT);
    user.setRegistrationDate(LocalDateTime.of(2025, 3, 4, 5, 6, 7));
    return user;
  }

  private AddressDto newAddress() {
    AddressDto address = build(AddressDto.class);
    address.setStreet("Rue Partagée");
    address.setNumber("1");
    CountryDto country = build(CountryDto.class);
    country.setCountryCode("BE");
    address.setCountry(country);
    address.setCity("Liège");
    address.setPostalCode("4000");
    return address;
  }

  private NominatedStudentDto newStudent(int userId, int addressId) {
    NominatedStudentDto student = build(NominatedStudentDto.class);
    student.setId(userId);
    student.setTitle("Ms");
    student.setBirthdate(LocalDate.of(2002, 2, 28));
    CountryDto nationality = build(CountryDto.class);
    nationality.setCountryCode("BE");
    student.setNationality(nationality);
    AddressDto address = build(AddressDto.class);
    address.setId(addressId);
    student.setAddress(address);
    student.setPhoneNumber("+32470000009");
    student.setGender("F");
    student.setNbrPassedYears(1);
    student.setIban("BE71096123456769");
    student.setBankName("BNP Paribas Fortis");
    student.setBic("GEBABEBB");
    return student;
  }
}
