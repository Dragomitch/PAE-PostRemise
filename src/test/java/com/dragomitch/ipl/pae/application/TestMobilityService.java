package com.dragomitch.ipl.pae.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dragomitch.ipl.pae.UnitTestConfig;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryMobilityChoiceRepository;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryMobilityRepository;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryUserRepository;
import com.dragomitch.ipl.pae.domain.dto.DenialReasonDto;
import com.dragomitch.ipl.pae.domain.dto.MobilityChoiceDto;
import com.dragomitch.ipl.pae.domain.dto.MobilityDto;
import com.dragomitch.ipl.pae.domain.dto.UserDto;
import com.dragomitch.ipl.pae.domain.exceptions.BusinessException;
import com.dragomitch.ipl.pae.domain.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.domain.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.domain.model.EntityFactory;
import com.dragomitch.ipl.pae.domain.model.Violations;
import com.dragomitch.ipl.pae.domain.port.in.DenialReasonService;
import com.dragomitch.ipl.pae.domain.port.in.MobilityChoiceService;
import com.dragomitch.ipl.pae.domain.port.in.MobilityService;
import com.dragomitch.ipl.pae.domain.port.in.UserService;
import com.dragomitch.ipl.pae.domain.port.out.MobilityChoiceRepository;
import com.dragomitch.ipl.pae.domain.port.out.MobilityRepository;
import com.dragomitch.ipl.pae.domain.port.out.UserRepository;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(UnitTestConfig.class)
public class TestMobilityService {

  @Autowired private ApplicationContext context;

  private EntityFactory entityFactory;
  private MockDtoFactory mockDtoFactory;
  private MobilityService mobilityService;
  private MobilityRepository mobilityRepository;
  private MobilityChoiceService mobilityChoiceService;
  private DenialReasonService denialReasonService;
  private UserService userService;

  /** Sets up the environment before every test. */
  @BeforeEach
  public void setUp() throws Exception {
    entityFactory = context.getBean(EntityFactory.class);
    mobilityService = context.getBean(MobilityService.class);
    mobilityRepository = context.getBean(MobilityRepository.class);
    mobilityChoiceService = context.getBean(MobilityChoiceService.class);
    userService = context.getBean(UserService.class);
    denialReasonService = context.getBean(DenialReasonService.class);
    mockDtoFactory = new MockDtoFactory(entityFactory);
    // Creates User

    UserDto user = mockDtoFactory.getUser(UserDto.ROLE_STUDENT);
    user.setEmail("Cyka@blyat.ru");
    user.setUsername("CykaBlyat");
    userService.signup(user);
    // Creates mobility
    MobilityChoiceDto mobilityChoice = mockDtoFactory.getMobilityChoice();
    mobilityChoice.setUser(user);
    mobilityChoice = mobilityChoiceService.create(mobilityChoice, 1, UserDto.ROLE_STUDENT);
    mobilityChoiceService.confirm(mobilityChoice.getId(), 1);
  }

  /** Cleans up the 'database'. */
  @AfterEach
  public void cleanUp() {
    ((InMemoryMobilityRepository) mobilityRepository).empty();
    MobilityChoiceRepository mobilityChoiceRepository =
        context.getBean(MobilityChoiceRepository.class);
    ((InMemoryMobilityChoiceRepository) mobilityChoiceRepository).empty();
    UserRepository userRepository = context.getBean(UserRepository.class);
    ((InMemoryUserRepository) userRepository).empty();
  }

  @Test
  public void testConfirmProEcoEncodingTC1() {
    assertThrows(
        ConstraintViolationException.class,
        () -> {
          mobilityService.confirmProEcoEncoding(-1, 1);
        });
  }

  @Test
  public void testConfirmProEcoEncodingTC2() {
    assertThrows(
        BusinessException.class,
        () -> {
          MobilityDto mobility = mobilityRepository.findById(1);
          mobility.setState(MobilityDto.STATE_CANCELLED);
          mobilityService.confirmProEcoEncoding(1, 1);
        });
  }

  @Test
  public void testConfirmProEcoEncodingTC3() {
    mobilityService.confirmProEcoEncoding(1, 1);
    MobilityDto mobility = mobilityRepository.findById(1);
    assertTrue(mobility.isEncodedInProEco());
  }

  @Test
  public void testConfirmProEcoEncodingTC4() {
    assertThrows(
        ResourceNotFoundException.class,
        () -> {
          mobilityService.confirmProEcoEncoding(99, 1);
        });
  }

  @Test
  public void testConfirmSecondSoftwareEncodingTC1() {
    assertThrows(
        ConstraintViolationException.class,
        () -> {
          mobilityService.confirmSecondSoftwareEncoding(0, 1);
        });
  }

  @Test
  public void testConfirmSecondSoftwareEncodingTC2() {
    assertThrows(
        BusinessException.class,
        () -> {
          MobilityDto mobility = mobilityRepository.findById(1);
          mobility.setState(MobilityDto.STATE_CANCELLED);
          mobilityService.confirmSecondSoftwareEncoding(1, 1);
        });
  }

  @Test
  public void testConfirmSecondSoftwareEncodingTC3() {
    mobilityService.confirmSecondSoftwareEncoding(1, 1);
    MobilityDto mobility = mobilityRepository.findById(1);
    assertTrue(mobility.isEncodedInSecondSoftware());
  }

  @Test
  public void testConfirmSecondSoftwareEncodingTC4() {
    assertThrows(
        ResourceNotFoundException.class,
        () -> {
          mobilityService.confirmSecondSoftwareEncoding(99, 1);
        });
  }

  @Test
  public void testCancelTC1() {
    assertThrows(
        ConstraintViolationException.class,
        () -> {
          mobilityService.cancel(-1, 1, null, 1, 1, UserDto.ROLE_PROFESSOR);
        });
  }

  @Test
  public void testCancelTC2() {
    assertEquals(
        ErrorCode.DENIAL_REASON_REQUIRED,
        Violations.errorCodeOf(() -> {
          mobilityService.cancel(1, 1, null, -1, 1, UserDto.ROLE_PROFESSOR);
        }));
  }

  @Test
  public void testCancelTC3() {
    assertEquals(
        ErrorCode.CANCELLATION_REASON_REQUIRED,
        Violations.errorCodeOf(() -> {
          mobilityService.cancel(1, 1, null, 1, 1, UserDto.ROLE_STUDENT);
        }));
  }

  @Test
  public void testCancelTC4() {
    assertThrows(
        ConstraintViolationException.class,
        () -> {
          mobilityService.cancel(1, 1, null, 1, -1, UserDto.ROLE_PROFESSOR);
        });
  }

  @Test
  public void testCancelTC5() {
    assertThrows(
        ConstraintViolationException.class,
        () -> {
          mobilityService.cancel(1, 1, null, 1, 1, null);
        });
  }

  @Test
  public void testCancelTC6() {
    assertThrows(
        BusinessException.class,
        () -> {
          mobilityService.cancel(1, 1, null, 99, 1, UserDto.ROLE_PROFESSOR);
        });
  }

  @Test
  public void testCancelTC7() {
    DenialReasonDto denialReason = denialReasonService.create(mockDtoFactory.getDenialReason());
    MobilityDto mobility =
        mobilityService.cancel(1, 1, null, denialReason.getId(), 1, UserDto.ROLE_PROFESSOR);
    assertEquals(MobilityDto.STATE_CANCELLED, mobility.getState());
    assertEquals(denialReason.getId(), mobility.getDenialReason().getId());
  }

  @Test
  public void testCancelTC8() {
    MobilityDto mobility = mobilityService.cancel(1, 1, "Toto 456", 0, 1, UserDto.ROLE_STUDENT);
    assertEquals(MobilityDto.STATE_CANCELLED, mobility.getState());
    assertEquals("Toto 456", mobility.getCancellationReason());
  }

  //  @Test(expected = InsufficientPermissionException.class)
  //  public void testCancelTC9() {
  //    // System.out.println(mobilityRepository.findById(1).getNominatedStudent().getId());
  //    // // TODO continuer
  //    //MobilityDto mobility = mobilityService.cancel(1, 1, "Toto 456", 0, 3,
  // UserDto.ROLE_STUDENT);
  //  }

}
