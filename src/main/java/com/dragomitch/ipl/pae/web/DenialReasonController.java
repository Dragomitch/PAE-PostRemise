package com.dragomitch.ipl.pae.web;

import com.dragomitch.ipl.pae.business.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.uccontrollers.DenialReasonUcc;

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

  private final DenialReasonUcc denialReasonUcc;

  public DenialReasonController(DenialReasonUcc denialReasonUcc) {
    this.denialReasonUcc = denialReasonUcc;
  }

  @PostMapping
  public DenialReasonDto create(@RequestBody DenialReasonDto denialReason) {
    return denialReasonUcc.create(denialReason);
  }

  @GetMapping
  public List<DenialReasonDto> showAll() {
    return denialReasonUcc.showAll();
  }

  @PutMapping("/{id}")
  public DenialReasonDto edit(@PathVariable int id, @RequestBody DenialReasonDto denialReason) {
    return denialReasonUcc.edit(id, denialReason);
  }
}
