package com.dragomitch.ipl.pae.application;

import static com.dragomitch.ipl.pae.domain.exceptions.BusinessExceptionAssert.assertThatBusinessException;
import static org.assertj.core.api.Assertions.assertThat;

import com.dragomitch.ipl.pae.UnitTestConfig;
import com.dragomitch.ipl.pae.domain.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.domain.model.Country;
import com.dragomitch.ipl.pae.domain.model.Option;
import com.dragomitch.ipl.pae.domain.model.Programme;
import com.dragomitch.ipl.pae.domain.port.in.CountryService;
import com.dragomitch.ipl.pae.domain.port.in.OptionService;
import com.dragomitch.ipl.pae.domain.port.in.ProgrammeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Lookups of the read-only reference data (countries, programmes, options) held by the mock DAOs:
 * GB and IE in the Erasmus+ programme, five options.
 */
@SpringJUnitConfig(UnitTestConfig.class)
class TestReferenceDataService {

  @Autowired private CountryService countryService;
  @Autowired private ProgrammeService programmeService;
  @Autowired private OptionService optionService;

  @Test
  void everyCountryIsListed() {
    assertThat(countryService.showAll())
        .extracting(Country::getCountryCode)
        .containsExactlyInAnyOrder("GB", "IE");
  }

  @Test
  void aCountryIsFoundByItsCodeWithItsProgramme() {
    Country ireland = countryService.showOne("IE");

    assertThat(ireland.getName()).isEqualTo("Ireland");
    assertThat(ireland.getProgramme().getProgrammeName()).isEqualTo("Erasmus+");
  }

  @Test
  void anUnknownCountryIsNotFound() {
    assertThatBusinessException(() -> countryService.showOne("ZZ"))
        .hasErrorCode(ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  void anUnknownProgrammeIsNotFound() {
    assertThatBusinessException(() -> programmeService.showOne(99))
        .hasErrorCode(ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  void everyProgrammeIsListed() {
    assertThat(programmeService.showAll())
        .extracting(Programme::getProgrammeName)
        .containsExactly("Erasmus+");
  }

  @Test
  void aProgrammeIsFoundByItsId() {
    Programme programme = programmeService.showOne(1);

    assertThat(programme.getProgrammeName()).isEqualTo("Erasmus+");
    assertThat(programme.getExternalSoftName()).isEqualTo("Mobility Tool");
  }

  @Test
  void everyOptionIsListed() {
    assertThat(optionService.showAll())
        .extracting(Option::getCode)
        .containsExactlyInAnyOrder("BIN", "BBM", "BCH", "BDI", "BIM");
  }

  @Test
  void thePartnersOfAnUnknownOptionAreNotFound() {
    assertThatBusinessException(() -> optionService.findAllPartnersByOption("XYZ"))
        .hasErrorCode(ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  void anOptionWithoutPartnersHasAnEmptyList() {
    assertThat(optionService.findAllPartnersByOption("BCH")).isEmpty();
  }
}
