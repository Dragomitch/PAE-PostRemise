package com.dragomitch.ipl.pae.uccontrollers.implementations;

import com.dragomitch.ipl.pae.business.dto.OptionDto;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.persistence.OptionDao;
import com.dragomitch.ipl.pae.persistence.PartnerOptionDao;
import com.dragomitch.ipl.pae.uccontrollers.OptionUcc;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class OptionUccImpl implements OptionUcc {

  private final OptionDao optionDao;
  private final PartnerOptionDao partnerOptionDao;

  OptionUccImpl(OptionDao optionDao, PartnerOptionDao partnerOptionDao) {
    this.optionDao = optionDao;
    this.partnerOptionDao = partnerOptionDao;
  }

  @Override
  public List<OptionDto> showAll() {
    return optionDao.findAll();
  }

  @Override
  public List<PartnerDto> findAllPartnersByOption(String optionCode) {
    if (optionDao.findByCode(optionCode) == null) {
      throw new ResourceNotFoundException();
    }
    return partnerOptionDao.findAllPartnersByOption(optionCode);
  }

}
