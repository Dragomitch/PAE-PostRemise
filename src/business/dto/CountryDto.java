package business.dto;

public interface CountryDto {

  String getCountryCode();

  void setCountryCode(String countryCode);

  String getName();

  void setName(String name);

  ProgrammeDto getProgramme();

  void setProgramme(ProgrammeDto programme);

  int getVersion();

  void setVersion(int version);

}
