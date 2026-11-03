package com.dragomitch.ipl.pae.adapter.in.web.mapping;

import com.dragomitch.ipl.pae.adapter.in.web.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.adapter.in.web.dto.UserDto;
import com.dragomitch.ipl.pae.domain.model.NominatedStudent;
import com.dragomitch.ipl.pae.domain.model.User;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Maps the users and the nominated students between the web DTOs and the domain. The password is
 * mapped both ways: the JSON reads it (sign-up) but never writes it. The role of a request is
 * ignored: the use cases set it (sign-up) or keep the stored one (edition).
 */
@Component
public class UserMapper {

  private final ReferenceDataMapper referenceData;
  private final AddressMapper addresses;

  public UserMapper(ReferenceDataMapper referenceData, AddressMapper addresses) {
    this.referenceData = referenceData;
    this.addresses = addresses;
  }

  public @Nullable UserDto toDto(@Nullable User user) {
    return user == null
        ? null
        : new UserDto(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getUsername(),
            user.getPassword(),
            user.getEmail(),
            referenceData.toDto(user.getOption()),
            user.getRegistrationDate(),
            user.getRole() == null ? null : user.getRole().label(),
            user.getVersion());
  }

  public List<UserDto> toUserDtos(List<User> users) {
    return ReferenceDataMapper.list(users, this::toDto);
  }

  public @Nullable User toDomain(@Nullable UserDto dto) {
    if (dto == null) {
      return null;
    }
    var user = new User();
    user.setId(dto.id());
    user.setFirstName(dto.firstName());
    user.setLastName(dto.lastName());
    user.setUsername(dto.username());
    user.setPassword(dto.password());
    user.setEmail(dto.email());
    user.setOption(referenceData.toDomain(dto.option()));
    user.setRegistrationDate(dto.registrationDate());
    user.setVersion(dto.version());
    return user;
  }

  public @Nullable NominatedStudentDto toDto(@Nullable NominatedStudent student) {
    return student == null
        ? null
        : new NominatedStudentDto(
            student.getId(),
            student.getFirstName(),
            student.getLastName(),
            student.getUsername(),
            student.getPassword(),
            student.getEmail(),
            referenceData.toDto(student.getOption()),
            student.getRegistrationDate(),
            student.getRole() == null ? null : student.getRole().label(),
            student.getTitle(),
            student.getBirthdate(),
            referenceData.toDto(student.getNationality()),
            addresses.toDto(student.getAddress()),
            student.getPhoneNumber(),
            student.getGender(),
            student.getNbrPassedYears(),
            student.getIban(),
            student.getCardHolder(),
            student.getBankName(),
            student.getBic(),
            student.getVersion());
  }

  public List<NominatedStudentDto> toNominatedStudentDtos(List<NominatedStudent> students) {
    return ReferenceDataMapper.list(students, this::toDto);
  }

  public @Nullable NominatedStudent toDomain(@Nullable NominatedStudentDto dto) {
    if (dto == null) {
      return null;
    }
    var student = new NominatedStudent();
    student.setId(dto.id());
    student.setFirstName(dto.firstName());
    student.setLastName(dto.lastName());
    student.setUsername(dto.username());
    student.setPassword(dto.password());
    student.setEmail(dto.email());
    student.setOption(referenceData.toDomain(dto.option()));
    student.setRegistrationDate(dto.registrationDate());
    student.setTitle(dto.title());
    student.setBirthdate(dto.birthdate());
    student.setNationality(referenceData.toDomain(dto.nationality()));
    student.setAddress(addresses.toDomain(dto.address()));
    student.setPhoneNumber(dto.phoneNumber());
    student.setGender(dto.gender());
    student.setNbrPassedYears(dto.nbrPassedYears());
    student.setIban(dto.iban());
    student.setCardHolder(dto.cardHolder());
    student.setBankName(dto.bankName());
    student.setBic(dto.bic());
    student.setVersion(dto.version());
    return student;
  }
}
