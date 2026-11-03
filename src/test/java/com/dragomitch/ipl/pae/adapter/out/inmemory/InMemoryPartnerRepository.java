package com.dragomitch.ipl.pae.adapter.out.inmemory;

import com.dragomitch.ipl.pae.domain.model.Partner;
import com.dragomitch.ipl.pae.domain.model.PartnerFilter;
import com.dragomitch.ipl.pae.domain.model.Role;
import com.dragomitch.ipl.pae.domain.port.out.PartnerRepository;
import java.util.ArrayList;
import java.util.List;

public class InMemoryPartnerRepository implements PartnerRepository, ResettableRepository {
  private List<Partner> partners;

  public InMemoryPartnerRepository() {
    partners = new ArrayList<Partner>();
  }

  @Override
  public Partner create(Partner partner) {
    partners.add(partner);
    partner.setId(partners.size());
    return partner;
  }

  @Override
  public Partner findById(int id) {
    if (id > 0 && id <= partners.size()) {
      return partners.get((id - 1));
    }
    return null;
  }

  @Override
  public List<Partner> findAll(PartnerFilter filter, String value, Role userRole, String option) {
    List<Partner> list = new ArrayList<Partner>();
    boolean[] optOk = new boolean[1];
    if (filter == PartnerFilter.ALL) {
      if (userRole == Role.PROFESSOR) {
        return partners;
      } else {
        for (Partner partnerDto : partners) {
          partnerDto
              .getOptions()
              .forEach(opt -> {
                if (opt.getCode().equals(option)) {
                  optOk[0] = true;
                }
              });
          if (optOk[0] && partnerDto.isArchived() == false && partnerDto.isOfficial()) {
            list.add(partnerDto);
          }
        }
        return list;
      }
    } else if (filter == PartnerFilter.COUNTRY) {
      if (userRole == Role.STUDENT) {
        for (Partner partnerDto : list) {
          partnerDto
              .getOptions()
              .forEach(opt -> {
                if (opt.getCode().equals(option)) {
                  optOk[0] = true;
                }
              });
          if (partnerDto.isOfficial()
              && partnerDto.getAddress().getCountry().getCountryCode().equals(value)
              && optOk[0]) {
            list.add(partnerDto);
          }
        }
        return list;
      } else {
        for (Partner partnerDto : list) {
          if (partnerDto.getAddress().getCountry().getCountryCode().equals(value)) {
            list.add(partnerDto);
          }
        }
        return list;
      }
    } else if (filter == PartnerFilter.ARCHIVED) {
      if (userRole == Role.STUDENT) {
        for (Partner partnerDto : partners) {
          partnerDto
              .getOptions()
              .forEach(opt -> {
                if (opt.getCode().equals(option)) {
                  optOk[0] = true;
                }
              });
          if (optOk[0] && partnerDto.isArchived()) {
            list.add(partnerDto);
          }
        }
        return list;
      } else {
        for (Partner partnerDto : partners) {
          if (partnerDto.isArchived()) {
            list.add(partnerDto);
          }
        }
        return list;
      }
    } else {
      throw new IllegalArgumentException("Unknown partner filter: " + filter);
    }
  }

  @Override
  public Partner update(Partner partner) {
    partner.setVersion(partners.get(partner.getId() - 1).getVersion() + 1);
    partners.set(partner.getId() - 1, partner);
    return partner;
  }

  @Override
  public void empty() {
    partners = new ArrayList<Partner>();
  }
}
