package com.dragomitch.ipl.pae.web;

import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.uccontrollers.CountryUcc;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.BASE + "/countries")
@PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
public class CountryController {

  private final CountryUcc countryUcc;

  public CountryController(CountryUcc countryUcc) {
    this.countryUcc = countryUcc;
  }

  @GetMapping
  public List<CountryDto> showAll() {
    return countryUcc.showAll();
  }

  @GetMapping("/{code}")
  public CountryDto showOne(@PathVariable String code) {
    return countryUcc.showOne(code);
  }
}
