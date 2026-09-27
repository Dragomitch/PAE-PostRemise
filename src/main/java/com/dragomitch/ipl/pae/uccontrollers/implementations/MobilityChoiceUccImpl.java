package com.dragomitch.ipl.pae.uccontrollers.implementations;

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
import com.dragomitch.ipl.pae.business.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.business.exceptions.InsufficientPermissionException;
import com.dragomitch.ipl.pae.business.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.persistence.CountryDao;
import com.dragomitch.ipl.pae.persistence.DenialReasonDao;
import com.dragomitch.ipl.pae.persistence.DocumentDao;
import com.dragomitch.ipl.pae.persistence.MobilityChoiceDao;
import com.dragomitch.ipl.pae.persistence.MobilityDao;
import com.dragomitch.ipl.pae.persistence.MobilityDocumentDao;
import com.dragomitch.ipl.pae.persistence.ProgrammeDao;
import com.dragomitch.ipl.pae.persistence.UserDao;
import com.dragomitch.ipl.pae.uccontrollers.MobilityChoiceUcc;
import com.dragomitch.ipl.pae.uccontrollers.PartnerUcc;
import com.dragomitch.ipl.pae.utils.CsvStringBuilder;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class MobilityChoiceUccImpl implements MobilityChoiceUcc {

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
    checkReferencesExist(mobilityChoice);
    if (userRole.equals(UserDto.ROLE_STUDENT) && mobilityChoice.getUser().getId() != userId) {
      throw new InsufficientPermissionException();
    }
    if (userRole.equals(UserDto.ROLE_PROFESSOR) && mobilityChoice.getUser().getId() == userId) {
      throw new BusinessException(ErrorCode.PROFESSOR_CANNOT_APPLY);
    }
    UserDto userDto;
    if ((userDto = userDao.findById(mobilityChoice.getUser().getId())) == null) {
      throw new BusinessException(ErrorCode.UNKNOWN_USER, mobilityChoice.getUser().getId());
    }
    mobilityChoice.setUser(userDto);
    return mobilityChoiceDao.create(mobilityChoice);
  }

  @Override
  @Transactional(readOnly = true)
  public List<MobilityChoiceDto> showAll(int userId, String userRole, String filter) {
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
    MobilityChoice mobilityChoice = (MobilityChoice) mobilityChoiceDao.findById(mobilityChoiceId);
    if (mobilityChoice == null) {
      throw new ResourceNotFoundException();
    }
    if (userId != mobilityChoice.getUser().getId()) {
      throw new InsufficientPermissionException();
    }
    if (mobilityChoice.getCancellationReason() != null || mobilityChoice.getDenialReason() != null) {
      throw new BusinessException(ErrorCode.MOBILITY_CHOICE_CLOSED);
    }
    MobilityDto mobility = mobilityDao.findById(mobilityChoiceId);
    if (mobility != null) {
      throw new BusinessException(ErrorCode.MOBILITY_CHOICE_ALREADY_CONFIRMED);
    }
    mobilityChoice.setCancellationReason(reason);
    mobilityChoiceDao.update(mobilityChoice);
  }

  @Override
  public void reject(int id, int reason) {
    MobilityChoice mobilityChoice = (MobilityChoice) mobilityChoiceDao.findById(id);
    if (mobilityChoice == null) {
      throw new ResourceNotFoundException();
    }
    if (isClosed(mobilityChoice)) {
      throw new BusinessException(ErrorCode.MOBILITY_CHOICE_CLOSED);
    }
    DenialReasonDto denialReason = denialReasonDao.findById(reason);
    if (denialReason == null) {
      throw new BusinessException(ErrorCode.UNKNOWN_DENIAL_REASON, reason);
    }
    MobilityDto mobility = mobilityDao.findById(id);
    if (mobility != null) {
      throw new BusinessException(ErrorCode.MOBILITY_CHOICE_ALREADY_CONFIRMED);
    }
    mobilityChoice.setDenialReason(denialReason);
    // optimistic locking: the DAO only updates the row if its version is still the one read above
    // and throws a ConcurrentModificationException otherwise (the transaction then rolls back)
    mobilityChoiceDao.update(mobilityChoice);
  }

  @Override
  @SuppressWarnings("unused")
  public void confirm(int id, int userId) {
    MobilityChoiceDto mobilityChoice = mobilityChoiceDao.findById(id);
    if (mobilityChoice == null) {
      throw new ResourceNotFoundException();
    }
    if (mobilityChoice.getCancellationReason() != null || mobilityChoice.getDenialReason() != null) {
      throw new BusinessException(ErrorCode.MOBILITY_CHOICE_CLOSED);
    }
    if (mobilityDao.findById(id) != null) {
      throw new BusinessException(ErrorCode.MOBILITY_CHOICE_ALREADY_CONFIRMED);
    }
    if (mobilityChoice.getPartner() == null) {
      throw new BusinessException(ErrorCode.PARTNER_REQUIRED_TO_CONFIRM);
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
          && choice.getTerm() == mobilityChoice.getTerm()
          && !isClosed(choice)) {
        reject(choice.getId(), 1);
      }
    }
  }

  @Override
  public void confirmWithNewPartner(int id, PartnerDto partner, int userId, String userRole) {
    MobilityChoiceDto mobilityChoice = mobilityChoiceDao.findById(id);
    if (mobilityChoice == null) {
      throw new ResourceNotFoundException();
    }
    if (mobilityChoice.getCancellationReason() != null || mobilityChoice.getDenialReason() != null) {
      throw new BusinessException(ErrorCode.MOBILITY_CHOICE_CLOSED);
    }
    if (userRole.equals(UserDto.ROLE_STUDENT) && userId != mobilityChoice.getUser().getId()) {
      throw new InsufficientPermissionException();
    }
    if (userRole.equals(UserDto.ROLE_STUDENT) && (partner.isOfficial())) {
      throw new InsufficientPermissionException();
    }
    if (mobilityDao.findById(id) != null) {
      throw new BusinessException(ErrorCode.MOBILITY_CHOICE_ALREADY_CONFIRMED);
    }
    // the partner must be in the country of the choice, if the student chose one
    CountryDto partnerCountry = partner.getAddress().getCountry();
    CountryDto choiceCountry = mobilityChoice.getCountry();
    if (choiceCountry != null && choiceCountry.getCountryCode() != null
        && !choiceCountry.getCountryCode().equals(partnerCountry.getCountryCode())) {
      throw new BusinessException(ErrorCode.COUNTRY_CHANGE_NOT_ALLOWED);
    }
    mobilityChoice.setCountry(partnerCountry);
    if (partner.getId() <= 0) {
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
          && choice.getTerm() == mobilityChoice.getTerm()
          && !isClosed(choice)) {
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
   * Tells whether a choice was cancelled by the student or rejected by a professor. The DAO reads
   * the denial reason of a rejected choice with its id only, without its text.
   */
  private static boolean isClosed(MobilityChoiceDto mobilityChoice) {
    return mobilityChoice.getCancellationReason() != null
        || mobilityChoice.getDenialReason() != null;
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
    if (userRole.equals(UserDto.ROLE_STUDENT) && onUserId != userId) {
      throw new InsufficientPermissionException();
    }
    List<MobilityChoiceDto> mobilityChoices = null;
    if ((userDao.findById(onUserId)) == null) {
      throw new BusinessException(ErrorCode.UNKNOWN_USER, onUserId);
    }
    mobilityChoices = mobilityChoiceDao.findByUser(userId);
    return mobilityChoices;
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
    PartnerDto partner = mobilityChoice.getPartner();
    entries[8] = partner == null ? "" : partner.getFullName();
    return entries;
  }

  @Override
  @Transactional(readOnly = true)
  public boolean findByPartner(int partnerId) {
    List<MobilityChoiceDto> mobilityChoices = mobilityChoiceDao.findByPartner(partnerId);
    return mobilityChoices.isEmpty();
  }

  /**
   * Checks that the entities referenced by a new mobility choice exist (their format is checked by
   * the constraints of {@link MobilityChoiceDto}).
   *
   * @param mobilityChoice the new mobility choice
   */
  private void checkReferencesExist(MobilityChoiceDto mobilityChoice) {
    if (mobilityChoice.getCountry() != null
        && countryDao.findById(mobilityChoice.getCountry().getCountryCode()) == null) {
      throw new BusinessException(ErrorCode.UNKNOWN_COUNTRY,
          mobilityChoice.getCountry().getCountryCode());
    }
    if (programmeDao.findById(mobilityChoice.getProgramme().getId()) == null) {
      throw new BusinessException(ErrorCode.UNKNOWN_PROGRAMME,
          mobilityChoice.getProgramme().getId());
    }
    if (userDao.findById(mobilityChoice.getUser().getId()) == null) {
      throw new BusinessException(ErrorCode.UNKNOWN_USER, mobilityChoice.getUser().getId());
    }
  }
}
