package com.dragomitch.ipl.pae.persistence.implementations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.dto.DocumentDto;
import com.dragomitch.ipl.pae.business.dto.OptionDto;
import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.persistence.CountryDao;
import com.dragomitch.ipl.pae.persistence.DocumentDao;
import com.dragomitch.ipl.pae.persistence.OptionDao;
import com.dragomitch.ipl.pae.persistence.ProgrammeDao;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Read-only DAOs over the reference data loaded by SQLRessources/init.sql (options, programmes,
 * countries, documents). No fixture is needed.
 */
class ReferenceDataDaoIT extends AbstractDaoIT {

  @Autowired
  private OptionDao optionDao;

  @Autowired
  private ProgrammeDao programmeDao;

  @Autowired
  private CountryDao countryDao;

  @Autowired
  private DocumentDao documentDao;

  @Nested
  class Options {

    @Test
    void findAllReturnsTheFiveIplOptionsInInsertionOrder() {
      List<OptionDto> options = inTransaction(() -> optionDao.findAll());

      assertThat(options).extracting(OptionDto::getCode, OptionDto::getName).containsExactly(
          tuple("BIN", "Bachelier en informatique de gestion"),
          tuple("BBM", "Bachelier en biologie médicale"),
          tuple("BCH", "Bachelier en chimie"),
          tuple("BDI", "Bachelier en diététique"),
          tuple("BIM", "Bachelier en imagerie médicale"));
    }

    @Test
    void findByCodeReturnsTheOption() {
      OptionDto option = inTransaction(() -> optionDao.findByCode("BCH"));

      assertThat(option.getCode()).isEqualTo("BCH");
      assertThat(option.getName()).isEqualTo("Bachelier en chimie");
    }

    @ParameterizedTest
    @ValueSource(strings = {"XXX", "bin", "", "BINX"})
    void findByCodeReturnsNullForAnUnknownOrDifferentlyCasedCode(String code) {
      assertThat(inTransaction(() -> optionDao.findByCode(code))).isNull();
    }
  }

  @Nested
  class Programmes {

    @Test
    void findAllReturnsTheThreeProgrammes() {
      List<ProgrammeDto> programmes = inTransaction(() -> programmeDao.findAll());

      assertThat(programmes)
          .extracting(ProgrammeDto::getId, ProgrammeDto::getProgrammeName,
              ProgrammeDto::getExternalSoftName)
          .containsExactly(
              tuple(1, "Erasmus+", "Mobility Tool"),
              tuple(2, "Erabel", "Mobi-ERABEL"),
              tuple(3, "FAME", "Mobi-FAME"));
    }

    @ParameterizedTest
    @CsvSource({"1, Erasmus+, Mobility Tool", "2, Erabel, Mobi-ERABEL", "3, FAME, Mobi-FAME"})
    void findByIdReturnsEveryField(int id, String name, String software) {
      ProgrammeDto programme = inTransaction(() -> programmeDao.findById(id));

      assertThat(programme.getId()).isEqualTo(id);
      assertThat(programme.getProgrammeName()).isEqualTo(name);
      assertThat(programme.getExternalSoftName()).isEqualTo(software);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 4, 99999})
    void findByIdReturnsNullForAnUnknownId(int id) {
      assertThat(inTransaction(() -> programmeDao.findById(id))).isNull();
    }
  }

  @Nested
  class Countries {

    @Test
    void findAllReturnsEveryCountrySortedByName() {
      List<Object> namesSortedByTheDatabase = new ArrayList<>();
      List<CountryDto> countries = inTransaction(() -> {
        query("SELECT name FROM student_exchange_tools.countries ORDER BY name")
            .forEach(row -> namesSortedByTheDatabase.add(row.get("name")));
        return countryDao.findAll();
      });

      assertThat(countries).hasSize(namesSortedByTheDatabase.size()).hasSizeGreaterThan(200);
      // sorted by name with the database collation (accented names such as "Îles Åland")
      assertThat(countries).extracting(CountryDto::getName)
          .containsExactlyElementsOf(namesSortedByTheDatabase.stream().map(String.class::cast)
              .toList())
          .contains("Belgique", "Algérie", "Îles Åland");
      assertThat(countries).allSatisfy(country -> {
        assertThat(country.getCountryCode()).hasSize(2);
        assertThat(country.getProgramme()).isNotNull();
        assertThat(country.getProgramme().getProgrammeName()).isNotBlank();
      });
    }

    @ParameterizedTest
    @CsvSource({
        "BE, Belgique, 2, Erabel, Mobi-ERABEL",
        "FR, France, 1, Erasmus+, Mobility Tool",
        "CA, Canada, 3, FAME, Mobi-FAME",
        "DZ, Algérie, 3, FAME, Mobi-FAME",
        // current ISO codes that had a flag but no row
        "RS, Serbie, 1, Erasmus+, Mobility Tool",
        "ME, Monténégro, 3, FAME, Mobi-FAME",
        "SS, Soudan du Sud, 3, FAME, Mobi-FAME",
        "CW, Curaçao, 3, FAME, Mobi-FAME",
        "SX, Saint-Martin (partie néerlandaise), 3, FAME, Mobi-FAME",
        "BQ, 'Bonaire, Saint-Eustache et Saba', 3, FAME, Mobi-FAME",
        "BL, Saint-Barthélemy, 3, FAME, Mobi-FAME",
        "GG, Guernesey, 3, FAME, Mobi-FAME",
        "JE, Jersey, 3, FAME, Mobi-FAME",
        // former codes, kept because data may reference them
        "CS, Serbie-et-Monténégro, 3, FAME, Mobi-FAME",
        "AN, Antilles Néerlandaises, 3, FAME, Mobi-FAME"})
    void findByIdJoinsTheProgramme(String code, String name, int programmeId,
        String programmeName, String software) {
      CountryDto country = inTransaction(() -> countryDao.findById(code));

      assertThat(country.getCountryCode()).isEqualTo(code);
      assertThat(country.getName()).isEqualTo(name);
      assertThat(country.getProgramme().getId()).isEqualTo(programmeId);
      assertThat(country.getProgramme().getProgrammeName()).isEqualTo(programmeName);
      assertThat(country.getProgramme().getExternalSoftName()).isEqualTo(software);
    }

    @Test
    void theScriptAddingTheMissingCountriesToAnExistingDatabaseIsIdempotent() throws Exception {
      String script = Files.readString(Path.of("SQLRessources", "add-missing-countries.sql"));
      String count = "SELECT count(*) AS n FROM student_exchange_tools.countries";

      runInTransaction(() -> {
        Object before = queryForRow(count).get("n");
        execute("DELETE FROM student_exchange_tools.countries WHERE country_code IN ('RS', 'JE')");

        execute(script);
        execute(script);

        assertThat(queryForRow(count).get("n")).isEqualTo(before);
        assertThat(countryDao.findById("RS").getProgramme().getId()).isEqualTo(1);
        assertThat(countryDao.findById("JE").getName()).isEqualTo("Jersey");
      });
    }

    @ParameterizedTest
    @ValueSource(strings = {"ZZ", "be", ""})
    void findByIdReturnsNullForAnUnknownCode(String code) {
      assertThat(inTransaction(() -> countryDao.findById(code))).isNull();
    }
  }

  @Nested
  class Documents {

    @ParameterizedTest
    @CsvSource({"1, 5, 4", "2, 4, 3", "3, 4, 3"})
    void findAllByProgrammeReturnsTheDepartureAndReturnDocumentsOfThatProgrammeOnly(int programme,
        int departures, int returns) {
      List<DocumentDto> documents = inTransaction(() -> documentDao.findAllByProgramme(programme));

      assertThat(documents).hasSize(departures + returns);
      assertThat(documents).filteredOn(d -> d.getCategory() == DocumentDto.DEPARTURE_DOCUMENT)
          .hasSize(departures);
      assertThat(documents).filteredOn(d -> d.getCategory() == DocumentDto.RETURN_DOCUMENT)
          .hasSize(returns);
    }

    @Test
    void findAllByProgrammeMapsIdNameCategoryAndProgramme() {
      List<DocumentDto> documents = inTransaction(() -> documentDao.findAllByProgramme(1));

      assertThat(documents).extracting(DocumentDto::getId, DocumentDto::getName,
          DocumentDto::getCategory).contains(
              tuple(1, "Contrat de bourse", 'D'),
              tuple(5, "Preuve du passage des tests linguistiques", 'D'),
              tuple(14, "Attestation séjour", 'R'));
      assertThat(documents).allSatisfy(document -> {
        assertThat(document.getProgramme().getId()).isEqualTo(1);
        // "filled in" belongs to the document of a mobility (MobilityDocumentDao)
        assertThat(document.isFilledIn()).isFalse();
      });
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 4, -1})
    void findAllByProgrammeReturnsAnEmptyListForAnUnknownProgramme(int programme) {
      assertThat(inTransaction(() -> documentDao.findAllByProgramme(programme))).isEmpty();
    }
  }
}
