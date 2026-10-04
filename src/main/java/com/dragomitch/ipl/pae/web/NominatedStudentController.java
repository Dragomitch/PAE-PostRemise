package com.dragomitch.ipl.pae.web;

import com.dragomitch.ipl.pae.business.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.security.CurrentUser;
import com.dragomitch.ipl.pae.uccontrollers.NominatedStudentUcc;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Personal data of the nominated students. A student may only read and write his own data.
 */
@RestController
@RequestMapping(ApiPaths.BASE + "/nominatedStudents")
public class NominatedStudentController {

  private final NominatedStudentUcc nominatedStudentUcc;

  public NominatedStudentController(NominatedStudentUcc nominatedStudentUcc) {
    this.nominatedStudentUcc = nominatedStudentUcc;
  }

  @PostMapping
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public NominatedStudentDto create(@RequestBody NominatedStudentDto nominatedStudent,
      CurrentUser currentUser) {
    return nominatedStudentUcc.create(nominatedStudent, currentUser.id(), currentUser.role());
  }

  @GetMapping("/{id}")
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public NominatedStudentDto showOne(@PathVariable int id, CurrentUser currentUser) {
    return nominatedStudentUcc.showOne(id, currentUser.id(), currentUser.role());
  }

  @GetMapping
  @PreAuthorize(ApiPaths.PROFESSOR)
  public List<NominatedStudentDto> showAll() {
    return nominatedStudentUcc.showAll();
  }

  /** Updates the data of the student identified by the path (the id of the body is ignored). */
  @PutMapping("/{id}")
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public NominatedStudentDto edit(@PathVariable int id,
      @RequestBody NominatedStudentDto nominatedStudent, CurrentUser currentUser) {
    nominatedStudent.setId(id);
    return nominatedStudentUcc.edit(nominatedStudent, currentUser.id(), currentUser.role());
  }
}
