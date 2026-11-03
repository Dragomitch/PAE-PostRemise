package com.dragomitch.ipl.pae.adapter.in.web;

import com.dragomitch.ipl.pae.adapter.in.web.security.CurrentUser;
import com.dragomitch.ipl.pae.domain.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.domain.port.in.NominatedStudentService;
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

/** Personal data of the nominated students. A student may only read and write his own data. */
@Tag(
    name = "Nominated students",
    description = "Personal and bank data of the students " + "selected for a mobility.")
@RestController
@RequestMapping(ApiPaths.BASE + "/nominatedStudents")
public class NominatedStudentController {

  private final NominatedStudentService nominatedStudentService;

  public NominatedStudentController(NominatedStudentService nominatedStudentService) {
    this.nominatedStudentService = nominatedStudentService;
  }

  @PostMapping
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public NominatedStudentDto create(
      @RequestBody @Valid NominatedStudentDto nominatedStudent, CurrentUser currentUser) {
    return nominatedStudentService.create(nominatedStudent, currentUser.id(), currentUser.role());
  }

  @GetMapping("/{id}")
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public NominatedStudentDto showOne(@PathVariable @Positive int id, CurrentUser currentUser) {
    return nominatedStudentService.showOne(id, currentUser.id(), currentUser.role());
  }

  @GetMapping
  @PreAuthorize(ApiPaths.PROFESSOR)
  public List<NominatedStudentDto> showAll() {
    return nominatedStudentService.showAll();
  }

  /** Updates the data of the student identified by the path (the id of the body is ignored). */
  @PutMapping("/{id}")
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public NominatedStudentDto edit(
      @PathVariable @Positive int id,
      @RequestBody @Valid NominatedStudentDto nominatedStudent,
      CurrentUser currentUser) {
    nominatedStudent.setId(id);
    return nominatedStudentService.edit(nominatedStudent, currentUser.id(), currentUser.role());
  }
}
