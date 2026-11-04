package com.dragomitch.ipl.pae.application;

import com.dragomitch.ipl.pae.domain.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.domain.model.Option;
import com.dragomitch.ipl.pae.domain.model.Partner;
import com.dragomitch.ipl.pae.domain.port.in.OptionService;
import com.dragomitch.ipl.pae.domain.port.out.OptionRepository;
import com.dragomitch.ipl.pae.domain.port.out.PartnerOptionRepository;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
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
  @Cacheable(ReferenceDataCaches.OPTIONS)
  public List<Option> showAll() {
    return List.copyOf(optionRepository.findAll());
  }

  @Override
  public List<Partner> findAllPartnersByOption(String optionCode) {
    if (optionRepository.findByCode(optionCode).isEmpty()) {
      throw new ResourceNotFoundException();
    }
    return partnerOptionRepository.findAllPartnersByOption(optionCode);
  }
}
