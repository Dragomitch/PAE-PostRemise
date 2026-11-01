package com.dragomitch.ipl.pae.adapter.out.persistence.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * The composite keys compare by value, as JPA requires: the persistence context finds an entity
 * by an equal key, not by the same key instance.
 */
class EmbeddedIdsTest {

  @Test
  void partnerOptionIdsAreEqualWhenOptionAndPartnerAre() {
    PartnerOptionId id = new PartnerOptionId("BIN", 3001);

    assertThat(id).isEqualTo(new PartnerOptionId("BIN", 3001))
        .hasSameHashCodeAs(new PartnerOptionId("BIN", 3001))
        .isNotEqualTo(new PartnerOptionId("BCH", 3001))
        .isNotEqualTo(new PartnerOptionId("BIN", 3002))
        .isNotEqualTo(new MobilityDocumentId(1, 3001))
        .isNotEqualTo(null);
    assertThat(id.getOptionCode()).isEqualTo("BIN");
    assertThat(id.getPartnerId()).isEqualTo(3001);
  }

  @Test
  void mobilityDocumentIdsAreEqualWhenDocumentAndMobilityAre() {
    MobilityDocumentId id = new MobilityDocumentId(1, 5006);

    assertThat(id).isEqualTo(new MobilityDocumentId(1, 5006))
        .hasSameHashCodeAs(new MobilityDocumentId(1, 5006))
        .isNotEqualTo(new MobilityDocumentId(2, 5006))
        .isNotEqualTo(new MobilityDocumentId(1, 5007))
        .isNotEqualTo(new PartnerOptionId("BIN", 5006))
        .isNotEqualTo(null);
    assertThat(id.getDocumentId()).isEqualTo(1);
    assertThat(id.getMobilityId()).isEqualTo(5006);
  }
}
