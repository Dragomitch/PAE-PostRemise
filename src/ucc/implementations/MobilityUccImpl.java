package ucc.implementations;

import static utils.DataValidationUtils.checkPositive;
import static utils.DataValidationUtils.checkString;

import business.DenialReason;
import business.Mobility;
import business.NominatedStudent;
import business.dto.DocumentDto;
import business.dto.MobilityDto;
import business.dto.NominatedStudentDto;
import business.dto.PartnerDto;
import business.dto.UserDto;
import business.exceptions.BusinessException;
import business.exceptions.ErrorFormat;
import business.exceptions.RessourceNotFoundException;
import main.annotations.Inject;
import persistence.DenialReasonDao;
import persistence.MobilityDao;
import persistence.MobilityDocumentDao;
import persistence.NominatedStudentDao;
import persistence.UserDao;
import presentation.CsvStringBuilder;
import presentation.annotations.ApiCollection;
import presentation.annotations.HttpParameter;
import presentation.annotations.PathParameter;
import presentation.annotations.Role;
import presentation.annotations.Route;
import presentation.annotations.SessionParameter;
import presentation.enums.HttpMethod;
import presentation.exceptions.InsufficientPermissionException;
import ucc.MobilityUcc;
import ucc.PartnerUcc;
import ucc.ProgrammeUcc;
import ucc.SessionUcc;
import ucc.UnitOfWork;

import java.time.LocalDateTime;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApiCollection(name = "Mobilities", endpoint = "/mobilities")
class MobilityUccImpl implements MobilityUcc {

  private static final int SOFTWARE_PRO_ECO = 1;
  private static final int SOFTWARE_SECOND = 2;

  private static final String DEPARTURE_DOCUMENTS_FILTER = "D";
  private static final String DEPARTURE_FILLED_DOCUMENTS_FILTER = "DF";
  private static final String RETURN_DOCUMENTS_FILTER = "R";
  private static final String RETURN_FILLED_DOCUMENTS_FILTER = "RF";

  private MobilityDao mobilityDao;
  private NominatedStudentDao nominatedStudentDao;
  private DenialReasonDao denialReasonDao;
  private MobilityDocumentDao mobilityDocumentDao;
  private PartnerUcc partnerUcc;
  private ProgrammeUcc programmeUcc;
  private UserDao userDao;
  private UnitOfWork unitOfWork;

  @Inject
  MobilityUccImpl(MobilityDao mobilityDao, NominatedStudentDao nominatedStudentDao,
      DenialReasonDao denialReasonDao, MobilityDocumentDao mobilityDocumentDao,
      PartnerUcc partnerUcc, ProgrammeUcc programmeUcc, UserDao userDao, UnitOfWork unitOfWork) {
    this.mobilityDao = mobilityDao;
    this.nominatedStudentDao = nominatedStudentDao;
    this.denialReasonDao = denialReasonDao;
    this.mobilityDocumentDao = mobilityDocumentDao;
    this.partnerUcc = partnerUcc;
    this.programmeUcc = programmeUcc;
    this.unitOfWork = unitOfWork;
    this.userDao = userDao;
  }

  @Override
  @Role({UserDto.ROLE_STUDENT, UserDto.ROLE_PROFESSOR})
  @Route(method = HttpMethod.GET)
  public Map<String, Object> showAll(@SessionParameter(SessionUcc.USER_ID) int userId,
      @SessionParameter(SessionUcc.USER_ROLE) String userRole) {
    checkPositive(userId);
    checkString(userRole);
    try {
      unitOfWork.startTransaction();
      List<MobilityDto> mobilities;
      if (userRole.equals(UserDto.ROLE_PROFESSOR)) {
        // Lists all mobilities
        mobilities = mobilityDao.findAll();
      } else {
        // Lists mobilities owned by the requester
        mobilities = mobilityDao.findByUser(userId);
      }
      Map<String, Object> data = new HashMap<String, Object>();
      data.put("data", mobilities); // Datatables compatible
      unitOfWork.commit();
      return data;
    } catch (Exception ex) {
      unitOfWork.rollback();
      throw ex;
    }
  }

  @Override
  @Role({UserDto.ROLE_STUDENT, UserDto.ROLE_PROFESSOR})
  @Route(method = HttpMethod.GET, template = "/{id}")
  public MobilityDto showOne(@PathParameter("id") int id,
      @SessionParameter(SessionUcc.USER_ROLE) String role,
      @SessionParameter(SessionUcc.USER_ID) int user) {
    checkString(role);
    checkPositive(user);
    try {
      unitOfWork.startTransaction();
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
      mobility.setPartner(partnerUcc.showOne(mobility.getPartner().getId()));
      mobility.setProgramme(programmeUcc.showOne(mobility.getProgramme().getId()));
      mobility.setProfessorInCharge(userDao.findById(mobility.getProfessorInCharge().getId()));
      unitOfWork.commit();
      return mobility;
    } catch (Exception ex) {
      unitOfWork.rollback();
      throw ex;
    }
  }

  @Override
  @Role({UserDto.ROLE_PROFESSOR})
  @Route(method = HttpMethod.PUT, template = "/{id}/confirmProEcoEncoding")
  public void confirmProEcoEncoding(@PathParameter("id") int id,
      @HttpParameter("version") int version) {
    confirmSoftwareEncoding(id, SOFTWARE_PRO_ECO, version);
  }

  @Override
  @Role({UserDto.ROLE_PROFESSOR})
  @Route(method = HttpMethod.PUT, template = "/{id}/confirmSecondSoftwareEncoding")
  public void confirmSecondSoftwareEncoding(@PathParameter("id") int id,
      @HttpParameter("version") int version) {
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
    checkPositive(software);
    try {
      unitOfWork.startTransaction();
      Mobility mobility = getMobility(id);
      if (mobility.getVersion() != version) {
        throw new ConcurrentModificationException();
      }
      mobility.checkNotCancelled();
      if (software == SOFTWARE_PRO_ECO) {
        if (mobility.isEncodedInProEco()) {
          unitOfWork.commit();
          return;
        }
        mobility.setProEcoEncoding(true);
      } else {
        if (mobility.isEncodedInSecondSoftware()) {
          unitOfWork.commit();
          return;
        }
        mobility.setSecondSoftwareEncoding(true);
      }
      mobilityDao.update(mobility);
      unitOfWork.commit();
    } catch (Exception ex) {
      unitOfWork.rollback();
      throw ex;
    }
  }

  @Override
  @Role({UserDto.ROLE_PROFESSOR})
  @Route(method = HttpMethod.PUT, template = "/{id}/confirmPayment")
  public MobilityDto confirmPayment(@PathParameter("id") int id,
      @HttpParameter("version") int version) {
    checkPositive(id);
    try {
      unitOfWork.startTransaction();
      Mobility mobility = getMobility(id);
      if (mobility.getVersion() != version) {
        throw new ConcurrentModificationException();
      }
      mobility.checkNotCancelledAndNotClosed();
      NominatedStudent nominatedStudent;
      if ((nominatedStudent = (NominatedStudent) nominatedStudentDao
          .findById(mobility.getNominatedStudent().getId())) == null) {
        throw new BusinessException(ErrorFormat.INCOMPLETE_BANK_DETAILS_506);
      }
      nominatedStudent.checkBankDetails();
      if (mobility.getFirstPaymentRequestDate() == null) {
        mobility.setFirstPaymentRequestDate(LocalDateTime.now());
        mobility.setState(Mobility.STATE_IN_PROGRESS);
      } else if (mobility.getState().equals(Mobility.STATE_BALANCE_TO_BE_PAID)) {
        mobility.setSecondPaymentRequestDate(LocalDateTime.now());
        mobility.setState(Mobility.STATE_CLOSED);
      } else {
        throw new BusinessException(ErrorFormat.INVALID_INPUT_DATA_110);
      }
      mobilityDao.update(mobility);
      unitOfWork.commit();
      return mobility;
    } catch (Exception ex) {
      unitOfWork.rollback();
      throw ex;
    }
  }

  @Override
  @Role({UserDto.ROLE_PROFESSOR})
  @Route(method = HttpMethod.PUT, template = "/{id}/confirmDocument")
  public MobilityDto confirmDocument(@PathParameter("id") int id,
      @HttpParameter("document") int document, @HttpParameter("version") int version) {
    checkPositive(document);
    try {
      unitOfWork.startTransaction();
      Mobility mobility = getMobility(id);
      if (mobility.getVersion() != version) {
        throw new ConcurrentModificationException();
      }
      mobility.setDocuments(mobilityDocumentDao.findAllByMobility(mobility.getId()));
      if (!mobility.fillInDocument(document)) {
        unitOfWork.commit();
        return mobility;
      }
      String currentState = mobility.getState();
      if (currentState.equals(Mobility.STATE_CREATED)) {
        mobility.setState(Mobility.STATE_IN_PREPARATION);
      } else if (currentState.equals(Mobility.STATE_IN_PREPARATION)) {
        if (mobility.allDepartureDocumentsFilledIn()) {
          mobility.setState(Mobility.STATE_TO_BE_PAID);
        }
      } else if (currentState.equals(Mobility.STATE_IN_PROGRESS)) {
        if (mobility.allDepartureDocumentsFilledIn() && mobility.allReturnDocumentsFilledIn()) {
          mobility.setState(Mobility.STATE_BALANCE_TO_BE_PAID);
        }
      }
      mobilityDocumentDao.fillInDocument(document, mobility.getId());
      mobility = (Mobility) mobilityDao.update(mobility);
      unitOfWork.commit();
      return mobility;
    } catch (Exception ex) {
      unitOfWork.rollback();
      throw ex;
    }
  }

  @Override
  @Role({UserDto.ROLE_STUDENT, UserDto.ROLE_PROFESSOR})
  @Route(method = HttpMethod.PUT, template = "/{id}/cancel")
  public MobilityDto cancel(@PathParameter("id") int id, @HttpParameter("version") int version,
      @HttpParameter("cancellationReason") String cancellationReason,
      @HttpParameter("denialReason") int denialReasonId,
      @SessionParameter(SessionUcc.USER_ID) int userId,
      @SessionParameter(SessionUcc.USER_ROLE) String userRole) {
    checkString(userRole);
    checkPositive(userId);
    try {
      unitOfWork.startTransaction();
      Mobility mobility = getMobility(id);
      if (mobility.getVersion() != version) {
        throw new ConcurrentModificationException();
      }
      mobility.checkNotClosed();
      // The mobility is already cancelled
      if (mobility.getState().equals(Mobility.STATE_CANCELLED)) {
        unitOfWork.commit();
        return mobility;
      }
      // The student does not own the mobility
      if (userRole.equals(UserDto.ROLE_STUDENT)
          && userId != mobility.getNominatedStudent().getId()) {
        throw new InsufficientPermissionException();
      }
      // The mobility can be cancelled
      String stateBeforeCancellation = mobility.getState();
      if (userRole.equals(UserDto.ROLE_PROFESSOR)) {
        checkPositive(denialReasonId);
        DenialReason denialReason = (DenialReason) denialReasonDao.findById(denialReasonId);
        if (denialReason == null) {
          throw new BusinessException(ErrorFormat.EXISTENCE_VIOLATION_DENIAL_REASON_ID_400);
        }
        mobility.setDenialReason(denialReason);
      } else {
        checkString(cancellationReason);
        mobility.setCancellationReason(cancellationReason);
      }
      // Updating the state and the state before cancellation
      mobility.setStateBeforeCancellation(stateBeforeCancellation);
      mobility.setState(Mobility.STATE_CANCELLED);
      MobilityDto mobi = mobilityDao.update(mobility);
      unitOfWork.commit();
      return mobi;
    } catch (Exception ex) {
      unitOfWork.rollback();
      throw ex;
    }
  }

  @Override
  @Role(UserDto.ROLE_PROFESSOR)
  @Route(method = HttpMethod.GET, template = "/{id}/export", contentType = "text/csv")
  public String exportDocuments(@PathParameter("id") int mobilityId,
      @HttpParameter("filter") String filter) {
    checkFilter(filter);
    filter = filter == null ? "" : filter;
    try {
      unitOfWork.startTransaction();
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
        char category = document.getCategory();
        if (filter.equals(DEPARTURE_DOCUMENTS_FILTER) && filter.charAt(0) == category) {
          documentsInMobility.put(document.getName(),
              document.isFilledIn() ? "Rempli" : "Non-rempli");
        } else if (filter.equals(DEPARTURE_FILLED_DOCUMENTS_FILTER) && filter.charAt(0) == category
            && document.isFilledIn()) {
          documentsInMobility.put(document.getName(),
              document.isFilledIn() ? "Rempli" : "Non-rempli");
        } else if (filter.equals(RETURN_DOCUMENTS_FILTER) && filter.charAt(0) == category) {
          documentsInMobility.put(document.getName(),
              document.isFilledIn() ? "Rempli" : "Non-rempli");
        } else if (filter.equals(RETURN_FILLED_DOCUMENTS_FILTER) && filter.charAt(0) == category
            && document.isFilledIn()) {
          documentsInMobility.put(document.getName(),
              document.isFilledIn() ? "Rempli" : "Non-rempli");
        } else {
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
      unitOfWork.commit();
      return csvStringBuilder.close();
    } catch (Exception ex) {
      unitOfWork.rollback();
      throw ex;
    }
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
    entries[2] = partner.getFullName();
    entries[3] = mobility.getMobilityType();
    entries[4] = mobility.getProgramme().getProgrammeName();
    entries[5] = mobility.getTerm() + "";
    return entries;
  }

  /**
   * Check if a filter for the document filter is correct or not. If a filter isn't correct it throw
   * the appropriate business exception.
   * 
   * @param filter the filter to test.
   */
  private void checkFilter(String filter) {
    if (filter != null && !filter.equals(DEPARTURE_DOCUMENTS_FILTER)
        && !filter.equals(DEPARTURE_FILLED_DOCUMENTS_FILTER)
        && !filter.equals(RETURN_DOCUMENTS_FILTER)
        && !filter.equals(RETURN_FILLED_DOCUMENTS_FILTER)) {
      throw new BusinessException(ErrorFormat.INVALID_DOCUMENT_FILTER_508);
    }
  }

  /**
   * Retrieve a mobility with the id passed in parameter.
   * 
   * @param mobilityId the id of the mobility we want to retrieve
   * @return a Mobility for the id passed in parameter.
   */
  private Mobility getMobility(int mobilityId) {
    checkPositive(mobilityId);
    Mobility mobility = null;
    if ((mobility = (Mobility) mobilityDao.findById(mobilityId)) == null) {
      throw new RessourceNotFoundException();
    }
    return mobility;
  }

}
