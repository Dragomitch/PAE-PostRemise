package com.dragomitch.ipl.pae.adapter.in.web;

import com.dragomitch.ipl.pae.domain.dto.CountryDto;
import com.dragomitch.ipl.pae.domain.port.in.CountryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Countries", description = "Reference data: the countries.")
@RestController
@RequestMapping(ApiPaths.BASE + "/countries")
@PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
public class CountryController {

  private final CountryService countryService;

  public CountryController(CountryService countryService) {
    this.countryService = countryService;
  }

  @GetMapping
  public List<CountryDto> showAll() {
    return countryService.showAll();
  }

  @GetMapping("/{code}")
  public CountryDto showOne(
      @PathVariable @Size(min = CountryDto.CODE_LENGTH, max = CountryDto.CODE_LENGTH) String code) {
    return countryService.showOne(code);
  }
}
