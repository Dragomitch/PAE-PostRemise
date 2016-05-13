package business.implementations;

import business.Payment;
import business.dto.CountryDto;
import business.dto.NominatedStudentDto;
import business.dto.PartnerDto;
import business.dto.ProgrammeDto;
import business.dto.UserDto;
import persistence.DaoClass;
import persistence.PaymentDao;

import com.owlike.genson.annotation.JsonIgnore;

import java.io.Serializable;
import java.time.LocalDateTime;

@DaoClass(PaymentDao.class)
public class PaymentImpl implements Payment, Serializable {

  private static final long serialVersionUID = 1L;

  private int mobilityChoiceId;
  private UserDto user;
  private NominatedStudentDto nominatedStudent;
  private String mobilityType;
  private String academicYear;
  private int term;
  private ProgrammeDto programme;
  private CountryDto country;
  private PartnerDto partner;
  private LocalDateTime paymentDate;
  private String paymentType;
  @JsonIgnore
  private int version;

  @Override
  public int getMobilityChoiceId() {
    return mobilityChoiceId;
  }

  @Override
  public void setMobilityChoiceId(int mobilityChoiceId) {
    this.mobilityChoiceId = mobilityChoiceId;
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
  public NominatedStudentDto getNominatedStudent() {
    return nominatedStudent;
  }

  @Override
  public void setNominatedStudent(NominatedStudentDto nominatedStudent) {
    this.nominatedStudent = nominatedStudent;
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
  public String getAcademicYear() {
    return academicYear;
  }

  @Override
  public void setAcademicYear(String academicYear) {
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
  public PartnerDto getPartner() {
    return partner;
  }

  @Override
  public void setPartner(PartnerDto partner) {
    this.partner = partner;
  }

  @Override
  public LocalDateTime getPaymentDate() {
    return paymentDate;
  }

  @Override
  public void setPaymentDate(LocalDateTime paymentDate) {
    this.paymentDate = paymentDate;
  }

  @Override
  public String getPaymentType() {
    return paymentType;
  }

  @Override
  public void setPaymentType(String paymentType) {
    this.paymentType = paymentType;
  }

  @Override
  public int getVersion() {
    return version;
  }

  @Override
  public void setVersion(int version) {
    this.version = version;
  }

  public static long getSerialversionuid() {
    return serialVersionUID;
  }

}
