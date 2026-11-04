package com.dragomitch.ipl.pae.adapter.in.web.mapping;

import com.dragomitch.ipl.pae.adapter.in.web.dto.DocumentDto;
import com.dragomitch.ipl.pae.adapter.in.web.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.adapter.in.web.dto.MobilityDto;
import com.dragomitch.ipl.pae.adapter.in.web.dto.PaymentDto;
import com.dragomitch.ipl.pae.domain.model.Document;
import com.dragomitch.ipl.pae.domain.model.Mobility;
import com.dragomitch.ipl.pae.domain.model.MobilityChoice;
import com.dragomitch.ipl.pae.domain.model.MobilityState;
import com.dragomitch.ipl.pae.domain.model.MobilityType;
import com.dragomitch.ipl.pae.domain.model.Payment;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Maps the mobility choices, the mobilities, their documents and the payments between the web DTOs
 * and the domain. Only a mobility choice is read from a request.
 */
@Component
public class MobilityMapper {

  private final ReferenceDataMapper referenceData;
  private final UserMapper users;
  private final PartnerMapper partners;

  public MobilityMapper(
      ReferenceDataMapper referenceData, UserMapper users, PartnerMapper partners) {
    this.referenceData = referenceData;
    this.users = users;
    this.partners = partners;
  }

  public @Nullable MobilityChoiceDto toDto(@Nullable MobilityChoice choice) {
    return choice == null
        ? null
        : new MobilityChoiceDto(
            choice.getId(),
            users.toDto(choice.getUser()),
            choice.getPreferenceOrder(),
            name(choice.getMobilityType()),
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

  public List<MobilityChoiceDto> toMobilityChoiceDtos(List<MobilityChoice> choices) {
    return ReferenceDataMapper.list(choices, this::toDto);
  }

  public @Nullable MobilityChoice toDomain(@Nullable MobilityChoiceDto dto) {
    if (dto == null) {
      return null;
    }
    var choice = new MobilityChoice();
    choice.setId(dto.id());
    choice.setUser(users.toDomain(dto.user()));
    choice.setPreferenceOrder(dto.preferenceOrder());
    choice.setMobilityType(mobilityType(dto.mobilityType()));
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

  public @Nullable DocumentDto toDto(@Nullable Document document) {
    return document == null
        ? null
        : new DocumentDto(
            document.getId(),
            document.getName(),
            document.getCategory() == null ? NO_CATEGORY : document.getCategory().code(),
            document.isFilledIn(),
            referenceData.toDto(document.getProgramme()),
            document.getVersion());
  }

  public @Nullable MobilityDto toDto(@Nullable Mobility mobility) {
    return mobility == null
        ? null
        : new MobilityDto(
            mobility.getId(),
            users.toDto(mobility.getUser()),
            mobility.getPreferenceOrder(),
            name(mobility.getMobilityType()),
            mobility.getAcademicYear(),
            mobility.getTerm(),
            referenceData.toDto(mobility.getProgramme()),
            referenceData.toDto(mobility.getCountry()),
            mobility.getSubmissionDate(),
            referenceData.toDto(mobility.getDenialReason()),
            mobility.getCancellationReason(),
            partners.toDto(mobility.getPartner()),
            label(mobility.getState()),
            label(mobility.getStateBeforeCancellation()),
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

  public List<MobilityDto> toMobilityDtos(List<Mobility> mobilities) {
    return ReferenceDataMapper.list(mobilities, this::toDto);
  }

  public @Nullable PaymentDto toDto(@Nullable Payment payment) {
    return payment == null
        ? null
        : new PaymentDto(
            payment.getMobilityChoiceId(),
            users.toDto(payment.getUser()),
            users.toDto(payment.getNominatedStudent()),
            name(payment.getMobilityType()),
            payment.getAcademicYear(),
            payment.getTerm(),
            referenceData.toDto(payment.getProgramme()),
            referenceData.toDto(payment.getCountry()),
            partners.toDto(payment.getPartner()),
            payment.getPaymentDate(),
            payment.getPaymentType());
  }

  public List<PaymentDto> toPaymentDtos(List<Payment> payments) {
    return ReferenceDataMapper.list(payments, this::toDto);
  }

  /** The category of a document read without its category. */
  private static final char NO_CATEGORY = '\0';

  private static @Nullable String name(@Nullable MobilityType type) {
    return type == null ? null : type.name();
  }

  /** The type of a validated request: SMS, SMP, or none (blank). */
  private static @Nullable MobilityType mobilityType(@Nullable String type) {
    return type == null || type.isBlank() ? null : MobilityType.valueOf(type);
  }

  private static @Nullable String label(@Nullable MobilityState state) {
    return state == null ? null : state.label();
  }
}
