package com.dragomitch.ipl.pae.adapter.in.web;

import com.dragomitch.ipl.pae.domain.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.domain.port.in.DenialReasonService;

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

@RestController
@RequestMapping(ApiPaths.BASE + "/denialReasons")
@PreAuthorize(ApiPaths.PROFESSOR)
public class DenialReasonController {

  private final DenialReasonService denialReasonService;

  public DenialReasonController(DenialReasonService denialReasonService) {
    this.denialReasonService = denialReasonService;
  }

  @PostMapping
  public DenialReasonDto create(@RequestBody @Valid DenialReasonDto denialReason) {
    return denialReasonService.create(denialReason);
  }

  @GetMapping
  public List<DenialReasonDto> showAll() {
    return denialReasonService.showAll();
  }

  @PutMapping("/{id}")
  public DenialReasonDto edit(@PathVariable @Positive int id,
      @RequestBody @Valid DenialReasonDto denialReason) {
    return denialReasonService.edit(id, denialReason);
  }
}
