package com.dragomitch.ipl.pae.adapter.in.web;

import com.dragomitch.ipl.pae.domain.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.domain.dto.MobilityDto;
import com.dragomitch.ipl.pae.adapter.in.web.security.CurrentUser;
import com.dragomitch.ipl.pae.domain.port.in.MobilityService;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mobilities (confirmed mobility choices). The state changes take the version of the mobility the
 * client read ({@code version} parameter): a stale version is answered with the concurrent
 * modification error (code 120).
 */
@RestController
@RequestMapping(ApiPaths.BASE + "/mobilities")
public class MobilityController {

  /** Value of a missing numeric parameter, as with the former router. */
  private static final String MISSING = "-1";

  private final MobilityService mobilityService;

  public MobilityController(MobilityService mobilityService) {
    this.mobilityService = mobilityService;
  }

  /** Every mobility for a professor, the own ones for a student. */
  @GetMapping
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public DataResponse<MobilityDto> showAll(CurrentUser currentUser) {
    return new DataResponse<>(mobilityService.showAll(currentUser.id(), currentUser.role()));
  }

  @GetMapping("/{id}")
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public MobilityDto showOne(@PathVariable @Positive int id, CurrentUser currentUser) {
    return mobilityService.showOne(id, currentUser.role(), currentUser.id());
  }

  @PutMapping("/{id}/confirmProEcoEncoding")
  @PreAuthorize(ApiPaths.PROFESSOR)
  public void confirmProEcoEncoding(@PathVariable @Positive int id,
      @RequestParam(required = false, defaultValue = MISSING) int version) {
    mobilityService.confirmProEcoEncoding(id, version);
  }

  @PutMapping("/{id}/confirmSecondSoftwareEncoding")
  @PreAuthorize(ApiPaths.PROFESSOR)
  public void confirmSecondSoftwareEncoding(@PathVariable @Positive int id,
      @RequestParam(required = false, defaultValue = MISSING) int version) {
    mobilityService.confirmSecondSoftwareEncoding(id, version);
  }

  @PutMapping("/{id}/confirmPayment")
  @PreAuthorize(ApiPaths.PROFESSOR)
  public MobilityDto confirmPayment(@PathVariable @Positive int id,
      @RequestParam(required = false, defaultValue = MISSING) int version) {
    return mobilityService.confirmPayment(id, version);
  }

  @PutMapping("/{id}/confirmDocument")
  @PreAuthorize(ApiPaths.PROFESSOR)
  public MobilityDto confirmDocument(@PathVariable @Positive int id,
      @RequestParam(required = false, defaultValue = MISSING) @Positive int document,
      @RequestParam(required = false, defaultValue = MISSING) int version) {
    return mobilityService.confirmDocument(id, document, version);
  }

  /**
   * Cancels a mobility: a professor gives a {@code denialReason} id, a student a free-text
   * {@code cancellationReason}.
   */
  @PutMapping("/{id}/cancel")
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public MobilityDto cancel(@PathVariable @Positive int id,
      @RequestParam(required = false, defaultValue = MISSING) int version,
      @RequestParam(required = false)
      @Size(max = MobilityChoiceDto.CANCELLATION_REASON_MAX_LENGTH) String cancellationReason,
      @RequestParam(required = false, defaultValue = MISSING) int denialReason,
      CurrentUser currentUser) {
    return mobilityService.cancel(id, version, cancellationReason, denialReason, currentUser.id(),
        currentUser.role());
  }

  /** CSV export of the documents of a mobility. */
  @GetMapping(value = "/{id}/export", produces = ApiPaths.TEXT_CSV_UTF8)
  @PreAuthorize(ApiPaths.PROFESSOR)
  public String exportDocuments(@PathVariable @Positive int id,
      @RequestParam(required = false) @Pattern(regexp = MobilityService.DOCUMENT_FILTER_REGEXP,
          message = MobilityService.DOCUMENT_FILTER_MESSAGE) String filter) {
    return mobilityService.exportDocuments(id, filter);
  }
}
