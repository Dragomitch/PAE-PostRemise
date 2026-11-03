package com.dragomitch.ipl.pae.adapter.in.web.mapping;

import com.dragomitch.ipl.pae.adapter.in.web.dto.DocumentDto;
import com.dragomitch.ipl.pae.adapter.in.web.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.adapter.in.web.dto.MobilityDto;
import com.dragomitch.ipl.pae.adapter.in.web.dto.PaymentDto;
import com.dragomitch.ipl.pae.domain.model.EntityFactory;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Maps the mobility choices, the mobilities, their documents and the payments between the web DTOs
 * and the domain. Only a mobility choice is read from a request.
 */
@Component
public class MobilityMapper {

  private final EntityFactory entityFactory;
  private final ReferenceDataMapper referenceData;
  private final UserMapper users;
  private final PartnerMapper partners;

  public MobilityMapper(
      EntityFactory entityFactory,
      ReferenceDataMapper referenceData,
      UserMapper users,
      PartnerMapper partners) {
    this.entityFactory = entityFactory;
    this.referenceData = referenceData;
    this.users = users;
    this.partners = partners;
  }

  public @Nullable MobilityChoiceDto toDto(
      com.dragomitch.ipl.pae.domain.dto.@Nullable MobilityChoiceDto choice) {
    return choice == null
        ? null
        : new MobilityChoiceDto(
            choice.getId(),
            users.toDto(choice.getUser()),
            choice.getPreferenceOrder(),
            choice.getMobilityType(),
            choice.getAcademicYear(),
            choice.getTerm(),
            referenceData.toDto(choice.getProgramme()),
            referenceData.toDto(choice.getCountry()),
            choice.getSubmissionDate(),
            referenceData.toDto(choice.getDenialReason()),
            choice.getCancellationReason(),
            partners.toDto(choice.getPartner()),
            choice.getVersion());
  }

  public List<MobilityChoiceDto> toMobilityChoiceDtos(
      List<com.dragomitch.ipl.pae.domain.dto.MobilityChoiceDto> choices) {
    return ReferenceDataMapper.list(choices, this::toDto);
  }

  public com.dragomitch.ipl.pae.domain.dto.@Nullable MobilityChoiceDto toDomain(
      @Nullable MobilityChoiceDto dto) {
    if (dto == null) {
      return null;
    }
    var choice =
        (com.dragomitch.ipl.pae.domain.dto.MobilityChoiceDto)
            entityFactory.build(com.dragomitch.ipl.pae.domain.dto.MobilityChoiceDto.class);
    choice.setId(dto.id());
    choice.setUser(users.toDomain(dto.user()));
    choice.setPreferenceOrder(dto.preferenceOrder());
    choice.setMobilityType(dto.mobilityType());
    choice.setAcademicYear(dto.academicYear());
    choice.setTerm(dto.term());
    choice.setProgramme(referenceData.toDomain(dto.programme()));
    choice.setCountry(referenceData.toDomain(dto.country()));
    choice.setSubmissionDate(dto.submissionDate());
    choice.setDenialReason(referenceData.toDomain(dto.denialReason()));
    choice.setCancellationReason(dto.cancellationReason());
    choice.setPartner(partners.toDomain(dto.partner()));
    choice.setVersion(dto.version());
    return choice;
  }

  public @Nullable DocumentDto toDto(
      com.dragomitch.ipl.pae.domain.dto.@Nullable DocumentDto document) {
    return document == null
        ? null
        : new DocumentDto(
            document.getId(),
            document.getName(),
            document.getCategory(),
            document.isFilledIn(),
            referenceData.toDto(document.getProgramme()),
            document.getVersion());
  }

  public @Nullable MobilityDto toDto(
      com.dragomitch.ipl.pae.domain.dto.@Nullable MobilityDto mobility) {
    return mobility == null
        ? null
        : new MobilityDto(
            mobility.getId(),
            users.toDto(mobility.getUser()),
            mobility.getPreferenceOrder(),
            mobility.getMobilityType(),
            mobility.getAcademicYear(),
            mobility.getTerm(),
            referenceData.toDto(mobility.getProgramme()),
            referenceData.toDto(mobility.getCountry()),
            mobility.getSubmissionDate(),
            referenceData.toDto(mobility.getDenialReason()),
            mobility.getCancellationReason(),
            partners.toDto(mobility.getPartner()),
            mobility.getState(),
            mobility.getStateBeforeCancellation(),
            mobility.getDocuments() == null
                ? null
                : ReferenceDataMapper.list(mobility.getDocuments(), this::toDto),
            mobility.getFirstPaymentRequestDate(),
            mobility.getSecondPaymentRequestDate(),
            mobility.isEncodedInProEco(),
            mobility.isEncodedInSecondSoftware(),
            users.toDto(mobility.getProfessorInCharge()),
            users.toDto(mobility.getNominatedStudent()),
            mobility.getVersion());
  }

  public List<MobilityDto> toMobilityDtos(
      List<com.dragomitch.ipl.pae.domain.dto.MobilityDto> mobilities) {
    return ReferenceDataMapper.list(mobilities, this::toDto);
  }

  public @Nullable PaymentDto toDto(
      com.dragomitch.ipl.pae.domain.dto.@Nullable PaymentDto payment) {
    return payment == null
        ? null
        : new PaymentDto(
            payment.getMobilityChoiceId(),
            users.toDto(payment.getUser()),
            users.toDto(payment.getNominatedStudent()),
            payment.getMobilityType(),
            payment.getAcademicYear(),
            payment.getTerm(),
            referenceData.toDto(payment.getProgramme()),
            referenceData.toDto(payment.getCountry()),
            partners.toDto(payment.getPartner()),
            payment.getPaymentDate(),
            payment.getPaymentType());
  }

  public List<PaymentDto> toPaymentDtos(
      List<com.dragomitch.ipl.pae.domain.dto.PaymentDto> payments) {
    return ReferenceDataMapper.list(payments, this::toDto);
  }
}
