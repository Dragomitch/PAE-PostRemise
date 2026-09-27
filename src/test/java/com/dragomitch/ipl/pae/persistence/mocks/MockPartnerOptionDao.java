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
    partnerOptions
        .add(new PartnerOption(partnerOption.getCode(), id, partnerOption.getDepartement()));
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
        // TODO something to do
      }
    }
    return options;
  }

  public void empty() {
    partnerOptions = new ArrayList<PartnerOption>();
  }

  private static class PartnerOption {
    private String optionCode;
    private int partnerId;
    private String departement;

    private PartnerOption(String optionCode, int partnerId, String departement) {
      this.optionCode = optionCode;
      this.partnerId = partnerId;
      this.departement = departement;
    }

    public String getOptionCode() {
      return optionCode;
    }

    public int getPartnerId() {
      return partnerId;
    }

    @SuppressWarnings("unused")
    public String getDepartement() {
      return departement;
    }
  }
}
