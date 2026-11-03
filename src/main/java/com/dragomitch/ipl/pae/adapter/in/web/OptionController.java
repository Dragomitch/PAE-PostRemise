package com.dragomitch.ipl.pae.adapter.in.web;

import com.dragomitch.ipl.pae.adapter.in.web.dto.OptionDto;
import com.dragomitch.ipl.pae.adapter.in.web.mapping.ReferenceDataMapper;
import com.dragomitch.ipl.pae.domain.dto.PartnerDto;
import com.dragomitch.ipl.pae.domain.port.in.OptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
    name = "Options",
    description =
        "Reference data: the study options (public list, shown on " + "the sign-up form).")
@RestController
@RequestMapping(ApiPaths.BASE + "/options")
public class OptionController {

  private final OptionService optionService;
  private final ReferenceDataMapper mapper;

  public OptionController(OptionService optionService, ReferenceDataMapper mapper) {
    this.optionService = optionService;
    this.mapper = mapper;
  }

  /** Public: the sign-up form lists the options. */
  @GetMapping
  public List<OptionDto> showAll() {
    return mapper.toOptionDtos(optionService.showAll());
  }

  /** The partners offering an option. */
  @Operation(summary = "The partners offering the option")
  @GetMapping("/{optionCode}")
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public List<PartnerDto> findAllPartnersByOption(
      @PathVariable @Size(min = OptionDto.CODE_LENGTH, max = OptionDto.CODE_LENGTH)
          String optionCode) {
    return optionService.findAllPartnersByOption(optionCode);
  }
}
