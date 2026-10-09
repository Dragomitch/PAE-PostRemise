package com.dragomitch.ipl.pae.web;

import com.dragomitch.ipl.pae.business.dto.OptionDto;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.uccontrollers.OptionUcc;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.BASE + "/options")
public class OptionController {

  private final OptionUcc optionUcc;

  public OptionController(OptionUcc optionUcc) {
    this.optionUcc = optionUcc;
  }

  /** Public: the sign-up form lists the options. */
  @GetMapping
  public List<OptionDto> showAll() {
    return optionUcc.showAll();
  }

  /** The partners offering an option. */
  @GetMapping("/{optionCode}")
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public List<PartnerDto> findAllPartnersByOption(@PathVariable String optionCode) {
    return optionUcc.findAllPartnersByOption(optionCode);
  }
}
