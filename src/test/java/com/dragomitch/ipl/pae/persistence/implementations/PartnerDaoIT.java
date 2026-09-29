package com.dragomitch.ipl.pae.persistence.implementations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.dragomitch.ipl.pae.business.dto.AddressDto;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.PartnerOptionDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.ErrorFormat;
import com.dragomitch.ipl.pae.persistence.PartnerDao;
import com.dragomitch.ipl.pae.persistence.PartnerOptionDao;

import java.util.ConcurrentModificationException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Partners and their search filters (see db/fixtures/partners.sql for the data set).
 *
 * <p>The options are LEFT-joined and merged into {@code getOptions()}: a partner without any option
 * (3005) is found with an empty option list, and a partner with several options is returned once.
 */
class PartnerDaoIT extends AbstractDaoIT {

  private static final String PARTNER_BY_ID =
      "SELECT * FROM student_exchange_tools.partners WHERE partner_id = ?";

  @Autowired
  private PartnerDao partnerDao;

  @Autowired
  private PartnerOptionDao partnerOptionDao;

  @Override
  protected List<String> fixtures() {
    return List.of("db/fixtures/partners.sql");
  }

  private PartnerDto newPartner(int addressId) {
    PartnerDto partner = build(PartnerDto.class);
    partner.setLegalName("Gent Tech NV");
    partner.setBusinessName("GentTech");
    partner.setFullName("Gent Technology Institute");
    partner.setOrganisationType("University");
    partner.setEmployeeCount(800);
    AddressDto address = build(AddressDto.class);
    address.setId(addressId);
    partner.setAddress(address);
    partner.setEmail("info@gent.test");
    partner.setWebsite("https://gent.test");
    partner.setPhoneNumber("+3290000000");
    partner.setOfficial(false);
    partner.setArchived(true);
    return partner;
  }

  private List<Integer> ids(List<PartnerDto> partners) {
    return partners.stream().map(PartnerDto::getId).toList();
  }

  @Test
  void findByIdMapsEveryFieldAndJoinsCountryAndProgramme() {
    PartnerDto partner = inTransaction(() -> partnerDao.findById(3001));

    assertThat(partner.getId()).isEqualTo(3001);
    assertThat(partner.getLegalName()).isEqualTo("Université Lyon SA");
    assertThat(partner.getBusinessName()).isEqualTo("UdL");
    assertThat(partner.getFullName()).isEqualTo("Université de Lyon");
    assertThat(partner.getOrganisationType()).isEqualTo("University");
    assertThat(partner.getEmployeeCount()).isEqualTo(3000);
    assertThat(partner.getEmail()).isEqualTo("contact@lyon.test");
    assertThat(partner.getWebsite()).isEqualTo("https://lyon.test");
    assertThat(partner.getPhoneNumber()).isEqualTo("+33400000001");
    assertThat(partner.isOfficial()).isTrue();
    assertThat(partner.isArchived()).isFalse();
    assertThat(partner.getVersion()).isEqualTo(2);
    assertThat(partner.getAddress().getId()).isEqualTo(2001);
    assertThat(partner.getAddress().getCountry().getCountryCode()).isEqualTo("FR");
    assertThat(partner.getAddress().getCountry().getName()).isEqualTo("France");
    assertThat(partner.getProgramme().getId()).isEqualTo(1);
    assertThat(partner.getProgramme().getProgrammeName()).isEqualTo("Erasmus+");
    assertThat(partner.getOptions())
        .extracting(PartnerOptionDto::getCode, PartnerOptionDto::getDepartement,
            PartnerOptionDto::getName)
        .containsExactly(tuple("BCH", "Chimie", "Bachelier en chimie"),
            tuple("BIN", "Informatique", "Bachelier en informatique de gestion"));
  }

  @Test
  void findByIdFindsAPartnerWithoutAnyOptionWithAnEmptyOptionList() {
    PartnerDto orphan = inTransaction(() -> partnerDao.findById(3005));

    assertThat(orphan.getFullName()).isEqualTo("Orphan Partner");
    assertThat(orphan.getAddress().getCountry().getCountryCode()).isEqualTo("FR");
    assertThat(orphan.getOptions()).isEmpty();
  }

  @Test
  void findByIdMapsArchivedAndUnofficialFlags() {
    runInTransaction(() -> {
      assertThat(partnerDao.findById(3002).isArchived()).isTrue();
      assertThat(partnerDao.findById(3003).isOfficial()).isFalse();
    });
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1, 2999, 3006})
  void findByIdReturnsNullWhenAbsent(int id) {
    assertThat(inTransaction(() -> partnerDao.findById(id))).isNull();
  }

  @Test
  void createInsertsANonArchivedPartnerWithVersionOne() {
    runInTransaction(() -> {
      PartnerDto created = partnerDao.create(newPartner(2002));

      assertThat(created.getId()).isGreaterThanOrEqualTo(100000);
      assertThat(created.getVersion()).isEqualTo(1);
      // is_archived is always FALSE on creation, whatever the DTO says
      assertThat(queryForRow(PARTNER_BY_ID, created.getId()))
          .containsEntry("legal_name", "Gent Tech NV").containsEntry("business_name", "GentTech")
          .containsEntry("full_name", "Gent Technology Institute")
          .containsEntry("organisation_type", "University").containsEntry("employee_count", 800)
          .containsEntry("address", 2002).containsEntry("email", "info@gent.test")
          .containsEntry("website", "https://gent.test")
          .containsEntry("phone_number", "+3290000000").containsEntry("is_official", false)
          .containsEntry("is_archived", false).containsEntry("version", 1);

      // visible before it has an option
      assertThat(partnerDao.findById(created.getId()).getOptions()).isEmpty();
      PartnerOptionDto option = build(PartnerOptionDto.class);
      option.setCode("BIN");
      option.setDepartement("Informatique");
      partnerOptionDao.create(option, created.getId());

      PartnerDto found = partnerDao.findById(created.getId());
      assertThat(found.getFullName()).isEqualTo("Gent Technology Institute");
      assertThat(found.getProgramme().getProgrammeName()).isEqualTo("Erabel");
      assertThat(found.getOptions()).extracting(PartnerOptionDto::getCode).containsExactly("BIN");
    });
  }

  @Test
  void updateWritesEveryFieldIncludingTheArchiveFlagAndIncrementsTheVersion() {
    runInTransaction(() -> {
      PartnerDto partner = partnerDao.findById(3001);
      partner.setLegalName("Lyon 2");
      partner.setBusinessName("L2");
      partner.setFullName("Université Lumière Lyon 2");
      partner.setOrganisationType("Public");
      partner.setEmployeeCount(2500);
      partner.getAddress().setId(2004);
      partner.setEmail("info@lyon2.test");
      partner.setWebsite("https://lyon2.test");
      partner.setPhoneNumber("+33400000099");
      partner.setOfficial(false);
      partner.setArchived(true);

      PartnerDto updated = partnerDao.update(partner);

      assertThat(updated.getVersion()).isEqualTo(3);
      assertThat(queryForRow(PARTNER_BY_ID, 3001)).containsEntry("legal_name", "Lyon 2")
          .containsEntry("business_name", "L2")
          .containsEntry("full_name", "Université Lumière Lyon 2")
          .containsEntry("organisation_type", "Public").containsEntry("employee_count", 2500)
          .containsEntry("address", 2004).containsEntry("email", "info@lyon2.test")
          .containsEntry("website", "https://lyon2.test")
          .containsEntry("phone_number", "+33400000099").containsEntry("is_official", false)
          .containsEntry("is_archived", true).containsEntry("version", 3);
    });
  }

  @Test
  void updateWithAStaleVersionFailsAndLeavesTheRowUntouched() {
    runInTransaction(() -> {
      PartnerDto stale = partnerDao.findById(3001);
      stale.setVersion(1);
      stale.setArchived(true);

      assertThatThrownBy(() -> partnerDao.update(stale))
          .isInstanceOf(ConcurrentModificationException.class);
      assertThat(queryForRow(PARTNER_BY_ID, 3001)).containsEntry("is_archived", false)
          .containsEntry("version", 2);
    });
  }

  @Test
  void findAllForAProfessorReturnsEveryPartnerOnceWithAllItsOptions() {
    List<PartnerDto> partners = inTransaction(() -> partnerDao.findAll(
        PartnerDao.FILTER_ALL_PARTNERS, null, UserDto.ROLE_PROFESSOR, "BIN"));

    // 3001 has two options but is listed once; 3005 has no option but is listed too
    assertThat(ids(partners)).containsExactly(3001, 3002, 3003, 3004, 3005);
    assertThat(partners.get(0).getOptions()).extracting(PartnerOptionDto::getCode)
        .containsExactly("BCH", "BIN");
    assertThat(partners.get(4).getOptions()).isEmpty();
  }

  @Test
  void findAllForAStudentKeepsEveryOptionOfTheMatchingPartners() {
    List<PartnerDto> partners = inTransaction(() -> partnerDao.findAll(
        PartnerDao.FILTER_ALL_PARTNERS, null, UserDto.ROLE_STUDENT, "BIN"));

    // filtered on BIN, but the BCH option of 3001 is still part of its option list
    assertThat(partners).singleElement().satisfies(lyon -> assertThat(lyon.getOptions())
        .extracting(PartnerOptionDto::getCode).containsExactly("BCH", "BIN"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"whatever", ""})
  void findAllRejectsAnUnknownFilter(String filter) {
    runInTransaction(() -> assertThatThrownBy(
        () -> partnerDao.findAll(filter, "x", UserDto.ROLE_PROFESSOR, "BIN"))
        .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(
            ex.getError().getErrorCode()).isEqualTo(ErrorFormat.INVALID_PARTNER_FILTER_709)));
  }

  @ParameterizedTest(name = "student of {0} sees {1}")
  @CsvSource({"BIN, 3001", "BCH, 3001;3004", "BDI, ''"})
  void findAllForAStudentReturnsOnlyOfficialNonArchivedPartnersOfTheirOption(String option,
      String expected) {
    List<PartnerDto> partners = inTransaction(() -> partnerDao.findAll(
        PartnerDao.FILTER_ALL_PARTNERS, null, UserDto.ROLE_STUDENT, option));

    assertThat(ids(partners)).containsExactlyInAnyOrderElementsOf(parseIds(expected));
  }

  @ParameterizedTest(name = "student of {1} searching country {0} sees {2}")
  @CsvSource({"FR, BIN, 3001", "FR, BCH, 3001", "CA, BCH, 3004", "BE, BIN, ''", "CA, BIN, ''"})
  void findAllByCountryForAStudentAppliesTheStudentRestrictions(String country, String option,
      String expected) {
    List<PartnerDto> partners = inTransaction(() -> partnerDao.findAll(
        PartnerDao.FILTER_COUNTRY, country, UserDto.ROLE_STUDENT, option));

    assertThat(ids(partners)).containsExactlyInAnyOrderElementsOf(parseIds(expected));
  }

  @ParameterizedTest(name = "professor searching country {0} sees {1}")
  @CsvSource({"FR, 3001;3002;3005", "BE, 3003", "CA, 3004", "DE, ''"})
  void findAllByCountryForAProfessorReturnsEveryPartnerOfThatCountry(String country,
      String expected) {
    List<PartnerDto> partners = inTransaction(() -> partnerDao.findAll(
        PartnerDao.FILTER_COUNTRY, country, UserDto.ROLE_PROFESSOR, "BIN"));

    assertThat(ids(partners)).containsExactlyInAnyOrderElementsOf(parseIds(expected));
  }

  @ParameterizedTest(name = "archived partners matching \"{0}\": {1}")
  @CsvSource({"paris, 3002", "PARIS, 3002", "chive, 3002", "'', 3002", "lyon, ''"})
  void findAllArchivedMatchesTheFullNameCaseInsensitively(String search, String expected) {
    List<PartnerDto> partners = inTransaction(() -> partnerDao.findAll(
        PartnerDao.FILTER_ARCHIVED_PARTNERS, search, UserDto.ROLE_PROFESSOR, "BIN"));

    assertThat(ids(partners)).containsExactlyInAnyOrderElementsOf(parseIds(expected));
    assertThat(partners).allMatch(PartnerDto::isArchived);
  }

  private static List<Integer> parseIds(String ids) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    return java.util.Arrays.stream(ids.split(";")).map(Integer::valueOf).toList();
  }
}
