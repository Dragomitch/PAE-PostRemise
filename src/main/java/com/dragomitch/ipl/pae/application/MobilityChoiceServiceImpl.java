package com.dragomitch.ipl.pae.application;

import com.dragomitch.ipl.pae.application.export.CsvStringBuilder;
import com.dragomitch.ipl.pae.domain.dto.CountryDto;
import com.dragomitch.ipl.pae.domain.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.domain.dto.DocumentDto;
import com.dragomitch.ipl.pae.domain.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.domain.dto.MobilityDto;
import com.dragomitch.ipl.pae.domain.dto.PartnerDto;
import com.dragomitch.ipl.pae.domain.dto.UserDto;
import com.dragomitch.ipl.pae.domain.exceptions.BusinessException;
import com.dragomitch.ipl.pae.domain.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.domain.exceptions.InsufficientPermissionException;
import com.dragomitch.ipl.pae.domain.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.domain.model.Document;
import com.dragomitch.ipl.pae.domain.model.EntityFactory;
import com.dragomitch.ipl.pae.domain.model.Mobility;
import com.dragomitch.ipl.pae.domain.model.MobilityChoice;
import com.dragomitch.ipl.pae.domain.model.MobilityChoiceFilter;
import com.dragomitch.ipl.pae.domain.model.Role;
import com.dragomitch.ipl.pae.domain.port.in.MobilityChoiceService;
import com.dragomitch.ipl.pae.domain.port.in.PartnerService;
import com.dragomitch.ipl.pae.domain.port.out.CountryRepository;
import com.dragomitch.ipl.pae.domain.port.out.DenialReasonRepository;
import com.dragomitch.ipl.pae.domain.port.out.DocumentRepository;
import com.dragomitch.ipl.pae.domain.port.out.MobilityChoiceRepository;
import com.dragomitch.ipl.pae.domain.port.out.MobilityDocumentRepository;
import com.dragomitch.ipl.pae.domain.port.out.MobilityRepository;
import com.dragomitch.ipl.pae.domain.port.out.ProgrammeRepository;
import com.dragomitch.ipl.pae.domain.port.out.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class MobilityChoiceServiceImpl implements MobilityChoiceService {

  private final UserRepository userRepository;
  private final MobilityChoiceRepository mobilityChoiceRepository;
  private final MobilityRepository mobilityRepository;
  private final DenialReasonRepository denialReasonRepository;
  private final DocumentRepository documentRepository;
  private final MobilityDocumentRepository mobilityDocumentRepository;
  private final EntityFactory entityFactory;
  private final CountryRepository countryRepository;
  private final ProgrammeRepository programmeRepository;
  private final PartnerService partnerService;
  private final Clock clock;

  MobilityChoiceServiceImpl(
      UserRepository userRepository,
      MobilityChoiceRepository mobilityChoiceRepository,
      MobilityRepository mobilityRepository,
      DenialReasonRepository denialReasonRepository,
      DocumentRepository documentRepository,
      MobilityDocumentRepository mobilityDocumentRepository,
      EntityFactory entityFactory,
      CountryRepository countryRepository,
      ProgrammeRepository programmeRepository,
      PartnerService partnerService,
      Clock clock) {
    this.userRepository = userRepository;
    this.mobilityChoiceRepository = mobilityChoiceRepository;
    this.mobilityRepository = mobilityRepository;
    this.denialReasonRepository = denialReasonRepository;
    this.documentRepository = documentRepository;
    this.mobilityDocumentRepository = mobilityDocumentRepository;
    this.entityFactory = entityFactory;
    this.countryRepository = countryRepository;
    this.programmeRepository = programmeRepository;
    this.partnerService = partnerService;
    this.clock = clock;
  }

  @Override
  public MobilityChoiceDto create(MobilityChoiceDto mobilityChoice, int userId, Role userRole) {
    checkReferencesExist(mobilityChoice);
    if (userRole == Role.STUDENT && mobilityChoice.getUser().getId() != userId) {
      throw new InsufficientPermissionException();
    }
    if (userRole == Role.PROFESSOR && mobilityChoice.getUser().getId() == userId) {
      throw new BusinessException(ErrorCode.PROFESSOR_CANNOT_APPLY);
    }
    UserDto userDto;
    if ((userDto = userRepository.findById(mobilityChoice.getUser().getId())) == null) {
      throw new BusinessException(ErrorCode.UNKNOWN_USER, mobilityChoice.getUser().getId());
    }
    mobilityChoice.setUser(userDto);
    return mobilityChoiceRepository.create(mobilityChoice);
  }

  @Override
  @Transactional(readOnly = true)
  public List<MobilityChoiceDto> showAll(int userId, Role userRole, MobilityChoiceFilter filter) {
    List<MobilityChoiceDto> mobilityChoices;
    if (userRole == Role.PROFESSOR) {
      // Lists all mobility choices in function of filter
      mobilityChoices = mobilityChoiceRepository.findAll(filter);
    } else {
      // Lists mobility choices owned by the requester
      mobilityChoices = getMobilityChoiceForUser(userId, userId, userRole);
    }
    return mobilityChoices;
  }

  @Override
  @Transactional(readOnly = true)
  public int countAll(int userId, Role userRole, MobilityChoiceFilter filter) {
    return showAll(userId, userRole, filter).size();
  }

  @Override
  public void cancel(int mobilityChoiceId, int userId, String reason) {
    MobilityChoice mobilityChoice =
        (MobilityChoice) mobilityChoiceRepository.findById(mobilityChoiceId);
    if (mobilityChoice == null) {
      throw new ResourceNotFoundException();
    }
    if (userId != mobilityChoice.getUser().getId()) {
      throw new InsufficientPermissionException();
    }
    if (mobilityChoice.getCancellationReason() != null
        || mobilityChoice.getDenialReason() != null) {
      throw new BusinessException(ErrorCode.MOBILITY_CHOICE_CLOSED);
    }
    MobilityDto mobility = mobilityRepository.findById(mobilityChoiceId);
    if (mobility != null) {
      throw new BusinessException(ErrorCode.MOBILITY_CHOICE_ALREADY_CONFIRMED);
    }
    mobilityChoice.setCancellationReason(reason);
    mobilityChoiceRepository.update(mobilityChoice);
  }

  @Override
  public void reject(int id, int reason) {
    MobilityChoice mobilityChoice = (MobilityChoice) mobilityChoiceRepository.findById(id);
    if (mobilityChoice == null) {
      throw new ResourceNotFoundException();
    }
    if (isClosed(mobilityChoice)) {
      throw new BusinessException(ErrorCode.MOBILITY_CHOICE_CLOSED);
    }
    DenialReasonDto denialReason = denialReasonRepository.findById(reason);
    if (denialReason == null) {
      throw new BusinessException(ErrorCode.UNKNOWN_DENIAL_REASON, reason);
    }
    MobilityDto mobility = mobilityRepository.findById(id);
    if (mobility != null) {
      throw new BusinessException(ErrorCode.MOBILITY_CHOICE_ALREADY_CONFIRMED);
    }
    mobilityChoice.setDenialReason(denialReason);
    // optimistic locking: the repository only updates the row if its version is still the one read
    // above
    // and throws a ConcurrentModificationException otherwise (the transaction then rolls back)
    mobilityChoiceRepository.update(mobilityChoice);
  }

  @Override
  public void confirm(int id, int userId) {
    MobilityChoiceDto mobilityChoice = mobilityChoiceRepository.findById(id);
    if (mobilityChoice == null) {
      throw new ResourceNotFoundException();
    }
    if (mobilityChoice.getCancellationReason() != null
        || mobilityChoice.getDenialReason() != null) {
      throw new BusinessException(ErrorCode.MOBILITY_CHOICE_CLOSED);
    }
    if (mobilityRepository.findById(id) != null) {
      throw new BusinessException(ErrorCode.MOBILITY_CHOICE_ALREADY_CONFIRMED);
    }
    if (mobilityChoice.getPartner() == null) {
      throw new BusinessException(ErrorCode.PARTNER_REQUIRED_TO_CONFIRM);
    }
    MobilityDto mobility = newMobility(id);
    mobility.setProfessorInCharge(userRepository.findById(userId));
    createMobility(mobility, mobilityChoice);
    List<MobilityChoiceDto> mobilityChoices =
        getMobilityChoiceForUser(mobilityChoice.getUser().getId(), userId, Role.PROFESSOR);
    for (MobilityChoiceDto choice : mobilityChoices) {
      // the other open choices of the same term are rejected; closed ones are left as they are
      if (choice.getId() != mobilityChoice.getId()
          && choice.getAcademicYear() == mobilityChoice.getAcademicYear()
          && choice.getTerm() == mobilityChoice.getTerm()
          && !isClosed(choice)) {
        reject(choice.getId(), 1);
      }
    }
  }

  @Override
  public void confirmWithNewPartner(int id, PartnerDto partner, int userId, Role userRole) {
    MobilityChoiceDto mobilityChoice = mobilityChoiceRepository.findById(id);
    if (mobilityChoice == null) {
      throw new ResourceNotFoundException();
    }
    if (mobilityChoice.getCancellationReason() != null
        || mobilityChoice.getDenialReason() != null) {
      throw new BusinessException(ErrorCode.MOBILITY_CHOICE_CLOSED);
    }
    if (userRole == Role.STUDENT && userId != mobilityChoice.getUser().getId()) {
      throw new InsufficientPermissionException();
    }
    if (userRole == Role.STUDENT && (partner.isOfficial())) {
      throw new InsufficientPermissionException();
    }
    if (mobilityRepository.findById(id) != null) {
      throw new BusinessException(ErrorCode.MOBILITY_CHOICE_ALREADY_CONFIRMED);
    }
    // the partner must be in the country of the choice, if the student chose one
    if (partner.getId() <= 0) {
      checkPartnerCountry(mobilityChoice, partner);
      partner = partnerService.create(partner, userRole);
    } else {
      // an existing partner is checked as stored, not as sent by the client (the transaction
      // rolls the restoration back if it is refused)
      partner = partnerService.restore(partner.getId(), userRole);
      checkPartnerCountry(mobilityChoice, partner);
    }
    mobilityChoice.setCountry(partner.getAddress().getCountry());
    mobilityChoice.setPartner(partner);
    mobilityChoiceRepository.update(mobilityChoice);
    createMobility(newMobility(id), mobilityChoice);
    List<MobilityChoiceDto> mobilityChoices =
        getMobilityChoiceForUser(mobilityChoice.getUser().getId(), userId, userRole);
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
  public String exportAll(int userId, Role userRole, MobilityChoiceFilter filter) {
    List<MobilityChoiceDto> mobilityChoices = showAll(userId, userRole, filter);
    CsvStringBuilder csvStringBuilder = new CsvStringBuilder(';');
    String[] headerCsv = {
      "N° ordre candidature",
      "Nom",
      "Prénom",
      "Option",
      "N° ordre préférence",
      "Programme de mobilité",
      "Type de mobilité",
      "Semestre de départ",
      "Partenaire"
    };
    csvStringBuilder.writeLine(headerCsv);
    for (MobilityChoiceDto mobiChoice : mobilityChoices) {
      csvStringBuilder.writeLine(transformToStringTable(mobiChoice));
    }
    return csvStringBuilder.close();
  }

  /**
   * Tells whether a choice was cancelled by the student or rejected by a professor. The repository
   * reads the denial reason of a rejected choice with its id only, without its text.
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
  private List<MobilityChoiceDto> getMobilityChoiceForUser(
      int userId, int onUserId, Role userRole) {
    if (userRole == Role.STUDENT && onUserId != userId) {
      throw new InsufficientPermissionException();
    }
    List<MobilityChoiceDto> mobilityChoices = null;
    if ((userRepository.findById(onUserId)) == null) {
      throw new BusinessException(ErrorCode.UNKNOWN_USER, onUserId);
    }
    mobilityChoices = mobilityChoiceRepository.findByUser(userId);
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
    List<MobilityChoiceDto> mobilityChoices = mobilityChoiceRepository.findByPartner(partnerId);
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
        && countryRepository.findById(mobilityChoice.getCountry().getCountryCode()) == null) {
      throw new BusinessException(
          ErrorCode.UNKNOWN_COUNTRY, mobilityChoice.getCountry().getCountryCode());
    }
    if (programmeRepository.findById(mobilityChoice.getProgramme().getId()) == null) {
      throw new BusinessException(
          ErrorCode.UNKNOWN_PROGRAMME, mobilityChoice.getProgramme().getId());
    }
    if (userRepository.findById(mobilityChoice.getUser().getId()) == null) {
      throw new BusinessException(ErrorCode.UNKNOWN_USER, mobilityChoice.getUser().getId());
    }
  }

  private MobilityDto newMobility(int id) {
    MobilityDto mobility = (MobilityDto) entityFactory.build(MobilityDto.class);
    mobility.setId(id);
    mobility.setSubmissionDate(LocalDateTime.now(clock));
    return mobility;
  }

  /**
   * Creates the mobility of a confirmed choice with the documents of its programme. It starts
   * {@value MobilityDto#STATE_CREATED}, or directly {@value MobilityDto#STATE_TO_BE_PAID} when the
   * programme has no departure document to fill in (nothing would ever move it forward).
   */
  private void createMobility(MobilityDto mobility, MobilityChoiceDto mobilityChoice) {
    List<DocumentDto> documents =
        documentRepository.findAllByProgramme(mobilityChoice.getProgramme().getId());
    boolean departureDocuments =
        documents.stream()
            .anyMatch(document -> document.getCategory() == Document.DEPARTURE_DOCUMENT);
    mobility.setState(departureDocuments ? Mobility.STATE_CREATED : Mobility.STATE_TO_BE_PAID);
    mobilityRepository.create(mobility);
    for (DocumentDto document : documents) {
      mobilityDocumentRepository.create(document.getId(), mobility.getId());
    }
  }

  /** Refuses a partner outside the country the student chose for the mobility choice, if any. */
  private static void checkPartnerCountry(MobilityChoiceDto mobilityChoice, PartnerDto partner) {
    CountryDto choiceCountry = mobilityChoice.getCountry();
    if (choiceCountry != null
        && choiceCountry.getCountryCode() != null
        && !choiceCountry
            .getCountryCode()
            .equals(partner.getAddress().getCountry().getCountryCode())) {
      throw new BusinessException(ErrorCode.COUNTRY_CHANGE_NOT_ALLOWED);
    }
  }
}
