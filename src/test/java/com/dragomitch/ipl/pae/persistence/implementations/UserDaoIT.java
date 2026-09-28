package com.dragomitch.ipl.pae.persistence.implementations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dragomitch.ipl.pae.business.dto.OptionDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.exceptions.FatalException;
import com.dragomitch.ipl.pae.persistence.UserDao;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

class UserDaoIT extends AbstractDaoIT {

  private static final String USER_BY_ID =
      "SELECT * FROM student_exchange_tools.users WHERE user_id = ?";

  @Autowired
  private UserDao userDao;

  @Override
  protected List<String> fixtures() {
    return List.of("db/fixtures/users.sql");
  }

  private UserDto newUser(String username, String optionCode) {
    UserDto user = build(UserDto.class);
    user.setUsername(username);
    user.setLastName("Lambert");
    user.setFirstName("Emma");
    user.setEmail(username + "@student.test");
    user.setPassword("hash-" + username);
    OptionDto option = build(OptionDto.class);
    option.setCode(optionCode);
    user.setOption(option);
    user.setRole(UserDto.ROLE_STUDENT);
    user.setRegistrationDate(LocalDateTime.of(2025, 3, 4, 5, 6, 7));
    return user;
  }

  @Test
  void createInsertsEveryColumnAndReturnsTheGeneratedIdWithVersionOne() {
    UserDto found = inTransaction(() -> {
      UserDto created = userDao.create(newUser("emma", "BDI"));

      assertThat(created.getId()).isGreaterThanOrEqualTo(100000);
      assertThat(created.getVersion()).isEqualTo(1);
      Map<String, Object> row = queryForRow(USER_BY_ID, created.getId());
      assertThat(row).containsEntry("username", "emma").containsEntry("last_name", "Lambert")
          .containsEntry("first_name", "Emma").containsEntry("email", "emma@student.test")
          .containsEntry("password", "hash-emma").containsEntry("role", "Student")
          .containsEntry("option", "BDI").containsEntry("version", 1)
          .containsEntry("registration_date",
              Timestamp.valueOf(LocalDateTime.of(2025, 3, 4, 5, 6, 7)));
      return userDao.findById(created.getId());
    });

    assertThat(found.getUsername()).isEqualTo("emma");
    assertThat(found.getOption().getName()).isEqualTo("Bachelier en diététique");
    assertThat(found.getRegistrationDate()).isEqualTo(LocalDateTime.of(2025, 3, 4, 5, 6, 7));
  }

  @Test
  void createWithAnUnknownOptionIsRejectedByTheForeignKey() {
    runInTransaction(() -> assertThatThrownBy(() -> userDao.create(newUser("zed", "XYZ")))
        .isInstanceOf(FatalException.class));
  }

  @Test
  void findByIdMapsEveryField() {
    UserDto user = inTransaction(() -> userDao.findById(1002));

    assertThat(user.getId()).isEqualTo(1002);
    assertThat(user.getUsername()).isEqualTo("bob");
    assertThat(user.getLastName()).isEqualTo("Dupont");
    assertThat(user.getFirstName()).isEqualTo("Bob");
    assertThat(user.getEmail()).isEqualTo("bob@prof.test");
    assertThat(user.getPassword()).isEqualTo("hash-bob");
    assertThat(user.getRole()).isEqualTo(UserDto.ROLE_PROFESSOR);
    assertThat(user.getRegistrationDate()).isEqualTo(LocalDateTime.of(2020, 1, 15, 10, 0));
    assertThat(user.getVersion()).isEqualTo(3);
    assertThat(user.getOption().getCode()).isEqualTo("BIN");
    assertThat(user.getOption().getName()).isEqualTo("Bachelier en informatique de gestion");
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1, 999, 100000})
  void findByIdReturnsNullWhenAbsent(int id) {
    assertThat(inTransaction(() -> userDao.findById(id))).isNull();
  }

  @Test
  void findAllReturnsEveryUser() {
    List<UserDto> users = inTransaction(() -> userDao.findAll());

    assertThat(users).extracting(UserDto::getUsername)
        .containsExactlyInAnyOrder("alice", "bob", "chloe", "david");
    assertThat(users).filteredOn(u -> u.getId() == 1003).singleElement().satisfies(chloe -> {
      assertThat(chloe.getFirstName()).isEqualTo("Chloé");
      assertThat(chloe.getOption().getCode()).isEqualTo("BCH");
    });
  }

  @Test
  void findAllReturnsAnEmptyListWithoutUsers() {
    List<UserDto> users = inTransaction(() -> {
      execute("DELETE FROM student_exchange_tools.users");
      return userDao.findAll();
    });
    assertThat(users).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({
      "username, alice, 1001",
      "username, david, 1004",
      "email, bob@prof.test, 1002",
      "last_name, Leroy, 1003"})
  void findByLooksUpAUserByAnyColumn(String column, String value, int expectedId) {
    UserDto user = inTransaction(() -> userDao.findBy(column, value));

    assertThat(user.getId()).isEqualTo(expectedId);
    assertThat(user.getOption()).isNotNull();
  }

  @ParameterizedTest
  @CsvSource({"username, Alice", "username, nobody", "email, ALICE@student.test"})
  void findByIsCaseSensitiveAndReturnsNullWhenAbsent(String column, String value) {
    assertThat(inTransaction(() -> userDao.findBy(column, value))).isNull();
  }

  @Test
  void updateWritesTheFieldsAndIncrementsTheVersion() {
    runInTransaction(() -> {
      UserDto david = userDao.findById(1004);
      david.setUsername("dpetit");
      david.setLastName("Petit-Jean");
      david.setFirstName("Dave");
      david.setEmail("dave@student.test");
      david.setPassword("new-hash");
      david.getOption().setCode("BIM");
      david.setRole(UserDto.ROLE_PROFESSOR);
      david.setRegistrationDate(LocalDateTime.of(1999, 1, 1, 0, 0));

      userDao.update(david);

      assertThat(david.getVersion()).isEqualTo(3);
      Map<String, Object> row = queryForRow(USER_BY_ID, 1004);
      assertThat(row).containsEntry("username", "dpetit").containsEntry("last_name", "Petit-Jean")
          .containsEntry("first_name", "Dave").containsEntry("email", "dave@student.test")
          .containsEntry("password", "new-hash").containsEntry("option", "BIM")
          .containsEntry("role", "Professor").containsEntry("version", 3)
          // the registration date is not updatable
          .containsEntry("registration_date",
              Timestamp.valueOf(LocalDateTime.of(2024, 9, 3, 17, 45)));
    });
  }

  @Test
  void updateWithAStaleVersionFailsAndLeavesTheRowUntouched() {
    runInTransaction(() -> {
      UserDto stale = userDao.findById(1002);
      stale.setVersion(2);
      stale.setUsername("hacker");

      assertThatThrownBy(() -> userDao.update(stale))
          .isInstanceOf(ConcurrentModificationException.class);
      assertThat(queryForRow(USER_BY_ID, 1002)).containsEntry("username", "bob")
          .containsEntry("version", 3);
    });
  }

  @Test
  void updateOfAnUnknownUserFails() {
    runInTransaction(() -> {
      UserDto ghost = newUser("ghost", "BIN");
      ghost.setId(424242);
      ghost.setVersion(1);
      assertThatThrownBy(() -> userDao.update(ghost))
          .isInstanceOf(ConcurrentModificationException.class);
    });
  }

  @Test
  void promoteToProfessorByIdChangesTheRoleAndIncrementsTheVersion() {
    runInTransaction(() -> {
      assertThat(userDao.promoteToProfessor(1004, 2)).isEqualTo(3);

      assertThat(queryForRow(USER_BY_ID, 1004)).containsEntry("role", "Professor")
          .containsEntry("version", 3).containsEntry("username", "david");
      assertThat(queryForRow(USER_BY_ID, 1003)).containsEntry("role", "Student")
          .containsEntry("version", 1);
    });
  }

  @Test
  void promoteToProfessorByUsernameChangesTheRoleAndIncrementsTheVersion() {
    runInTransaction(() -> {
      assertThat(userDao.promoteToProfessor("alice", 1)).isEqualTo(2);

      UserDto alice = userDao.findById(1001);
      assertThat(alice.getRole()).isEqualTo(UserDto.ROLE_PROFESSOR);
      assertThat(alice.getVersion()).isEqualTo(2);
      assertThat(userDao.findById(1003).getRole()).isEqualTo(UserDto.ROLE_STUDENT);
    });
  }

  @ParameterizedTest(name = "user {0}, expected version {1}")
  @CsvSource({"1004, 1", "1004, 3", "424242, 1"})
  void promoteToProfessorByIdWithAStaleVersionOrAnUnknownIdFailsAndWritesNothing(int id,
      int version) {
    runInTransaction(() -> {
      assertThatThrownBy(() -> userDao.promoteToProfessor(id, version))
          .isInstanceOf(ConcurrentModificationException.class);
      assertThat(queryForRow(USER_BY_ID, 1004)).containsEntry("role", "Student")
          .containsEntry("version", 2);
    });
  }

  @ParameterizedTest(name = "user {0}, expected version {1}")
  @CsvSource({"alice, 2", "Alice, 1", "nobody, 1"})
  void promoteToProfessorByUsernameWithAStaleVersionOrAnUnknownUsernameFailsAndWritesNothing(
      String username, int version) {
    runInTransaction(() -> {
      assertThatThrownBy(() -> userDao.promoteToProfessor(username, version))
          .isInstanceOf(ConcurrentModificationException.class);
      assertThat(queryForRow(USER_BY_ID, 1001)).containsEntry("role", "Student")
          .containsEntry("version", 1);
    });
  }

  @Test
  void isEmptyIsFalseWhenAtLeastOneUserExists() {
    assertThat(inTransaction(() -> userDao.isEmpty())).isFalse();
  }

  @Test
  void isEmptyIsTrueWithoutUsers() {
    assertThat(inTransaction(() -> {
      execute("DELETE FROM student_exchange_tools.users");
      return userDao.isEmpty();
    })).isTrue();
  }
}
