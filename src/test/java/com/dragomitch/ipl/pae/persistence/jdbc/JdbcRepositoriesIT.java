package com.dragomitch.ipl.pae.persistence.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dragomitch.ipl.pae.persistence.implementations.AbstractDaoIT;
import com.dragomitch.ipl.pae.persistence.jdbc.entity.AddressEntity;
import com.dragomitch.ipl.pae.persistence.jdbc.entity.CountryEntity;
import com.dragomitch.ipl.pae.persistence.jdbc.entity.DocumentEntity;
import com.dragomitch.ipl.pae.persistence.jdbc.entity.UserEntity;
import com.dragomitch.ipl.pae.persistence.jdbc.repository.AddressRepository;
import com.dragomitch.ipl.pae.persistence.jdbc.repository.CountryRepository;
import com.dragomitch.ipl.pae.persistence.jdbc.repository.DenialReasonRepository;
import com.dragomitch.ipl.pae.persistence.jdbc.repository.DocumentRepository;
import com.dragomitch.ipl.pae.persistence.jdbc.repository.UserRepository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.jdbc.core.JdbcAggregateOperations;
import org.springframework.data.jdbc.core.mapping.AggregateReference;

/**
 * The Spring Data JDBC mapping and the repository methods written for this schema (derived
 * queries, {@code @Query}, {@code @Version}, {@code @InsertOnlyProperty}), against the real
 * database. The DAO integration tests cover the DAOs built on top of them.
 */
class JdbcRepositoriesIT extends AbstractDaoIT {

  private static final String ADDRESS_BY_ID =
      "SELECT * FROM student_exchange_tools.addresses WHERE address_id = ?";
  private static final String USER_BY_ID =
      "SELECT * FROM student_exchange_tools.users WHERE user_id = ?";

  @Autowired
  private CountryRepository countries;

  @Autowired
  private DocumentRepository documents;

  @Autowired
  private DenialReasonRepository denialReasons;

  @Autowired
  private AddressRepository addresses;

  @Autowired
  private UserRepository users;

  @Autowired
  private JdbcAggregateOperations aggregates;

  @Override
  protected List<String> fixtures() {
    return List.of("db/fixtures/users.sql", "db/fixtures/partners.sql");
  }

  @Nested
  class DerivedQueries {

    @Test
    void countriesAreSortedByNameByTheDatabaseAndReferenceTheirProgrammeById() {
      List<CountryEntity> sorted = inTransaction(() -> countries.findAllByOrderByNameAsc());
      List<String> expected = inTransaction(
          () -> query("SELECT name FROM student_exchange_tools.countries ORDER BY name")).stream()
          .map(row -> (String) row.get("name")).toList();

      assertThat(sorted).extracting(CountryEntity::name).containsExactlyElementsOf(expected);
      assertThat(sorted).filteredOn(country -> country.code().equals("BE")).singleElement()
          .satisfies(be -> assertThat(be.programme().getId()).isEqualTo(2));
    }

    @Test
    void documentsAreFoundByTheIdOfTheirProgramme() {
      List<DocumentEntity> erabel =
          inTransaction(() -> documents.findByProgramme(AggregateReference.to(2)));

      assertThat(erabel).hasSize(7).allSatisfy(document -> {
        assertThat(document.programme().getId()).isEqualTo(2);
        assertThat(document.category()).isIn("D", "R");
      });
      assertThat(inTransaction(() -> documents.findByProgramme(AggregateReference.to(99))))
          .isEmpty();
    }
  }

  @Nested
  class ModifyingQueries {

    @Test
    void updateReasonReportsTheNumberOfUpdatedRows() {
      runInTransaction(() -> {
        assertThat(denialReasons.updateReason(1, "Autre motif")).isEqualTo(1);
        assertThat(denialReasons.findById(1).orElseThrow().reason()).isEqualTo("Autre motif");
        assertThat(denialReasons.updateReason(31337, "ghost")).isZero();
      });
    }

    @Test
    void updateRoleByIdChecksAndIncrementsTheVersion() {
      runInTransaction(() -> {
        assertThat(users.updateRoleById(1004, "Professor", 1)).isZero();
        assertThat(users.updateRoleById(424242, "Professor", 2)).isZero();
        assertThat(queryForRow(USER_BY_ID, 1004)).containsEntry("role", "Student")
            .containsEntry("version", 2);

        assertThat(users.updateRoleById(1004, "Professor", 2)).isEqualTo(1);
        assertThat(queryForRow(USER_BY_ID, 1004)).containsEntry("role", "Professor")
            .containsEntry("version", 3);
      });
    }

    @Test
    void updateRoleByUsernameChecksAndIncrementsTheVersion() {
      runInTransaction(() -> {
        assertThat(users.updateRoleByUsername("david", "Professor", 1)).isZero();
        assertThat(users.updateRoleByUsername("David", "Professor", 2)).isZero();

        assertThat(users.updateRoleByUsername("david", "Professor", 2)).isEqualTo(1);
        assertThat(queryForRow(USER_BY_ID, 1004)).containsEntry("role", "Professor")
            .containsEntry("version", 3);
      });
    }
  }

  @Nested
  class OptimisticLocking {

    private AddressEntity newAddress() {
      return new AddressEntity(null, "Rue Neuve", "1", AggregateReference.to("BE"), "Bruxelles",
          "1000", null, 0);
    }

    @Test
    void anInsertedAggregateStartsAtVersionOne() {
      runInTransaction(() -> {
        AddressEntity created = aggregates.insert(newAddress());

        assertThat(created.id()).isGreaterThanOrEqualTo(100000);
        assertThat(created.version()).isEqualTo(1);
        assertThat(queryForRow(ADDRESS_BY_ID, created.id())).containsEntry("version", 1);
      });
    }

    @Test
    void anUpdateChecksAndIncrementsTheVersion() {
      runInTransaction(() -> {
        AddressEntity address = addresses.findById(2002).orElseThrow();
        assertThat(address.version()).isEqualTo(4);

        AddressEntity updated = aggregates.update(new AddressEntity(address.id(), "Place Flagey",
            address.number(), address.country(), address.city(), address.postalCode(),
            address.region(), address.version()));

        assertThat(updated.version()).isEqualTo(5);
        assertThat(queryForRow(ADDRESS_BY_ID, 2002)).containsEntry("street", "Place Flagey")
            .containsEntry("version", 5);
      });
    }

    @Test
    void anUpdateWithAStaleVersionOrAnUnknownIdIsAnOptimisticLockingFailure() {
      runInTransaction(() -> {
        AddressEntity address = addresses.findById(2002).orElseThrow();
        AddressEntity stale = new AddressEntity(address.id(), "Stale", address.number(),
            address.country(), address.city(), address.postalCode(), address.region(), 3);
        AddressEntity ghost = new AddressEntity(777, "Ghost", "1", AggregateReference.to("BE"),
            "Bruxelles", "1000", null, 1);

        assertThatThrownBy(() -> aggregates.update(stale))
            .isInstanceOf(OptimisticLockingFailureException.class);
        assertThatThrownBy(() -> aggregates.update(ghost))
            .isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(queryForRow(ADDRESS_BY_ID, 2002)).containsEntry("street", address.street())
            .containsEntry("version", 4);
      });
    }

    @Test
    void theRegistrationDateOfAUserIsInsertOnly() {
      runInTransaction(() -> {
        UserEntity david = users.findById(1004).orElseThrow();

        aggregates.update(new UserEntity(david.id(), "dpetit", david.lastName(),
            david.firstName(), david.email(), david.password(), david.role(), david.option(),
            LocalDateTime.of(1999, 1, 1, 0, 0), david.version()));

        assertThat(queryForRow(USER_BY_ID, 1004)).containsEntry("username", "dpetit")
            .containsEntry("version", 3).containsEntry("registration_date",
                Timestamp.valueOf(LocalDateTime.of(2024, 9, 3, 17, 45)));
      });
    }
  }
}
