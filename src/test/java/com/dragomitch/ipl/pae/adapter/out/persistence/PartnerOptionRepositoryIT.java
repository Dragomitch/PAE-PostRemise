package com.dragomitch.ipl.pae.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.dragomitch.ipl.pae.domain.model.Partner;
import com.dragomitch.ipl.pae.domain.model.PartnerOption;
import com.dragomitch.ipl.pae.domain.port.out.PartnerOptionRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class PartnerOptionRepositoryIT extends AbstractRepositoryIT {

  @Autowired private PartnerOptionRepository partnerOptionRepository;

  @Override
  protected List<String> fixtures() {
    return List.of("db/fixtures/partners.sql");
  }

  private PartnerOption option(String code, String departement) {
    PartnerOption option = new PartnerOption();
    option.setCode(code);
    option.setDepartement(departement);
    return option;
  }

  @Test
  void findAllOptionsByPartnerReturnsCodeNameAndDepartement() {
    List<PartnerOption> options =
        inTransaction(() -> partnerOptionRepository.findAllOptionsByPartner(3001));

    assertThat(options)
        .extracting(PartnerOption::getCode, PartnerOption::getName, PartnerOption::getDepartement)
        .containsExactlyInAnyOrder(
            tuple("BIN", "Bachelier en informatique de gestion", "Informatique"),
            tuple("BCH", "Bachelier en chimie", "Chimie"));
  }

  @Test
  void findAllOptionsByPartnerReturnsAnEmptyListForAPartnerWithoutOption() {
    assertThat(inTransaction(() -> partnerOptionRepository.findAllOptionsByPartner(3005)))
        .isEmpty();
    assertThat(inTransaction(() -> partnerOptionRepository.findAllOptionsByPartner(-1))).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({"BIN, 3001;3002;3003", "BCH, 3001;3004", "BDI, ''", "XXX, ''"})
  void findAllPartnersByOptionReturnsPartnerIdsOnly(String code, String expected) {
    List<Partner> partners =
        inTransaction(() -> partnerOptionRepository.findAllPartnersByOption(code));

    List<Integer> expectedIds =
        expected.isEmpty()
            ? List.of()
            : java.util.Arrays.stream(expected.split(";")).map(Integer::valueOf).toList();
    assertThat(partners)
        .extracting(Partner::getId)
        .containsExactlyInAnyOrderElementsOf(expectedIds);
    assertThat(partners).allSatisfy(partner -> assertThat(partner.getFullName()).isNull());
  }

  @Test
  void createLinksAnOptionToAPartner() {
    runInTransaction(() -> {
      PartnerOption option = option("BDI", "Nutrition");

      assertThat(partnerOptionRepository.create(option, 3005)).isSameAs(option);
      assertThat(partnerOptionRepository.findAllOptionsByPartner(3005))
          .extracting(PartnerOption::getCode, PartnerOption::getDepartement)
          .containsExactly(tuple("BDI", "Nutrition"));
    });
  }

  @Test
  void createTwiceTheSameOptionForAPartnerViolatesThePrimaryKey() {
    runInTransaction(() ->
        assertThatThrownBy(() -> partnerOptionRepository.create(option("BIN", "Autre"), 3001))
            .isInstanceOf(DataIntegrityViolationException.class));
  }

  @Test
  void createWithAnUnknownOptionOrPartnerIsRejected() {
    runInTransaction(() ->
        assertThatThrownBy(() -> partnerOptionRepository.create(option("XXX", "Autre"), 3001))
            .isInstanceOf(DataIntegrityViolationException.class));
    runInTransaction(() ->
        assertThatThrownBy(() -> partnerOptionRepository.create(option("BIN", "Autre"), 9999))
            .isInstanceOf(DataIntegrityViolationException.class));
  }
}
