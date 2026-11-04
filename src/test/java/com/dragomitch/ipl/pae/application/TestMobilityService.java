package com.dragomitch.ipl.pae.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dragomitch.ipl.pae.UnitTestConfig;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryMobilityChoiceRepository;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryMobilityRepository;
import com.dragomitch.ipl.pae.adapter.out.inmemory.InMemoryUserRepository;
import com.dragomitch.ipl.pae.domain.exceptions.BusinessException;
import com.dragomitch.ipl.pae.domain.exceptions.ErrorCode;
import com.dragomitch.ipl.pae.domain.exceptions.ResourceNotFoundException;
import com.dragomitch.ipl.pae.domain.model.DenialReason;
import com.dragomitch.ipl.pae.domain.model.Mobility;
import com.dragomitch.ipl.pae.domain.model.MobilityChoice;
import com.dragomitch.ipl.pae.domain.model.MobilityState;
import com.dragomitch.ipl.pae.domain.model.Role;
import com.dragomitch.ipl.pae.domain.model.User;
import com.dragomitch.ipl.pae.domain.model.Violations;
import com.dragomitch.ipl.pae.domain.port.in.DenialReasonService;
import com.dragomitch.ipl.pae.domain.port.in.MobilityChoiceService;
import com.dragomitch.ipl.pae.domain.port.in.MobilityService;
import com.dragomitch.ipl.pae.domain.port.in.UserService;
import com.dragomitch.ipl.pae.domain.port.out.MobilityChoiceRepository;
import com.dragomitch.ipl.pae.domain.port.out.MobilityRepository;
import com.dragomitch.ipl.pae.domain.port.out.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(UnitTestConfig.class)
public class TestMobilityService {

  @Autowired private ApplicationContext context;

  private MockDtoFactory mockDtoFactory;
  private MobilityService mobilityService;
  private MobilityRepository mobilityRepository;
  private MobilityChoiceService mobilityChoiceService;
  private DenialReasonService denialReasonService;
  private UserService userService;

  /** Sets up the environment before every test. */
  @BeforeEach
  public void setUp() throws Exception {
    mobilityService = context.getBean(MobilityService.class);
    mobilityRepository = context.getBean(MobilityRepository.class);
    mobilityChoiceService = context.getBean(MobilityChoiceService.class);
    userService = context.getBean(UserService.class);
    denialReasonService = context.getBean(DenialReasonService.class);
    mockDtoFactory = new MockDtoFactory();
    // Creates User

    User user = mockDtoFactory.getUser(Role.STUDENT);
    user.setEmail("Cyka@blyat.ru");
    user.setUsername("CykaBlyat");
    userService.signup(user);
    // Creates mobility
    MobilityChoice mobilityChoice = mockDtoFactory.getMobilityChoice();
    mobilityChoice.setUser(user);
    mobilityChoice = mobilityChoiceService.create(mobilityChoice, 1, Role.STUDENT);
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
  public void testConfirmProEcoEncodingTC2() {
    assertThrows(
        BusinessException.class,
        () -> {
          Mobility mobility = mobilityRepository.findById(1).orElse(null);
          mobility.setState(MobilityState.CANCELLED);
          mobilityService.confirmProEcoEncoding(1, 1);
        });
  }

  @Test
  public void testConfirmProEcoEncodingTC3() {
    mobilityService.confirmProEcoEncoding(1, 1);
    Mobility mobility = mobilityRepository.findById(1).orElse(null);
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
  public void testConfirmSecondSoftwareEncodingTC2() {
    assertThrows(
        BusinessException.class,
        () -> {
          Mobility mobility = mobilityRepository.findById(1).orElse(null);
          mobility.setState(MobilityState.CANCELLED);
          mobilityService.confirmSecondSoftwareEncoding(1, 1);
        });
  }

  @Test
  public void testConfirmSecondSoftwareEncodingTC3() {
    mobilityService.confirmSecondSoftwareEncoding(1, 1);
    Mobility mobility = mobilityRepository.findById(1).orElse(null);
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
  public void testCancelTC2() {
    assertEquals(
        ErrorCode.DENIAL_REASON_REQUIRED,
        Violations.errorCodeOf(() -> {
          mobilityService.cancel(1, 1, null, -1, 1, Role.PROFESSOR);
        }));
  }

  @Test
  public void testCancelTC3() {
    assertEquals(
        ErrorCode.CANCELLATION_REASON_REQUIRED,
        Violations.errorCodeOf(() -> {
          mobilityService.cancel(1, 1, null, 1, 1, Role.STUDENT);
        }));
  }

  @Test
  public void testCancelTC6() {
    assertThrows(
        BusinessException.class,
        () -> {
          mobilityService.cancel(1, 1, null, 99, 1, Role.PROFESSOR);
        });
  }

  @Test
  public void testCancelTC7() {
    DenialReason denialReason = denialReasonService.create(mockDtoFactory.getDenialReason());
    Mobility mobility = mobilityService.cancel(1, 1, null, denialReason.getId(), 1, Role.PROFESSOR);
    assertEquals(MobilityState.CANCELLED, mobility.getState());
    assertEquals(denialReason.getId(), mobility.getDenialReason().getId());
  }

  @Test
  public void testCancelTC8() {
    Mobility mobility = mobilityService.cancel(1, 1, "Toto 456", 0, 1, Role.STUDENT);
    assertEquals(MobilityState.CANCELLED, mobility.getState());
    assertEquals("Toto 456", mobility.getCancellationReason());
  }

  //  @Test(expected = InsufficientPermissionException.class)
  //  public void testCancelTC9() {
  //    //
  // System.out.println(mobilityRepository.findById(1).orElseThrow().getNominatedStudent().getId());
  //    // // TODO continuer
  //    //Mobility mobility = mobilityService.cancel(1, 1, "Toto 456", 0, 3,
  // Role.STUDENT);
  //  }

}
