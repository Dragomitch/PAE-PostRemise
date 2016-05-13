package business;

import static org.junit.Assert.assertEquals;

import business.dto.OptionDto;
import business.dto.UserDto;
import business.exceptions.BusinessException;
import main.ContextManager;
import main.DependencyManager;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.LocalDateTime;

public class TestUser {
  private static final int ID = 1;
  private static final String USERNAME = "username";
  public static final String LAST_NAME = "Last Name";
  private static final String FIRST_NAME = "First Name";
  private static final String EMAIL = "email@domain.com";
  private static final String PASSWORD = "password";
  private static final String OPTION_CODE = "BIN";
  private static final String ROLE = "student";
  private static final LocalDateTime REGISTRATION_DATE = LocalDateTime.now();
  private static final String LONG_STRING_21 = "asdasdasdasdasdasdasd";
  private static final String LONG_STRING_36 = "asdasdasdasdasdasdasdasdasdasdasdasd";
  private static final String LONG_STRING_256 =
      "asdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdas"
          + "dasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasda"
          + "sdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasda";
  private EntityFactory entityFactory;
  private User user;

  /**
   * Loads the necessary resources for the execution of tests.
   */
  @BeforeClass
  public static void setUpBeforeClass() {
    ContextManager.loadContext(ContextManager.ENV_TEST);
  }

  /**
   * Creates a new User instance.
   */
  @Before
  public void setUp() throws Exception {
    entityFactory = DependencyManager.getInstance(EntityFactory.class);
    user = setUpCorrectUser();
  }

  @Test
  public void testSetAndIdTC1() {
    user.setId(ID);
    assertEquals(ID, user.getId());
  }

  @Test
  public void testSetAndGetUsername() {
    user.setUsername(USERNAME);
    assertEquals(USERNAME, user.getUsername());
  }

  @Test
  public void testSetAndGetLastName() {
    user.setLastName(LAST_NAME);
    assertEquals(LAST_NAME, user.getLastName());
  }

  @Test
  public void testSetAndGetFirstName() {
    user.setFirstName(FIRST_NAME);
    assertEquals(FIRST_NAME, user.getFirstName());
  }

  @Test
  public void testSetAndGetEmail() {
    user.setEmail(EMAIL);
    assertEquals(EMAIL, user.getEmail());
  }

  @Test
  public void testSetAndGetPassword() {
    user.setPassword(PASSWORD);
    assertEquals(PASSWORD, user.getPassword());
  }

  @Test
  public void testSetAndGetOption() {
    OptionDto option = (OptionDto) entityFactory.build(OptionDto.class);
    option.setCode(OPTION_CODE);
    user.setOption(option);
    assertEquals(option, user.getOption());
  }

  @Test
  public void testSetAndGetRole() {
    user.setRole(ROLE);
    assertEquals(ROLE, user.getRole());
  }

  @Test
  public void testSetAndGetRegistrationDate() {
    user.setRegistrationDate(REGISTRATION_DATE);
    assertEquals(REGISTRATION_DATE, user.getRegistrationDate());
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC1() {
    user.setUsername(null);
    user.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC2() {
    user.setUsername("");
    user.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC3() {
    user.setUsername(LONG_STRING_21);
    user.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC4() {
    user.setLastName(null);
    user.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC5() {
    user.setLastName("");
    user.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC6() {
    user.setLastName(LONG_STRING_36);
    user.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC7() {
    user.setFirstName(null);
    user.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC8() {
    user.setFirstName("");
    user.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC9() {
    user.setFirstName(LONG_STRING_36);
    user.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC10() {
    user.setPassword(null);
    user.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC11() {
    user.setPassword("");
    user.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC12() {
    user.setPassword(LONG_STRING_256);
    user.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC13() {
    user.setEmail(null);
    user.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC14() {
    user.setEmail("");
    user.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC15() {
    user.setEmail(LONG_STRING_256);
    user.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC16() {
    user.setOption(null);
    user.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC17() {
    user.getOption().setCode(null);
    user.checkDataIntegrity();
  }

  @Test(expected = BusinessException.class)
  public void testCheckDataIntegrityTC18() {
    user.getOption().setCode("");
    user.checkDataIntegrity();
  }

  @Test
  public void testCheckDataIntegrityTC19() {
    user.checkDataIntegrity();
  }

  private User setUpCorrectUser() {
    User user = (User) entityFactory.build(UserDto.class);
    user.setFirstName(FIRST_NAME);
    user.setLastName(LAST_NAME);
    user.setUsername(USERNAME);
    user.setEmail(EMAIL);
    user.setPassword(PASSWORD);
    OptionDto option = (OptionDto) entityFactory.build(OptionDto.class);
    option.setCode(OPTION_CODE);
    user.setOption(option);
    user.setRole(ROLE);
    user.setRegistrationDate(REGISTRATION_DATE);
    return user;
  }
}
