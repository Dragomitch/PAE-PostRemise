package com.dragomitch.ipl.pae.web;

import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.PartnerOptionDto;
import com.dragomitch.ipl.pae.security.CurrentUser;
import com.dragomitch.ipl.pae.uccontrollers.PartnerUcc;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.BASE + "/partners")
public class PartnerController {

  private final PartnerUcc partnerUcc;

  public PartnerController(PartnerUcc partnerUcc) {
    this.partnerUcc = partnerUcc;
  }

  /** Creates a partner (students may only create non-official ones). */
  @PostMapping
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public PartnerDto create(@RequestBody PartnerDto partner, CurrentUser currentUser) {
    return partnerUcc.create(partner, currentUser.role());
  }

  @GetMapping("/{id}")
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public PartnerDto showOne(@PathVariable int id) {
    return partnerUcc.showOne(id);
  }

  /**
   * The partners visible to the user, optionally filtered ({@code filter=archived|country} with a
   * {@code value}), wrapped in {@code {"data": [...]}}.
   */
  @GetMapping
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public DataResponse<PartnerDto> showAll(@RequestParam(required = false) String filter,
      @RequestParam(required = false) String value, CurrentUser currentUser) {
    return new DataResponse<>(
        partnerUcc.showAll(filter, value, currentUser.role(), currentUser.id()));
  }

  @PutMapping("/{id}")
  @PreAuthorize(ApiPaths.PROFESSOR)
  public PartnerDto edit(@PathVariable int id, @RequestBody PartnerDto partner,
      CurrentUser currentUser) {
    return partnerUcc.edit(id, partner, currentUser.role());
  }

  /** Adds an option to a partner. */
  @PostMapping("/{id}")
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public void addOption(@PathVariable int id, @RequestBody PartnerOptionDto partnerOption) {
    partnerUcc.addOption(id, partnerOption);
  }

  @GetMapping("/partnersOptions/{id}")
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public List<PartnerOptionDto> findAllPartnerOption(@PathVariable int id) {
    return partnerUcc.findAllPartnerOption(id);
  }

  /** Restores an archived partner (students may only restore official ones). */
  @PutMapping("/{id}/restore")
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public PartnerDto restore(@PathVariable int id, CurrentUser currentUser) {
    return partnerUcc.restore(id, currentUser.role());
  }
}
