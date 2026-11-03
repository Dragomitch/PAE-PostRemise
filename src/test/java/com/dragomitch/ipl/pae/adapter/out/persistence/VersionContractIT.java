package com.dragomitch.ipl.pae.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;

import com.dragomitch.ipl.pae.domain.dto.AddressDto;
import com.dragomitch.ipl.pae.domain.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.domain.dto.MobilityDto;
import com.dragomitch.ipl.pae.domain.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.domain.dto.PartnerDto;
import com.dragomitch.ipl.pae.domain.dto.UserDto;
import com.dragomitch.ipl.pae.domain.port.out.AddressRepository;
import com.dragomitch.ipl.pae.domain.port.out.MobilityChoiceRepository;
import com.dragomitch.ipl.pae.domain.port.out.MobilityRepository;
import com.dragomitch.ipl.pae.domain.port.out.NominatedStudentRepository;
import com.dragomitch.ipl.pae.domain.port.out.PartnerRepository;
import com.dragomitch.ipl.pae.domain.port.out.UserRepository;

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
class VersionContractIT extends AbstractRepositoryIT {

  @Autowired
  private AddressRepository addressRepository;
  @Autowired
  private MobilityChoiceRepository mobilityChoiceRepository;
  @Autowired
  private MobilityRepository mobilityRepository;
  @Autowired
  private NominatedStudentRepository nominatedStudentRepository;
  @Autowired
  private PartnerRepository partnerRepository;
  @Autowired
  private UserRepository userRepository;

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
        update("AddressRepository.update", "addresses", "address_id", 2002, it -> {
          AddressDto address = it.addressRepository.findById(2002);
          return it.addressRepository.update(address).getVersion();
        }),
        update("MobilityChoiceRepository.update", "mobility_choices", "mobility_choice_id", 5001, it -> {
          MobilityChoiceDto choice = it.mobilityChoiceRepository.findById(5001);
          it.mobilityChoiceRepository.update(choice);
          return choice.getVersion();
        }),
        update("MobilityRepository.update", "mobilities", "mobility_choice_id", 5006, it -> {
          MobilityDto mobility = it.mobilityRepository.findById(5006);
          return it.mobilityRepository.update(mobility).getVersion();
        }),
        update("NominatedStudentRepository.update", "nominated_students", "user_id", 1004, it -> {
          NominatedStudentDto student = it.nominatedStudentRepository.findById(1004);
          return it.nominatedStudentRepository.update(student).getVersion();
        }),
        update("PartnerRepository.update", "partners", "partner_id", 3001, it -> {
          PartnerDto partner = it.partnerRepository.findById(3001);
          return it.partnerRepository.update(partner).getVersion();
        }),
        update("UserRepository.update", "users", "user_id", 1002, it -> {
          UserDto user = it.userRepository.findById(1002);
          it.userRepository.update(user);
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
