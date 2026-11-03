package com.dragomitch.ipl.pae.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dragomitch.ipl.pae.UnitTestConfig;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryNominatedStudentRepository;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryUserRepository;
import com.dragomitch.ipl.pae.domain.model.NominatedStudent;
import com.dragomitch.ipl.pae.domain.model.Role;
import com.dragomitch.ipl.pae.domain.model.User;
import com.dragomitch.ipl.pae.domain.port.in.NominatedStudentService;
import com.dragomitch.ipl.pae.domain.port.out.NominatedStudentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(UnitTestConfig.class)
public class TestNominatedStudentService {

  @Autowired private ApplicationContext context;

  private NominatedStudentRepository nominatedStudentRepository;
  private NominatedStudentService nominatedStudentService;
  private MockDtoFactory mockDtoFactory;
  private NominatedStudent nominatedStud;

  /** Sets up the environment before every test. */
  @BeforeEach
  public void setUp() {
    nominatedStudentRepository = context.getBean(NominatedStudentRepository.class);
    nominatedStudentService = context.getBean(NominatedStudentService.class);
    mockDtoFactory = new MockDtoFactory();
    nominatedStud = mockDtoFactory.getNominatedStudent();
  }

  /** Cleans up the 'database'. */
  @AfterEach
  public void cleanUp() {
    ((InMemoryNominatedStudentRepository) nominatedStudentRepository).empty();
  }

  @Test
  public void createKeepsTheVersionOfTheUserAndReturnsIt() {
    InMemoryUserRepository userRepository = context.getBean(InMemoryUserRepository.class);
    userRepository.empty();
    User user = userRepository.create(mockDtoFactory.getUser(Role.STUDENT));
    user.setVersion(3);
    NominatedStudent student = validStudent(user.getId());

    NominatedStudent created = nominatedStudentService.create(student, user.getId(), Role.STUDENT);

    assertEquals(3, created.getVersion(), "the returned version is the stored one");
    assertEquals(3, nominatedStudentRepository.findById(user.getId()).getVersion());
    userRepository.empty();
  }

  private NominatedStudent validStudent(int userId) {
    NominatedStudent student = mockDtoFactory.getNominatedStudent();
    student.setId(userId);
    student.setIban("BE68539007547034");
    student.setBic("GKCCBEBB");
    student.setBirthdate(java.time.LocalDate.of(2000, 1, 1));
    return student;
  }
}
