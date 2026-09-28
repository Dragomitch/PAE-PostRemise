package com.dragomitch.ipl.pae.uccontrollers.implementations;

import static com.dragomitch.ipl.pae.utils.DataValidationUtils.checkObject;
import static com.dragomitch.ipl.pae.utils.DataValidationUtils.checkPositive;
import static com.dragomitch.ipl.pae.utils.DataValidationUtils.checkString;
import static com.dragomitch.ipl.pae.utils.DataValidationUtils.isAValidEmail;
import static com.dragomitch.ipl.pae.utils.DataValidationUtils.isAValidString;
import static com.dragomitch.ipl.pae.utils.DataValidationUtils.isPositive;

import com.dragomitch.ipl.pae.business.dto.*;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.ErrorFormat;
import com.dragomitch.ipl.pae.business.exceptions.RessourceNotFoundException;
import java.util.LinkedList;
import java.util.List;
import com.dragomitch.ipl.pae.persistence.AddressDao;
import com.dragomitch.ipl.pae.persistence.MobilityChoiceDao;
import com.dragomitch.ipl.pae.persistence.OptionDao;
import com.dragomitch.ipl.pae.persistence.PartnerDao;
import com.dragomitch.ipl.pae.persistence.PartnerOptionDao;
import com.dragomitch.ipl.pae.persistence.ProgrammeDao;
import com.dragomitch.ipl.pae.persistence.UserDao;
import com.dragomitch.ipl.pae.business.exceptions.InsufficientPermissionException;
import com.dragomitch.ipl.pae.uccontrollers.PartnerUcc;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    checkDataIntegrity(partner);
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
    checkPositive(id);
    PartnerDto partner = partnerDao.findById(id);
    if (partner == null) {
      throw new RessourceNotFoundException();
    }
    partner.setAddress(addressDao.findById(partner.getAddress().getId()));
    partner.setProgramme(programmeDao.findById(partner.getProgramme().getId()));
    partner.setOptions(partnerOptionDao.findAllOptionsByPartner(id));
    List<MobilityChoiceDto> mobilityChoices = mobilityChoiceDao.findByActivePartner(id);
    partner.setArchivable(mobilityChoices.isEmpty());
    return partner;
  }

  /**
   * Check if a filter for the mobility choices is correct or not. If a filter isn't correct it throw the appropriate business exception.
   *
   * @param filter the filter to test.
   */
  private void checkFilter(String filter) {
    if (filter != null && !filter.equals(PartnerDao.FILTER_ARCHIVED_PARTNERS)
        && !filter.equals(PartnerDao.FILTER_COUNTRY)) {
      throw new BusinessException(ErrorFormat.INVALID_PARTNER_FILTER_709);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<PartnerDto> showAll(String filter, String value, String userRole, int userId) {
    checkFilter(filter);
    String filterToUse = filter;
    if (filter == null) {
      filterToUse = PartnerDao.FILTER_ALL_PARTNERS;
    } else { // TODO check if this else is really doing something
      checkString(value);
    }
    String option;
    UserDto user;
    if ((user = userDao.findById(userId)) == null) {
      throw new RessourceNotFoundException();
    } else {
      option = user.getOption().getCode();
    }
    return partnerDao.findAll(filterToUse, value, userRole, option);
  }

  @Override
  public PartnerDto edit(int id, PartnerDto partner, String userRole) {
    checkPositive(id);
    checkObject(partner);
    if (userRole.equals(UserDto.ROLE_STUDENT)) {
      throw new InsufficientPermissionException();
    }
    if (partnerDao.findById(id) == null) {
      throw new RessourceNotFoundException();
    }
    partner.setId(id);
    if (partner.isArchived()) {
      if (!mobilityChoiceDao.findByPartner(id).isEmpty()) {
        throw new BusinessException(ErrorFormat.EXISTENCE_VIOLATION_ARCHIVING_710);
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
      throw new BusinessException(ErrorFormat.PARTNER_OPTION_REQUIRED_712);
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
    checkPositive(id);
    checkObject(partnerOption);
    checkString(partnerOption.getCode());
    checkString(partnerOption.getDepartement());
    if (optionDao.findByCode(partnerOption.getCode()) == null) {
      throw new RessourceNotFoundException();
    }
    partnerOptionDao.create(partnerOption, id);
  }

  @Override
  @Transactional(readOnly = true)
  public List<PartnerOptionDto> findAllPartnerOption(int partnerId) {
    checkPositive(partnerId);
    if (partnerDao.findById(partnerId) == null) {
      throw new RessourceNotFoundException();
    }
    return partnerOptionDao.findAllOptionsByPartner(partnerId);
  }

  @Override
  public PartnerDto restore(int id, String role) {
    checkPositive(id);
    PartnerDto partner = null;
    if ((partner = partnerDao.findById(id)) == null) {
      throw new RessourceNotFoundException();
    }
    if (!partner.isArchived()) {
      throw new BusinessException(ErrorFormat.PARTNER_NOT_ARCHIVED_711);
    }
    if (role.equals(UserDto.ROLE_STUDENT) && !partner.isOfficial()) {
      throw new InsufficientPermissionException();
    }
    if (partnerOptionDao.findAllOptionsByPartner(id).isEmpty()) {
      throw new BusinessException(ErrorFormat.PARTNER_OPTION_REQUIRED_712);
    }
    partner.setArchived(false);
    partner.setArchivable(true);
    return partnerDao.update(partner);
  }

  private void checkDataIntegrity(PartnerDto partner) {
    List<Integer> violations = new LinkedList<Integer>();
    if (!isAValidString(partner.getLegalName())) {
      violations.add(ErrorFormat.INVALID_LEGAL_NAME_701);
    }
    if (!isAValidString(partner.getBusinessName())) {
      violations.add(ErrorFormat.INVALID_BUSINESS_NAME_702);
    }
    if (!isAValidString(partner.getFullName())) {
      violations.add(ErrorFormat.INVALID_FULL_NAME_703);
    }
    if (!isAValidString(partner.getOrganisationType())) {
      violations.add(ErrorFormat.INVALID_ORGANISATION_TYPE_704);
    }
    if (!isPositive(partner.getEmployeeCount())) {
      violations.add(ErrorFormat.INVALID_EMPLOYEE_COUNT_705);
    }
    if (!isPositive(partner.getAddress().getId())) {
      violations.add(ErrorFormat.EXISTENCE_VIOLATION_ADDRESS_ID_800);
    }
    if (!isAValidEmail(partner.getEmail())) {
      violations.add(ErrorFormat.INVALID_EMAIL_706);
    }
    if (!isAValidString(partner.getPhoneNumber())) {
      violations.add(ErrorFormat.INVALID_PHONE_NUMBER_708);
    }//TODO In the test Scenario there is a partner without phone number, is that the correct comportment ?
    if (partner.getOptions() == null || partner.getOptions().isEmpty()) {
      violations.add(ErrorFormat.PARTNER_OPTION_REQUIRED_712);
    }
    if (violations.size() != 0) {
      throw new BusinessException(ErrorFormat.INVALID_INPUT_DATA_110, violations);
    }
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
