package com.dragomitch.ipl.pae.adapter.in.web.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import com.dragomitch.ipl.pae.adapter.in.web.dto.CountryDto;
import com.dragomitch.ipl.pae.adapter.in.web.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.adapter.in.web.dto.OptionDto;
import com.dragomitch.ipl.pae.adapter.in.web.dto.ProgrammeDto;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The reference data survive a round trip through the domain, and a null stays null. */
class ReferenceDataMapperTest {

  private final ReferenceDataMapper mapper = new ReferenceDataMapper();

  @Test
  void anOptionRoundTrips() {
    OptionDto option = new OptionDto("BIN", "Informatique");

    assertThat(mapper.toDto(mapper.toDomain(option))).isEqualTo(option);
    assertThat(mapper.toOptionDtos(List.of(mapper.toDomain(option)))).containsExactly(option);
  }

  @Test
  void aProgrammeRoundTrips() {
    ProgrammeDto programme = new ProgrammeDto(1, "Erasmus+", "Mobility Tool");

    assertThat(mapper.toDto(mapper.toDomain(programme))).isEqualTo(programme);
    assertThat(mapper.toProgrammeDtos(List.of(mapper.toDomain(programme))))
        .containsExactly(programme);
  }

  @Test
  void aCountryRoundTripsWithItsProgramme() {
    CountryDto country =
        new CountryDto("BE", "Belgique", new ProgrammeDto(2, "Erabel", "Mobi-ERABEL"), 3);

    assertThat(mapper.toDto(mapper.toDomain(country))).isEqualTo(country);
    assertThat(mapper.toCountryDtos(List.of(mapper.toDomain(country)))).containsExactly(country);
  }

  @Test
  void aCountryKnownByItsCodeOnlyKeepsItsCodeOnly() {
    CountryDto country = new CountryDto("FR", null, null, 0);

    assertThat(mapper.toDto(mapper.toDomain(country))).isEqualTo(country);
  }

  @Test
  void aDenialReasonRoundTrips() {
    DenialReasonDto reason = new DenialReasonDto(4, "Dossier incomplet", 0);

    assertThat(mapper.toDto(mapper.toDomain(reason))).isEqualTo(reason);
    assertThat(mapper.toDenialReasonDtos(List.of(mapper.toDomain(reason)))).containsExactly(reason);
  }

  @Test
  void nullStaysNull() {
    assertThat(mapper.toDto((com.dragomitch.ipl.pae.domain.model.Option) null)).isNull();
    assertThat(mapper.toDomain((OptionDto) null)).isNull();
    assertThat(mapper.toDto((com.dragomitch.ipl.pae.domain.model.Programme) null)).isNull();
    assertThat(mapper.toDomain((ProgrammeDto) null)).isNull();
    assertThat(mapper.toDto((com.dragomitch.ipl.pae.domain.model.Country) null)).isNull();
    assertThat(mapper.toDomain((CountryDto) null)).isNull();
    assertThat(mapper.toDto((com.dragomitch.ipl.pae.domain.model.DenialReason) null)).isNull();
    assertThat(mapper.toDomain((DenialReasonDto) null)).isNull();
  }
}
