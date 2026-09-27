package com.dragomitch.ipl.pae.uccontrollers;

import static com.dragomitch.ipl.pae.business.exceptions.BusinessExceptionAssert.assertThatBusinessException;
import static org.assertj.core.api.Assertions.assertThat;

import com.dragomitch.ipl.pae.UnitTestConfig;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.dto.OptionDto;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.business.exceptions.ErrorCode;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Lookups of the read-only reference data (countries, programmes, options) held by the mock DAOs:
 * GB and IE in the Erasmus+ programme, five options.
 */
@SpringJUnitConfig(UnitTestConfig.class)
class TestReferenceDataUcc {

  @Autowired
  private CountryUcc countryUcc;
  @Autowired
  private ProgrammeUcc programmeUcc;
  @Autowired
  private OptionUcc optionUcc;

  @Test
  void everyCountryIsListed() {
    assertThat(countryUcc.showAll()).extracting(CountryDto::getCountryCode)
        .containsExactlyInAnyOrder("GB", "IE");
  }

  @Test
  void aCountryIsFoundByItsCodeWithItsProgramme() {
    CountryDto ireland = countryUcc.showOne("IE");

    assertThat(ireland.getName()).isEqualTo("Ireland");
    assertThat(ireland.getProgramme().getProgrammeName()).isEqualTo("Erasmus+");
  }

  @Test
  void everyProgrammeIsListed() {
    assertThat(programmeUcc.showAll()).extracting(ProgrammeDto::getProgrammeName)
        .containsExactly("Erasmus+");
  }

  @Test
  void aProgrammeIsFoundByItsId() {
    ProgrammeDto programme = programmeUcc.showOne(1);

    assertThat(programme.getProgrammeName()).isEqualTo("Erasmus+");
    assertThat(programme.getExternalSoftName()).isEqualTo("Mobility Tool");
  }

  @Test
  void everyOptionIsListed() {
    assertThat(optionUcc.showAll()).extracting(OptionDto::getCode)
        .containsExactlyInAnyOrder("BIN", "BBM", "BCH", "BDI", "BIM");
  }

  @Test
  void thePartnersOfAnUnknownOptionAreNotFound() {
    assertThatBusinessException(() -> optionUcc.findAllPartnersByOption("XYZ"))
        .hasErrorCode(ErrorCode.RESOURCE_NOT_FOUND);
  }

  @Test
  void anOptionWithoutPartnersHasAnEmptyList() {
    assertThat(optionUcc.findAllPartnersByOption("BCH")).isEmpty();
  }
}
