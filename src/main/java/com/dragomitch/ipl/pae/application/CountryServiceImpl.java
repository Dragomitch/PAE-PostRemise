package com.dragomitch.ipl.pae.application;

import com.dragomitch.ipl.pae.domain.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.domain.model.Country;
import com.dragomitch.ipl.pae.domain.port.in.CountryService;
import com.dragomitch.ipl.pae.domain.port.out.CountryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class CountryServiceImpl implements CountryService {

  private final CountryRepository countryRepository;

  CountryServiceImpl(CountryRepository countryRepository) {
    this.countryRepository = countryRepository;
  }

  @Override
  public List<Country> showAll() {
    return countryRepository.findAll();
  }

  @Override
  public Country showOne(String countryCode) {
    Country country =
        countryRepository.findById(countryCode).orElseThrow(ResourceNotFoundException::new);
    return country;
  }
}
