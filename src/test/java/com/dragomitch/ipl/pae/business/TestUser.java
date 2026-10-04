package com.dragomitch.ipl.pae.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.dragomitch.ipl.pae.UnitTestConfig;
import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.User;
import com.dragomitch.ipl.pae.business.dto.OptionDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(UnitTestConfig.class)
public class TestUser {

  @Autowired
  private ApplicationContext context;
  private static final int ID = 1;
  private static final String USERNAME = "username";
  public static final String LAST_NAME = "Last Name";
  private static final String FIRST_NAME = "First Name";
  private static final String EMAIL = "email@do.com";
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
   * Creates a new User instance.
   */
  @BeforeEach
  public void setUp() throws Exception {
    entityFactory = context.getBean(EntityFactory.class);
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

  @Test
  public void testCheckDataIntegrityTC1() {
    user.setUsername(null);
    assertNotEquals(List.of(), Violations.onCreate(user));
  }

  @Test
  public void testCheckDataIntegrityTC2() {
    user.setUsername("");
    assertNotEquals(List.of(), Violations.onCreate(user));
  }

  @Test
  public void testCheckDataIntegrityTC3() {
    user.setUsername(LONG_STRING_21);
    assertNotEquals(List.of(), Violations.onCreate(user));
  }

  @Test
  public void testCheckDataIntegrityTC4() {
    user.setLastName(null);
    assertNotEquals(List.of(), Violations.onCreate(user));
  }

  @Test
  public void testCheckDataIntegrityTC5() {
    user.setLastName("");
    assertNotEquals(List.of(), Violations.onCreate(user));
  }

  @Test
  public void testCheckDataIntegrityTC6() {
    user.setLastName(LONG_STRING_36);
    assertNotEquals(List.of(), Violations.onCreate(user));
  }

  @Test
  public void testCheckDataIntegrityTC7() {
    user.setFirstName(null);
    assertNotEquals(List.of(), Violations.onCreate(user));
  }

  @Test
  public void testCheckDataIntegrityTC8() {
    user.setFirstName("");
    assertNotEquals(List.of(), Violations.onCreate(user));
  }

  @Test
  public void testCheckDataIntegrityTC9() {
    user.setFirstName(LONG_STRING_36);
    assertNotEquals(List.of(), Violations.onCreate(user));
  }

  @Test
  public void testCheckDataIntegrityTC10() {
    user.setPassword(null);
    assertNotEquals(List.of(), Violations.onCreate(user));
  }

  @Test
  public void testCheckDataIntegrityTC11() {
    user.setPassword("");
    assertNotEquals(List.of(), Violations.onCreate(user));
  }

  @Test
  public void testCheckDataIntegrityTC12() {
    user.setPassword(LONG_STRING_256);
    assertNotEquals(List.of(), Violations.onCreate(user));
  }

  @Test
  public void testCheckDataIntegrityTC13() {
    user.setEmail(null);
    assertNotEquals(List.of(), Violations.onCreate(user));
  }

  @Test
  public void testCheckDataIntegrityTC14() {
    user.setEmail("");
    assertNotEquals(List.of(), Violations.onCreate(user));
  }

  @Test
  public void testCheckDataIntegrityTC15() {
    user.setEmail(LONG_STRING_256);
    assertNotEquals(List.of(), Violations.onCreate(user));
  }

  @Test
  public void testCheckDataIntegrityTC16() {
    user.setOption(null);
    assertNotEquals(List.of(), Violations.onCreate(user));
  }

  @Test
  public void testCheckDataIntegrityTC17() {
    user.getOption().setCode(null);
    assertNotEquals(List.of(), Violations.onCreate(user));
  }

  @Test
  public void testCheckDataIntegrityTC18() {
    user.getOption().setCode("");
    assertNotEquals(List.of(), Violations.onCreate(user));
  }

  @Test
  public void testCheckDataIntegrityTC19() {
    assertEquals(List.of(), Violations.onCreate(user));
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
