package com.dragomitch.ipl.pae.adapter.in.web.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import com.dragomitch.ipl.pae.adapter.in.web.dto.AddressDto;
import com.dragomitch.ipl.pae.adapter.in.web.dto.CountryDto;
import com.dragomitch.ipl.pae.adapter.in.web.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.adapter.in.web.dto.OptionDto;
import com.dragomitch.ipl.pae.adapter.in.web.dto.PartnerDto;
import com.dragomitch.ipl.pae.adapter.in.web.dto.PartnerOptionDto;
import com.dragomitch.ipl.pae.adapter.in.web.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.adapter.in.web.dto.UserDto;
import com.dragomitch.ipl.pae.domain.model.NominatedStudent;
import com.dragomitch.ipl.pae.domain.model.Role;
import com.dragomitch.ipl.pae.domain.model.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The users, nominated students, addresses and partners survive a round trip. */
class UserAndPartnerMappersTest {

  private final ReferenceDataMapper referenceData = new ReferenceDataMapper();
  private final AddressMapper addresses = new AddressMapper(referenceData);
  private final UserMapper users = new UserMapper(referenceData, addresses);
  private final PartnerMapper partners = new PartnerMapper(referenceData, addresses);

  static final CountryDto BELGIUM = new CountryDto("BE", "Belgique", null, 0);
  static final AddressDto ADDRESS =
      new AddressDto(4, "Rue du Bois", "16", BELGIUM, "Bruxelles", "1000", "", 2);

  static UserDto user() {
    return new UserDto(
        2,
        "Alice",
        "Tester",
        "alice",
        "secret",
        "alice@example.test",
        new OptionDto("BIN", "Informatique"),
        LocalDateTime.of(2026, 1, 2, 3, 4, 5),
        null,
        3);
  }

  @Test
  void theRoleIsWrittenButNeverRead() {
    User user = new User();
    user.setRole(Role.PROFESSOR);

    assertThat(users.toDto(user).role()).isEqualTo("Professor");
    assertThat(
            users
                .toDomain(new UserDto(1, null, null, null, null, null, null, null, "Professor", 0))
                .getRole())
        .isNull();
    NominatedStudent student = new NominatedStudent();
    student.setRole(Role.STUDENT);
    assertThat(users.toDto(student).role()).isEqualTo("Student");
  }

  @Test
  void aUserRoundTripsWithItsPassword() {
    UserDto user = user();

    assertThat(users.toDto(users.toDomain(user))).isEqualTo(user);
    assertThat(users.toUserDtos(List.of(users.toDomain(user)))).containsExactly(user);
  }

  @Test
  void aNominatedStudentRoundTripsWithItsAddress() {
    NominatedStudentDto student =
        new NominatedStudentDto(
            2,
            "Alice",
            "Tester",
            "alice",
            null,
            "alice@example.test",
            new OptionDto("BIN", null),
            LocalDateTime.of(2026, 1, 2, 3, 4, 5),
            null,
            "Ms",
            LocalDate.of(2001, 4, 12),
            BELGIUM,
            ADDRESS,
            "+32493",
            "F",
            2,
            "BE68539007547034",
            "Alice Tester",
            "Belfius",
            "GKCCBEBB",
            5);

    assertThat(users.toDto(users.toDomain(student))).isEqualTo(student);
    assertThat(users.toNominatedStudentDtos(List.of(users.toDomain(student))))
        .containsExactly(student);
  }

  @Test
  void aPartnerRoundTripsWithItsAddressItsOptionsAndItsProgramme() {
    PartnerDto partner =
        new PartnerDto(
            7,
            "Legal",
            "Business",
            "Full",
            "PME",
            12,
            ADDRESS,
            "contact@partner.test",
            "",
            "+33123",
            true,
            false,
            true,
            List.of(new PartnerOptionDto("BIN", "Informatique", "Informatique")),
            new ProgrammeDto(1, "Erasmus+", null),
            4);

    assertThat(partners.toDto(partners.toDomain(partner))).isEqualTo(partner);
    assertThat(partners.toPartnerDtos(List.of(partners.toDomain(partner))))
        .containsExactly(partner);
    assertThat(partners.toPartnerOptionDtos(List.of(partners.toDomain(partner.options().get(0)))))
        .containsExactly(partner.options().get(0));
  }

  @Test
  void aPartnerWithoutOptionsKeepsNoOptions() {
    PartnerDto partner =
        new PartnerDto(
            7, null, null, "Full", null, 0, null, null, null, null, false, false, false, null, null,
            0);

    assertThat(partners.toDto(partners.toDomain(partner))).isEqualTo(partner);
  }

  @Test
  void nullStaysNull() {
    assertThat(addresses.toDto(null)).isNull();
    assertThat(addresses.toDomain(null)).isNull();
    assertThat(users.toDto((com.dragomitch.ipl.pae.domain.model.User) null)).isNull();
    assertThat(users.toDomain((UserDto) null)).isNull();
    assertThat(users.toDto((com.dragomitch.ipl.pae.domain.model.NominatedStudent) null)).isNull();
    assertThat(users.toDomain((NominatedStudentDto) null)).isNull();
    assertThat(partners.toDto((com.dragomitch.ipl.pae.domain.model.Partner) null)).isNull();
    assertThat(partners.toDomain((PartnerDto) null)).isNull();
    assertThat(partners.toDto((com.dragomitch.ipl.pae.domain.model.PartnerOption) null)).isNull();
    assertThat(partners.toDomain((PartnerOptionDto) null)).isNull();
  }
}
