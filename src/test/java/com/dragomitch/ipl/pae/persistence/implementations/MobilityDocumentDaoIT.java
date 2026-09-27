package com.dragomitch.ipl.pae.persistence.implementations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.dragomitch.ipl.pae.business.dto.DocumentDto;
import com.dragomitch.ipl.pae.exceptions.FatalException;
import com.dragomitch.ipl.pae.persistence.MobilityDocumentDao;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

class MobilityDocumentDaoIT extends AbstractDaoIT {

  private static final String LINK =
      "SELECT * FROM student_exchange_tools.mobility_documents "
          + "WHERE document_id = ? AND mobility_id = ?";

  @Autowired
  private MobilityDocumentDao mobilityDocumentDao;

  @Override
  protected List<String> fixtures() {
    return List.of("db/fixtures/users.sql", "db/fixtures/partners.sql",
        "db/fixtures/mobility_choices.sql", "db/fixtures/mobilities.sql");
  }

  @Test
  void findAllByMobilityReturnsTheDocumentsWithTheirFilledInFlag() {
    List<DocumentDto> documents = inTransaction(() -> mobilityDocumentDao.findAllByMobility(5006));

    assertThat(documents).extracting(DocumentDto::getId, DocumentDto::getName,
        DocumentDto::getCategory, DocumentDto::isFilledIn).containsExactlyInAnyOrder(
            tuple(1, "Contrat de bourse", 'D', true),
            tuple(2, "Convention de stage / Convention d'études", 'D', false),
            tuple(14, "Attestation séjour", 'R', false));
  }

  @Test
  void findAllByMobilityReturnsAnEmptyListWhenNoDocumentIsLinked() {
    runInTransaction(() -> {
      assertThat(mobilityDocumentDao.findAllByMobility(5007)).isEmpty();
      assertThat(mobilityDocumentDao.findAllByMobility(424242)).isEmpty();
    });
  }

  @Test
  void createLinksANotFilledInDocumentWithVersionOne() {
    runInTransaction(() -> {
      mobilityDocumentDao.create(3, 5006);

      assertThat(queryForRow(LINK, 3, 5006)).containsEntry("is_filled_in", false)
          .containsEntry("version", 1);
      assertThat(mobilityDocumentDao.findAllByMobility(5006)).extracting(DocumentDto::getId)
          .containsExactlyInAnyOrder(1, 2, 3, 14);
    });
  }

  @ParameterizedTest(name = "document {0} / mobility {1}")
  @CsvSource({
      "1, 5006",     // already linked (primary key)
      "999, 5006",   // unknown document
      "3, 5001"})    // mobility choice that is not a mobility
  void createIsRejectedByTheConstraints(int document, int mobility) {
    runInTransaction(() -> assertThatThrownBy(() -> mobilityDocumentDao.create(document, mobility))
        .isInstanceOf(FatalException.class));
  }

  @Test
  void fillInDocumentMarksTheDocumentAsFilledInAndIncrementsItsVersion() {
    runInTransaction(() -> {
      mobilityDocumentDao.fillInDocument(2, 5006);

      assertThat(queryForRow(LINK, 2, 5006)).containsEntry("is_filled_in", true)
          .containsEntry("version", 2);
      assertThat(queryForRow(LINK, 14, 5006)).containsEntry("is_filled_in", false)
          .containsEntry("version", 1);
    });
  }

  @Test
  void fillInDocumentOfAnAlreadyFilledInDocumentOnlyBumpsTheVersion() {
    runInTransaction(() -> {
      mobilityDocumentDao.fillInDocument(1, 5006);

      assertThat(queryForRow(LINK, 1, 5006)).containsEntry("is_filled_in", true)
          .containsEntry("version", 3);
    });
  }

  @Test
  void fillInDocumentOfAnUnlinkedDocumentIsSilentlyIgnored() {
    runInTransaction(() -> {
      mobilityDocumentDao.fillInDocument(5, 5006);

      assertThat(query("SELECT * FROM student_exchange_tools.mobility_documents")).hasSize(3);
    });
  }
}
