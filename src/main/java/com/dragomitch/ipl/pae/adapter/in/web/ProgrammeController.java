package com.dragomitch.ipl.pae.adapter.in.web;

import com.dragomitch.ipl.pae.domain.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.domain.port.in.ProgrammeService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Programmes", description = "Reference data: the mobility programmes.")
@RestController
@RequestMapping(ApiPaths.BASE + "/programmes")
@PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
public class ProgrammeController {

  private final ProgrammeService programmeService;

  public ProgrammeController(ProgrammeService programmeService) {
    this.programmeService = programmeService;
  }

  @GetMapping
  public List<ProgrammeDto> showAll() {
    return programmeService.showAll();
  }

  @GetMapping("/{id}")
  public ProgrammeDto showOne(@PathVariable @Positive int id) {
    return programmeService.showOne(id);
  }
}
