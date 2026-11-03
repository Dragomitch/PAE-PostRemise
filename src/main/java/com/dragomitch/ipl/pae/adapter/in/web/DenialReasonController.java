package com.dragomitch.ipl.pae.adapter.in.web;

import com.dragomitch.ipl.pae.adapter.in.web.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.adapter.in.web.mapping.ReferenceDataMapper;
import com.dragomitch.ipl.pae.domain.port.in.DenialReasonService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
    name = "Denial reasons",
    description =
        "The reasons a professor gives to reject a mobility "
            + "choice or cancel a mobility (professors only).")
@RestController
@RequestMapping(ApiPaths.BASE + "/denialReasons")
@PreAuthorize(ApiPaths.PROFESSOR)
public class DenialReasonController {

  private final DenialReasonService denialReasonService;
  private final ReferenceDataMapper mapper;

  public DenialReasonController(
      DenialReasonService denialReasonService, ReferenceDataMapper mapper) {
    this.denialReasonService = denialReasonService;
    this.mapper = mapper;
  }

  @PostMapping
  public DenialReasonDto create(@RequestBody @Valid DenialReasonDto denialReason) {
    return mapper.toDto(denialReasonService.create(mapper.toDomain(denialReason)));
  }

  @GetMapping
  public List<DenialReasonDto> showAll() {
    return mapper.toDenialReasonDtos(denialReasonService.showAll());
  }

  @PutMapping("/{id}")
  public DenialReasonDto edit(
      @PathVariable @Positive int id, @RequestBody @Valid DenialReasonDto denialReason) {
    return mapper.toDto(denialReasonService.edit(id, mapper.toDomain(denialReason)));
  }
}
