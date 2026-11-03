package com.dragomitch.ipl.pae.application;

import com.dragomitch.ipl.pae.domain.dto.OptionDto;
import com.dragomitch.ipl.pae.domain.dto.PartnerDto;
import com.dragomitch.ipl.pae.domain.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.domain.port.in.OptionService;
import com.dragomitch.ipl.pae.domain.port.out.OptionRepository;
import com.dragomitch.ipl.pae.domain.port.out.PartnerOptionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class OptionServiceImpl implements OptionService {

  private final OptionRepository optionRepository;
  private final PartnerOptionRepository partnerOptionRepository;

  OptionServiceImpl(
      OptionRepository optionRepository, PartnerOptionRepository partnerOptionRepository) {
    this.optionRepository = optionRepository;
    this.partnerOptionRepository = partnerOptionRepository;
  }

  @Override
  public List<OptionDto> showAll() {
    return optionRepository.findAll();
  }

  @Override
  public List<PartnerDto> findAllPartnersByOption(String optionCode) {
    if (optionRepository.findByCode(optionCode) == null) {
      throw new ResourceNotFoundException();
    }
    return partnerOptionRepository.findAllPartnersByOption(optionCode);
  }
}
