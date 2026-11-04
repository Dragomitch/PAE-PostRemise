package com.dragomitch.ipl.pae.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class BCryptPasswordHasherTest {

  private final BCryptPasswordHasher hasher = new BCryptPasswordHasher(new BCryptPasswordEncoder());

  @Test
  void aHashMatchesItsRawPassword() {
    String hash = hasher.hash("plain password");

    assertThat(hash).startsWith("$2").isNotEqualTo("plain password");
    assertThat(hasher.matches("plain password", hash)).isTrue();
  }

  @Test
  void aWrongPasswordDoesNotMatch() {
    String hash = hasher.hash("plain password");

    assertThat(hasher.matches("another password", hash)).isFalse();
  }

  @Test
  void twoHashesOfTheSamePasswordDiffer() {
    String first = hasher.hash("plain password");
    String second = hasher.hash("plain password");

    assertThat(first).isNotEqualTo(second);
    assertThat(hasher.matches("plain password", second)).isTrue();
  }

  @Test
  void aHashStoredByThePreviousEncoderStillMatches() {
    String stored = new BCryptPasswordEncoder().encode("plain password");

    assertThat(hasher.matches("plain password", stored)).isTrue();
  }
}
