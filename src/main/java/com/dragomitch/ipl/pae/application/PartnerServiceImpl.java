package com.dragomitch.ipl.pae.application;

import com.dragomitch.ipl.pae.domain.dto.AddressDto;
import com.dragomitch.ipl.pae.domain.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.domain.dto.MobilityDto;
import com.dragomitch.ipl.pae.domain.dto.PartnerDto;
import com.dragomitch.ipl.pae.domain.dto.PartnerOptionDto;
import com.dragomitch.ipl.pae.domain.dto.PartnerSearch;
import com.dragomitch.ipl.pae.domain.dto.UserDto;
import com.dragomitch.ipl.pae.domain.exceptions.BusinessException;
import com.dragomitch.ipl.pae.domain.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.domain.exceptions.InsufficientPermissionException;
import com.dragomitch.ipl.pae.domain.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.domain.port.in.PartnerService;
import com.dragomitch.ipl.pae.domain.port.out.AddressRepository;
import com.dragomitch.ipl.pae.domain.port.out.MobilityChoiceRepository;
import com.dragomitch.ipl.pae.domain.port.out.MobilityRepository;
import com.dragomitch.ipl.pae.domain.port.out.OptionRepository;
import com.dragomitch.ipl.pae.domain.port.out.PartnerOptionRepository;
import com.dragomitch.ipl.pae.domain.port.out.PartnerRepository;
import com.dragomitch.ipl.pae.domain.port.out.ProgrammeRepository;
import com.dragomitch.ipl.pae.domain.port.out.UserRepository;
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
class PartnerServiceImpl implements PartnerService {

  private final AddressRepository addressRepository;
  private final OptionRepository optionRepository;
  private final PartnerRepository partnerRepository;
  private final PartnerOptionRepository partnerOptionRepository;
  private final MobilityChoiceRepository mobilityChoiceRepository;
  private final MobilityRepository mobilityRepository;
  private final ProgrammeRepository programmeRepository;
  private final UserRepository userRepository;

  PartnerServiceImpl(
      AddressRepository addressRepository,
      OptionRepository optionRepository,
      PartnerRepository partnerRepository,
      PartnerOptionRepository partnerOptionRepository,
      MobilityChoiceRepository mobilityChoiceRepository,
      MobilityRepository mobilityRepository,
      ProgrammeRepository programmeRepository,
      UserRepository userRepository) {
    this.addressRepository = addressRepository;
    this.optionRepository = optionRepository;
    this.partnerRepository = partnerRepository;
    this.partnerOptionRepository = partnerOptionRepository;
    this.programmeRepository = programmeRepository;
    this.userRepository = userRepository;
    this.mobilityChoiceRepository = mobilityChoiceRepository;
    this.mobilityRepository = mobilityRepository;
  }

  @Override
  public PartnerDto create(PartnerDto partner, String userRole) {
    if ((userRole.equals(UserDto.ROLE_STUDENT)) && (partner.isOfficial() == true)) {
      throw new InsufficientPermissionException();
    }
    partner.setAddress(addressRepository.create(partner.getAddress()));
    partner = partnerRepository.create(partner);
    List<PartnerOptionDto> options = partner.getOptions();
    for (PartnerOptionDto partnerOption : options) {
      createOption(partner.getId(), partnerOption);
    }
    partner.setProgramme(partner.getAddress().getCountry().getProgramme());
    return partner;
  }

  @Override
  @Transactional(readOnly = true)
  public PartnerDto showOne(int id) {
    PartnerDto partner = partnerRepository.findById(id);
    if (partner == null) {
      throw new ResourceNotFoundException();
    }
    partner.setAddress(addressRepository.findById(partner.getAddress().getId()));
    partner.setProgramme(programmeRepository.findById(partner.getProgramme().getId()));
    partner.setOptions(partnerOptionRepository.findAllOptionsByPartner(id));
    List<MobilityChoiceDto> mobilityChoices = mobilityChoiceRepository.findByActivePartner(id);
    partner.setArchivable(mobilityChoices.isEmpty());
    return partner;
  }

  @Override
  @Transactional(readOnly = true)
  public List<PartnerDto> showAll(PartnerSearch search, String userRole, int userId) {
    // the value of a filter is required by the constraints of PartnerSearch
    String filterToUse =
        search.filter() == null ? PartnerRepository.FILTER_ALL_PARTNERS : search.filter();
    String option;
    UserDto user;
    if ((user = userRepository.findById(userId)) == null) {
      throw new ResourceNotFoundException();
    } else {
      option = user.getOption().getCode();
    }
    return partnerRepository.findAll(filterToUse, search.value(), userRole, option);
  }

  @Override
  public PartnerDto edit(int id, PartnerDto partner, String userRole) {
    if (userRole.equals(UserDto.ROLE_STUDENT)) {
      throw new InsufficientPermissionException();
    }
    if (partnerRepository.findById(id) == null) {
      throw new ResourceNotFoundException();
    }
    partner.setId(id);
    if (partner.isArchived()) {
      if (!mobilityChoiceRepository.findByPartner(id).isEmpty()) {
        throw new BusinessException(ErrorCode.PARTNER_HAS_MOBILITY_CHOICES);
      }
    }
    PartnerDto partnerDb = partnerRepository.findById(partner.getId());
    List<PartnerOptionDto> optionsDb =
        partnerOptionRepository.findAllOptionsByPartner(partnerDb.getId());
    List<PartnerOptionDto> optionsToAdd = new LinkedList<PartnerOptionDto>();
    if (partner.getOptions() != null) {
      for (PartnerOptionDto option : partner.getOptions()) {
        // the options are never null here (List<@NotNull @Valid PartnerOptionDto>)
        if (!containsOption(optionsDb, option.getCode())
            && !containsOption(optionsToAdd, option.getCode())) {
          optionsToAdd.add(option);
        }
      }
    }
    // Options are never removed by an edit, so the partner keeps its existing ones plus the new
    // ones.
    if (optionsDb.isEmpty() && optionsToAdd.isEmpty()) {
      throw new BusinessException(ErrorCode.PARTNER_OPTION_REQUIRED);
    }
    AddressDto addressDb = partnerDb.getAddress();
    partner.getAddress().setId(addressDb.getId());
    partner.setAddress(addressRepository.update(partner.getAddress()));
    partner.setVersion((partnerDb.getVersion()));
    for (PartnerOptionDto option : optionsToAdd) {
      createOption(partner.getId(), option);
    }
    partner = partnerRepository.update(partner);
    return partner;
  }

  @Override
  public void addOption(int id, PartnerOptionDto partnerOption, int userId, String userRole) {
    PartnerDto partner = partnerRepository.findById(id);
    if (partner == null) {
      throw new ResourceNotFoundException();
    }
    if (!userRole.equals(UserDto.ROLE_PROFESSOR)
        && (partner.isOfficial() || !usesPartner(userId, id))) {
      throw new InsufficientPermissionException();
    }
    createOption(id, partnerOption);
  }

  /** Stores an option of a partner, after checking that the option exists. */
  private void createOption(int partnerId, PartnerOptionDto partnerOption) {
    if (optionRepository.findByCode(partnerOption.getCode()) == null) {
      throw new BusinessException(ErrorCode.UNKNOWN_OPTION, partnerOption.getCode());
    }
    partnerOptionRepository.create(partnerOption, partnerId);
  }

  /**
   * Tells whether the partner is the one of a mobility choice of the student, still open or already
   * a mobility (the partners table has no creator: a student reaches a non-official partner through
   * his own choices only).
   */
  private boolean usesPartner(int userId, int partnerId) {
    for (MobilityChoiceDto choice : mobilityChoiceRepository.findByUser(userId)) {
      if (choice.getPartner() != null && choice.getPartner().getId() == partnerId) {
        return true;
      }
    }
    for (MobilityDto mobility : mobilityRepository.findByUser(userId)) {
      if (mobility.getPartner() != null && mobility.getPartner().getId() == partnerId) {
        return true;
      }
    }
    return false;
  }

  @Override
  @Transactional(readOnly = true)
  public List<PartnerOptionDto> findAllPartnerOption(int partnerId) {
    if (partnerRepository.findById(partnerId) == null) {
      throw new ResourceNotFoundException();
    }
    return partnerOptionRepository.findAllOptionsByPartner(partnerId);
  }

  @Override
  public PartnerDto restore(int id, String role) {
    PartnerDto partner = null;
    if ((partner = partnerRepository.findById(id)) == null) {
      throw new ResourceNotFoundException();
    }
    if (!partner.isArchived()) {
      throw new BusinessException(ErrorCode.PARTNER_NOT_ARCHIVED);
    }
    if (role.equals(UserDto.ROLE_STUDENT) && !partner.isOfficial()) {
      throw new InsufficientPermissionException();
    }
    if (partnerOptionRepository.findAllOptionsByPartner(id).isEmpty()) {
      throw new BusinessException(ErrorCode.PARTNER_OPTION_REQUIRED);
    }
    partner.setArchived(false);
    partner.setArchivable(true);
    return partnerRepository.update(partner);
  }

  /** Tells whether a list of partner options already contains an option with the given code. */
  private static boolean containsOption(List<PartnerOptionDto> options, String code) {
    if (code == null) {
      return false;
    }
    for (PartnerOptionDto option : options) {
      if (code.equals(option.getCode())) {
        return true;
      }
    }
    return false;
  }
}
