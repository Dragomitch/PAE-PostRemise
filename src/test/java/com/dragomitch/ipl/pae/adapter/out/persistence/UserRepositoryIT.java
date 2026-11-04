package com.dragomitch.ipl.pae.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dragomitch.ipl.pae.domain.model.Option;
import com.dragomitch.ipl.pae.domain.model.Role;
import com.dragomitch.ipl.pae.domain.model.User;
import com.dragomitch.ipl.pae.domain.port.out.UserRepository;
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
import org.springframework.dao.DataIntegrityViolationException;

class UserRepositoryIT extends AbstractRepositoryIT {

  private static final String USER_BY_ID =
      "SELECT * FROM student_exchange_tools.users WHERE user_id = ?";

  @Autowired private UserRepository userRepository;

  @Override
  protected List<String> fixtures() {
    return List.of("db/fixtures/users.sql");
  }

  private User newUser(String username, String optionCode) {
    User user = new User();
    user.setUsername(username);
    user.setLastName("Lambert");
    user.setFirstName("Emma");
    user.setEmail(username + "@student.test");
    user.setPassword("hash-" + username);
    Option option = new Option();
    option.setCode(optionCode);
    user.setOption(option);
    user.setRole(Role.STUDENT);
    user.setRegistrationDate(LocalDateTime.of(2025, 3, 4, 5, 6, 7));
    return user;
  }

  @Test
  void createInsertsEveryColumnAndReturnsTheGeneratedIdWithVersionOne() {
    User found =
        inTransaction(() -> {
          User created = userRepository.create(newUser("emma", "BDI"));

          assertThat(created.getId()).isGreaterThanOrEqualTo(100000);
          assertThat(created.getVersion()).isEqualTo(1);
          Map<String, Object> row = queryForRow(USER_BY_ID, created.getId());
          assertThat(row)
              .containsEntry("username", "emma")
              .containsEntry("last_name", "Lambert")
              .containsEntry("first_name", "Emma")
              .containsEntry("email", "emma@student.test")
              .containsEntry("password", "hash-emma")
              .containsEntry("role", "Student")
              .containsEntry("option", "BDI")
              .containsEntry("version", 1)
              .containsEntry(
                  "registration_date", Timestamp.valueOf(LocalDateTime.of(2025, 3, 4, 5, 6, 7)));
          return userRepository.findById(created.getId()).orElse(null);
        });

    assertThat(found.getUsername()).isEqualTo("emma");
    assertThat(found.getOption().getName()).isEqualTo("Bachelier en diététique");
    assertThat(found.getRegistrationDate()).isEqualTo(LocalDateTime.of(2025, 3, 4, 5, 6, 7));
  }

  @Test
  void createWithAnUnknownOptionIsRejectedByTheForeignKey() {
    runInTransaction(() ->
        assertThatThrownBy(() -> userRepository.create(newUser("zed", "XYZ")))
            .isInstanceOf(DataIntegrityViolationException.class));
  }

  @Test
  void findByIdMapsEveryField() {
    User user = inTransaction(() -> userRepository.findById(1002).orElse(null));

    assertThat(user.getId()).isEqualTo(1002);
    assertThat(user.getUsername()).isEqualTo("bob");
    assertThat(user.getLastName()).isEqualTo("Dupont");
    assertThat(user.getFirstName()).isEqualTo("Bob");
    assertThat(user.getEmail()).isEqualTo("bob@prof.test");
    assertThat(user.getPassword()).isEqualTo("hash-bob");
    assertThat(user.getRole()).isEqualTo(Role.PROFESSOR);
    assertThat(user.getRegistrationDate()).isEqualTo(LocalDateTime.of(2020, 1, 15, 10, 0));
    assertThat(user.getVersion()).isEqualTo(3);
    assertThat(user.getOption().getCode()).isEqualTo("BIN");
    assertThat(user.getOption().getName()).isEqualTo("Bachelier en informatique de gestion");
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1, 999, 100000})
  void findByIdReturnsNullWhenAbsent(int id) {
    assertThat(inTransaction(() -> userRepository.findById(id))).isEmpty();
  }

  @Test
  void findAllReturnsEveryUser() {
    List<User> users = inTransaction(() -> userRepository.findAll());

    assertThat(users)
        .extracting(User::getUsername)
        .containsExactlyInAnyOrder("alice", "bob", "chloe", "david");
    assertThat(users)
        .filteredOn(u -> u.getId() == 1003)
        .singleElement()
        .satisfies(chloe -> {
          assertThat(chloe.getFirstName()).isEqualTo("Chloé");
          assertThat(chloe.getOption().getCode()).isEqualTo("BCH");
        });
  }

  @Test
  void findAllReturnsAnEmptyListWithoutUsers() {
    List<User> users =
        inTransaction(() -> {
          execute("DELETE FROM student_exchange_tools.users");
          return userRepository.findAll();
        });
    assertThat(users).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({"alice, 1001", "david, 1004"})
  void findByUsernameReturnsTheUserWithItsOption(String username, int expectedId) {
    User user = inTransaction(() -> userRepository.findByUsername(username)).orElseThrow();

    assertThat(user.getId()).isEqualTo(expectedId);
    assertThat(user.getOption()).isNotNull();
  }

  @Test
  void findByEmailReturnsTheUserWithItsOption() {
    User user = inTransaction(() -> userRepository.findByEmail("bob@prof.test")).orElseThrow();

    assertThat(user.getId()).isEqualTo(1002);
    assertThat(user.getOption()).isNotNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {"Alice", "nobody"})
  void findByUsernameIsCaseSensitiveAndEmptyWhenAbsent(String username) {
    assertThat(inTransaction(() -> userRepository.findByUsername(username))).isEmpty();
  }

  @Test
  void findByEmailIsCaseSensitiveAndEmptyWhenAbsent() {
    assertThat(inTransaction(() -> userRepository.findByEmail("ALICE@student.test"))).isEmpty();
  }

  @Test
  void updateWritesTheFieldsAndIncrementsTheVersion() {
    runInTransaction(() -> {
      User david = userRepository.findById(1004).orElse(null);
      david.setUsername("dpetit");
      david.setLastName("Petit-Jean");
      david.setFirstName("Dave");
      david.setEmail("dave@student.test");
      david.setPassword("new-hash");
      david.getOption().setCode("BIM");
      david.setRole(Role.PROFESSOR);
      david.setRegistrationDate(LocalDateTime.of(1999, 1, 1, 0, 0));

      userRepository.update(david);

      assertThat(david.getVersion()).isEqualTo(3);
      Map<String, Object> row = queryForRow(USER_BY_ID, 1004);
      assertThat(row)
          .containsEntry("username", "dpetit")
          .containsEntry("last_name", "Petit-Jean")
          .containsEntry("first_name", "Dave")
          .containsEntry("email", "dave@student.test")
          .containsEntry("password", "new-hash")
          .containsEntry("option", "BIM")
          .containsEntry("role", "Professor")
          .containsEntry("version", 3)
          // the registration date is not updatable
          .containsEntry(
              "registration_date", Timestamp.valueOf(LocalDateTime.of(2024, 9, 3, 17, 45)));
    });
  }

  @Test
  void updateWithAStaleVersionFailsAndLeavesTheRowUntouched() {
    runInTransaction(() -> {
      User stale = userRepository.findById(1002).orElse(null);
      stale.setVersion(2);
      stale.setUsername("hacker");

      assertThatThrownBy(() -> userRepository.update(stale))
          .isInstanceOf(ConcurrentModificationException.class);
      assertThat(queryForRow(USER_BY_ID, 1002))
          .containsEntry("username", "bob")
          .containsEntry("version", 3);
    });
  }

  @Test
  void updateOfAnUnknownUserFails() {
    runInTransaction(() -> {
      User ghost = newUser("ghost", "BIN");
      ghost.setId(424242);
      ghost.setVersion(1);
      assertThatThrownBy(() -> userRepository.update(ghost))
          .isInstanceOf(ConcurrentModificationException.class);
    });
  }

  @Test
  void promoteToProfessorByIdChangesTheRoleAndIncrementsTheVersion() {
    runInTransaction(() -> {
      assertThat(userRepository.promoteToProfessor(1004, 2)).isEqualTo(3);

      assertThat(queryForRow(USER_BY_ID, 1004))
          .containsEntry("role", "Professor")
          .containsEntry("version", 3)
          .containsEntry("username", "david");
      assertThat(queryForRow(USER_BY_ID, 1003))
          .containsEntry("role", "Student")
          .containsEntry("version", 1);
    });
  }

  @Test
  void promoteToProfessorByUsernameChangesTheRoleAndIncrementsTheVersion() {
    runInTransaction(() -> {
      assertThat(userRepository.promoteToProfessor("alice", 1)).isEqualTo(2);

      User alice = userRepository.findById(1001).orElse(null);
      assertThat(alice.getRole()).isEqualTo(Role.PROFESSOR);
      assertThat(alice.getVersion()).isEqualTo(2);
      assertThat(userRepository.findById(1003).orElseThrow().getRole()).isEqualTo(Role.STUDENT);
    });
  }

  @ParameterizedTest(name = "user {0}, expected version {1}")
  @CsvSource({"1004, 1", "1004, 3", "424242, 1"})
  void promoteToProfessorByIdWithAStaleVersionOrAnUnknownIdFailsAndWritesNothing(
      int id, int version) {
    runInTransaction(() -> {
      assertThatThrownBy(() -> userRepository.promoteToProfessor(id, version))
          .isInstanceOf(ConcurrentModificationException.class);
      assertThat(queryForRow(USER_BY_ID, 1004))
          .containsEntry("role", "Student")
          .containsEntry("version", 2);
    });
  }

  @ParameterizedTest(name = "user {0}, expected version {1}")
  @CsvSource({"alice, 2", "Alice, 1", "nobody, 1"})
  void promoteToProfessorByUsernameWithAStaleVersionOrAnUnknownUsernameFailsAndWritesNothing(
      String username, int version) {
    runInTransaction(() -> {
      assertThatThrownBy(() -> userRepository.promoteToProfessor(username, version))
          .isInstanceOf(ConcurrentModificationException.class);
      assertThat(queryForRow(USER_BY_ID, 1001))
          .containsEntry("role", "Student")
          .containsEntry("version", 1);
    });
  }

  @Test
  void isEmptyIsFalseWhenAtLeastOneUserExists() {
    assertThat(inTransaction(() -> userRepository.isEmpty())).isFalse();
  }

  @Test
  void isEmptyIsTrueWithoutUsers() {
    assertThat(
            inTransaction(() -> {
              execute("DELETE FROM student_exchange_tools.users");
              return userRepository.isEmpty();
            }))
        .isTrue();
  }
}
