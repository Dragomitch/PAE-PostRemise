package persistence.mocks;

import business.dto.PartnerDto;
import business.dto.UserDto;
import persistence.PartnerDao;

import java.util.ArrayList;
import java.util.List;

public class MockPartnerDao implements PartnerDao {
  private List<PartnerDto> partners;

  public MockPartnerDao() {
    partners = new ArrayList<PartnerDto>();
  }

  @Override
  public PartnerDto create(PartnerDto partner) {
    partners.add(partner);
    partner.setId(partners.size());
    return partner;
  }

  @Override
  public PartnerDto findById(int id) {
    if (id > 0 && id <= partners.size()) {
      return partners.get((id - 1));
    }
    return null;
  }

  @Override
  public List<PartnerDto> findAll(String filter, String value, String userRole, String option) {
    List<PartnerDto> list = new ArrayList<PartnerDto>();
    boolean[] optOk = new boolean[1];
    if (filter.equals("all")) {
      if (userRole.equals(UserDto.ROLE_PROFESSOR)) {
        return partners;
      } else {
        for (PartnerDto partnerDto : partners) {
          partnerDto.getOptions().forEach(opt -> {
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
    } else if (filter.equals("country")) {
      if (userRole.equals(UserDto.ROLE_STUDENT)) {
        for (PartnerDto partnerDto : list) {
          partnerDto.getOptions().forEach(opt -> {
            if (opt.getCode().equals(option)) {
              optOk[0] = true;
            }
          });
          if (partnerDto.isOfficial()
              && partnerDto.getAddress().getCountry().getCountryCode().equals(value) && optOk[0]) {
            list.add(partnerDto);
          }
        }
        return list;
      } else {
        for (PartnerDto partnerDto : list) {
          if (partnerDto.getAddress().getCountry().getCountryCode().equals(value)) {
            list.add(partnerDto);
          }
        }
        return list;
      }
    } else {
      if (userRole.equals(UserDto.ROLE_STUDENT)) {
        for (PartnerDto partnerDto : partners) {
          partnerDto.getOptions().forEach(opt -> {
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
        for (PartnerDto partnerDto : partners) {
          if (partnerDto.isArchived()) {
            list.add(partnerDto);
          }
        }
        return list;
      }
    }
  }

  @Override
  public PartnerDto update(PartnerDto partner) {
    partner.setVersion(partners.get(partner.getId() - 1).getVersion() + 1);
    partners.set(partner.getId() - 1, partner);
    return partner;
  }

  public void empty() {
    partners = new ArrayList<PartnerDto>();
  }

}
