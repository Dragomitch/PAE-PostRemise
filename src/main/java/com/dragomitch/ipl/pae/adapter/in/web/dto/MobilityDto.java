package com.dragomitch.ipl.pae.adapter.in.web.dto;

import com.dragomitch.ipl.pae.domain.validation.ValidationGroups.Reference;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.ConvertGroup;
import jakarta.validation.groups.Default;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A confirmed mobility choice: the components of its {@link MobilityChoiceDto} (with their
 * constraints, which the OpenAPI document shows; a mobility is never read from a request) and its
 * life cycle.
 *
 * @param id see {@link MobilityChoiceDto}
 * @param user see {@link MobilityChoiceDto}
 * @param preferenceOrder see {@link MobilityChoiceDto}
 * @param mobilityType see {@link MobilityChoiceDto}
 * @param academicYear see {@link MobilityChoiceDto}
 * @param term see {@link MobilityChoiceDto}
 * @param programme see {@link MobilityChoiceDto}
 * @param country see {@link MobilityChoiceDto}
 * @param submissionDate the confirmation date, {@code yyyy-MM-dd HH:mm:ss}
 * @param denialReason the reason of a cancellation by a professor
 * @param cancellationReason the reason of a cancellation by the student
 * @param partner see {@link MobilityChoiceDto}
 * @param state the state, in French
 * @param stateBeforeCancellation the state the mobility was in when it was cancelled
 * @param documents the documents, when they are read
 * @param firstPaymentRequestDate the date of the first payment
 * @param secondPaymentRequestDate the date of the second payment
 * @param encodedInProEco whether the mobility is encoded in ProEco
 * @param encodedInSecondSoftware whether the mobility is encoded in the software of its programme
 * @param professorInCharge the professor who confirmed the choice
 * @param nominatedStudent the student
 * @param version the version
 */
public record MobilityDto(
    @Positive(groups = Reference.class) int id,
    @NotNull @Valid @ConvertGroup(from = Default.class, to = Reference.class) UserDto user,
    @Schema(
            description =
                "Rank of this choice among the applications of the student (1 = first choice).")
        @Min(1)
        @Max(MobilityChoiceDto.MAX_ORDER_CHOICE)
        int preferenceOrder,
    @Schema(description = "SMS (studies abroad) or SMP (internship abroad).")
        @NotBlank
        @Pattern(
            regexp = MobilityChoiceDto.MOBILITY_TYPE_REGEXP,
            message = "{pae.validation.mobilityType.message}")
        String mobilityType,
    @Positive int academicYear,
    @Schema(description = "Term of the academic year (1 = first term, 2 = second term).")
        @Min(1)
        @Max(MobilityChoiceDto.MAX_TERM_CHOICE)
        int term,
    @NotNull @Valid @ConvertGroup(from = Default.class, to = Reference.class)
        ProgrammeDto programme,
    @Valid @ConvertGroup(from = Default.class, to = Reference.class) CountryDto country,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime submissionDate,
    DenialReasonDto denialReason,
    @Size(max = MobilityChoiceDto.CANCELLATION_REASON_MAX_LENGTH) String cancellationReason,
    PartnerDto partner,
    @Schema(
            description =
                "Créée, En préparation (a document is filled in), A payer (every departure"
                    + " document is filled in), En cours (first payment), Solde à payer (every"
                    + " document is filled in), Terminée (second payment) or Annulée.",
            allowableValues = {
              "Créée",
              "En préparation",
              "A payer",
              "En cours",
              "Solde à payer",
              "Terminée",
              "Annulée"
            })
        String state,
    @Schema(description = "The state the mobility was in when it was cancelled.")
        String stateBeforeCancellation,
    List<DocumentDto> documents,
    LocalDateTime firstPaymentRequestDate,
    LocalDateTime secondPaymentRequestDate,
    boolean encodedInProEco,
    boolean encodedInSecondSoftware,
    UserDto professorInCharge,
    NominatedStudentDto nominatedStudent,
    @Schema(description = ApiDocs.VERSION) int version) {}
