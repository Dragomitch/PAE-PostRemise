package com.dragomitch.ipl.pae.adapter.out.persistence;

import com.dragomitch.ipl.pae.adapter.out.persistence.entity.AddressEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.CountryEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.NominatedStudentEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.entity.UserEntity;
import com.dragomitch.ipl.pae.adapter.out.persistence.repository.NominatedStudentJpaRepository;
import com.dragomitch.ipl.pae.business.dto.AddressDto;
import com.dragomitch.ipl.pae.business.dto.NominatedStudentDto;
import com.dragomitch.ipl.pae.persistence.NominatedStudentDao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.ConcurrentModificationException;
import java.util.List;

import org.springframework.stereotype.Repository;

/** The personal data of the students nominated for a mobility. */
@Repository
class NominatedStudentPersistenceAdapter implements NominatedStudentDao {

  private final NominatedStudentJpaRepository students;
  private final PersistenceMapper mapper;

  @PersistenceContext
  private EntityManager entityManager;

  NominatedStudentPersistenceAdapter(NominatedStudentJpaRepository students,
      PersistenceMapper mapper) {
    this.students = students;
    this.mapper = mapper;
  }

  /**
   * Stores the personal data of the user (the DTO's id) with the DTO's version, and writes them
   * at once (their key is not generated).
   */
  @Override
  public NominatedStudentDto create(NominatedStudentDto nominatedStudent) {
    NominatedStudentEntity entity = new NominatedStudentEntity();
    entity.setUser(entityManager.getReference(UserEntity.class, nominatedStudent.getId()));
    entity.setAddress(
        entityManager.getReference(AddressEntity.class, nominatedStudent.getAddress().getId()));
    copy(nominatedStudent, entity);
    entity.setVersion(nominatedStudent.getVersion());
    entityManager.persist(entity);
    entityManager.flush();
    return nominatedStudent;
  }

  @Override
  public NominatedStudentDto findById(int id) {
    return students.findWithUserById(id).map(this::student).orElse(null);
  }

  @Override
  public List<NominatedStudentDto> findAll() {
    return students.findAllWithUserByOrderById().stream().map(this::student).toList();
  }

  /**
   * {@inheritDoc}
   *
   * <p>The address is not changed.
   *
   * @throws ConcurrentModificationException if the student does not exist or their version is
   *         not the DTO's (they were changed since they were read)
   */
  @Override
  public NominatedStudentDto update(NominatedStudentDto nominatedStudent) {
    NominatedStudentEntity entity = students.findById(nominatedStudent.getId())
        .filter(found -> found.getVersion() != null
            && found.getVersion() == nominatedStudent.getVersion())
        .orElseThrow(ConcurrentModificationException::new);
    copy(nominatedStudent, entity);
    Versions.flushIncrementingVersion(entityManager, entity, entity::getVersion,
        nominatedStudent.getVersion());
    nominatedStudent.setVersion(entity.getVersion());
    return nominatedStudent;
  }

  /** The columns a create and an update write. */
  private void copy(NominatedStudentDto student, NominatedStudentEntity entity) {
    entity.setTitle(student.getTitle());
    entity.setBirthdate(student.getBirthdate().atStartOfDay());
    entity.setNationality(entityManager.getReference(CountryEntity.class,
        student.getNationality().getCountryCode()));
    entity.setPhoneNumber(student.getPhoneNumber());
    entity.setGender(student.getGender());
    entity.setPassedYearsCount(student.getNbrPassedYears());
    entity.setIban(student.getIban());
    entity.setCardHolder(student.getCardHolder());
    entity.setBankName(student.getBankName());
    entity.setBic(student.getBic());
  }

  /**
   * A student: their user columns and option, their personal data, their nationality (code and
   * name) and their address (id only). The version is the personal data's.
   */
  private NominatedStudentDto student(NominatedStudentEntity entity) {
    UserEntity user = entity.getUser();
    NominatedStudentDto student = mapper.build(NominatedStudentDto.class);
    student.setId(user.getId());
    student.setLastName(user.getLastName());
    student.setFirstName(user.getFirstName());
    student.setUsername(user.getUsername());
    student.setPassword(user.getPassword());
    student.setEmail(user.getEmail());
    student.setRegistrationDate(user.getRegistrationDate());
    student.setRole(user.getRole());
    student.setOption(mapper.option(user.getOption()));
    student.setTitle(entity.getTitle());
    student.setBirthdate(entity.getBirthdate().toLocalDate());
    student.setNationality(mapper.countryNamed(entity.getNationality()));
    student.setPhoneNumber(entity.getPhoneNumber());
    student.setGender(entity.getGender());
    student.setNbrPassedYears(entity.getPassedYearsCount());
    student.setIban(entity.getIban());
    student.setCardHolder(entity.getCardHolder());
    student.setBankName(entity.getBankName());
    student.setBic(entity.getBic());
    AddressDto address = mapper.build(AddressDto.class);
    address.setId(entity.getAddress().getId());
    student.setAddress(address);
    student.setVersion(entity.getVersion() == null ? 0 : entity.getVersion());
    return student;
  }
}
