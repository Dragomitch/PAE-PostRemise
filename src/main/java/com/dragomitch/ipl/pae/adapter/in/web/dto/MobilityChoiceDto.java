package com.dragomitch.ipl.pae.adapter.in.web.dto;

import com.dragomitch.ipl.pae.adapter.in.web.validation.ValidationGroups.Reference;
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

/**
 * An application of a student for a mobility. Lengths: {@code mobility_choices}.
 *
 * @param id the id
 * @param user the applicant, referenced by id
 * @param preferenceOrder the rank of this choice among the applications of the student
 * @param mobilityType SMS or SMP
 * @param academicYear the academic year
 * @param term the term of the academic year
 * @param programme the mobility programme, referenced by id
 * @param country the destination country, referenced by its code; optional
 * @param submissionDate the submission date, set by the server
 * @param denialReason the reason of a rejection by a professor
 * @param cancellationReason the reason of a cancellation by the student
 * @param partner the partner, when one is chosen
 * @param version the version
 */
public record MobilityChoiceDto(
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
    LocalDateTime submissionDate,
    DenialReasonDto denialReason,
    @Size(max = MobilityChoiceDto.CANCELLATION_REASON_MAX_LENGTH) String cancellationReason,
    PartnerDto partner,
    @Schema(description = ApiDocs.VERSION) int version) {

  /** Highest preference order of a mobility choice (1 = first choice). */
  public static final int MAX_ORDER_CHOICE = 3;

  /** Highest term (1 = first term, 2 = second term). */
  public static final int MAX_TERM_CHOICE = 2;

  /** The mobility types (an empty value is reported by {@code @NotBlank} only). */
  public static final String MOBILITY_TYPE_REGEXP = "(SMS|SMP)?";

  public static final int CANCELLATION_REASON_MAX_LENGTH = 500;
}
