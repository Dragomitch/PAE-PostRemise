package com.dragomitch.ipl.pae.uccontrollers.implementations;

import com.dragomitch.ipl.pae.business.DenialReason;
import com.dragomitch.ipl.pae.business.Mobility;
import com.dragomitch.ipl.pae.business.NominatedStudent;
import com.dragomitch.ipl.pae.business.dto.DocumentDto;
import com.dragomitch.ipl.pae.business.dto.MobilityDto;
import com.dragomitch.ipl.pae.business.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.business.dto.PartnerDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.business.exceptions.InsufficientPermissionException;
import com.dragomitch.ipl.pae.business.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.persistence.DenialReasonDao;
import com.dragomitch.ipl.pae.persistence.MobilityDao;
import com.dragomitch.ipl.pae.persistence.MobilityDocumentDao;
import com.dragomitch.ipl.pae.persistence.NominatedStudentDao;
import com.dragomitch.ipl.pae.persistence.UserDao;
import com.dragomitch.ipl.pae.uccontrollers.MobilityUcc;
import com.dragomitch.ipl.pae.uccontrollers.PartnerUcc;
import com.dragomitch.ipl.pae.uccontrollers.ProgrammeUcc;
import com.dragomitch.ipl.pae.utils.CsvStringBuilder;

import java.time.LocalDateTime;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
class MobilityUccImpl implements MobilityUcc {

  private static final int SOFTWARE_PRO_ECO = 1;
  private static final int SOFTWARE_SECOND = 2;

  private static final String DEPARTURE_DOCUMENTS_FILTER = "D";
  private static final String DEPARTURE_FILLED_DOCUMENTS_FILTER = "DF";
  private static final String RETURN_DOCUMENTS_FILTER = "R";
  private static final String RETURN_FILLED_DOCUMENTS_FILTER = "RF";

  private final MobilityDao mobilityDao;
  private final NominatedStudentDao nominatedStudentDao;
  private final DenialReasonDao denialReasonDao;
  private final MobilityDocumentDao mobilityDocumentDao;
  private final PartnerUcc partnerUcc;
  private final ProgrammeUcc programmeUcc;
  private final UserDao userDao;

  MobilityUccImpl(MobilityDao mobilityDao, NominatedStudentDao nominatedStudentDao,
      DenialReasonDao denialReasonDao, MobilityDocumentDao mobilityDocumentDao,
      PartnerUcc partnerUcc, ProgrammeUcc programmeUcc, UserDao userDao) {
    this.mobilityDao = mobilityDao;
    this.nominatedStudentDao = nominatedStudentDao;
    this.denialReasonDao = denialReasonDao;
    this.mobilityDocumentDao = mobilityDocumentDao;
    this.partnerUcc = partnerUcc;
    this.programmeUcc = programmeUcc;
    this.userDao = userDao;
  }

  @Override
  @Transactional(readOnly = true)
  public List<MobilityDto> showAll(int userId, String userRole) {
    List<MobilityDto> mobilities;
    if (userRole.equals(UserDto.ROLE_PROFESSOR)) {
      // Lists all mobilities
      mobilities = mobilityDao.findAll();
    } else {
      // Lists mobilities owned by the requester
      mobilities = mobilityDao.findByUser(userId);
    }
    return mobilities;
  }

  @Override
  @Transactional(readOnly = true)
  public MobilityDto showOne(int id, String role, int user) {
    Mobility mobility = getMobility(id);
    if (role.equals(UserDto.ROLE_STUDENT) && user != mobility.getNominatedStudent().getId()) {
      throw new InsufficientPermissionException();
    }
    mobility.setDocuments(mobilityDocumentDao.findAllByMobility(mobility.getId()));
    NominatedStudentDto student =
        nominatedStudentDao.findById(mobility.getNominatedStudent().getId());
    if (student != null) {
      mobility.setNominatedStudent(student);
    }
    // the choice may have had no partner, and the professor in charge may be unknown
    if (mobility.getPartner() != null) {
      mobility.setPartner(partnerUcc.showOne(mobility.getPartner().getId()));
    }
    mobility.setProgramme(programmeUcc.showOne(mobility.getProgramme().getId()));
    // a mobility confirmed with a new partner has no professor in charge
    if (mobility.getProfessorInCharge() != null) {
      mobility.setProfessorInCharge(userDao.findById(mobility.getProfessorInCharge().getId()));
    }
    return mobility;
  }

  @Override
  public void confirmProEcoEncoding(int id, int version) {
    confirmSoftwareEncoding(id, SOFTWARE_PRO_ECO, version);
  }

  @Override
  public void confirmSecondSoftwareEncoding(int id, int version) {
    confirmSoftwareEncoding(id, SOFTWARE_SECOND, version);
  }

  /**
   * Confirm the encoding of a mobility in an external software.
   * 
   * @param id the mobility id
   * @param software the software id
   * @param version the current version of the mobility to update
   */
  private void confirmSoftwareEncoding(int id, int software, int version) {
    Mobility mobility = getMobility(id);
    if (mobility.getVersion() != version) {
      throw new ConcurrentModificationException();
    }
    mobility.checkNotCancelled();
    if (software == SOFTWARE_PRO_ECO) {
      if (mobility.isEncodedInProEco()) {
        return;
      }
      mobility.setProEcoEncoding(true);
    } else {
      if (mobility.isEncodedInSecondSoftware()) {
        return;
      }
      mobility.setSecondSoftwareEncoding(true);
    }
    mobilityDao.update(mobility);
  }

  /**
   * {@inheritDoc}
   *
   * <p>A payment is only accepted when the state machine expects it: the first one in
   * {@value MobilityDto#STATE_TO_BE_PAID} (the departure documents are filled in), the second one
   * in {@value MobilityDto#STATE_BALANCE_TO_BE_PAID} (every document is filled in); any other
   * state is a {@link ErrorCode#PAYMENT_NOT_EXPECTED} conflict. After the first payment the
   * mobility is {@value MobilityDto#STATE_IN_PROGRESS}, or directly
   * {@value MobilityDto#STATE_BALANCE_TO_BE_PAID} when its return documents were already filled
   * in (no document would be left to move it on).
   */
  @Override
  public MobilityDto confirmPayment(int id, int version) {
    Mobility mobility = getMobility(id);
    if (mobility.getVersion() != version) {
      throw new ConcurrentModificationException();
    }
    mobility.checkNotCancelledAndNotClosed();
    boolean firstPayment = mobility.getState().equals(Mobility.STATE_TO_BE_PAID)
        && mobility.getFirstPaymentRequestDate() == null;
    boolean secondPayment = mobility.getState().equals(Mobility.STATE_BALANCE_TO_BE_PAID)
        && mobility.getFirstPaymentRequestDate() != null;
    if (!firstPayment && !secondPayment) {
      throw new BusinessException(ErrorCode.PAYMENT_NOT_EXPECTED);
    }
    NominatedStudent nominatedStudent;
    if ((nominatedStudent = (NominatedStudent) nominatedStudentDao
        .findById(mobility.getNominatedStudent().getId())) == null) {
      throw new BusinessException(ErrorCode.INCOMPLETE_BANK_DETAILS);
    }
    nominatedStudent.checkBankDetails();
    if (firstPayment) {
      mobility.setFirstPaymentRequestDate(LocalDateTime.now());
      mobility.setDocuments(mobilityDocumentDao.findAllByMobility(mobility.getId()));
      mobility.setState(mobility.allReturnDocumentsFilledIn()
          ? Mobility.STATE_BALANCE_TO_BE_PAID : Mobility.STATE_IN_PROGRESS);
    } else {
      mobility.setSecondPaymentRequestDate(LocalDateTime.now());
      mobility.setState(Mobility.STATE_CLOSED);
    }
    mobilityDao.update(mobility);
    return mobility;
  }

  /**
   * {@inheritDoc}
   *
   * <p>State transitions: {@value MobilityDto#STATE_CREATED} and
   * {@value MobilityDto#STATE_IN_PREPARATION} become {@value MobilityDto#STATE_TO_BE_PAID} as soon
   * as every departure document is filled in (also when the first document filled in completes
   * them), {@value MobilityDto#STATE_IN_PREPARATION} otherwise; {@value MobilityDto#STATE_IN_PROGRESS}
   * becomes {@value MobilityDto#STATE_BALANCE_TO_BE_PAID} when every document is filled in. The
   * other states do not change (a payment is pending). The documents of a cancelled or closed
   * mobility are frozen: {@link ErrorCode#MOBILITY_CANCELLED} / {@link ErrorCode#MOBILITY_CLOSED}.
   */
  @Override
  public MobilityDto confirmDocument(int id, int document, int version) {
    Mobility mobility = getMobility(id);
    if (mobility.getVersion() != version) {
      throw new ConcurrentModificationException();
    }
    mobility.checkNotCancelledAndNotClosed();
    mobility.setDocuments(mobilityDocumentDao.findAllByMobility(mobility.getId()));
    if (!mobility.fillInDocument(document)) {
      return mobility;
    }
    String currentState = mobility.getState();
    if (currentState.equals(Mobility.STATE_CREATED)
        || currentState.equals(Mobility.STATE_IN_PREPARATION)) {
      mobility.setState(mobility.allDepartureDocumentsFilledIn()
          ? Mobility.STATE_TO_BE_PAID : Mobility.STATE_IN_PREPARATION);
    } else if (currentState.equals(Mobility.STATE_IN_PROGRESS)) {
      if (mobility.allDepartureDocumentsFilledIn() && mobility.allReturnDocumentsFilledIn()) {
        mobility.setState(Mobility.STATE_BALANCE_TO_BE_PAID);
      }
    }
    mobilityDocumentDao.fillInDocument(document, mobility.getId());
    mobility = (Mobility) mobilityDao.update(mobility);
    return mobility;
  }

  @Override
  public MobilityDto cancel(int id, int version, String cancellationReason, int denialReasonId,
      int userId, String userRole) {
    Mobility mobility = getMobility(id);
    if (mobility.getVersion() != version) {
      throw new ConcurrentModificationException();
    }
    // The student does not own the mobility: checked first, so that the state and the reasons
    // of the mobility of another student are not disclosed
    if (userRole.equals(UserDto.ROLE_STUDENT)
        && userId != mobility.getNominatedStudent().getId()) {
      throw new InsufficientPermissionException();
    }
    mobility.checkNotClosed();
    // The mobility is already cancelled
    if (mobility.getState().equals(Mobility.STATE_CANCELLED)) {
      return mobility;
    }
    // The mobility can be cancelled
    String stateBeforeCancellation = mobility.getState();
    if (userRole.equals(UserDto.ROLE_PROFESSOR)) {
      if (denialReasonId <= 0) {
        throw new BusinessException(ErrorCode.DENIAL_REASON_REQUIRED);
      }
      DenialReason denialReason = (DenialReason) denialReasonDao.findById(denialReasonId);
      if (denialReason == null) {
        throw new BusinessException(ErrorCode.UNKNOWN_DENIAL_REASON, denialReasonId);
      }
      mobility.setDenialReason(denialReason);
    } else {
      if (!StringUtils.hasText(cancellationReason)) {
        throw new BusinessException(ErrorCode.CANCELLATION_REASON_REQUIRED);
      }
      mobility.setCancellationReason(cancellationReason);
    }
    // Updating the state and the state before cancellation
    mobility.setStateBeforeCancellation(stateBeforeCancellation);
    mobility.setState(Mobility.STATE_CANCELLED);
    return mobilityDao.update(mobility);
  }

  @Override
  @Transactional(readOnly = true)
  public String exportDocuments(int mobilityId, String filter) {
    filter = filter == null ? "" : filter;
    Mobility mobility = getMobility(mobilityId);
    mobility.setDocuments(mobilityDocumentDao.findAllByMobility(mobility.getId()));
    CsvStringBuilder csvStringBuilder = new CsvStringBuilder(';');
    String[] mobilityData = mobilityToStringTable(mobility);
    String[] csvMobilityHeader =
        {"Nom", "Prénom", "Partenaire", "Type de mobilité", "Programme de mobilité", "Semestre"};
    String[] csvDocumentsHeader =
        {"Contrat de bourse", "Convention de stage / Convention d'études", "Charte de l'étudiant",
            "Document d'engagement", "Preuve du passage des tests linguistiques",
            "Attestation séjour", "Relevé de notes (SMS) ou certificat de stage (SMP)",
            "Rapport final (complété en ligne)",
            "Preuve du passage des tests linguistiques après la mobilité"};
    Map<String, String> documentsInMobility = new HashMap<String, String>();
    csvStringBuilder.write(csvMobilityHeader);
    csvStringBuilder.writeLine(csvDocumentsHeader);
    csvStringBuilder.write(mobilityData);
    List<DocumentDto> documents = mobility.getDocuments();
    for (DocumentDto document : documents) {
      if (isExported(document, filter)) {
        documentsInMobility.put(document.getName(),
            document.isFilledIn() ? "Rempli" : "Non-rempli");
      }
    }
    for (String clef : csvDocumentsHeader) {
      String valeur;
      if ((valeur = documentsInMobility.get(clef)) == null) {
        csvStringBuilder.write("");
      } else {
        csvStringBuilder.write(valeur);
      }
    }
    return csvStringBuilder.close();
  }

  /**
   * Tells whether the export keeps a document: the documents of the category of the filter (D or
   * R), only the filled-in ones for DF and RF, every document without a filter.
   */
  private static boolean isExported(DocumentDto document, String filter) {
    return switch (filter) {
      case DEPARTURE_DOCUMENTS_FILTER, RETURN_DOCUMENTS_FILTER ->
          document.getCategory() == filter.charAt(0);
      case DEPARTURE_FILLED_DOCUMENTS_FILTER, RETURN_FILLED_DOCUMENTS_FILTER ->
          document.getCategory() == filter.charAt(0) && document.isFilledIn();
      default -> true;
    };
  }

  /**
   * Construct a table of strings to represent the mobilityDto to serialize.
   * 
   * @param mobility the mobility to serialize
   * @return a table of strings serializable.
   */
  private String[] mobilityToStringTable(MobilityDto mobility) {
    String[] entries = new String[6];
    UserDto user = mobility.getNominatedStudent();
    PartnerDto partner = mobility.getPartner();
    entries[0] = user.getLastName();
    entries[1] = user.getFirstName();
    entries[2] = partner == null ? "" : partner.getFullName();
    entries[3] = mobility.getMobilityType();
    entries[4] = mobility.getProgramme().getProgrammeName();
    entries[5] = mobility.getTerm() + "";
    return entries;
  }

  /**
   * Retrieve a mobility with the id passed in parameter.
   * 
   * @param mobilityId the id of the mobility we want to retrieve
   * @return a Mobility for the id passed in parameter.
   */
  private Mobility getMobility(int mobilityId) {
    Mobility mobility = null;
    if ((mobility = (Mobility) mobilityDao.findById(mobilityId)) == null) {
      throw new ResourceNotFoundException();
    }
    return mobility;
  }

}
