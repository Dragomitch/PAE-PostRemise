package com.dragomitch.ipl.pae.uccontrollers;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dragomitch.ipl.pae.business.EntityFactory;
import com.dragomitch.ipl.pae.business.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.business.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.business.dto.MobilityDto;
import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.business.exceptions.BusinessException;
import com.dragomitch.ipl.pae.business.exceptions.RessourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.dragomitch.ipl.pae.persistence.MobilityChoiceDao;
import com.dragomitch.ipl.pae.persistence.MobilityDao;
import com.dragomitch.ipl.pae.persistence.UserDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockMobilityChoiceDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockMobilityDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockUserDao;
import com.dragomitch.ipl.pae.uccontrollers.DenialReasonUcc;
import com.dragomitch.ipl.pae.uccontrollers.MobilityChoiceUcc;
import com.dragomitch.ipl.pae.uccontrollers.MobilityUcc;
import com.dragomitch.ipl.pae.uccontrollers.UserUcc;
import com.dragomitch.ipl.pae.UnitTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(UnitTestConfig.class)
public class TestMobilityUcc extends AbstractUccTest {

  @Autowired
  private ApplicationContext context;

  private EntityFactory entityFactory;
  private MockDtoFactory mockDtoFactory;
  private MobilityUcc mobilityUcc;
  private MobilityDao mobilityDao;
  private MobilityChoiceUcc mobilityChoiceUcc;
  private DenialReasonUcc denialReasonUcc;
  private UserUcc userUcc;

  /**
   * Sets up the environment before every test.
   */
  @BeforeEach
  public void setUp() throws Exception {
    entityFactory = context.getBean(EntityFactory.class);
    mobilityUcc = context.getBean(MobilityUcc.class);
    mobilityDao = context.getBean(MobilityDao.class);
    mobilityChoiceUcc = context.getBean(MobilityChoiceUcc.class);
    userUcc = context.getBean(UserUcc.class);
    denialReasonUcc = context.getBean(DenialReasonUcc.class);
    mockDtoFactory = new MockDtoFactory(entityFactory);
    // Creates User

    UserDto user = mockDtoFactory.getUser(UserDto.ROLE_STUDENT);
    user.setEmail("Cyka@blyat.ru");
    user.setUsername("CykaBlyat");
    userUcc.signup(user);
    // Creates mobility
    MobilityChoiceDto mobilityChoice = mockDtoFactory.getMobilityChoice();
    mobilityChoice.setUser(user);
    mobilityChoice = mobilityChoiceUcc.create(mobilityChoice, 1, UserDto.ROLE_STUDENT);
    mobilityChoiceUcc.confirm(mobilityChoice.getId(), 1);
  }

  /**
   * Cleans up the 'database'.
   */
  @AfterEach
  public void cleanUp() {
    ((MockMobilityDao) mobilityDao).empty();
    MobilityChoiceDao mobilityChoiceDao = context.getBean(MobilityChoiceDao.class);
    ((MockMobilityChoiceDao) mobilityChoiceDao).empty();
    UserDao userDao = context.getBean(UserDao.class);
    ((MockUserDao) userDao).empty();
  }

  @Test
  public void testConfirmProEcoEncodingTC1() {
    assertThrows(IllegalArgumentException.class, () -> {
      mobilityUcc.confirmProEcoEncoding(-1, 1);
    });
  }

  @Test
  public void testConfirmProEcoEncodingTC2() {
    assertThrows(BusinessException.class, () -> {
      MobilityDto mobility = mobilityDao.findById(1);
      mobility.setState(MobilityDto.STATE_CANCELLED);
      mobilityUcc.confirmProEcoEncoding(1, 1);
    });
  }

  @Test
  public void testConfirmProEcoEncodingTC3() {
    mobilityUcc.confirmProEcoEncoding(1, 1);
    MobilityDto mobility = mobilityDao.findById(1);
    assertTrue(mobility.isEncodedInProEco());
  }

  @Test
  public void testConfirmProEcoEncodingTC4() {
    assertThrows(RessourceNotFoundException.class, () -> {
      mobilityUcc.confirmProEcoEncoding(99, 1);
    });
  }

  @Test
  public void testConfirmSecondSoftwareEncodingTC1() {
    assertThrows(IllegalArgumentException.class, () -> {
      mobilityUcc.confirmSecondSoftwareEncoding(0, 1);
    });
  }

  @Test
  public void testConfirmSecondSoftwareEncodingTC2() {
    assertThrows(BusinessException.class, () -> {
      MobilityDto mobility = mobilityDao.findById(1);
      mobility.setState(MobilityDto.STATE_CANCELLED);
      mobilityUcc.confirmSecondSoftwareEncoding(1, 1);
    });
  }

  @Test
  public void testConfirmSecondSoftwareEncodingTC3() {
    mobilityUcc.confirmSecondSoftwareEncoding(1, 1);
    MobilityDto mobility = mobilityDao.findById(1);
    assertTrue(mobility.isEncodedInSecondSoftware());
  }

  @Test
  public void testConfirmSecondSoftwareEncodingTC4() {
    assertThrows(RessourceNotFoundException.class, () -> {
      mobilityUcc.confirmSecondSoftwareEncoding(99, 1);
    });
  }

  @Test
  public void testCancelTC1() {
    assertThrows(IllegalArgumentException.class, () -> {
      mobilityUcc.cancel(-1, 1, null, 1, 1, UserDto.ROLE_PROFESSOR);
    });
  }

  @Test
  public void testCancelTC2() {
    assertThrows(IllegalArgumentException.class, () -> {
      mobilityUcc.cancel(1, 1, null, -1, 1, UserDto.ROLE_PROFESSOR);
    });
  }

  @Test
  public void testCancelTC3() {
    assertThrows(IllegalArgumentException.class, () -> {
      mobilityUcc.cancel(1, 1, null, 1, 1, UserDto.ROLE_STUDENT);
    });
  }

  @Test
  public void testCancelTC4() {
    assertThrows(IllegalArgumentException.class, () -> {
      mobilityUcc.cancel(1, 1, null, 1, -1, UserDto.ROLE_PROFESSOR);
    });
  }

  @Test
  public void testCancelTC5() {
    assertThrows(IllegalArgumentException.class, () -> {
      mobilityUcc.cancel(1, 1, null, 1, 1, null);
    });
  }

  @Test
  public void testCancelTC6() {
    assertThrows(BusinessException.class, () -> {
      mobilityUcc.cancel(1, 1, null, 99, 1, UserDto.ROLE_PROFESSOR);
    });
  }

  @Test
  public void testCancelTC7() {
    DenialReasonDto denialReason = denialReasonUcc.create(mockDtoFactory.getDenialReason());
    MobilityDto mobility = mobilityUcc.cancel(1, 1, null, denialReason.getId(), 1, UserDto.ROLE_PROFESSOR);
    assertEquals(MobilityDto.STATE_CANCELLED, mobility.getState());
    assertEquals(denialReason.getId(), mobility.getDenialReason().getId());
  }

  @Test
  public void testCancelTC8() {
    MobilityDto mobility = mobilityUcc.cancel(1, 1, "Toto 456", 0, 1, UserDto.ROLE_STUDENT);
    assertEquals(MobilityDto.STATE_CANCELLED, mobility.getState());
    assertEquals("Toto 456", mobility.getCancellationReason());
  }

//  @Test(expected = InsufficientPermissionException.class)
//  public void testCancelTC9() {
//    // System.out.println(mobilityDao.findById(1).getNominatedStudent().getId());
//    // // TODO continuer
//    //MobilityDto mobility = mobilityUcc.cancel(1, 1, "Toto 456", 0, 3, UserDto.ROLE_STUDENT);
//  }

}
