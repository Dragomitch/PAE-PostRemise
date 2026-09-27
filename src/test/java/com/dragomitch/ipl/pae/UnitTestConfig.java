package com.dragomitch.ipl.pae;

import com.dragomitch.ipl.pae.persistence.mocks.MockAddressDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockCountryDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockDenialReasonDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockDocumentDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockMobilityChoiceDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockMobilityDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockMobilityDocumentDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockNominatedStudentDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockOptionDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockPartnerDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockPartnerOptionDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockPaymentDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockProgrammeDao;
import com.dragomitch.ipl.pae.persistence.mocks.MockUserDao;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Spring context for the unit tests: the real business objects and use-case controllers, wired
 * to the in-memory mock DAOs instead of the JDBC implementations. The context is cached by the
 * Spring TestContext framework and shared by every test class using it, so the tests empty the
 * mocks they fill.
 *
 * <p>It is a {@link TestConfiguration} so that {@code @SpringBootTest} component scanning of the
 * application ignores it.
 */
@TestConfiguration(proxyBeanMethods = false)
@ComponentScan(basePackages = {"com.dragomitch.ipl.pae.business",
    "com.dragomitch.ipl.pae.uccontrollers"})
@Import({
    MockAddressDao.class,
    MockCountryDao.class,
    MockDenialReasonDao.class,
    MockDocumentDao.class,
    MockMobilityChoiceDao.class,
    MockMobilityDao.class,
    MockMobilityDocumentDao.class,
    MockNominatedStudentDao.class,
    MockOptionDao.class,
    MockPartnerDao.class,
    MockPartnerOptionDao.class,
    MockPaymentDao.class,
    MockProgrammeDao.class,
    MockUserDao.class
})
public class UnitTestConfig {

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
