package com.dragomitch.ipl.pae.adapter.in.web.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import com.dragomitch.ipl.pae.adapter.in.web.dto.CountryDto;
import com.dragomitch.ipl.pae.adapter.in.web.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.adapter.in.web.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.adapter.in.web.dto.MobilityDto;
import com.dragomitch.ipl.pae.adapter.in.web.dto.PaymentDto;
import com.dragomitch.ipl.pae.adapter.in.web.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.domain.model.EntityFactories;
import com.dragomitch.ipl.pae.domain.model.EntityFactory;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The mobility choices, mobilities and payments are mapped with everything they carry. */
class MobilityMapperTest {

  private final EntityFactory factory = EntityFactories.create();
  private final ReferenceDataMapper referenceData = new ReferenceDataMapper(factory);
  private final AddressMapper addresses = new AddressMapper(factory, referenceData);
  private final UserMapper users = new UserMapper(factory, referenceData, addresses);
  private final PartnerMapper partners = new PartnerMapper(factory, referenceData, addresses);
  private final MobilityMapper mapper = new MobilityMapper(factory, referenceData, users, partners);

  private static final LocalDateTime DATE = LocalDateTime.of(2026, 3, 4, 5, 6, 7);

  @Test
  void aMobilityChoiceRoundTrips() {
    MobilityChoiceDto choice =
        new MobilityChoiceDto(
            1,
            UserAndPartnerMappersTest.user(),
            2,
            "SMS",
            2026,
            1,
            new ProgrammeDto(1, "Erasmus+", null),
            new CountryDto("FR", "France", null, 0),
            DATE,
            new DenialReasonDto(3, "Dossier incomplet", 0),
            "Changed my mind",
            null,
            4);

    assertThat(mapper.toDto(mapper.toDomain(choice))).isEqualTo(choice);
    assertThat(mapper.toMobilityChoiceDtos(List.of(mapper.toDomain(choice))))
        .containsExactly(choice);
  }

  @Test
  void aMobilityCarriesItsLifeCycleItsDocumentsAndItsPeople() {
    var mobility =
        (com.dragomitch.ipl.pae.domain.dto.MobilityDto)
            factory.build(com.dragomitch.ipl.pae.domain.dto.MobilityDto.class);
    mobility.setId(1);
    mobility.setState("En cours");
    mobility.setStateBeforeCancellation("Créée");
    mobility.setFirstPaymentRequestDate(DATE);
    mobility.setSecondPaymentRequestDate(DATE.plusDays(1));
    mobility.setProEcoEncoding(true);
    mobility.setSubmissionDate(DATE);
    var document =
        (com.dragomitch.ipl.pae.domain.dto.DocumentDto)
            factory.build(com.dragomitch.ipl.pae.domain.dto.DocumentDto.class);
    document.setId(5);
    document.setName("Contrat de bourse");
    document.setCategory('D');
    document.setFilledIn(true);
    mobility.setDocuments(List.of(document));
    var professor =
        (com.dragomitch.ipl.pae.domain.dto.UserDto)
            factory.build(com.dragomitch.ipl.pae.domain.dto.UserDto.class);
    professor.setId(9);
    mobility.setProfessorInCharge(professor);
    var student =
        (com.dragomitch.ipl.pae.domain.dto.NominatedStudentDto)
            factory.build(com.dragomitch.ipl.pae.domain.dto.NominatedStudentDto.class);
    student.setId(2);
    mobility.setNominatedStudent(student);
    mobility.setVersion(7);

    MobilityDto dto = mapper.toDto(mobility);

    assertThat(dto.state()).isEqualTo("En cours");
    assertThat(dto.stateBeforeCancellation()).isEqualTo("Créée");
    assertThat(dto.submissionDate()).isEqualTo(DATE);
    assertThat(dto.firstPaymentRequestDate()).isEqualTo(DATE);
    assertThat(dto.secondPaymentRequestDate()).isEqualTo(DATE.plusDays(1));
    assertThat(dto.encodedInProEco()).isTrue();
    assertThat(dto.encodedInSecondSoftware()).isFalse();
    assertThat(dto.documents())
        .singleElement()
        .satisfies(d -> {
          assertThat(d.id()).isEqualTo(5);
          assertThat(d.category()).isEqualTo('D');
          assertThat(d.filledIn()).isTrue();
          assertThat(d.programme()).isNull();
        });
    assertThat(dto.professorInCharge().id()).isEqualTo(9);
    assertThat(dto.nominatedStudent().id()).isEqualTo(2);
    assertThat(dto.user()).isNull();
    assertThat(dto.version()).isEqualTo(7);
    assertThat(mapper.toMobilityDtos(List.of(mobility))).containsExactly(dto);

    mobility.setDocuments(null);
    assertThat(mapper.toDto(mobility).documents()).isNull();
  }

  @Test
  void aPaymentCarriesItsMobilityAndItsDate() {
    var payment =
        (com.dragomitch.ipl.pae.domain.dto.PaymentDto)
            factory.build(com.dragomitch.ipl.pae.domain.dto.PaymentDto.class);
    payment.setMobilityChoiceId(1);
    payment.setMobilityType("SMP");
    payment.setAcademicYear("2026");
    payment.setTerm(2);
    payment.setPaymentDate(DATE);
    payment.setPaymentType("R");

    PaymentDto dto = mapper.toDto(payment);

    assertThat(dto)
        .isEqualTo(new PaymentDto(1, null, null, "SMP", "2026", 2, null, null, null, DATE, "R"));
    assertThat(mapper.toPaymentDtos(List.of(payment))).containsExactly(dto);
  }

  @Test
  void nullStaysNull() {
    assertThat(mapper.toDto((com.dragomitch.ipl.pae.domain.dto.MobilityChoiceDto) null)).isNull();
    assertThat(mapper.toDomain(null)).isNull();
    assertThat(mapper.toDto((com.dragomitch.ipl.pae.domain.dto.MobilityDto) null)).isNull();
    assertThat(mapper.toDto((com.dragomitch.ipl.pae.domain.dto.DocumentDto) null)).isNull();
    assertThat(mapper.toDto((com.dragomitch.ipl.pae.domain.dto.PaymentDto) null)).isNull();
  }
}
