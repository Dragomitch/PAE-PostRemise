package ucc.implementations;

import static utils.DataValidationUtils.checkObject;
import static utils.DataValidationUtils.checkPositive;
import static utils.DataValidationUtils.checkString;
import static utils.DataValidationUtils.isAValidObject;
import static utils.DataValidationUtils.isAValidString;
import static utils.DataValidationUtils.isPositive;

import business.EntityFactory;
import business.Mobility;
import business.MobilityChoice;
import business.dto.DenialReasonDto;
import business.dto.DocumentDto;
import business.dto.MobilityChoiceDto;
import business.dto.MobilityDto;
import business.dto.PartnerDto;
import business.dto.UserDto;
import business.exceptions.BusinessException;
import business.exceptions.ErrorFormat;
import business.exceptions.RessourceNotFoundException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import main.annotations.Inject;
import persistence.CountryDao;
import persistence.DenialReasonDao;
import persistence.DocumentDao;
import persistence.MobilityChoiceDao;
import persistence.MobilityDao;
import persistence.MobilityDocumentDao;
import persistence.ProgrammeDao;
import persistence.UserDao;
import presentation.CsvStringBuilder;
import presentation.annotations.HttpParameter;
import presentation.annotations.PathParameter;
import presentation.annotations.Role;
import presentation.annotations.Route;
import presentation.annotations.SessionParameter;
import presentation.enums.HttpMethod;
import presentation.exceptions.InsufficientPermissionException;
import ucc.MobilityChoiceUcc;
import ucc.PartnerUcc;
import ucc.SessionUcc;
import ucc.UnitOfWork;

class MobilityChoiceUccImpl implements MobilityChoiceUcc {

  private UserDao userDao;
  private MobilityChoiceDao mobilityChoiceDao;
  private MobilityDao mobilityDao;
  private DenialReasonDao denialReasonDao;
  private DocumentDao documentDao;
  private MobilityDocumentDao mobilityDocumentDao;
  private EntityFactory entityFactory;
  private CountryDao countryDao;
  private ProgrammeDao programmeDao;
  private PartnerUcc partnerUcc;
  private UnitOfWork unitOfWork;

  @Inject
  public MobilityChoiceUccImpl(UserDao userDao, MobilityChoiceDao mobilityChoiceDao, MobilityDao mobilityDao,
      DenialReasonDao denialReasonDao, DocumentDao documentDao, MobilityDocumentDao mobilityDocumentDao,
      EntityFactory entityFactory, CountryDao countryDao, ProgrammeDao programmeDao, PartnerUcc partnerUcc,
      UnitOfWork unitOfWork) {
    this.userDao = userDao;
    this.mobilityChoiceDao = mobilityChoiceDao;
    this.mobilityDao = mobilityDao;
    this.denialReasonDao = denialReasonDao;
    this.documentDao = documentDao;
    this.mobilityDocumentDao = mobilityDocumentDao;
    this.entityFactory = entityFactory;
    this.countryDao = countryDao;
    this.programmeDao = programmeDao;
    this.partnerUcc = partnerUcc;
    this.unitOfWork = unitOfWork;
  }

  @Override
  @Role({UserDto.ROLE_STUDENT, UserDto.ROLE_PROFESSOR})
  @Route(method = HttpMethod.POST, template = "/mobilityChoice")
  public MobilityChoiceDto create(@HttpParameter("data") MobilityChoiceDto mobilityChoice,
      @SessionParameter(SessionUcc.USER_ID) int userId, @SessionParameter(SessionUcc.USER_ROLE) String userRole) {
    try {
      unitOfWork.startTransaction();
      checkDataIntegrity(mobilityChoice);
      if (userRole.equals(UserDto.ROLE_STUDENT) && mobilityChoice.getUser().getId() != userId) {
        throw new InsufficientPermissionException();
      }
      if (userRole.equals(UserDto.ROLE_PROFESSOR) && mobilityChoice.getUser().getId() == userId) {
        throw new BusinessException(ErrorFormat.INVALID_PROFESSOR_MOBILITY_CHOICE_CREATION_322);
      }
      UserDto userDto;
      if ((userDto = userDao.findById(mobilityChoice.getUser().getId())) == null) {
        throw new BusinessException(ErrorFormat.EXISTENCE_VIOLATION_USER_ID_200);
      }
      mobilityChoice.setUser(userDto);
      MobilityChoiceDto mobilityChoiceCreated = mobilityChoiceDao.create(mobilityChoice);
      unitOfWork.commit();
      return mobilityChoiceCreated;
    } catch (Exception ex) {
      unitOfWork.rollback();
      throw ex;
    }
  }

  @Override
  @Role({UserDto.ROLE_STUDENT, UserDto.ROLE_PROFESSOR})
  @Route(method = HttpMethod.GET, template = "/mobilityChoices")
  public Map<String, Object> showAll(@SessionParameter(SessionUcc.USER_ID) int userId,
      @SessionParameter(SessionUcc.USER_ROLE) String userRole, @HttpParameter("filter") String filter) {
    checkFilter(filter);
    String filterToUse = filter == null ? MobilityChoiceDao.FILTER_ACTIVE_MOBILITIES_CHOICES : filter;
    Map<String, Object> dataToReturn = new HashMap<String, Object>();
    List<MobilityChoiceDto> mobilityChoices;
    try {
      unitOfWork.startTransaction();
      if (userRole.equals(UserDto.ROLE_PROFESSOR)) {
        // Lists all mobility choices in function of filter
        mobilityChoices = mobilityChoiceDao.findAll(filterToUse);
      } else {
        // Lists mobility choices owned by the requester
        mobilityChoices = getMobilityChoiceForUser(userId, userId, userRole);
      }
      dataToReturn.put("data", mobilityChoices);
      unitOfWork.commit();
      return dataToReturn;
    } catch (Exception ex) {
      unitOfWork.rollback();
      throw ex;
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  @Role({UserDto.ROLE_STUDENT, UserDto.ROLE_PROFESSOR})
  @Route(method = HttpMethod.GET, template = "/mobilityChoices/count")
  public Map<String, Object> countAll(@SessionParameter(SessionUcc.USER_ID) int userId,
      @SessionParameter(SessionUcc.USER_ROLE) String userRole, @HttpParameter("filter") String filter) {
    Map<String, Object> mobilityChoices = showAll(userId, userRole, filter);
    List<MobilityChoiceDto> mobilityChoicesList = (List<MobilityChoiceDto>) mobilityChoices.get("data");
    mobilityChoices.put("count", mobilityChoicesList.size());
    mobilityChoices.remove("data");
    return mobilityChoices;
  }

  @Override
  @Role({UserDto.ROLE_STUDENT})
  @Route(method = HttpMethod.PUT, template = "/mobilityChoices/{id}/cancel")
  public void cancel(@PathParameter("id") int mobilityChoiceId, @SessionParameter("userId") int userId,
      @HttpParameter("reason") String reason) {
    checkPositive(mobilityChoiceId);
    checkString(reason);
    try {
      unitOfWork.startTransaction();
      MobilityChoice mobilityChoice = (MobilityChoice) mobilityChoiceDao.findById(mobilityChoiceId);
      if (mobilityChoice == null) {
        throw new RessourceNotFoundException("Ressource mobilityChoice not rightly loaded");
      }
      if (userId != mobilityChoice.getUser().getId()) {
        throw new InsufficientPermissionException(
            "User " + userId + " cannot cancel a mobility choice he does not own");
      }
      if (mobilityChoice.getCancellationReason() != null || mobilityChoice.getDenialReason() != null) {
        throw new BusinessException(ErrorFormat.INVALID_STATE_MOBILITY_CHOICE_317);
      }
      MobilityDto mobility = mobilityDao.findById(mobilityChoiceId);
      if (mobility != null) {
        throw new BusinessException(ErrorFormat.MOBILITY_CHOICE_ALREADY_CONFIRMED_301);
      }
      mobilityChoice.setCancellationReason(reason);
      mobilityChoiceDao.update(mobilityChoice);
      unitOfWork.commit();
    } catch (Exception ex) {
      unitOfWork.rollback();
      throw ex;
    }
  }

  @Override
  @Role({UserDto.ROLE_PROFESSOR})
  @Route(method = HttpMethod.PUT, template = "/mobilitychoices/{id}/reject")
  public void reject(@PathParameter("id") int id, @HttpParameter("reason") int reason) {
    checkPositive(id);
    checkPositive(reason);
    try {
      unitOfWork.startTransaction();
      MobilityChoice mobilityChoice = (MobilityChoice) mobilityChoiceDao.findById(id);
      if (mobilityChoice == null) {
        throw new RessourceNotFoundException();
      }
      if (mobilityChoice.getCancellationReason() != null || (mobilityChoice.getDenialReason() != null
          && mobilityChoice.getDenialReason().getReason() != null)) {
        throw new BusinessException(ErrorFormat.INVALID_STATE_MOBILITY_CHOICE_317);
      }
      DenialReasonDto denialReason = denialReasonDao.findById(reason);
      if (denialReason == null) {
        throw new BusinessException(ErrorFormat.EXISTENCE_VIOLATION_DENIAL_REASON_NULL_134);
      }
      MobilityDto mobility = mobilityDao.findById(id);
      if (mobility != null) {
        throw new BusinessException(ErrorFormat.MOBILITY_CHOICE_ALREADY_CONFIRMED_301);
      }
      mobilityChoice.setDenialReason(denialReason);
      unitOfWork.update(mobilityChoice);
      unitOfWork.commit();
    } catch (Exception ex) {
      unitOfWork.rollback();
      throw ex;
    }
  }

  @Override
  @Role({UserDto.ROLE_PROFESSOR})
  @Route(method = HttpMethod.PUT, template = "/mobilitychoices/{id}/confirm")
  @SuppressWarnings("unused")
  public void confirm(@PathParameter("id") int id, @SessionParameter("userId") int userId) {
    checkPositive(id);
    try {
      unitOfWork.startTransaction();
      MobilityChoiceDto mobilityChoice = mobilityChoiceDao.findById(id);
      if (mobilityChoice == null) {
        throw new RessourceNotFoundException();
      }
      if (mobilityChoice.getCancellationReason() != null || mobilityChoice.getDenialReason() != null) {
        throw new BusinessException(ErrorFormat.INVALID_STATE_MOBILITY_CHOICE_317);
      }
      if (mobilityDao.findById(id) != null) {
        throw new BusinessException(ErrorFormat.MOBILITY_CHOICE_ALREADY_CONFIRMED_321);
      }
      if (!isAValidObject(mobilityChoice.getPartner())) {
        throw new BusinessException(ErrorFormat.CONFIRM_WITHOUT_PARTNER_324);
      }
      MobilityDto mobility = (MobilityDto) entityFactory.build(MobilityDto.class);
      mobility.setId(id);
      mobility.setState(Mobility.STATE_CREATED);
      mobility.setSubmissionDate(LocalDateTime.now());

      UserDto user = userDao.findById(userId);
      mobility.setProfessorInCharge(userDao.findById(userId));
      mobilityDao.create(mobility);
      List<DocumentDto> documents = documentDao.findAllByProgramme(mobilityChoice.getProgramme().getId());
      for (DocumentDto document : documents) {
        mobilityDocumentDao.create(document.getId(), id);
      }
      List<MobilityChoiceDto> mobilityChoices = getMobilityChoiceForUser(mobilityChoice.getUser().getId(), userId,
          UserDto.ROLE_PROFESSOR);
      for (MobilityChoiceDto choice : mobilityChoices) {
        if (choice.getId() != mobilityChoice.getId()
            && choice.getAcademicYear() == mobilityChoice.getAcademicYear()
            && choice.getTerm() == mobilityChoice.getTerm()) {
          reject(choice.getId(), 1);
        }
      }
      unitOfWork.commit();
    } catch (Exception ex) {
      unitOfWork.rollback();
      throw ex;
    }
  }

  @Override
  @Role({UserDto.ROLE_STUDENT, UserDto.ROLE_PROFESSOR})
  @Route(method = HttpMethod.PUT, template = "/mobilitychoices/{id}/confirmWithNewPartner")
  public void confirmWithNewPartner(@PathParameter("id") int id, @HttpParameter("data") PartnerDto partner,
      @SessionParameter("userId") int userId, @SessionParameter("userRole") String userRole) {
    checkPositive(id);
    checkObject(partner);
    try {
      unitOfWork.startTransaction();
      MobilityChoiceDto mobilityChoice = mobilityChoiceDao.findById(id);
      if (mobilityChoice == null) {
        throw new RessourceNotFoundException();
      }
      if (mobilityChoice.getCancellationReason() != null || mobilityChoice.getDenialReason() != null) {
        throw new BusinessException(ErrorFormat.INVALID_STATE_MOBILITY_CHOICE_317);
      }
      if (userRole.equals(UserDto.ROLE_STUDENT) && userId != mobilityChoice.getUser().getId()) {
        throw new InsufficientPermissionException();
      }
      if (userRole.equals(UserDto.ROLE_STUDENT) && (partner.isOfficial())) {
        throw new InsufficientPermissionException("Actual user cannot create an offifcial partner");
      }
      if (mobilityDao.findById(id) != null) {
        throw new BusinessException(ErrorFormat.MOBILITY_CHOICE_ALREADY_CONFIRMED_321);
      }
      mobilityChoice.setCountry(partner.getAddress().getCountry());
      if (!mobilityChoice.getCountry().getCountryCode()
          .equals(partner.getAddress().getCountry().getCountryCode())) {
        throw new BusinessException(ErrorFormat.COUNTRY_CHANGE_NOT_ALLOWED_320);
      }
      if (!isPositive(partner.getId())) {
        partner = partnerUcc.create(partner, userRole);
      } else {
        partner = partnerUcc.restore(partner.getId(), userRole);
      }
      MobilityDto mobility = (MobilityDto) entityFactory.build(MobilityDto.class);
      mobility.setId(id);
      mobility.setState(Mobility.STATE_CREATED);
      mobility.setSubmissionDate(LocalDateTime.now());
      mobilityChoice.setPartner(partner);
      mobilityChoiceDao.update(mobilityChoice);
      mobilityDao.create(mobility);
      List<DocumentDto> documents = documentDao.findAllByProgramme(mobilityChoice.getProgramme().getId());
      for (DocumentDto document : documents) {
        mobilityDocumentDao.create(document.getId(), id);
      }
      List<MobilityChoiceDto> mobilityChoices = getMobilityChoiceForUser(mobilityChoice.getUser().getId(), userId, userRole);
      for (MobilityChoiceDto choice : mobilityChoices) {
        if (choice.getId() != mobilityChoice.getId()
            && choice.getAcademicYear() == mobilityChoice.getAcademicYear()
            && choice.getTerm() == mobilityChoice.getTerm() && choice.getCancellationReason() == null
            && (choice.getDenialReason() == null || choice.getDenialReason().getReason() == null)) {
          reject(choice.getId(), 1);
        }
      }
      unitOfWork.commit();
    } catch (Exception ex) {
      unitOfWork.rollback();
      throw ex;
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  @Role(UserDto.ROLE_PROFESSOR)
  @Route(method = HttpMethod.GET, template = "/mobilitychoices/export", contentType = "text/csv")
  public String exportAll(@SessionParameter(SessionUcc.USER_ID) int userId,
      @SessionParameter(SessionUcc.USER_ROLE) String userRole, @HttpParameter("filter") String filter) {
    try {
      unitOfWork.startTransaction();
      List<MobilityChoiceDto> mobilityChoices = (List<MobilityChoiceDto>) showAll(userId, userRole, filter)
          .get("data");
      CsvStringBuilder csvStringBuilder = new CsvStringBuilder(';');
      String[] headerCsv = {"N° ordre candidature", "Nom", "Prénom", "Option", "N° ordre préférence",
          "Programme de mobilité", "Type de mobilité", "Semestre de départ", "Partenaire"};
      csvStringBuilder.writeLine(headerCsv);
      for (MobilityChoiceDto mobiChoice : mobilityChoices) {
        csvStringBuilder.writeLine(transformToStringTable(mobiChoice));
      }
      unitOfWork.commit();
      return csvStringBuilder.close();
    } catch (Exception ex) {
      unitOfWork.rollback();
      throw ex;
    }
  }

  /**
   * Returns the mobilityChoices for a specified userId
   *
   * @param userId the userId of the requester
   * @param onUserId the userId of the user we wants all the mobility choices.
   * @param userRole the UserRole of the requester
   * @return the list of mobility choices for the userId specified.
   */
  private List<MobilityChoiceDto> getMobilityChoiceForUser(int userId, int onUserId, String userRole) {
    checkPositive(onUserId);
    checkPositive(userId);
    checkString(userRole);
    if (userRole.equals(UserDto.ROLE_STUDENT) && onUserId != userId) {
      throw new InsufficientPermissionException("Students can't check the mobilities for another user!");
    }
    List<MobilityChoiceDto> mobilityChoices = null;
    try {
      unitOfWork.startTransaction();
      if ((userDao.findById(onUserId)) == null) {
        throw new BusinessException(ErrorFormat.EXISTENCE_VIOLATION_USER_ID_200);
      }
      mobilityChoices = mobilityChoiceDao.findByUser(userId);
      unitOfWork.commit();
      return mobilityChoices;
    } catch (Exception ex) {
      unitOfWork.rollback();
      throw ex;
    }
  }

  /**
   * Check if a filter for the mobility choices is correct or not. If a filter isn't correct it throw the appropriate business exception.
   *
   * @param filter the filter to test.
   */
  private void checkFilter(String filter) {
    if (filter != null && !filter.equals(MobilityChoiceDao.FILTER_ACTIVE_MOBILITIES_CHOICES)
        && !filter.equals(MobilityChoiceDao.FILTER_ALL_MOBILITIES_CHOICES)
        && !filter.equals(MobilityChoiceDao.FILTER_CANCELED_MOBILITIES_CHOICES)
        && !filter.equals(MobilityChoiceDao.FILTER_PASSED_MOBILITIES_CHOICES)
        && !filter.equals(MobilityChoiceDao.FILTER_REJECTED_MOBILITIES_CHOICES)) {
      throw new BusinessException(ErrorFormat.INVALID_MOBILITY_CHOICE_FILTER_323);
    }
  }

  /**
   * Construct a table of strings to represent the mobilityChoiceDto to serialize.
   *
   * @param mobilityChoice the mobility choice to serialize
   * @return a table of strings serializable.
   */
  private String[] transformToStringTable(MobilityChoiceDto mobilityChoice) {
    String[] entries = new String[9];
    UserDto user = mobilityChoice.getUser();
    entries[0] = mobilityChoice.getId() + "";
    entries[1] = user.getLastName();
    entries[2] = user.getFirstName();
    entries[3] = user.getOption().getName();
    entries[4] = mobilityChoice.getPreferenceOrder() + "";
    entries[5] = mobilityChoice.getProgramme().getProgrammeName();
    entries[6] = mobilityChoice.getMobilityType();
    entries[7] = mobilityChoice.getTerm() + "";
    if (mobilityChoice.getPartner() != null) {
      entries[8] = mobilityChoice.getPartner().getFullName();
    }
    return entries;
  }

  @Override
  public boolean findByPartner(int partnerId) {
    List<MobilityChoiceDto> mobilityChoices = mobilityChoiceDao.findByPartner(partnerId);
    return mobilityChoices.isEmpty();
  }

  /**
   * Verify the validity of the information received.
   *
   * @param mobilityChoice the MobilityChoiceDto we verify.
   */
  private void checkDataIntegrity(MobilityChoiceDto mobilityChoice) {
    List<Integer> violations = new LinkedList<Integer>();
    if (mobilityChoice == null) {
      throw new BusinessException(ErrorFormat.EXISTENCE_VIOLATION_MOBILITY_CHOICE_NULL_133);
    }
    try {
      ((MobilityChoice) mobilityChoice).checkDataIntegrity();
    } catch (BusinessException ex) {
      List<ErrorFormat> errors = ex.getError().getDetails();
      for (ErrorFormat oneError : errors) {
        System.out.println("error : " + oneError.getErrorCode() + " " + oneError.getDeveloperMessage());
        violations.add(oneError.getErrorCode());
      }
    }
    if (isAValidString(mobilityChoice.getMobilityType())
        && mobilityChoice.getMobilityType().length() > MobilityChoiceDao.MAX_LENGTH_MOBILITY_TYPE) {
      violations.add(ErrorFormat.MAX_LENGTH_MOBILITY_TYPE_OVERFLOW_305);
    }
    if (isAValidObject(mobilityChoice.getCountry())
        && isAValidString(mobilityChoice.getCountry().getCountryCode())) {
      if (mobilityChoice.getCountry().getCountryCode().length() > MobilityChoiceDao.MAX_LENGTH_COUNTRY) {
        violations.add(ErrorFormat.MAX_LENGTH_COUNTRY_CODE_OVERFLOW_314);
      } else if (countryDao.findById(mobilityChoice.getCountry().getCountryCode()) == null) {
        violations.add(ErrorFormat.EXISTENCE_VIOLATION_COUNTRY_CODE_900);
      }
    }
    if (isAValidObject(mobilityChoice.getProgramme())
        && programmeDao.findById(mobilityChoice.getProgramme().getId()) == null) {
      violations.add(ErrorFormat.EXISTENCE_VIOLATION_PROGRAMME_ID_1000);
    }
    if (isAValidObject(mobilityChoice.getUser()) && userDao.findById(mobilityChoice.getUser().getId()) == null) {
      violations.add(ErrorFormat.EXISTENCE_VIOLATION_USER_ID_200);
    }
    if (violations.size() > 0) {
      System.out.println("violations : " + violations.get(0));
      throw new BusinessException(ErrorFormat.INVALID_INPUT_DATA_110, violations);
    }
  }
}
