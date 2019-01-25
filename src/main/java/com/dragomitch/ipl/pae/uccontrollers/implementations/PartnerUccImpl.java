package com.dragomitch.ipl.pae.uccontrollers.implementations;

import static com.dragomitch.ipl.pae.utils.DataValidationUtils.checkObject;
import static com.dragomitch.ipl.pae.utils.DataValidationUtils.checkPositive;
import static com.dragomitch.ipl.pae.utils.DataValidationUtils.checkString;
import static com.dragomitch.ipl.pae.utils.DataValidationUtils.isAValidEmail;
import static com.dragomitch.ipl.pae.utils.DataValidationUtils.isAValidString;
import static com.dragomitch.ipl.pae.utils.DataValidationUtils.isPositive;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.PartnerOptionDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.ErrorFormat;
import com.dragomitch.ipl.pae.business.exceptions.RessourceNotFoundException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import com.dragomitch.ipl.pae.annotations.Inject;
import com.dragomitch.ipl.pae.persistence.AddressDao;
import com.dragomitch.ipl.pae.persistence.DalServices;
import com.dragomitch.ipl.pae.persistence.MobilityChoiceDao;
import com.dragomitch.ipl.pae.persistence.OptionDao;
import com.dragomitch.ipl.pae.persistence.PartnerDao;
import com.dragomitch.ipl.pae.persistence.PartnerOptionDao;
import com.dragomitch.ipl.pae.persistence.ProgrammeDao;
import com.dragomitch.ipl.pae.persistence.UserDao;
import com.dragomitch.ipl.pae.presentation.annotations.HttpParameter;
import com.dragomitch.ipl.pae.presentation.annotations.PathParameter;
import com.dragomitch.ipl.pae.presentation.annotations.Role;
import com.dragomitch.ipl.pae.presentation.annotations.Route;
import com.dragomitch.ipl.pae.presentation.annotations.SessionParameter;
import com.dragomitch.ipl.pae.presentation.enums.HttpMethod;
import com.dragomitch.ipl.pae.presentation.exceptions.InsufficientPermissionException;
import com.dragomitch.ipl.pae.uccontrollers.PartnerUcc;
import com.dragomitch.ipl.pae.uccontrollers.SessionUcc;
import com.dragomitch.ipl.pae.uccontrollers.UnitOfWork;

class PartnerUccImpl implements PartnerUcc {

  private AddressDao addressDao;
  private OptionDao optionDao;
  private PartnerDao partnerDao;
  private PartnerOptionDao partnerOptionDao;
  private MobilityChoiceDao mobilityChoiceDao;
  private ProgrammeDao programmeDao;
  private UserDao userDao;
  private DalServices dalServices;
  private UnitOfWork unitOfWork;
  private EntityFactory entityFactory;

  @Inject
  public PartnerUccImpl(AddressDao addressDao, OptionDao optionDao, PartnerDao partnerDao,
      PartnerOptionDao partnerOptionDao, MobilityChoiceDao mobilityChoiceDao, ProgrammeDao programmeDao,
      UserDao userDao, DalServices dalServices, UnitOfWork unitOfWork, EntityFactory entityFactory) {
    this.addressDao = addressDao;
    this.optionDao = optionDao;
    this.partnerDao = partnerDao;
    this.partnerOptionDao = partnerOptionDao;
    this.programmeDao = programmeDao;
    this.userDao = userDao;
    this.dalServices = dalServices;
    this.mobilityChoiceDao = mobilityChoiceDao;
    this.unitOfWork = unitOfWork;
    this.entityFactory = entityFactory;
  }

  @Override
  @Role({UserDto.ROLE_STUDENT, UserDto.ROLE_PROFESSOR})
  @Route(method = HttpMethod.POST, template = "/partners")
  public PartnerDto create(@HttpParameter("data") PartnerDto partner, @SessionParameter("userRole") String userRole) {
    if ((userRole.equals(UserDto.ROLE_STUDENT)) && (partner.isOfficial() == true)) {
      throw new InsufficientPermissionException();
    }
    try {
      unitOfWork.startTransaction();
      partner.setAddress(addressDao.create(partner.getAddress()));
      checkDataIntegrity(partner);
      partner = partnerDao.create(partner);
      List<PartnerOptionDto> options = partner.getOptions();
      for (PartnerOptionDto partnerOption: options) {
        addOption(partner.getId(), partnerOption);
      }
      partner.setProgramme(partner.getAddress().getCountry().getProgramme());
      unitOfWork.commit();
      return partner;
    } catch (Exception ex) {
      unitOfWork.rollback();
      throw ex;
    }
  }

  @Override
  @Role({UserDto.ROLE_STUDENT, UserDto.ROLE_PROFESSOR})
  @Route(method = HttpMethod.GET, template = "/partners/{id}")
  public PartnerDto showOne(@PathParameter("id") int id) {
    checkPositive(id);
    unitOfWork.startTransaction();
    PartnerDto partner = partnerDao.findById(id);
    partner.setAddress(addressDao.findById(partner.getAddress().getId()));
    partner.setProgramme(programmeDao.findById(partner.getProgramme().getId()));
    partner.setOptions(partnerOptionDao.findAllOptionsByPartner(id));
    List<MobilityChoiceDto> mobilityChoices = mobilityChoiceDao.findByActivePartner(id);
    partner.setArchivable(mobilityChoices.size() > 0 ? false : true);
    unitOfWork.commit();
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
  @Role({UserDto.ROLE_PROFESSOR, UserDto.ROLE_STUDENT})
  @Route(method = HttpMethod.GET, template = "/partners")
  public Map<String, Object> showAll(@HttpParameter("filter") String filter, @HttpParameter("value") String value,
                                     @SessionParameter(SessionUcc.USER_ROLE) String userRole, @SessionParameter(SessionUcc.USER_ID) int userId) {
    checkFilter(filter);
    String filterToUse = filter;
    if (filter == null) {
      filterToUse = PartnerDao.FILTER_ALL_PARTNERS;
    } else { // TODO check if this else is really doing something
      checkString(value);
    }
    try {
      unitOfWork.startTransaction();
      Map<String, Object> map = new HashMap<>();
      String option;
      UserDto user;
      if ((user = userDao.findById(userId)) == null) {
        throw new RessourceNotFoundException();
      } else {
        option = user.getOption().getCode();
      }
      map.put("data", partnerDao.findAll(filterToUse, value, userRole, option));
      unitOfWork.commit();
      return map;
    } catch (Exception ex) {
      unitOfWork.rollback();
      throw ex;
    }
  }

  @Override
  @Role({UserDto.ROLE_PROFESSOR})
  @Route(method = HttpMethod.PUT, template = "/partners/{id}")
  public PartnerDto edit(@PathParameter("id") int id, @HttpParameter("data") PartnerDto partner,
      @SessionParameter("userRole") String userRole) {
    checkPositive(id);
    checkObject(partner);
    if (userRole.equals(UserDto.ROLE_STUDENT)) {
      throw new InsufficientPermissionException();
    }
    try {
      unitOfWork.startTransaction();
      if (partnerDao.findById(id) == null) {
        throw new RessourceNotFoundException();
      }
      partner.setId(id);
      if (partner.isArchived()) {
        if (!mobilityChoiceDao.findByPartner(id).isEmpty()) {
          throw new BusinessException(ErrorFormat.EXISTENCE_VIOLATION_ARCHIVING_710);
        }
      }
      partner.setAddress(addressDao.update(partner.getAddress()));
      PartnerDto partnerDb = partnerDao.findById(partner.getId());
      partner.setVersion((partnerDb.getVersion()));
      List<PartnerOptionDto> optionsDb = partnerOptionDao.findAllOptionsByPartner(partnerDb.getId());
      List<PartnerOptionDto> options = partner.getOptions();

      for (int i = 0; i < options.size(); i++) {
        for (int j = 0; j < optionsDb.size(); j++) {
          if (options.get(i).getCode().equals((optionsDb).get(j).getCode())) {
            break;
          } else {
            addOption(partner.getId(), options.get(i));
          }
        }
      }
      partner = partnerDao.update(partner);
      unitOfWork.commit();
      return partner;
    } catch (Exception ex) {
      unitOfWork.rollback();
      throw ex;
    }
  }

  @Override
  @Role({UserDto.ROLE_PROFESSOR, UserDto.ROLE_STUDENT})
  @Route(method = HttpMethod.POST, template = "/partners/{id}")
  public void addOption(@PathParameter("id") int id, @HttpParameter("data") PartnerOptionDto partnerOption) {
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
  @Role({UserDto.ROLE_PROFESSOR, UserDto.ROLE_STUDENT})
  @Route(method = HttpMethod.GET, template = "/partners/partnersOptions/{id}")
  public List<PartnerOptionDto> findAllPartnerOption(@PathParameter("id") int partnerId) {
    checkPositive(partnerId);
    dalServices.openConnection();
    if (partnerDao.findById(partnerId) == null) {
      dalServices.closeConnection();
      throw new RessourceNotFoundException();
    }
    List<PartnerOptionDto> partnerOptions = partnerOptionDao.findAllOptionsByPartner(partnerId);
    dalServices.closeConnection();
    return partnerOptions;
  }

  @Override
  @Role({UserDto.ROLE_PROFESSOR, UserDto.ROLE_STUDENT})
  @Route(method = HttpMethod.PUT, template = "/partners/{id}/restore")
  public PartnerDto restore(@PathParameter("id") int id, @SessionParameter("role") String role) {
    checkPositive(id);
    PartnerDto partner = null;
    try {
      unitOfWork.startTransaction();
      if ((partner = partnerDao.findById(id)) == null) {
        throw new RessourceNotFoundException();
      }
      if (!partner.isArchived()) {
        throw new BusinessException(3);// "Partner is not archived");
      }
      if (role.equals(UserDto.ROLE_STUDENT) && !partner.isOfficial()) {
        throw new InsufficientPermissionException();
      }
      partner.setArchived(false);
      partner.setArchivable(true);
      partner = partnerDao.update(partner);
      unitOfWork.commit();
    } catch (Exception ex) {
      unitOfWork.rollback();
      throw ex;
    }
    return partner;
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
    /*if (!isAValidString(partner.getWebsite())) {
      violations.add(ErrorFormat.INVALID_WEBSITE_707);
    }*///TODO transform this validation to not be mandatory
    if (!isAValidString(partner.getPhoneNumber())) {
      violations.add(ErrorFormat.INVALID_PHONE_NUMBER_708);
    }//TODO In the test Scenario there is a partner without phone number, is that the correct comportment ?
    if (partner.getOptions().size() < 1) {
      violations.add(ErrorFormat.EXISTENCE_VIOLATION_OPTION_NULL_141);
    }
    if (violations.size() != 0) {
      throw new BusinessException(ErrorFormat.INVALID_INPUT_DATA_110, violations);
    }
  }
}
