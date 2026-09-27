package com.dragomitch.ipl.pae.web;

import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.PartnerOptionDto;
import com.dragomitch.ipl.pae.business.dto.PartnerSearch;
import com.dragomitch.ipl.pae.business.validation.ValidationGroups.OnCreate;
import com.dragomitch.ipl.pae.security.CurrentUser;
import com.dragomitch.ipl.pae.uccontrollers.PartnerUcc;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.groups.Default;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
  public PartnerDto create(
      @RequestBody @Validated({Default.class, OnCreate.class}) PartnerDto partner,
      CurrentUser currentUser) {
    return partnerUcc.create(partner, currentUser.role());
  }

  @GetMapping("/{id}")
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public PartnerDto showOne(@PathVariable @Positive int id) {
    return partnerUcc.showOne(id);
  }

  /**
   * The partners visible to the user, optionally filtered ({@code filter=archived|country} with a
   * {@code value}), wrapped in {@code {"data": [...]}}.
   */
  @GetMapping
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public DataResponse<PartnerDto> showAll(@Valid PartnerSearch search, CurrentUser currentUser) {
    return new DataResponse<>(partnerUcc.showAll(search, currentUser.role(), currentUser.id()));
  }

  @PutMapping("/{id}")
  @PreAuthorize(ApiPaths.PROFESSOR)
  public PartnerDto edit(@PathVariable @Positive int id, @RequestBody @Valid PartnerDto partner,
      CurrentUser currentUser) {
    return partnerUcc.edit(id, partner, currentUser.role());
  }

  /** Adds an option to a partner. */
  @PostMapping("/{id}")
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public void addOption(@PathVariable @Positive int id,
      @RequestBody @Valid PartnerOptionDto partnerOption) {
    partnerUcc.addOption(id, partnerOption);
  }

  @GetMapping("/partnersOptions/{id}")
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public List<PartnerOptionDto> findAllPartnerOption(@PathVariable @Positive int id) {
    return partnerUcc.findAllPartnerOption(id);
  }

  /** Restores an archived partner (students may only restore official ones). */
  @PutMapping("/{id}/restore")
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public PartnerDto restore(@PathVariable @Positive int id, CurrentUser currentUser) {
    return partnerUcc.restore(id, currentUser.role());
  }
}
