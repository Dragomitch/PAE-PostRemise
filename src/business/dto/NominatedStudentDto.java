package business.dto;

import java.time.LocalDate;

public interface NominatedStudentDto extends UserDto {

  String getTitle();

  void setTitle(String title);

  LocalDate getBirthdate();

  void setBirthdate(LocalDate birthdate);

  CountryDto getNationality();

  void setNationality(CountryDto nationality);

  AddressDto getAddress();

  void setAddress(AddressDto address);

  String getPhoneNumber();

  void setPhoneNumber(String phoneNumber);

  String getGender();

  void setGender(String gender);

  int getNbrPassedYears();

  void setNbrPassedYears(int nbrPassedYears);

  String getIban();

  void setIban(String iban);

  String getCardHolder();

  void setCardHolder(String cardHolder);

  String getBankName();

  void setBankName(String bankName);

  String getBic();

  void setBic(String bic);

}
