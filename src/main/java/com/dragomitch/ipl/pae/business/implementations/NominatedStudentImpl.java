package com.dragomitch.ipl.pae.business.implementations;

import static com.dragomitch.ipl.pae.utils.DataValidationUtils.isAValidBic;
import static com.dragomitch.ipl.pae.utils.DataValidationUtils.isAValidIban;
import static com.dragomitch.ipl.pae.utils.DataValidationUtils.isAValidObject;
import static com.dragomitch.ipl.pae.utils.DataValidationUtils.isAValidString;
import static com.dragomitch.ipl.pae.utils.DataValidationUtils.isPositive;

import com.dragomitch.ipl.pae.business.NominatedStudent;
import com.dragomitch.ipl.pae.business.dto.AddressDto;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.ErrorFormat;
import com.dragomitch.ipl.pae.persistence.CountryDao;
import com.dragomitch.ipl.pae.persistence.DaoClass;
import com.dragomitch.ipl.pae.persistence.NominatedStudentDao;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;

@DaoClass(NominatedStudentDao.class)
class NominatedStudentImpl extends UserImpl implements NominatedStudent {
  private static final long serialVersionUID = 1L;

  private String title;
  private LocalDate birthdate;
  private CountryDto nationality;
  private AddressDto address;
  private String phoneNumber;
  private String gender;
  private int nbrPassedYears;
  private String iban;
  private String cardHolder;
  private String bankName;
  private String bic;

  @Override
  public String getTitle() {
    return title;
  }

  @Override
  public void setTitle(String title) {
    this.title = title;
  }

  @Override
  @JsonFormat(pattern = "yyyy-MM-dd")
  public LocalDate getBirthdate() {
    return birthdate;
  }

  @Override
  public void setBirthdate(LocalDate birthdate) {
    this.birthdate = birthdate;
  }

  @Override
  public CountryDto getNationality() {
    return nationality;
  }

  @Override
  public void setNationality(CountryDto nationality) {
    this.nationality = nationality;
  }

  @Override
  public AddressDto getAddress() {
    return address;
  }

  @Override
  public void setAddress(AddressDto address) {
    this.address = address;
  }

  @Override
  public String getPhoneNumber() {
    return phoneNumber;
  }

  @Override
  public void setPhoneNumber(String tel) {
    this.phoneNumber = tel;
  }

  @Override
  public String getGender() {
    return gender;
  }

  @Override
  public void setGender(String gender) {
    this.gender = gender;
  }

  @Override
  public int getNbrPassedYears() {
    return nbrPassedYears;
  }

  @Override
  public void setNbrPassedYears(int nbrPassedYears) {
    this.nbrPassedYears = nbrPassedYears;
  }

  @Override
  public String getIban() {
    return iban;
  }

  @Override
  public void setIban(String iban) {
    this.iban = iban;
  }

  @Override
  public String getCardHolder() {
    return cardHolder;
  }

  @Override
  public void setCardHolder(String cardHolder) {
    this.cardHolder = cardHolder;
  }

  @Override
  public String getBankName() {
    return bankName;
  }

  @Override
  public void setBankName(String bankName) {
    this.bankName = bankName;
  }

  @Override
  public String getBic() {
    return bic;
  }

  @Override
  public void setBic(String bic) {
    this.bic = bic;
  }

  @Override
  public void checkBankDetails() {
    if (!isAValidString(iban) || !isAValidString(bankName) || !isAValidString(bic)) {
      throw new BusinessException(ErrorFormat.INCOMPLETE_BANK_DETAILS_506);
    }
  }

  @Override
  public void checkDataIntegrity() {
    List<Integer> violations = new LinkedList<Integer>();
    if (!isAValidString(title)) {
      violations.add(ErrorFormat.INVALID_TITLE_601);
    } else if (title.length() > NominatedStudentDao.TITLE_MAX_LENGTH) {
      violations.add(ErrorFormat.TITLE_MAX_LENGTH_OVERFLOW_603);
    } else if (!title.equals("Mr") && !title.equals("M") && !title.equals("Mrs")
        && !title.equals("Ms")) {
      violations.add(ErrorFormat.INVALID_TITLE_VALUE_602);
    }
    if (!isAValidObject(birthdate)) {
      violations.add(ErrorFormat.INVALID_BIRTHDATE_NULL_604);
    } else if (!birthdate.isBefore(LocalDate.now())) {
      violations.add(ErrorFormat.INVALID_BIRTHDATE_605);
    }
    if (!isAValidObject(nationality)) {
      violations.add(ErrorFormat.INVALID_NATIONALITY_NULL_606);
    } else if (!isAValidString(nationality.getCountryCode())) {
      violations.add(ErrorFormat.EXISTENCE_VIOLATION_COUNTRY_CODE_900);
    } else if (nationality.getCountryCode().length() != CountryDao.CODE_LENGTH) {
      violations.add(ErrorFormat.MAX_LENGTH_COUNTRY_CODE_OVERFLOW_314);
    }
    if (!isAValidObject(address)) {
      violations.add(ErrorFormat.INVALID_ADDRESS_NULL_608);
    }
    if (!isAValidString(phoneNumber)) {
      violations.add(ErrorFormat.INVALID_PHONE_NUMBER_619);
    } else if (phoneNumber.length() > NominatedStudentDao.PHONE_NUMBER_MAX_LENGTH) {
      violations.add(ErrorFormat.PHONE_NUMBER_MAX_LENGTH_OVERFLOW_609);
    }
    if (!isAValidString(gender)) {
      violations.add(ErrorFormat.INVALID_GENDER_610);
    } else if (!gender.equals("M") && !gender.equals("F") && !gender.equals("O")) {
      violations.add(ErrorFormat.INVALID_GENDER_VALUE_611);
    }
    if (!isPositive(nbrPassedYears)) {
      violations.add(ErrorFormat.INVALID_NUMBER_OF_PASSED_YEARS_612);
    }
    if (!isAValidIban(iban)) {
      violations.add(ErrorFormat.INVALID_IBAN_613);
    }
    if (!isAValidString(cardHolder)) {
      setCardHolder(super.getFirstName() + " " + super.getLastName());
    } else if (cardHolder.length() > NominatedStudentDao.CARD_HOLDER_MAX_LENGTH) {
      violations.add(ErrorFormat.CARD_HOLDER_MAX_LENGTH_OVERFLOW_614);
    }
    if (!isAValidString(bankName)) {
      violations.add(ErrorFormat.INVALID_BANK_NAME_615);
    } else if (bankName.length() > NominatedStudentDao.BANK_NAME_MAX_LENGTH) {
      violations.add(ErrorFormat.BANK_NAME_MAX_LENGTH_OVERFLOW_616);
    }
    if (!isAValidBic(bic)) {
      violations.add(ErrorFormat.INVALID_BIC_617);
    }
    if (violations.size() > 0) {
      throw new BusinessException(ErrorFormat.INVALID_INPUT_DATA_110, violations);
    }
  }
}
