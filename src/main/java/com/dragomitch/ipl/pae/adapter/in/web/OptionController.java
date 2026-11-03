package com.dragomitch.ipl.pae.adapter.in.web;

import com.dragomitch.ipl.pae.domain.dto.OptionDto;
import com.dragomitch.ipl.pae.domain.dto.PartnerDto;
import com.dragomitch.ipl.pae.domain.port.in.OptionService;

import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.BASE + "/options")
public class OptionController {

  private final OptionService optionService;

  public OptionController(OptionService optionService) {
    this.optionService = optionService;
  }

  /** Public: the sign-up form lists the options. */
  @GetMapping
  public List<OptionDto> showAll() {
    return optionService.showAll();
  }

  /** The partners offering an option. */
  @GetMapping("/{optionCode}")
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public List<PartnerDto> findAllPartnersByOption(
      @PathVariable @Size(min = OptionDto.CODE_LENGTH, max = OptionDto.CODE_LENGTH)
      String optionCode) {
    return optionService.findAllPartnersByOption(optionCode);
  }
}
