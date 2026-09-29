package com.dragomitch.ipl.pae.uccontrollers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dragomitch.ipl.pae.business.exceptions.RessourceNotFoundException;
import com.dragomitch.ipl.pae.persistence.DalServices;
import com.dragomitch.ipl.pae.persistence.mocks.MockDalServices;
import com.dragomitch.ipl.pae.presentation.exceptions.UnauthenticatedUserException;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A use case releases the connection (and the transaction) it opened, also when it fails.
 */
class ConnectionHandlingTest extends AbstractUccTest {

  @Autowired
  private DalServices dalServices;
  @Autowired
  private OptionUcc optionUcc;
  @Autowired
  private ProgrammeUcc programmeUcc;
  @Autowired
  private SessionUcc sessionUcc;
  @Autowired
  private PartnerUcc partnerUcc;

  private MockDalServices dal() {
    return (MockDalServices) dalServices;
  }

  @AfterEach
  void everythingWasReleased() {
    assertThat(dal().getOpenConnections()).as("open connections").isZero();
    assertThat(dal().getOpenTransactions()).as("open transactions").isZero();
  }

  private void assertFails(ThrowingCallable call, Class<? extends Throwable> expected) {
    assertThatThrownBy(call).isInstanceOf(expected);
  }

  @Test
  void anUnknownOptionReleasesTheConnection() {
    assertFails(() -> optionUcc.findAllPartnersByOption("XXX"), RessourceNotFoundException.class);
  }

  @Test
  void aSuccessfulReadReleasesTheConnection() {
    assertThat(optionUcc.showAll()).hasSize(5);
    assertThat(programmeUcc.showAll()).isNotEmpty();
    assertThat(programmeUcc.showOne(1).getProgrammeName()).isEqualTo("Erasmus+");
  }

  @Test
  void aFailedSigninReleasesTheConnection() {
    assertFails(() -> sessionUcc.signin("nobody", "secret"), UnauthenticatedUserException.class);
  }

  @Test
  void theOptionsOfAnUnknownPartnerReleaseTheConnection() {
    assertFails(() -> partnerUcc.findAllPartnerOption(42), RessourceNotFoundException.class);
  }

  @Test
  void anUnknownPartnerIsNotFoundAndTheTransactionIsRolledBack() {
    assertFails(() -> partnerUcc.showOne(42), RessourceNotFoundException.class);

    assertThat(dal().getRollbacks()).isEqualTo(1);
    assertThat(dal().getCommits()).isZero();
  }
}
