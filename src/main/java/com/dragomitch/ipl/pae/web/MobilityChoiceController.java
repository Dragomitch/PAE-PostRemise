package com.dragomitch.ipl.pae.web;

import com.dragomitch.ipl.pae.business.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.security.CurrentUser;
import com.dragomitch.ipl.pae.uccontrollers.MobilityChoiceUcc;

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
@RestController
@RequestMapping(ApiPaths.BASE)
public class MobilityChoiceController {

  /**
   * Number of mobility choices.
   *
   * @param count the number
   */
  public record CountResponse(int count) {
  }

  private final MobilityChoiceUcc mobilityChoiceUcc;

  public MobilityChoiceController(MobilityChoiceUcc mobilityChoiceUcc) {
    this.mobilityChoiceUcc = mobilityChoiceUcc;
  }

  @PostMapping("/mobilityChoice")
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public MobilityChoiceDto create(@RequestBody @Valid MobilityChoiceDto mobilityChoice,
      CurrentUser currentUser) {
    return mobilityChoiceUcc.create(mobilityChoice, currentUser.id(), currentUser.role());
  }

  /** Every mobility choice matching the filter for a professor, the own ones for a student. */
  @GetMapping("/mobilityChoices")
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public DataResponse<MobilityChoiceDto> showAll(@RequestParam(required = false) @Pattern(
      regexp = MobilityChoiceUcc.FILTER_REGEXP, message = MobilityChoiceUcc.FILTER_MESSAGE)
      String filter, CurrentUser currentUser) {
    return new DataResponse<>(
        mobilityChoiceUcc.showAll(currentUser.id(), currentUser.role(), filter));
  }

  @GetMapping("/mobilityChoices/count")
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public CountResponse countAll(@RequestParam(required = false) @Pattern(
      regexp = MobilityChoiceUcc.FILTER_REGEXP, message = MobilityChoiceUcc.FILTER_MESSAGE)
      String filter, CurrentUser currentUser) {
    return new CountResponse(
        mobilityChoiceUcc.countAll(currentUser.id(), currentUser.role(), filter));
  }

  @PutMapping("/mobilityChoices/{id}/cancel")
  @PreAuthorize(ApiPaths.STUDENT)
  public void cancel(@PathVariable @Positive int id, @RequestParam(required = false) @NotBlank
      @Size(max = MobilityChoiceDto.CANCELLATION_REASON_MAX_LENGTH) String reason,
      CurrentUser currentUser) {
    mobilityChoiceUcc.cancel(id, currentUser.id(), reason);
  }

  @PutMapping("/mobilityChoices/{id}/reject")
  @PreAuthorize(ApiPaths.PROFESSOR)
  public void reject(@PathVariable @Positive int id,
      @RequestParam(required = false, defaultValue = "-1") @Positive int reason) {
    mobilityChoiceUcc.reject(id, reason);
  }

  @PutMapping("/mobilityChoices/{id}/confirm")
  @PreAuthorize(ApiPaths.PROFESSOR)
  public void confirm(@PathVariable @Positive int id, CurrentUser currentUser) {
    mobilityChoiceUcc.confirm(id, currentUser.id());
  }

  @PutMapping("/mobilityChoices/{id}/confirmWithNewPartner")
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public void confirmWithNewPartner(@PathVariable @Positive int id,
      @RequestBody @Valid PartnerDto partner,
      CurrentUser currentUser) {
    mobilityChoiceUcc.confirmWithNewPartner(id, partner, currentUser.id(), currentUser.role());
  }

  /** CSV export of the mobility choices matching the filter. */
  @GetMapping(value = "/mobilityChoices/export", produces = ApiPaths.TEXT_CSV_UTF8)
  @PreAuthorize(ApiPaths.PROFESSOR)
  public String exportAll(@RequestParam(required = false) @Pattern(
      regexp = MobilityChoiceUcc.FILTER_REGEXP, message = MobilityChoiceUcc.FILTER_MESSAGE)
      String filter, CurrentUser currentUser) {
    return mobilityChoiceUcc.exportAll(currentUser.id(), currentUser.role(), filter);
  }
}
