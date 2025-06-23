package com.dragomitch.ipl.pae.uccontrollers;

import static org.junit.Assert.assertEquals;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.OptionDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.RessourceNotFoundException;
import com.dragomitch.ipl.pae.context.ContextManager;
import com.dragomitch.ipl.pae.context.DependencyManager;
import com.dragomitch.ipl.pae.persistence.UserDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockUserDao;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dragomitch.ipl.pae.uccontrollers.UserUcc;

public class TestUserUcc {

  private EntityFactory entityFactory;
  private UserDao userDao;
  private UserUcc userUcc;
  private MockDtoFactory mockDtoFactory;
  private UserDto prof;
  private UserDto stud;


  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    ContextManager.loadContext(ContextManager.ENV_TEST);
  }

  /**
   * Sets up the environment before every test.
   */
  @Before
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
  @After
  public void cleanUp() {
    ((MockUserDao) userDao).empty();
  }

  @Test
  public void testCreateTC1() {
    userUcc.signup(prof);
    prof.setId(1);
    assertEquals(prof.getId(), (userDao.findById(prof.getId())).getId());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateTC2() {
    userUcc.signup(null);
    prof.setId(1);
    assertEquals(prof.getId(), (userDao.findById(prof.getId())).getId());
  }

  @Test
  public void testCreateTC3() {
    userUcc.signup(stud);
    userUcc.signup(stud);
    assertEquals(2, (userDao.findAll().size()));
  }

  @Test(expected = BusinessException.class)
  public void testCreateTC4() {
    OptionDto option = mockDtoFactory.getOption();
    option.setCode("Wrong Code Man");
    stud.setOption(option);
    userUcc.signup(stud);
  }

  @Test(expected = BusinessException.class)
  public void testCreateTC5() {
    userUcc.signup(stud);
    userUcc.signup(prof);
  }

  @Test(expected = BusinessException.class)
  public void testCreateTC6() {
    userUcc.signup(stud);
    prof.setUsername("chikiBriki");
    userUcc.signup(prof);
  }

  @Test(expected = BusinessException.class)
  public void testCreateTC7() {
    userUcc.signup(stud);
    prof.setEmail("MamyFaitDesBlagues@joke.be");
    userUcc.signup(prof);
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

  @Test(expected = RessourceNotFoundException.class)
  public void testPromoteToProfessorTC2() {
    userUcc.signup(stud);
    userUcc.promoteToProfessor(69);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPromoteToProfessorTC3() {
    userUcc.signup(stud);
    userUcc.promoteToProfessor(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEditTC1() {
    userUcc.signup(stud);
    userUcc.edit(null, 1, UserDto.ROLE_STUDENT);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEditTC2() {
    userUcc.signup(stud);
    userUcc.edit(stud, -1, UserDto.ROLE_STUDENT);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEditTC3() {
    userUcc.signup(stud);
    userUcc.edit(stud, -1, "");
  }

  // Edit d'un User avec des données déjà existantes dans un autre User
  @Test(expected = BusinessException.class)
  public void testEditTC4() {
    userUcc.signup(prof);
    userUcc.edit(stud, 1, UserDto.ROLE_PROFESSOR);
  }

  @Test(expected = RessourceNotFoundException.class)
  public void testEditTC5() {
    userUcc.signup(prof);
    prof.setUsername("chikiBriki");
    prof.setEmail("MamyFaitDesBlagues@joke.be");
    userUcc.edit(stud, 1, UserDto.ROLE_PROFESSOR);
  }



}
