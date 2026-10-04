package com.dragomitch.ipl.pae.uccontrollers.implementations;

import static com.dragomitch.ipl.pae.utils.DataValidationUtils.checkObject;
import static com.dragomitch.ipl.pae.utils.DataValidationUtils.checkPositive;
import static com.dragomitch.ipl.pae.utils.DataValidationUtils.checkString;
import static com.dragomitch.ipl.pae.utils.DataValidationUtils.isAValidObject;
import static com.dragomitch.ipl.pae.utils.DataValidationUtils.isAValidString;
import static com.dragomitch.ipl.pae.utils.DataValidationUtils.isPositive;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.Mobility;
import com.dragomitch.ipl.pae.business.MobilityChoice;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.business.dto.DocumentDto;
import com.dragomitch.ipl.pae.business.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.business.dto.MobilityDto;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.ErrorFormat;
import com.dragomitch.ipl.pae.business.exceptions.RessourceNotFoundException;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import com.dragomitch.ipl.pae.persistence.CountryDao;
import com.dragomitch.ipl.pae.persistence.DenialReasonDao;
import com.dragomitch.ipl.pae.persistence.DocumentDao;
import com.dragomitch.ipl.pae.persistence.MobilityChoiceDao;
import com.dragomitch.ipl.pae.persistence.MobilityDao;
import com.dragomitch.ipl.pae.persistence.MobilityDocumentDao;
import com.dragomitch.ipl.pae.persistence.ProgrammeDao;
import com.dragomitch.ipl.pae.persistence.UserDao;
import com.dragomitch.ipl.pae.utils.CsvStringBuilder;
import com.dragomitch.ipl.pae.business.exceptions.InsufficientPermissionException;
import com.dragomitch.ipl.pae.uccontrollers.PartnerUcc;
import com.dragomitch.ipl.pae.uccontrollers.MobilityChoiceUcc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class MobilityChoiceUccImpl implements MobilityChoiceUcc {

  private static final Logger logger = LoggerFactory.getLogger(MobilityChoiceUccImpl.class);

  private final UserDao userDao;
  private final MobilityChoiceDao mobilityChoiceDao;
  private final MobilityDao mobilityDao;
  private final DenialReasonDao denialReasonDao;
  private final DocumentDao documentDao;
  private final MobilityDocumentDao mobilityDocumentDao;
  private final EntityFactory entityFactory;
  private final CountryDao countryDao;
  private final ProgrammeDao programmeDao;
  private final PartnerUcc partnerUcc;

  MobilityChoiceUccImpl(UserDao userDao, MobilityChoiceDao mobilityChoiceDao, MobilityDao mobilityDao,
      DenialReasonDao denialReasonDao, DocumentDao documentDao, MobilityDocumentDao mobilityDocumentDao,
      EntityFactory entityFactory, CountryDao countryDao, ProgrammeDao programmeDao, PartnerUcc partnerUcc) {
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
  }

  @Override
  public MobilityChoiceDto create(MobilityChoiceDto mobilityChoice, int userId,
      String userRole) {
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
    return mobilityChoiceDao.create(mobilityChoice);
  }

  @Override
  @Transactional(readOnly = true)
  public List<MobilityChoiceDto> showAll(int userId, String userRole, String filter) {
    checkFilter(filter);
    String filterToUse = filter == null ? MobilityChoiceDao.FILTER_ACTIVE_MOBILITIES_CHOICES : filter;
    List<MobilityChoiceDto> mobilityChoices;
    if (userRole.equals(UserDto.ROLE_PROFESSOR)) {
      // Lists all mobility choices in function of filter
      mobilityChoices = mobilityChoiceDao.findAll(filterToUse);
    } else {
      // Lists mobility choices owned by the requester
      mobilityChoices = getMobilityChoiceForUser(userId, userId, userRole);
    }
    return mobilityChoices;
  }

  @Override
  @Transactional(readOnly = true)
  public int countAll(int userId, String userRole, String filter) {
    return showAll(userId, userRole, filter).size();
  }

  @Override
  public void cancel(int mobilityChoiceId, int userId, String reason) {
    checkPositive(mobilityChoiceId);
    checkString(reason);
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
  }

  @Override
  public void reject(int id, int reason) {
    checkPositive(id);
    checkPositive(reason);
    MobilityChoice mobilityChoice = (MobilityChoice) mobilityChoiceDao.findById(id);
    if (mobilityChoice == null) {
      throw new RessourceNotFoundException();
    }
    if (mobilityChoice.getCancellationReason() != null
        || mobilityChoice.getDenialReason() != null) {
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
    // optimistic locking: the DAO only updates the row if its version is still the one read above
    // and throws a ConcurrentModificationException otherwise (the transaction then rolls back)
    mobilityChoiceDao.update(mobilityChoice);
  }

  @Override
  public void confirm(int id, int userId) {
    checkPositive(id);
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

    mobility.setProfessorInCharge(userDao.findById(userId));
    mobilityDao.create(mobility);
    List<DocumentDto> documents = documentDao.findAllByProgramme(mobilityChoice.getProgramme().getId());
    for (DocumentDto document : documents) {
      mobilityDocumentDao.create(document.getId(), id);
    }
    List<MobilityChoiceDto> mobilityChoices = getMobilityChoiceForUser(mobilityChoice.getUser().getId(), userId,
        UserDto.ROLE_PROFESSOR);
    for (MobilityChoiceDto choice : mobilityChoices) {
      // the other open choices of the same term are rejected; closed ones are left as they are
      if (choice.getId() != mobilityChoice.getId()
          && choice.getAcademicYear() == mobilityChoice.getAcademicYear()
          && choice.getTerm() == mobilityChoice.getTerm()
          && choice.getCancellationReason() == null && choice.getDenialReason() == null) {
        reject(choice.getId(), 1);
      }
    }
  }

  @Override
  public void confirmWithNewPartner(int id, PartnerDto partner, int userId, String userRole) {
    checkPositive(id);
    checkObject(partner);
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
    if (!isPositive(partner.getId())) {
      partner = partnerUcc.create(partner, userRole);
    } else {
      partner = partnerUcc.restore(partner.getId(), userRole);
    }
    // the partner must be in the country the student chose, if any (the whole transaction,
    // including a partner created above, rolls back otherwise)
    CountryDto partnerCountry = partner.getAddress().getCountry();
    if (mobilityChoice.getCountry() != null && !mobilityChoice.getCountry().getCountryCode()
        .equals(partnerCountry.getCountryCode())) {
      throw new BusinessException(ErrorFormat.COUNTRY_CHANGE_NOT_ALLOWED_320);
    }
    mobilityChoice.setCountry(partnerCountry);
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
          && choice.getTerm() == mobilityChoice.getTerm()
          && choice.getCancellationReason() == null
          && choice.getDenialReason() == null) {
        reject(choice.getId(), 1);
      }
    }
  }

  @Override
  @Transactional(readOnly = true)
  public String exportAll(int userId, String userRole, String filter) {
    List<MobilityChoiceDto> mobilityChoices = showAll(userId, userRole, filter);
    CsvStringBuilder csvStringBuilder = new CsvStringBuilder(';');
    String[] headerCsv = {"N° ordre candidature", "Nom", "Prénom", "Option", "N° ordre préférence",
        "Programme de mobilité", "Type de mobilité", "Semestre de départ", "Partenaire"};
    csvStringBuilder.writeLine(headerCsv);
    for (MobilityChoiceDto mobiChoice : mobilityChoices) {
      csvStringBuilder.writeLine(transformToStringTable(mobiChoice));
    }
    return csvStringBuilder.close();
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
    if ((userDao.findById(onUserId)) == null) {
      throw new BusinessException(ErrorFormat.EXISTENCE_VIOLATION_USER_ID_200);
    }
    mobilityChoices = mobilityChoiceDao.findByUser(userId);
    return mobilityChoices;
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
  @Transactional(readOnly = true)
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
      logger.debug("Invalid mobility choice, violations: {}", violations);
      throw new BusinessException(ErrorFormat.INVALID_INPUT_DATA_110, violations);
    }
  }
}
