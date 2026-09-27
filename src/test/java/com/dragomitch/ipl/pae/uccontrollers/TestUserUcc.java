package com.dragomitch.ipl.pae.uccontrollers;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.OptionDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.RessourceNotFoundException;
import com.dragomitch.ipl.pae.context.ContextManager;
import com.dragomitch.ipl.pae.context.DependencyManager;
import com.dragomitch.ipl.pae.persistence.UserDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockUserDao;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.dragomitch.ipl.pae.uccontrollers.UserUcc;

public class TestUserUcc {

  private EntityFactory entityFactory;
  private UserDao userDao;
  private UserUcc userUcc;
  private MockDtoFactory mockDtoFactory;
  private UserDto prof;
  private UserDto stud;


  @BeforeAll
  public static void setUpBeforeClass() throws Exception {
    ContextManager.loadContext(ContextManager.ENV_TEST);
  }

  /**
   * Sets up the environment before every test.
   */
  @BeforeEach
  public void setUp() {
    entityFactory = DependencyManager.getInstance(EntityFactory.class);
    userDao = DependencyManager.getInstance(UserDao.class);
    userUcc = DependencyManager.getInstance(UserUcc.class);
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
    assertThrows(IllegalArgumentException.class, () -> {
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
    assertThrows(BusinessException.class, () -> {
      OptionDto option = mockDtoFactory.getOption();
      option.setCode("Wrong Code Man");
      stud.setOption(option);
      userUcc.signup(stud);
    });
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
    assertThrows(RessourceNotFoundException.class, () -> {
      userUcc.signup(stud);
      userUcc.promoteToProfessor(69);
    });
  }

  @Test
  public void testPromoteToProfessorTC3() {
    assertThrows(IllegalArgumentException.class, () -> {
      userUcc.signup(stud);
      userUcc.promoteToProfessor(-1);
    });
  }

  @Test
  public void testEditTC1() {
    assertThrows(IllegalArgumentException.class, () -> {
      userUcc.signup(stud);
      userUcc.edit(null, 1, UserDto.ROLE_STUDENT);
    });
  }

  @Test
  public void testEditTC2() {
    assertThrows(IllegalArgumentException.class, () -> {
      userUcc.signup(stud);
      userUcc.edit(stud, -1, UserDto.ROLE_STUDENT);
    });
  }

  @Test
  public void testEditTC3() {
    assertThrows(IllegalArgumentException.class, () -> {
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
    assertThrows(RessourceNotFoundException.class, () -> {
      userUcc.signup(prof);
      prof.setUsername("chikiBriki");
      prof.setEmail("MamyFaitDesBlagues@joke.be");
      userUcc.edit(stud, 1, UserDto.ROLE_PROFESSOR);
    });
  }



}
