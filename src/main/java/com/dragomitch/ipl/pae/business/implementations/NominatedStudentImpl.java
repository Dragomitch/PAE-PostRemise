package com.dragomitch.ipl.pae.business.implementations;

import com.dragomitch.ipl.pae.business.NominatedStudent;
import com.dragomitch.ipl.pae.business.dto.AddressDto;
import com.dragomitch.ipl.pae.business.dto.CountryDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.ErrorCode;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import org.springframework.util.StringUtils;

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
    if (!StringUtils.hasText(iban) || !StringUtils.hasText(bankName)
        || !StringUtils.hasText(bic)) {
      throw new BusinessException(ErrorCode.INCOMPLETE_BANK_DETAILS);
    }
  }

}
