package com.dragomitch.ipl.pae.uccontrollers.implementations;

import com.dragomitch.ipl.pae.business.dto.AddressDto;
import com.dragomitch.ipl.pae.business.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.PartnerOptionDto;
import com.dragomitch.ipl.pae.business.dto.PartnerSearch;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.business.exceptions.InsufficientPermissionException;
import com.dragomitch.ipl.pae.business.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.persistence.AddressDao;
import com.dragomitch.ipl.pae.persistence.MobilityChoiceDao;
import com.dragomitch.ipl.pae.persistence.OptionDao;
import com.dragomitch.ipl.pae.persistence.PartnerDao;
import com.dragomitch.ipl.pae.persistence.PartnerOptionDao;
import com.dragomitch.ipl.pae.persistence.ProgrammeDao;
import com.dragomitch.ipl.pae.persistence.UserDao;
import com.dragomitch.ipl.pae.uccontrollers.PartnerUcc;

import java.util.LinkedList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Partners. Their format is checked by the constraints of {@link PartnerDto} (at creation, at least
 * one option); this class checks the rules that need the database or the requester.
 */
@Service
@Transactional
class PartnerUccImpl implements PartnerUcc {

  private final AddressDao addressDao;
  private final OptionDao optionDao;
  private final PartnerDao partnerDao;
  private final PartnerOptionDao partnerOptionDao;
  private final MobilityChoiceDao mobilityChoiceDao;
  private final ProgrammeDao programmeDao;
  private final UserDao userDao;

  PartnerUccImpl(AddressDao addressDao, OptionDao optionDao, PartnerDao partnerDao,
      PartnerOptionDao partnerOptionDao, MobilityChoiceDao mobilityChoiceDao,
      ProgrammeDao programmeDao, UserDao userDao) {
    this.addressDao = addressDao;
    this.optionDao = optionDao;
    this.partnerDao = partnerDao;
    this.partnerOptionDao = partnerOptionDao;
    this.programmeDao = programmeDao;
    this.userDao = userDao;
    this.mobilityChoiceDao = mobilityChoiceDao;
  }

  @Override
  public PartnerDto create(PartnerDto partner, String userRole) {
    if ((userRole.equals(UserDto.ROLE_STUDENT)) && (partner.isOfficial() == true)) {
      throw new InsufficientPermissionException();
    }
    partner.setAddress(addressDao.create(partner.getAddress()));
    partner = partnerDao.create(partner);
    List<PartnerOptionDto> options = partner.getOptions();
    for (PartnerOptionDto partnerOption: options) {
      addOption(partner.getId(), partnerOption);
    }
    partner.setProgramme(partner.getAddress().getCountry().getProgramme());
    return partner;
  }

  @Override
  @Transactional(readOnly = true)
  public PartnerDto showOne(int id) {
    PartnerDto partner = partnerDao.findById(id);
    if (partner == null) {
      throw new ResourceNotFoundException();
    }
    partner.setAddress(addressDao.findById(partner.getAddress().getId()));
    partner.setProgramme(programmeDao.findById(partner.getProgramme().getId()));
    partner.setOptions(partnerOptionDao.findAllOptionsByPartner(id));
    List<MobilityChoiceDto> mobilityChoices = mobilityChoiceDao.findByActivePartner(id);
    partner.setArchivable(mobilityChoices.isEmpty());
    return partner;
  }

  @Override
  @Transactional(readOnly = true)
  public List<PartnerDto> showAll(PartnerSearch search, String userRole, int userId) {
    // the value of a filter is required by the constraints of PartnerSearch
    String filterToUse = search.filter() == null ? PartnerDao.FILTER_ALL_PARTNERS : search.filter();
    String option;
    UserDto user;
    if ((user = userDao.findById(userId)) == null) {
      throw new ResourceNotFoundException();
    } else {
      option = user.getOption().getCode();
    }
    return partnerDao.findAll(filterToUse, search.value(), userRole, option);
  }

  @Override
  public PartnerDto edit(int id, PartnerDto partner, String userRole) {
    if (userRole.equals(UserDto.ROLE_STUDENT)) {
      throw new InsufficientPermissionException();
    }
    if (partnerDao.findById(id) == null) {
      throw new ResourceNotFoundException();
    }
    partner.setId(id);
    if (partner.isArchived()) {
      if (!mobilityChoiceDao.findByPartner(id).isEmpty()) {
        throw new BusinessException(ErrorCode.PARTNER_HAS_MOBILITY_CHOICES);
      }
    }
    PartnerDto partnerDb = partnerDao.findById(partner.getId());
    List<PartnerOptionDto> optionsDb = partnerOptionDao.findAllOptionsByPartner(partnerDb.getId());
    List<PartnerOptionDto> optionsToAdd = new LinkedList<PartnerOptionDto>();
    if (partner.getOptions() != null) {
      for (PartnerOptionDto option : partner.getOptions()) {
        if (option == null
            || (!containsOption(optionsDb, option.getCode()) && !containsOption(optionsToAdd, option.getCode()))) {
          optionsToAdd.add(option);
        }
      }
    }
    // Options are never removed by an edit, so the partner keeps its existing ones plus the new ones.
    if (optionsDb.isEmpty() && optionsToAdd.isEmpty()) {
      throw new BusinessException(ErrorCode.PARTNER_OPTION_REQUIRED);
    }
    AddressDto addressDb = partnerDb.getAddress();
    partner.getAddress().setId(addressDb.getId());
    partner.setAddress(addressDao.update(partner.getAddress()));
    partner.setVersion((partnerDb.getVersion()));
    for (PartnerOptionDto option : optionsToAdd) {
      addOption(partner.getId(), option);
    }
    partner = partnerDao.update(partner);
    return partner;
  }

  @Override
  public void addOption(int id, PartnerOptionDto partnerOption) {
    if (optionDao.findByCode(partnerOption.getCode()) == null) {
      throw new BusinessException(ErrorCode.UNKNOWN_OPTION, partnerOption.getCode());
    }
    partnerOptionDao.create(partnerOption, id);
  }

  @Override
  @Transactional(readOnly = true)
  public List<PartnerOptionDto> findAllPartnerOption(int partnerId) {
    if (partnerDao.findById(partnerId) == null) {
      throw new ResourceNotFoundException();
    }
    return partnerOptionDao.findAllOptionsByPartner(partnerId);
  }

  @Override
  public PartnerDto restore(int id, String role) {
    PartnerDto partner = null;
    if ((partner = partnerDao.findById(id)) == null) {
      throw new ResourceNotFoundException();
    }
    if (!partner.isArchived()) {
      throw new BusinessException(ErrorCode.PARTNER_NOT_ARCHIVED);
    }
    if (role.equals(UserDto.ROLE_STUDENT) && !partner.isOfficial()) {
      throw new InsufficientPermissionException();
    }
    if (partnerOptionDao.findAllOptionsByPartner(id).isEmpty()) {
      throw new BusinessException(ErrorCode.PARTNER_OPTION_REQUIRED);
    }
    partner.setArchived(false);
    partner.setArchivable(true);
    return partnerDao.update(partner);
  }

  /**
   * Tells whether a list of partner options already contains an option with the given code.
   */
  private static boolean containsOption(List<PartnerOptionDto> options, String code) {
    if (code == null) {
      return false;
    }
    for (PartnerOptionDto option : options) {
      if (option != null && code.equals(option.getCode())) {
        return true;
      }
    }
    return false;
  }
}
