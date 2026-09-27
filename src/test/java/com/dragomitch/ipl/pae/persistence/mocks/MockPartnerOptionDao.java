package com.dragomitch.ipl.pae.persistence.mocks;

import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.PartnerOptionDto;
import com.dragomitch.ipl.pae.persistence.PartnerDao;
import com.dragomitch.ipl.pae.persistence.PartnerOptionDao;

import java.util.ArrayList;
import java.util.List;

public class MockPartnerOptionDao implements PartnerOptionDao {

  private List<PartnerOption> partnerOptions;
  private PartnerDao partnerDao;

  /**
   * Sole constructor.
   * 
   */
  public MockPartnerOptionDao(PartnerDao partnerDao) {
    partnerOptions = new ArrayList<PartnerOption>();
    this.partnerDao = partnerDao;
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
        partners.add(partnerDao.findById(partnerOption.getPartnerId()));
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
