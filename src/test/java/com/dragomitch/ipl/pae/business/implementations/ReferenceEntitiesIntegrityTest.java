package com.dragomitch.ipl.pae.business.implementations;

import static org.assertj.core.api.Assertions.assertThat;

import com.dragomitch.ipl.pae.business.Country;
import com.dragomitch.ipl.pae.business.DenialReason;
import com.dragomitch.ipl.pae.business.Option;
import com.dragomitch.ipl.pae.business.Programme;
import com.dragomitch.ipl.pae.business.Violations;
import com.dragomitch.ipl.pae.business.validation.ValidationGroups.Reference;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Constraints of the small reference entities: option, programme, country, denial reason (their
 * former {@code checkDataIntegrity()}), in full ({@code Default} group) and as references
 * ({@code Reference} group: only the identifier).
 */
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
    assertThat(Violations.of(option("BIN", "Informatique"))).isEmpty();
  }

  @ParameterizedTest
  @CsvSource(value = {"NULL, option code, code:NotBlank", "'', empty code, code:NotBlank",
      "BI, code too short, code:Size", "BINF, code too long, code:Size"}, nullValues = "NULL")
  void anOptionIsIdentifiedByACodeOfThreeCharacters(String code, String why, String violation) {
    assertThat(Violations.of(option(code, "Informatique"))).as(why).contains(violation);
    assertThat(Violations.of(option(code, "Informatique"), Reference.class)).contains(violation);
  }

  @Test
  void theNameOfAnOptionIsNotRequired() {
    // clients only send the code of an option (sign-up form, partner options)
    assertThat(Violations.of(option("BIN", null))).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 3})
  void aProgrammeWithAPositiveOrZeroIdAndNamesIsValid(int id) {
    assertThat(Violations.of(programme(id, "Erasmus+", "Mobility Tool"))).isEmpty();
  }

  @ParameterizedTest
  @CsvSource(value = {"-1, Erasmus+, Mobility Tool, id:PositiveOrZero",
      "1, NULL, Mobility Tool, programmeName:NotBlank", "1, '', Tool, programmeName:NotBlank",
      "1, Erasmus+, NULL, externalSoftName:NotBlank", "1, Erasmus+, '', externalSoftName:NotBlank"},
      nullValues = "NULL")
  void aProgrammeNeedsANonNegativeIdAndBothNames(int id, String name, String software,
      String violation) {
    assertThat(Violations.of(programme(id, name, software))).containsExactly(violation);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  void aReferencedProgrammeNeedsAPositiveId(int id) {
    assertThat(Violations.of(programme(id, null, null), Reference.class))
        .containsExactly("id:Positive");
  }

  @Test
  void aCountryIsValidWithCodeNameAndAValidProgramme() {
    Country country = (Country) factory.build(Country.class);
    country.setCountryCode("BE");
    country.setName("Belgique");
    country.setProgramme(programme(2, "Erabel", "Mobi-ERABEL"));

    assertThat(Violations.of(country)).isEmpty();
  }

  @ParameterizedTest
  @CsvSource(value = {"NULL, Belgique, Erabel, countryCode:NotBlank",
      "BEL, Belgique, Erabel, countryCode:Size", "BE, '', Erabel, name:NotBlank",
      "BE, Belgique, '', programme.programmeName:NotBlank"}, nullValues = "NULL")
  void aCountryChecksItsFieldsAndItsProgramme(String code, String name, String programmeName,
      String violation) {
    Country country = (Country) factory.build(Country.class);
    country.setCountryCode(code);
    country.setName(name);
    country.setProgramme(programme(2, programmeName, "Mobi-ERABEL"));

    assertThat(Violations.of(country)).containsExactly(violation);
  }

  @Test
  void aReferencedCountryOnlyNeedsItsCode() {
    Country country = (Country) factory.build(Country.class);
    country.setCountryCode("BE");

    assertThat(Violations.of(country, Reference.class)).isEmpty();
    assertThat(Violations.of(country)).containsExactly("name:NotBlank", "programme:NotNull");
  }

  @Test
  void aDenialReasonWithATextIsValid() {
    DenialReason reason = (DenialReason) factory.build(DenialReason.class);
    reason.setReason("Dossier incomplet");

    assertThat(Violations.of(reason)).isEmpty();
  }

  @ParameterizedTest
  @NullAndEmptySource
  void aDenialReasonNeedsAText(String text) {
    DenialReason reason = (DenialReason) factory.build(DenialReason.class);
    reason.setReason(text);

    assertThat(Violations.of(reason)).containsExactly("reason:NotBlank");
  }

  @ParameterizedTest(name = "{0} characters: {1}")
  @CsvSource({"299, true", "300, true", "301, false", "1000, false"})
  void aDenialReasonFitsInTheColumn(int length, boolean valid) {
    // denial_reasons.reason is VARCHAR(300): a longer text is rejected before reaching the database
    DenialReason reason = (DenialReason) factory.build(DenialReason.class);
    reason.setReason("x".repeat(length));

    assertThat(Violations.of(reason))
        .isEqualTo(valid ? List.of() : List.of("reason:Size"));
  }
}
