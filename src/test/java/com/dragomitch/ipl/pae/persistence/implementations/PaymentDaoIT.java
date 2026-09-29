package com.dragomitch.ipl.pae.persistence.implementations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.dragomitch.ipl.pae.business.dto.PaymentDto;
import com.dragomitch.ipl.pae.persistence.PaymentDao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Payments are not a table: one row per requested payment of a mobility, "D" (departure) for
 * the first payment request and "R" (return) for the second one.
 */
class PaymentDaoIT extends AbstractDaoIT {

  @Autowired
  private PaymentDao paymentDao;

  @Override
  protected List<String> fixtures() {
    return List.of("db/fixtures/users.sql", "db/fixtures/partners.sql",
        "db/fixtures/mobility_choices.sql", "db/fixtures/mobilities.sql");
  }

  @Test
  void findAllReturnsOneRowPerRequestedPayment() {
    List<PaymentDto> payments = inTransaction(() -> paymentDao.findAll());

    // 5006 has only its first payment requested; 5007 has both
    assertThat(payments).extracting(PaymentDto::getMobilityChoiceId, PaymentDto::getPaymentType,
        PaymentDto::getPaymentDate).containsExactlyInAnyOrder(
            tuple(5006, "D", LocalDateTime.of(2025, 2, 1, 9, 0)),
            tuple(5007, "D", LocalDateTime.of(2025, 3, 1, 8, 0)),
            tuple(5007, "R", LocalDateTime.of(2025, 6, 30, 16, 30)));
  }

  @Test
  void findAllMapsTheMobilityStudentProgrammeCountryAndPartner() {
    PaymentDto payment = inTransaction(() -> paymentDao.findAll()).stream()
        .filter(p -> p.getMobilityChoiceId() == 5006).findFirst().orElseThrow();

    assertThat(payment.getUser().getId()).isEqualTo(1004);
    assertThat(payment.getUser().getFirstName()).isEqualTo("David");
    assertThat(payment.getUser().getLastName()).isEqualTo("Petit");
    assertThat(payment.getMobilityType()).isEqualTo("SMP");
    assertThat(payment.getAcademicYear()).isEqualTo(String.valueOf(LocalDate.now().getYear()));
    assertThat(payment.getTerm()).isEqualTo(2);
    assertThat(payment.getProgramme().getId()).isEqualTo(1);
    assertThat(payment.getProgramme().getProgrammeName()).isEqualTo("Erasmus+");
    assertThat(payment.getCountry().getCountryCode()).isEqualTo("FR");
    assertThat(payment.getCountry().getName()).isEqualTo("France");
    assertThat(payment.getPartner().getId()).isEqualTo(3001);
    assertThat(payment.getPartner().getFullName()).isEqualTo("Université de Lyon");
  }

  @Test
  void findAllListsThePaymentsOfAMobilityWithoutPartnerNorCountry() {
    List<PaymentDto> payments = inTransaction(() -> {
      execute("INSERT INTO student_exchange_tools.mobilities (mobility_choice_id, submission_date, "
          + "state, first_payment_request_date, version) "
          + "VALUES (5003, '2025-02-02 10:00:00', 'En cours', '2025-02-03 10:00:00', 1)");
      return paymentDao.findAll();
    });

    assertThat(payments).filteredOn(p -> p.getMobilityChoiceId() == 5003).singleElement()
        .satisfies(payment -> {
          assertThat(payment.getPaymentType()).isEqualTo("D");
          assertThat(payment.getPartner()).isNull();
          assertThat(payment.getCountry()).isNull();
          assertThat(payment.getUser().getFirstName()).isEqualTo("Alice");
        });
  }

  @Test
  void findAllReturnsNothingWhenNoPaymentWasRequested() {
    List<PaymentDto> payments = inTransaction(() -> {
      execute("UPDATE student_exchange_tools.mobilities SET first_payment_request_date = NULL, "
          + "second_payment_request_date = NULL");
      return paymentDao.findAll();
    });

    assertThat(payments).isEmpty();
  }

  @Test
  void findAllListsASecondPaymentEvenWithoutAFirstOne() {
    List<PaymentDto> payments = inTransaction(() -> {
      execute("UPDATE student_exchange_tools.mobilities SET first_payment_request_date = NULL");
      return paymentDao.findAll();
    });

    assertThat(payments).extracting(PaymentDto::getMobilityChoiceId, PaymentDto::getPaymentType)
        .containsExactly(tuple(5007, "R"));
  }
}
