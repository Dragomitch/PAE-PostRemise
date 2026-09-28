package com.dragomitch.ipl.pae.persistence.implementations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.PartnerOptionDto;
import com.dragomitch.ipl.pae.exceptions.FatalException;
import com.dragomitch.ipl.pae.persistence.PartnerOptionDao;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

class PartnerOptionDaoIT extends AbstractDaoIT {

  @Autowired
  private PartnerOptionDao partnerOptionDao;

  @Override
  protected List<String> fixtures() {
    return List.of("db/fixtures/partners.sql");
  }

  private PartnerOptionDto option(String code, String departement) {
    PartnerOptionDto option = build(PartnerOptionDto.class);
    option.setCode(code);
    option.setDepartement(departement);
    return option;
  }

  @Test
  void findAllOptionsByPartnerReturnsCodeNameAndDepartement() {
    List<PartnerOptionDto> options =
        inTransaction(() -> partnerOptionDao.findAllOptionsByPartner(3001));

    assertThat(options)
        .extracting(PartnerOptionDto::getCode, PartnerOptionDto::getName,
            PartnerOptionDto::getDepartement)
        .containsExactlyInAnyOrder(
            tuple("BIN", "Bachelier en informatique de gestion", "Informatique"),
            tuple("BCH", "Bachelier en chimie", "Chimie"));
  }

  @Test
  void findAllOptionsByPartnerReturnsAnEmptyListForAPartnerWithoutOption() {
    assertThat(inTransaction(() -> partnerOptionDao.findAllOptionsByPartner(3005))).isEmpty();
    assertThat(inTransaction(() -> partnerOptionDao.findAllOptionsByPartner(-1))).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({"BIN, 3001;3002;3003", "BCH, 3001;3004", "BDI, ''", "XXX, ''"})
  void findAllPartnersByOptionReturnsPartnerIdsOnly(String code, String expected) {
    List<PartnerDto> partners = inTransaction(() -> partnerOptionDao.findAllPartnersByOption(code));

    List<Integer> expectedIds = expected.isEmpty() ? List.of()
        : java.util.Arrays.stream(expected.split(";")).map(Integer::valueOf).toList();
    assertThat(partners).extracting(PartnerDto::getId)
        .containsExactlyInAnyOrderElementsOf(expectedIds);
    assertThat(partners).allSatisfy(partner -> assertThat(partner.getFullName()).isNull());
  }

  @Test
  void createLinksAnOptionToAPartner() {
    runInTransaction(() -> {
      PartnerOptionDto option = option("BDI", "Nutrition");

      assertThat(partnerOptionDao.create(option, 3005)).isSameAs(option);
      assertThat(partnerOptionDao.findAllOptionsByPartner(3005))
          .extracting(PartnerOptionDto::getCode, PartnerOptionDto::getDepartement)
          .containsExactly(tuple("BDI", "Nutrition"));
    });
  }

  @Test
  void createTwiceTheSameOptionForAPartnerViolatesThePrimaryKey() {
    runInTransaction(() -> assertThatThrownBy(
        () -> partnerOptionDao.create(option("BIN", "Autre"), 3001))
        .isInstanceOf(FatalException.class));
  }

  @Test
  void createWithAnUnknownOptionOrPartnerIsRejected() {
    runInTransaction(() -> assertThatThrownBy(
        () -> partnerOptionDao.create(option("XXX", "Autre"), 3001))
        .isInstanceOf(FatalException.class));
    runInTransaction(() -> assertThatThrownBy(
        () -> partnerOptionDao.create(option("BIN", "Autre"), 9999))
        .isInstanceOf(FatalException.class));
  }
}
