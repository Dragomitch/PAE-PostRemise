package com.dragomitch.ipl.pae.adapter.out.inmemory;

import com.dragomitch.ipl.pae.domain.model.Partner;
import com.dragomitch.ipl.pae.domain.model.PartnerOption;
import com.dragomitch.ipl.pae.domain.port.out.PartnerOptionRepository;
import com.dragomitch.ipl.pae.domain.port.out.PartnerRepository;
import java.util.ArrayList;
import java.util.List;

public class InMemoryPartnerOptionRepository
    implements PartnerOptionRepository, ResettableRepository {

  private List<StoredOption> partnerOptions;
  private PartnerRepository partnerRepository;

  /** Sole constructor. */
  public InMemoryPartnerOptionRepository(PartnerRepository partnerRepository) {
    partnerOptions = new ArrayList<StoredOption>();
    this.partnerRepository = partnerRepository;
  }

  @Override
  public PartnerOption create(PartnerOption partnerOption, int id) {
    partnerOptions.add(new StoredOption(partnerOption, id));
    return partnerOption;
  }

  @Override
  public List<Partner> findAllPartnersByOption(String optionCode) {
    List<Partner> partners = new ArrayList<Partner>();
    for (StoredOption partnerOption : partnerOptions) {
      if (partnerOption.getOptionCode().equals(optionCode)) {
        partners.add(partnerRepository.findById(partnerOption.getPartnerId()));
      }
    }
    return partners;
  }

  @Override
  public List<PartnerOption> findAllOptionsByPartner(int partnerId) {
    List<PartnerOption> options = new ArrayList<PartnerOption>();
    for (StoredOption partnerOption : partnerOptions) {
      if (partnerOption.getPartnerId() == partnerId) {
        options.add(partnerOption.getDto());
      }
    }
    return options;
  }

  @Override
  public void empty() {
    partnerOptions = new ArrayList<StoredOption>();
  }

  private static class StoredOption {
    private PartnerOption dto;
    private int partnerId;

    private StoredOption(PartnerOption dto, int partnerId) {
      this.dto = dto;
      this.partnerId = partnerId;
    }

    public String getOptionCode() {
      return dto.getCode();
    }

    public int getPartnerId() {
      return partnerId;
    }

    public PartnerOption getDto() {
      return dto;
    }
  }
}
