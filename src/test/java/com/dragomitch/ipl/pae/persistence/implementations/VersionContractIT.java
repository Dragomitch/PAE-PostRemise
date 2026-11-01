package com.dragomitch.ipl.pae.persistence.implementations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;

import com.dragomitch.ipl.pae.business.dto.AddressDto;
import com.dragomitch.ipl.pae.business.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.business.dto.MobilityDto;
import com.dragomitch.ipl.pae.business.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.persistence.AddressDao;
import com.dragomitch.ipl.pae.persistence.MobilityChoiceDao;
import com.dragomitch.ipl.pae.persistence.MobilityDao;
import com.dragomitch.ipl.pae.persistence.NominatedStudentDao;
import com.dragomitch.ipl.pae.persistence.PartnerDao;
import com.dragomitch.ipl.pae.persistence.UserDao;

import java.util.List;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Every update of a versioned row increments its version, <strong>even when it changes
 * nothing</strong>, as the JDBC DAOs did. The use cases rely on it: a partner edit takes the stored
 * partner version and detects a stale form through its address update.
 */
class VersionContractIT extends AbstractDaoIT {

  @Autowired
  private AddressDao addressDao;
  @Autowired
  private MobilityChoiceDao mobilityChoiceDao;
  @Autowired
  private MobilityDao mobilityDao;
  @Autowired
  private NominatedStudentDao nominatedStudentDao;
  @Autowired
  private PartnerDao partnerDao;
  @Autowired
  private UserDao userDao;

  @Override
  protected List<String> fixtures() {
    return List.of("db/fixtures/users.sql", "db/fixtures/partners.sql",
        "db/fixtures/nominated_students.sql", "db/fixtures/mobility_choices.sql",
        "db/fixtures/mobilities.sql");
  }

  /** An unchanged update of the row: it reads it, updates it as is and returns the new version. */
  private static Arguments update(String port, String table, String idColumn, int id,
      ToIntFunction<VersionContractIT> unchangedUpdate) {
    return Arguments.of(named(port, unchangedUpdate),
        "SELECT version FROM student_exchange_tools." + table + " WHERE " + idColumn + " = ?", id);
  }

  static Stream<Arguments> updates() {
    return Stream.of(
        update("AddressDao.update", "addresses", "address_id", 2002, it -> {
          AddressDto address = it.addressDao.findById(2002);
          return it.addressDao.update(address).getVersion();
        }),
        update("MobilityChoiceDao.update", "mobility_choices", "mobility_choice_id", 5001, it -> {
          MobilityChoiceDto choice = it.mobilityChoiceDao.findById(5001);
          it.mobilityChoiceDao.update(choice);
          return choice.getVersion();
        }),
        update("MobilityDao.update", "mobilities", "mobility_choice_id", 5006, it -> {
          MobilityDto mobility = it.mobilityDao.findById(5006);
          return it.mobilityDao.update(mobility).getVersion();
        }),
        update("NominatedStudentDao.update", "nominated_students", "user_id", 1004, it -> {
          NominatedStudentDto student = it.nominatedStudentDao.findById(1004);
          return it.nominatedStudentDao.update(student).getVersion();
        }),
        update("PartnerDao.update", "partners", "partner_id", 3001, it -> {
          PartnerDto partner = it.partnerDao.findById(3001);
          return it.partnerDao.update(partner).getVersion();
        }),
        update("UserDao.update", "users", "user_id", 1002, it -> {
          UserDto user = it.userDao.findById(1002);
          it.userDao.update(user);
          return user.getVersion();
        }));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("updates")
  void anUpdateThatChangesNothingStillIncrementsTheVersion(
      ToIntFunction<VersionContractIT> unchangedUpdate, String versionQuery, int id) {
    runInTransaction(() -> {
      int before = ((Number) queryForRow(versionQuery, id).get("version")).intValue();

      int returned = unchangedUpdate.applyAsInt(this);

      assertThat(returned).as("the version given to the DTO").isEqualTo(before + 1);
      assertThat(((Number) queryForRow(versionQuery, id).get("version")).intValue())
          .as("the stored version").isEqualTo(before + 1);
    });
  }
}
