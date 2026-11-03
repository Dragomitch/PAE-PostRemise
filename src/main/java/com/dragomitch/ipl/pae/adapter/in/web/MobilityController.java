package com.dragomitch.ipl.pae.adapter.in.web;

import com.dragomitch.ipl.pae.adapter.in.web.security.CurrentUser;
import com.dragomitch.ipl.pae.domain.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.domain.dto.MobilityDto;
import com.dragomitch.ipl.pae.domain.model.DocumentFilter;
import com.dragomitch.ipl.pae.domain.port.in.MobilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(
    name = "Mobilities",
    description =
        "Confirmed mobility choices: documents, software " + "encodings, payments, cancellation.")
@RestController
@RequestMapping(ApiPaths.BASE + "/mobilities")
public class MobilityController {

  /**
   * Values of the {@code filter} parameter of the document export ({@link DocumentFilter}):
   * departure documents (D), filled-in departure documents (DF), return documents (R), filled-in
   * return documents (RF); every document without a filter. An unknown value is refused with a 400
   * {@code VALIDATION_FAILED} problem on the {@code filter} field.
   */
  static final String DOCUMENT_FILTER_REGEXP = "D|DF|R|RF";

  /** Message of an unknown document filter. */
  static final String DOCUMENT_FILTER_MESSAGE =
      "{pae.validation.MobilityService.documentFilter.message}";

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

  @Operation(summary = "Records that the mobility is encoded in ProEco")
  @PutMapping("/{id}/confirmProEcoEncoding")
  @PreAuthorize(ApiPaths.PROFESSOR)
  public void confirmProEcoEncoding(
      @PathVariable @Positive int id,
      @RequestParam(required = false, defaultValue = MISSING) int version) {
    mobilityService.confirmProEcoEncoding(id, version);
  }

  @Operation(summary = "Records that the mobility is encoded in the software of its programme")
  @PutMapping("/{id}/confirmSecondSoftwareEncoding")
  @PreAuthorize(ApiPaths.PROFESSOR)
  public void confirmSecondSoftwareEncoding(
      @PathVariable @Positive int id,
      @RequestParam(required = false, defaultValue = MISSING) int version) {
    mobilityService.confirmSecondSoftwareEncoding(id, version);
  }

  @Operation(
      summary =
          "Records the next payment: the first one in A payer (moves to En cours, "
              + "or to Solde à payer when every document is filled in), the second one in Solde à payer "
              + "(moves to Terminée)")
  @PutMapping("/{id}/confirmPayment")
  @PreAuthorize(ApiPaths.PROFESSOR)
  public MobilityDto confirmPayment(
      @PathVariable @Positive int id,
      @RequestParam(required = false, defaultValue = MISSING) int version) {
    return mobilityService.confirmPayment(id, version);
  }

  @Operation(
      summary =
          "Records that a document of the mobility is filled in (may move it to En "
              + "préparation, A payer or Solde à payer)")
  @PutMapping("/{id}/confirmDocument")
  @PreAuthorize(ApiPaths.PROFESSOR)
  public MobilityDto confirmDocument(
      @PathVariable @Positive int id,
      @RequestParam(required = false, defaultValue = MISSING) @Positive int document,
      @RequestParam(required = false, defaultValue = MISSING) int version) {
    return mobilityService.confirmDocument(id, document, version);
  }

  /**
   * Cancels a mobility: a professor gives a {@code denialReason} id, a student a free-text {@code
   * cancellationReason}.
   */
  @Operation(
      summary =
          "Cancels the mobility: a professor gives a denialReason id, a student a "
              + "free-text cancellationReason")
  @PutMapping("/{id}/cancel")
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public MobilityDto cancel(
      @PathVariable @Positive int id,
      @RequestParam(required = false, defaultValue = MISSING) int version,
      @RequestParam(required = false) @Size(max = MobilityChoiceDto.CANCELLATION_REASON_MAX_LENGTH)
          String cancellationReason,
      @RequestParam(required = false, defaultValue = MISSING) int denialReason,
      CurrentUser currentUser) {
    return mobilityService.cancel(
        id, version, cancellationReason, denialReason, currentUser.id(), currentUser.role());
  }

  /** CSV export of the documents of a mobility. */
  @Operation(summary = "CSV export of the documents of the mobility, optionally filtered")
  @GetMapping(value = "/{id}/export", produces = ApiPaths.TEXT_CSV_UTF8)
  @PreAuthorize(ApiPaths.PROFESSOR)
  public String exportDocuments(
      @PathVariable @Positive int id,
      @RequestParam(required = false)
          @Pattern(regexp = DOCUMENT_FILTER_REGEXP, message = DOCUMENT_FILTER_MESSAGE)
          String filter) {
    return mobilityService.exportDocuments(
        id, filter == null ? null : DocumentFilter.valueOf(filter));
  }
}
