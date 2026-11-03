package com.dragomitch.ipl.pae.adapter.in.web;

import com.dragomitch.ipl.pae.adapter.in.web.security.CurrentUser;
import com.dragomitch.ipl.pae.domain.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.domain.dto.PartnerDto;
import com.dragomitch.ipl.pae.domain.port.in.MobilityChoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mobility choices (applications of the students). Note the singular {@code /mobilityChoice} used
 * by the creation, kept for the existing clients.
 */
@Tag(
    name = "Mobility choices",
    description = "The applications of the students, before they " + "become mobilities.")
@RestController
@RequestMapping(ApiPaths.BASE)
public class MobilityChoiceController {

  /**
   * Number of mobility choices.
   *
   * @param count the number
   */
  public record CountResponse(int count) {}

  private final MobilityChoiceService mobilityChoiceService;

  public MobilityChoiceController(MobilityChoiceService mobilityChoiceService) {
    this.mobilityChoiceService = mobilityChoiceService;
  }

  @PostMapping("/mobilityChoice")
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public MobilityChoiceDto create(
      @RequestBody @Valid MobilityChoiceDto mobilityChoice, CurrentUser currentUser) {
    return mobilityChoiceService.create(mobilityChoice, currentUser.id(), currentUser.role());
  }

  /** Every mobility choice matching the filter for a professor, the own ones for a student. */
  @GetMapping("/mobilityChoices")
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public DataResponse<MobilityChoiceDto> showAll(
      @RequestParam(required = false)
          @Pattern(
              regexp = MobilityChoiceService.FILTER_REGEXP,
              message = MobilityChoiceService.FILTER_MESSAGE)
          String filter,
      CurrentUser currentUser) {
    return new DataResponse<>(
        mobilityChoiceService.showAll(currentUser.id(), currentUser.role(), filter));
  }

  @GetMapping("/mobilityChoices/count")
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public CountResponse countAll(
      @RequestParam(required = false)
          @Pattern(
              regexp = MobilityChoiceService.FILTER_REGEXP,
              message = MobilityChoiceService.FILTER_MESSAGE)
          String filter,
      CurrentUser currentUser) {
    return new CountResponse(
        mobilityChoiceService.countAll(currentUser.id(), currentUser.role(), filter));
  }

  @Operation(summary = "Cancels an own mobility choice, with a free-text reason (students)")
  @PutMapping("/mobilityChoices/{id}/cancel")
  @PreAuthorize(ApiPaths.STUDENT)
  public void cancel(
      @PathVariable @Positive int id,
      @RequestParam(required = false)
          @NotBlank
          @Size(max = MobilityChoiceDto.CANCELLATION_REASON_MAX_LENGTH)
          String reason,
      CurrentUser currentUser) {
    mobilityChoiceService.cancel(id, currentUser.id(), reason);
  }

  @Operation(summary = "Rejects a mobility choice, with the id of a denial reason (professors)")
  @PutMapping("/mobilityChoices/{id}/reject")
  @PreAuthorize(ApiPaths.PROFESSOR)
  public void reject(
      @PathVariable @Positive int id,
      @RequestParam(required = false, defaultValue = "-1") @Positive int reason) {
    mobilityChoiceService.reject(id, reason);
  }

  @Operation(summary = "Confirms a mobility choice with its partner: it becomes a mobility")
  @PutMapping("/mobilityChoices/{id}/confirm")
  @PreAuthorize(ApiPaths.PROFESSOR)
  public void confirm(@PathVariable @Positive int id, CurrentUser currentUser) {
    mobilityChoiceService.confirm(id, currentUser.id());
  }

  @Operation(
      summary =
          "Creates the partner of the body (non-official when created by a "
              + "student), sets it as the partner of the mobility choice and confirms it")
  @PutMapping("/mobilityChoices/{id}/confirmWithNewPartner")
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public void confirmWithNewPartner(
      @PathVariable @Positive int id,
      @RequestBody @Valid PartnerDto partner,
      CurrentUser currentUser) {
    mobilityChoiceService.confirmWithNewPartner(id, partner, currentUser.id(), currentUser.role());
  }

  /** CSV export of the mobility choices matching the filter. */
  @Operation(summary = "CSV export of the mobility choices matching the filter")
  @GetMapping(value = "/mobilityChoices/export", produces = ApiPaths.TEXT_CSV_UTF8)
  @PreAuthorize(ApiPaths.PROFESSOR)
  public String exportAll(
      @RequestParam(required = false)
          @Pattern(
              regexp = MobilityChoiceService.FILTER_REGEXP,
              message = MobilityChoiceService.FILTER_MESSAGE)
          String filter,
      CurrentUser currentUser) {
    return mobilityChoiceService.exportAll(currentUser.id(), currentUser.role(), filter);
  }
}
