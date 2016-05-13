package business.implementations;

import static utils.DataValidationUtils.isAValidObject;
import static utils.DataValidationUtils.isAValidString;
import static utils.DataValidationUtils.isPositive;

import business.MobilityChoice;
import business.dto.CountryDto;
import business.dto.DenialReasonDto;
import business.dto.PartnerDto;
import business.dto.ProgrammeDto;
import business.dto.UserDto;
import business.exceptions.BusinessException;
import business.exceptions.ErrorFormat;
import persistence.DaoClass;
import persistence.MobilityChoiceDao;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

@DaoClass(MobilityChoiceDao.class)
class MobilityChoiceImpl implements MobilityChoice, Serializable {

  private static final long serialVersionUID = 1L;

  private int id;
  private UserDto user;
  private int preferenceOrder;
  private String mobilityType;
  private int academicYear;
  private int term;
  private ProgrammeDto programme;
  private CountryDto country;
  private LocalDateTime submissionDate;
  private DenialReasonDto denialReason;
  private String cancellationReason;
  private PartnerDto partner;
  private int version;

  @Override
  public int getId() {
    return id;
  }

  @Override
  public void setId(int id) {
    this.id = id;
  }

  @Override
  public UserDto getUser() {
    return user;
  }

  @Override
  public void setUser(UserDto user) {
    this.user = user;
  }

  @Override
  public int getPreferenceOrder() {
    return preferenceOrder;
  }

  @Override
  public void setPreferenceOrder(int preferenceOrder) {
    this.preferenceOrder = preferenceOrder;
  }

  @Override
  public String getMobilityType() {
    return mobilityType;
  }

  @Override
  public void setMobilityType(String mobilityType) {
    this.mobilityType = mobilityType;
  }

  @Override
  public int getAcademicYear() {
    return academicYear;
  }

  @Override
  public void setAcademicYear(int academicYear) {
    this.academicYear = academicYear;
  }

  @Override
  public int getTerm() {
    return term;
  }

  @Override
  public void setTerm(int term) {
    this.term = term;
  }

  @Override
  public ProgrammeDto getProgramme() {
    return programme;
  }

  @Override
  public void setProgramme(ProgrammeDto programme) {
    this.programme = programme;
  }

  @Override
  public CountryDto getCountry() {
    return country;
  }

  @Override
  public void setCountry(CountryDto country) {
    this.country = country;
  }

  @Override
  public LocalDateTime getSubmissionDate() {
    return submissionDate;
  }

  @Override
  public void setSubmissionDate(LocalDateTime submissionDate) {
    this.submissionDate = submissionDate;
  }

  @Override
  public DenialReasonDto getDenialReason() {
    return denialReason;
  }

  @Override
  public void setDenialReason(DenialReasonDto denialReason) {
    this.denialReason = denialReason;
  }

  @Override
  public String getCancellationReason() {
    return cancellationReason;
  }

  @Override
  public void setCancellationReason(String cancellationReason) {
    this.cancellationReason = cancellationReason;
  }

  @Override
  public PartnerDto getPartner() {
    return partner;
  }

  @Override
  public void setPartner(PartnerDto partner) {
    this.partner = partner;
  }

  @Override
  public int getVersion() {
    return version;
  }

  @Override
  public void setVersion(int version) {
    this.version = version;
  }

  @Override
  public void checkDataIntegrity() {
    List<Integer> violations = new LinkedList<Integer>();
    if (!isPositive(preferenceOrder)) {
      violations.add(ErrorFormat.INVALID_PREFERENCE_ORDER_302);
    } else if (preferenceOrder > MobilityChoice.MAX_ORDER_CHOICE) {
      violations.add(ErrorFormat.INVALID_PREFERENCE_ORDER_VALUE_303);
    }
    if (!isAValidString(mobilityType)) {
      violations.add(ErrorFormat.INVALID_MOBILITY_TYPE_304);
    } else if (!mobilityType.equals(MobilityChoice.MOBILITY_TYPE_SMS)
        && !mobilityType.equals(MobilityChoice.MOBILITY_TYPE_SMP)) {
      violations.add(ErrorFormat.INVALID_MOBILITY_TYPE_VALUE_306);
    }
    if (!isPositive(academicYear)) {
      violations.add(ErrorFormat.INVALID_ACADEMIC_YEAR_307);
    }
    if (!isPositive(term)) {
      violations.add(ErrorFormat.INVALID_TERM_308);
    } else if (term > MobilityChoice.MAX_TERM_CHOICE) {
      violations.add(ErrorFormat.INVALID_TERM_VALUE_309);
    }
    if (!isAValidObject(user)) {
      violations.add(ErrorFormat.EXISTENCE_VIOLATION_USER_NULL_132);
    } else if (!isPositive(user.getId())) {
      violations.add(ErrorFormat.EXISTENCE_VIOLATION_USER_ID_200);
    }
    if (!isAValidObject(programme)) {
      violations.add(ErrorFormat.EXISTENCE_VIOLATION_PROGRAMME_NULL_140);
    } else if (!isPositive(programme.getId())) {
      violations.add(ErrorFormat.EXISTENCE_VIOLATION_PROGRAMME_ID_1000);
    }
    if (violations.size() > 0) {
      throw new BusinessException(ErrorFormat.INVALID_INPUT_DATA_110, violations);
    }
  }
}
