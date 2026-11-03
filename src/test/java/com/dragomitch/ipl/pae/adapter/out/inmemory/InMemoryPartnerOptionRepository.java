package com.dragomitch.ipl.pae.adapter.out.inmemory;

import com.dragomitch.ipl.pae.domain.dto.PartnerDto;
import com.dragomitch.ipl.pae.domain.dto.PartnerOptionDto;
import com.dragomitch.ipl.pae.domain.model.PartnerOption;
import com.dragomitch.ipl.pae.domain.port.out.PartnerOptionRepository;
import com.dragomitch.ipl.pae.domain.port.out.PartnerRepository;

import java.util.ArrayList;
import java.util.List;

public class InMemoryPartnerOptionRepository implements PartnerOptionRepository, ResettableRepository {

  private List<PartnerOption> partnerOptions;
  private PartnerRepository partnerRepository;

  /**
   * Sole constructor.
   * 
   */
  public InMemoryPartnerOptionRepository(PartnerRepository partnerRepository) {
    partnerOptions = new ArrayList<PartnerOption>();
    this.partnerRepository = partnerRepository;
  }

  @Override
  public PartnerOptionDto create(PartnerOptionDto partnerOption, int id) {
    partnerOptions.add(new PartnerOption(partnerOption, id));
    return partnerOption;
  }

  @Override
  public List<PartnerDto> findAllPartnersByOption(String optionCode) {
    List<PartnerDto> partners = new ArrayList<PartnerDto>();
    for (PartnerOption partnerOption : partnerOptions) {
      if (partnerOption.getOptionCode().equals(optionCode)) {
        partners.add(partnerRepository.findById(partnerOption.getPartnerId()));
      }
    }
    return partners;
  }

  @Override
  public List<PartnerOptionDto> findAllOptionsByPartner(int partnerId) {
    List<PartnerOptionDto> options = new ArrayList<PartnerOptionDto>();
    for (PartnerOption partnerOption : partnerOptions) {
      if (partnerOption.getPartnerId() == partnerId) {
        options.add(partnerOption.getDto());
      }
    }
    return options;
  }

  @Override
  public void empty() {
    partnerOptions = new ArrayList<PartnerOption>();
  }

  private static class PartnerOption {
    private PartnerOptionDto dto;
    private int partnerId;

    private PartnerOption(PartnerOptionDto dto, int partnerId) {
      this.dto = dto;
      this.partnerId = partnerId;
    }

    public String getOptionCode() {
      return dto.getCode();
    }

    public int getPartnerId() {
      return partnerId;
    }

    public PartnerOptionDto getDto() {
      return dto;
    }
  }
}
