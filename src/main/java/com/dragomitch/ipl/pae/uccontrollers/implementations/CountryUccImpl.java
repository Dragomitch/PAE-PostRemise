package com.dragomitch.ipl.pae.uccontrollers.implementations;

import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.persistence.CountryDao;
import com.dragomitch.ipl.pae.uccontrollers.CountryUcc;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class CountryUccImpl implements CountryUcc {

  private final CountryDao countryDao;

  CountryUccImpl(CountryDao countryDao) {
    this.countryDao = countryDao;
  }

  @Override
  public List<CountryDto> showAll() {
    return countryDao.findAll();
  }

  @Override
  public CountryDto showOne(String countryCode) {
    return countryDao.findById(countryCode);
  }

}
