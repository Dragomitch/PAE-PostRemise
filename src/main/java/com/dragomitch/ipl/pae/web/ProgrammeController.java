package com.dragomitch.ipl.pae.web;

import com.dragomitch.ipl.pae.business.dto.ProgrammeDto;
import com.dragomitch.ipl.pae.uccontrollers.ProgrammeUcc;

import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.BASE + "/programmes")
@PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
public class ProgrammeController {

  private final ProgrammeUcc programmeUcc;

  public ProgrammeController(ProgrammeUcc programmeUcc) {
    this.programmeUcc = programmeUcc;
  }

  @GetMapping
  public List<ProgrammeDto> showAll() {
    return programmeUcc.showAll();
  }

  @GetMapping("/{id}")
  public ProgrammeDto showOne(@PathVariable @Positive int id) {
    return programmeUcc.showOne(id);
  }
}
