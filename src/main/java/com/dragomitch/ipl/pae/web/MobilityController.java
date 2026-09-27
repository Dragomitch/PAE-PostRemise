package com.dragomitch.ipl.pae.web;

import com.dragomitch.ipl.pae.business.dto.MobilityDto;
import com.dragomitch.ipl.pae.security.CurrentUser;
import com.dragomitch.ipl.pae.uccontrollers.MobilityUcc;

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

  private final MobilityUcc mobilityUcc;

  public MobilityController(MobilityUcc mobilityUcc) {
    this.mobilityUcc = mobilityUcc;
  }

  /** Every mobility for a professor, the own ones for a student. */
  @GetMapping
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public DataResponse<MobilityDto> showAll(CurrentUser currentUser) {
    return new DataResponse<>(mobilityUcc.showAll(currentUser.id(), currentUser.role()));
  }

  @GetMapping("/{id}")
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public MobilityDto showOne(@PathVariable int id, CurrentUser currentUser) {
    return mobilityUcc.showOne(id, currentUser.role(), currentUser.id());
  }

  @PutMapping("/{id}/confirmProEcoEncoding")
  @PreAuthorize(ApiPaths.PROFESSOR)
  public void confirmProEcoEncoding(@PathVariable int id,
      @RequestParam(required = false, defaultValue = MISSING) int version) {
    mobilityUcc.confirmProEcoEncoding(id, version);
  }

  @PutMapping("/{id}/confirmSecondSoftwareEncoding")
  @PreAuthorize(ApiPaths.PROFESSOR)
  public void confirmSecondSoftwareEncoding(@PathVariable int id,
      @RequestParam(required = false, defaultValue = MISSING) int version) {
    mobilityUcc.confirmSecondSoftwareEncoding(id, version);
  }

  @PutMapping("/{id}/confirmPayment")
  @PreAuthorize(ApiPaths.PROFESSOR)
  public MobilityDto confirmPayment(@PathVariable int id,
      @RequestParam(required = false, defaultValue = MISSING) int version) {
    return mobilityUcc.confirmPayment(id, version);
  }

  @PutMapping("/{id}/confirmDocument")
  @PreAuthorize(ApiPaths.PROFESSOR)
  public MobilityDto confirmDocument(@PathVariable int id,
      @RequestParam(required = false, defaultValue = MISSING) int document,
      @RequestParam(required = false, defaultValue = MISSING) int version) {
    return mobilityUcc.confirmDocument(id, document, version);
  }

  /**
   * Cancels a mobility: a professor gives a {@code denialReason} id, a student a free-text
   * {@code cancellationReason}.
   */
  @PutMapping("/{id}/cancel")
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public MobilityDto cancel(@PathVariable int id,
      @RequestParam(required = false, defaultValue = MISSING) int version,
      @RequestParam(required = false) String cancellationReason,
      @RequestParam(required = false, defaultValue = MISSING) int denialReason,
      CurrentUser currentUser) {
    return mobilityUcc.cancel(id, version, cancellationReason, denialReason, currentUser.id(),
        currentUser.role());
  }

  /** CSV export of the documents of a mobility. */
  @GetMapping(value = "/{id}/export", produces = ApiPaths.TEXT_CSV_UTF8)
  @PreAuthorize(ApiPaths.PROFESSOR)
  public String exportDocuments(@PathVariable int id,
      @RequestParam(required = false) String filter) {
    return mobilityUcc.exportDocuments(id, filter);
  }
}
