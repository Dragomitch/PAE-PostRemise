package com.dragomitch.ipl.pae.business.implementations;

import static com.dragomitch.ipl.pae.business.exceptions.ErrorFormat.INVALID_REASON_401;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dragomitch.ipl.pae.business.Country;
import com.dragomitch.ipl.pae.business.DenialReason;
import com.dragomitch.ipl.pae.business.Option;
import com.dragomitch.ipl.pae.business.Programme;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** checkDataIntegrity() of the small reference entities: option, programme, country, reason. */
class ReferenceEntitiesIntegrityTest {

  private final EntityFactoryImpl factory = new EntityFactoryImpl();

  private Option option(String code, String name) {
    Option option = (Option) factory.build(Option.class);
    option.setCode(code);
    option.setName(name);
    return option;
  }

  private Programme programme(int id, String name, String software) {
    Programme programme = (Programme) factory.build(Programme.class);
    programme.setId(id);
    programme.setProgrammeName(name);
    programme.setExternalSoftName(software);
    return programme;
  }

  @Test
  void aCompleteOptionIsValid() {
    assertThatCode(() -> option("BIN", "Informatique").checkDataIntegrity())
        .doesNotThrowAnyException();
  }

  @ParameterizedTest
  @CsvSource(value = {"NULL, Informatique", "'', Informatique", "BIN, NULL", "BIN, ''"},
      nullValues = "NULL")
  void anOptionNeedsACodeAndAName(String code, String name) {
    assertThatThrownBy(() -> option(code, name).checkDataIntegrity())
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 3})
  void aProgrammeWithAPositiveOrZeroIdAndNamesIsValid(int id) {
    assertThatCode(() -> programme(id, "Erasmus+", "Mobility Tool").checkDataIntegrity())
        .doesNotThrowAnyException();
  }

  @ParameterizedTest
  @CsvSource(value = {"-1, Erasmus+, Mobility Tool", "1, NULL, Mobility Tool", "1, '', Tool",
      "1, Erasmus+, NULL", "1, Erasmus+, ''"}, nullValues = "NULL")
  void aProgrammeNeedsANonNegativeIdAndBothNames(int id, String name, String software) {
    assertThatThrownBy(() -> programme(id, name, software).checkDataIntegrity())
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void aCountryIsValidWithCodeNameAndAValidProgramme() {
    Country country = (Country) factory.build(Country.class);
    country.setCountryCode("BE");
    country.setName("Belgique");
    country.setProgramme(programme(2, "Erabel", "Mobi-ERABEL"));

    assertThatCode(country::checkDataIntegrity).doesNotThrowAnyException();
  }

  @ParameterizedTest
  @CsvSource(value = {"NULL, Belgique, Erabel", "BE, '', Erabel", "BE, Belgique, ''"},
      nullValues = "NULL")
  void aCountryChecksItsFieldsAndItsProgramme(String code, String name, String programmeName) {
    Country country = (Country) factory.build(Country.class);
    country.setCountryCode(code);
    country.setName(name);
    country.setProgramme(programme(2, programmeName, "Mobi-ERABEL"));

    assertThatThrownBy(country::checkDataIntegrity).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void aDenialReasonWithATextIsValid() {
    DenialReason reason = (DenialReason) factory.build(DenialReason.class);
    reason.setReason("Dossier incomplet");

    assertThat(Violations.of(reason::checkDataIntegrity)).isEmpty();
  }

  @ParameterizedTest
  @NullAndEmptySource
  void aDenialReasonNeedsAText(String text) {
    DenialReason reason = (DenialReason) factory.build(DenialReason.class);
    reason.setReason(text);

    assertThat(Violations.of(reason::checkDataIntegrity)).containsExactly(INVALID_REASON_401);
  }

  @Test
  void aDenialReasonLongerThanTheColumnIsNotRejectedByTheBusinessCheck() {
    // documented gap: denial_reasons.reason is VARCHAR(300) but only emptiness is checked, so a
    // longer text fails in the database (see DenialReasonDaoIT)
    DenialReason reason = (DenialReason) factory.build(DenialReason.class);
    reason.setReason("x".repeat(301));

    assertThat(Violations.of(reason::checkDataIntegrity)).isEmpty();
  }
}
