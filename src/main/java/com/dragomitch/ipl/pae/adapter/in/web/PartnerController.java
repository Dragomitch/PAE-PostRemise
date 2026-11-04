package com.dragomitch.ipl.pae.adapter.in.web;

import com.dragomitch.ipl.pae.adapter.in.web.dto.PartnerDto;
import com.dragomitch.ipl.pae.adapter.in.web.dto.PartnerOptionDto;
import com.dragomitch.ipl.pae.adapter.in.web.dto.PartnerSearch;
import com.dragomitch.ipl.pae.adapter.in.web.mapping.PartnerMapper;
import com.dragomitch.ipl.pae.adapter.in.web.security.CurrentUser;
import com.dragomitch.ipl.pae.adapter.in.web.validation.ValidationGroups.OnCreate;
import com.dragomitch.ipl.pae.domain.model.PartnerFilter;
import com.dragomitch.ipl.pae.domain.port.in.PartnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(
    name = "Partners",
    description = "Partner institutions and companies, and the options they " + "offer.")
@RestController
@RequestMapping(ApiPaths.BASE + "/partners")
public class PartnerController {

  private final PartnerService partnerService;
  private final PartnerMapper mapper;

  public PartnerController(PartnerService partnerService, PartnerMapper mapper) {
    this.partnerService = partnerService;
    this.mapper = mapper;
  }

  /** Creates a partner (students may only create non-official ones). */
  @PostMapping
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public PartnerDto create(
      @RequestBody @Validated({Default.class, OnCreate.class}) PartnerDto partner,
      CurrentUser currentUser) {
    return mapper.toDto(partnerService.create(mapper.toDomain(partner), currentUser.role()));
  }

  @GetMapping("/{id}")
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public PartnerDto showOne(@PathVariable @Positive int id) {
    return mapper.toDto(partnerService.showOne(id));
  }

  /**
   * The partners visible to the user, optionally filtered ({@code filter=archived|country} with a
   * {@code value}), wrapped in {@code {"data": [...]}}.
   */
  @GetMapping
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public DataResponse<PartnerDto> showAll(@Valid PartnerSearch search, CurrentUser currentUser) {
    PartnerFilter filter =
        search.filter() == null ? PartnerFilter.ALL : PartnerFilter.fromValue(search.filter());
    return new DataResponse<>(
        mapper.toPartnerDtos(
            partnerService.showAll(filter, search.value(), currentUser.role(), currentUser.id())));
  }

  @PutMapping("/{id}")
  @PreAuthorize(ApiPaths.PROFESSOR)
  public PartnerDto edit(
      @PathVariable @Positive int id,
      @RequestBody @Valid PartnerDto partner,
      CurrentUser currentUser) {
    return mapper.toDto(partnerService.edit(id, mapper.toDomain(partner), currentUser.role()));
  }

  /**
   * Adds an option to a partner: any partner for a professor, only a non-official partner of one of
   * his own mobility choices or mobilities for a student (403 otherwise).
   */
  @Operation(summary = "Adds an option to the partner")
  @PostMapping("/{id}")
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public void addOption(
      @PathVariable @Positive int id,
      @RequestBody @Valid PartnerOptionDto partnerOption,
      CurrentUser currentUser) {
    partnerService.addOption(
        id, mapper.toDomain(partnerOption), currentUser.id(), currentUser.role());
  }

  @Operation(summary = "The options offered by the partner")
  @GetMapping("/partnersOptions/{id}")
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public List<PartnerOptionDto> findAllPartnerOption(@PathVariable @Positive int id) {
    return mapper.toPartnerOptionDtos(partnerService.findAllPartnerOption(id));
  }

  /** Restores an archived partner (students may only restore official ones). */
  @PutMapping("/{id}/restore")
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public PartnerDto restore(@PathVariable @Positive int id, CurrentUser currentUser) {
    return mapper.toDto(partnerService.restore(id, currentUser.role()));
  }
}
