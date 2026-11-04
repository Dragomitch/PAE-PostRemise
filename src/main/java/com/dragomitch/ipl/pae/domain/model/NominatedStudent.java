package com.dragomitch.ipl.pae.domain.model;

import com.dragomitch.ipl.pae.domain.exceptions.BusinessException;
import com.dragomitch.ipl.pae.domain.exceptions.ErrorCode;
import java.time.LocalDate;

/**
 * A student nominated for a mobility: his user account and his personal and bank data. Both share
 * the id and the version of the user.
 */
public class NominatedStudent extends User {

  /** Longest card holder ({@code nominated_students.card_holder}). */
  public static final int CARD_HOLDER_MAX_LENGTH = 35;

  private String title;
  private LocalDate birthdate;
  private Country nationality;
  private Address address;
  private String phoneNumber;
  private String gender;
  private int nbrPassedYears;
  private String iban;
  private String cardHolder;
  private String bankName;
  private String bic;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public LocalDate getBirthdate() {
    return birthdate;
  }

  public void setBirthdate(LocalDate birthdate) {
    this.birthdate = birthdate;
  }

  public Country getNationality() {
    return nationality;
  }

  public void setNationality(Country nationality) {
    this.nationality = nationality;
  }

  public Address getAddress() {
    return address;
  }

  public void setAddress(Address address) {
    this.address = address;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  public String getGender() {
    return gender;
  }

  public void setGender(String gender) {
    this.gender = gender;
  }

  public int getNbrPassedYears() {
    return nbrPassedYears;
  }

  public void setNbrPassedYears(int nbrPassedYears) {
    this.nbrPassedYears = nbrPassedYears;
  }

  public String getIban() {
    return iban;
  }

  public void setIban(String iban) {
    this.iban = iban;
  }

  public String getCardHolder() {
    return cardHolder;
  }

  public void setCardHolder(String cardHolder) {
    this.cardHolder = cardHolder;
  }

  public String getBankName() {
    return bankName;
  }

  public void setBankName(String bankName) {
    this.bankName = bankName;
  }

  public String getBic() {
    return bic;
  }

  public void setBic(String bic) {
    this.bic = bic;
  }

  /**
   * Checks that the bank details needed to pay the student are known.
   *
   * @throws BusinessException {@code INCOMPLETE_BANK_DETAILS} if the IBAN, the BIC or the bank name
   *     is missing
   */
  public void checkBankDetails() {
    if (isBlank(iban) || isBlank(bankName) || isBlank(bic)) {
      throw new BusinessException(ErrorCode.INCOMPLETE_BANK_DETAILS);
    }
  }

  /**
   * The holder of the bank account: the student himself (first and last names, cut to {@value
   * #CARD_HOLDER_MAX_LENGTH} characters) when none is given.
   */
  public void defaultCardHolder() {
    if (isBlank(cardHolder)) {
      String holder = getFirstName() + " " + getLastName();
      cardHolder =
          holder.length() > CARD_HOLDER_MAX_LENGTH
              ? holder.substring(0, CARD_HOLDER_MAX_LENGTH)
              : holder;
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
