package com.dragomitch.ipl.pae.application;

import com.dragomitch.ipl.pae.domain.exceptions.BusinessException;
import com.dragomitch.ipl.pae.domain.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.domain.exceptions.InsufficientPermissionException;
import com.dragomitch.ipl.pae.domain.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.domain.model.Address;
import com.dragomitch.ipl.pae.domain.model.Mobility;
import com.dragomitch.ipl.pae.domain.model.MobilityChoice;
import com.dragomitch.ipl.pae.domain.model.Partner;
import com.dragomitch.ipl.pae.domain.model.PartnerFilter;
import com.dragomitch.ipl.pae.domain.model.PartnerOption;
import com.dragomitch.ipl.pae.domain.model.Role;
import com.dragomitch.ipl.pae.domain.model.User;
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
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Partners. Their format is checked by the constraints of {@link Partner} (at creation, at least
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
  public Partner create(Partner partner, Role userRole) {
    if ((userRole == Role.STUDENT) && (partner.isOfficial() == true)) {
      throw new InsufficientPermissionException();
    }
    partner.setAddress(addressRepository.create(partner.getAddress()));
    partner = partnerRepository.create(partner);
    List<PartnerOption> options = partner.getOptions();
    for (PartnerOption partnerOption : options) {
      createOption(partner.getId(), partnerOption);
    }
    partner.setProgramme(partner.getAddress().getCountry().getProgramme());
    return partner;
  }

  @Override
  @Transactional(readOnly = true)
  public Partner showOne(int id) {
    Partner partner = partnerRepository.findById(id);
    if (partner == null) {
      throw new ResourceNotFoundException();
    }
    partner.setAddress(addressRepository.findById(partner.getAddress().getId()));
    partner.setProgramme(programmeRepository.findById(partner.getProgramme().getId()));
    partner.setOptions(partnerOptionRepository.findAllOptionsByPartner(id));
    List<MobilityChoice> mobilityChoices = mobilityChoiceRepository.findByActivePartner(id);
    partner.setArchivable(mobilityChoices.isEmpty());
    return partner;
  }

  @Override
  @Transactional(readOnly = true)
  public List<Partner> showAll(
      PartnerFilter filter, @Nullable String value, Role userRole, int userId) {
    String option;
    User user;
    if ((user = userRepository.findById(userId)) == null) {
      throw new ResourceNotFoundException();
    } else {
      option = user.getOption().getCode();
    }
    return partnerRepository.findAll(filter, value, userRole, option);
  }

  @Override
  public Partner edit(int id, Partner partner, Role userRole) {
    if (userRole == Role.STUDENT) {
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
    Partner partnerDb = partnerRepository.findById(partner.getId());
    List<PartnerOption> optionsDb =
        partnerOptionRepository.findAllOptionsByPartner(partnerDb.getId());
    List<PartnerOption> optionsToAdd = new LinkedList<PartnerOption>();
    if (partner.getOptions() != null) {
      for (PartnerOption option : partner.getOptions()) {
        // the options are never null here (List<@NotNull @Valid PartnerOption>)
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
    Address addressDb = partnerDb.getAddress();
    partner.getAddress().setId(addressDb.getId());
    partner.setAddress(addressRepository.update(partner.getAddress()));
    partner.setVersion((partnerDb.getVersion()));
    for (PartnerOption option : optionsToAdd) {
      createOption(partner.getId(), option);
    }
    partner = partnerRepository.update(partner);
    return partner;
  }

  @Override
  public void addOption(int id, PartnerOption partnerOption, int userId, Role userRole) {
    Partner partner = partnerRepository.findById(id);
    if (partner == null) {
      throw new ResourceNotFoundException();
    }
    if (userRole != Role.PROFESSOR && (partner.isOfficial() || !usesPartner(userId, id))) {
      throw new InsufficientPermissionException();
    }
    createOption(id, partnerOption);
  }

  /** Stores an option of a partner, after checking that the option exists. */
  private void createOption(int partnerId, PartnerOption partnerOption) {
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
    for (MobilityChoice choice : mobilityChoiceRepository.findByUser(userId)) {
      if (choice.getPartner() != null && choice.getPartner().getId() == partnerId) {
        return true;
      }
    }
    for (Mobility mobility : mobilityRepository.findByUser(userId)) {
      if (mobility.getPartner() != null && mobility.getPartner().getId() == partnerId) {
        return true;
      }
    }
    return false;
  }

  @Override
  @Transactional(readOnly = true)
  public List<PartnerOption> findAllPartnerOption(int partnerId) {
    if (partnerRepository.findById(partnerId) == null) {
      throw new ResourceNotFoundException();
    }
    return partnerOptionRepository.findAllOptionsByPartner(partnerId);
  }

  @Override
  public Partner restore(int id, Role role) {
    Partner partner = null;
    if ((partner = partnerRepository.findById(id)) == null) {
      throw new ResourceNotFoundException();
    }
    if (!partner.isArchived()) {
      throw new BusinessException(ErrorCode.PARTNER_NOT_ARCHIVED);
    }
    if (role == Role.STUDENT && !partner.isOfficial()) {
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
  private static boolean containsOption(List<PartnerOption> options, String code) {
    if (code == null) {
      return false;
    }
    for (PartnerOption option : options) {
      if (code.equals(option.getCode())) {
        return true;
      }
    }
    return false;
  }
}
