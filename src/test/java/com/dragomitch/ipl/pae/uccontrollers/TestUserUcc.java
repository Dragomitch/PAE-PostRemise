package com.dragomitch.ipl.pae.uccontrollers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dragomitch.ipl.pae.UnitTestConfig;
import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.Violations;
import com.dragomitch.ipl.pae.business.dto.OptionDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.persistence.UserDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockUserDao;
import com.dragomitch.ipl.pae.uccontrollers.UserUcc;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(UnitTestConfig.class)
public class TestUserUcc {

  @Autowired
  private ApplicationContext context;

  private EntityFactory entityFactory;
  private UserDao userDao;
  private UserUcc userUcc;
  private MockDtoFactory mockDtoFactory;
  private UserDto prof;
  private UserDto stud;


  /**
   * Sets up the environment before every test.
   */
  @BeforeEach
  public void setUp() {
    entityFactory = context.getBean(EntityFactory.class);
    userDao = context.getBean(UserDao.class);
    userUcc = context.getBean(UserUcc.class);
    mockDtoFactory = new MockDtoFactory(entityFactory);
    prof = mockDtoFactory.getUser(UserDto.ROLE_PROFESSOR);
    stud = mockDtoFactory.getUser(UserDto.ROLE_STUDENT);
  }

  /**
   * Cleans up the 'database'.
   */
  @AfterEach
  public void cleanUp() {
    ((MockUserDao) userDao).empty();
  }

  @Test
  public void testCreateTC1() {
    userUcc.signup(prof);
    prof.setId(1);
    assertEquals(prof.getId(), (userDao.findById(prof.getId())).getId());
  }

  @Test
  public void testCreateTC2() {
    assertThrows(ConstraintViolationException.class, () -> {
      userUcc.signup(null);
      prof.setId(1);
      assertEquals(prof.getId(), (userDao.findById(prof.getId())).getId());
    });
  }

  @Test
  public void testCreateTC3() {
    userUcc.signup(stud);
    userUcc.signup(stud);
    assertEquals(2, (userDao.findAll().size()));
  }

  @Test
  public void testCreateTC4() {
    assertEquals(List.of("signup.user.option.code:Size"),
        Violations.thrownBy(() -> {
          OptionDto option = mockDtoFactory.getOption();
          option.setCode("Wrong Code Man");
          stud.setOption(option);
          userUcc.signup(stud);
        }));
  }

  @Test
  public void testCreateTC5() {
    assertThrows(BusinessException.class, () -> {
      userUcc.signup(stud);
      userUcc.signup(prof);
    });
  }

  @Test
  public void testCreateTC6() {
    assertThrows(BusinessException.class, () -> {
      userUcc.signup(stud);
      prof.setUsername("chikiBriki");
      userUcc.signup(prof);
    });
  }

  @Test
  public void testCreateTC7() {
    assertThrows(BusinessException.class, () -> {
      userUcc.signup(stud);
      prof.setEmail("MamyFaitDesBlagues@joke.be");
      userUcc.signup(prof);
    });
  }

  @Test
  public void testShowAllTC1() {
    userUcc.signup(stud);
    prof.setUsername("chikiBriki");
    prof.setEmail("MamyFaitDesBlagues@joke.be");
    userUcc.signup(prof);
    assertEquals(2, userDao.findAll().size());
  }

  @Test
  public void testPromoteToProfessorTC1() {
    userUcc.signup(stud);
    userUcc.promoteToProfessor(stud.getId());
    assertEquals(UserDto.ROLE_PROFESSOR, stud.getRole());
  }

  @Test
  public void testPromoteToProfessorTC2() {
    assertThrows(ResourceNotFoundException.class, () -> {
      userUcc.signup(stud);
      userUcc.promoteToProfessor(69);
    });
  }

  @Test
  public void testPromoteToProfessorTC3() {
    assertThrows(ConstraintViolationException.class, () -> {
      userUcc.signup(stud);
      userUcc.promoteToProfessor(-1);
    });
  }

  @Test
  public void testEditTC1() {
    assertThrows(ConstraintViolationException.class, () -> {
      userUcc.signup(stud);
      userUcc.edit(null, 1, UserDto.ROLE_STUDENT);
    });
  }

  @Test
  public void testEditTC2() {
    assertThrows(ConstraintViolationException.class, () -> {
      userUcc.signup(stud);
      userUcc.edit(stud, -1, UserDto.ROLE_STUDENT);
    });
  }

  @Test
  public void testEditTC3() {
    assertThrows(ConstraintViolationException.class, () -> {
      userUcc.signup(stud);
      userUcc.edit(stud, -1, "");
    });
  }

  // Edit d'un User avec des données déjà existantes dans un autre User
  @Test
  public void testEditTC4() {
    assertThrows(BusinessException.class, () -> {
      userUcc.signup(prof);
      userUcc.edit(stud, 1, UserDto.ROLE_PROFESSOR);
    });
  }

  @Test
  public void testEditTC5() {
    assertThrows(ResourceNotFoundException.class, () -> {
      userUcc.signup(prof);
      prof.setUsername("chikiBriki");
      prof.setEmail("MamyFaitDesBlagues@joke.be");
      userUcc.edit(stud, 1, UserDto.ROLE_PROFESSOR);
    });
  }



}
