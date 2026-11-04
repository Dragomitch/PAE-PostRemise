package com.dragomitch.ipl.pae.adapter.out.security;

import com.dragomitch.ipl.pae.domain.port.out.PasswordHasher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * The {@link PasswordHasher} on top of the {@link PasswordEncoder} bean of {@code SecurityConfig}
 * (BCrypt), so that the stored hashes and the sign-in behaviour do not change.
 */
@Component
class BCryptPasswordHasher implements PasswordHasher {

  private final PasswordEncoder passwordEncoder;

  BCryptPasswordHasher(PasswordEncoder passwordEncoder) {
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public String hash(String rawPassword) {
    return passwordEncoder.encode(rawPassword);
  }

  @Override
  public boolean matches(String rawPassword, String hash) {
    return passwordEncoder.matches(rawPassword, hash);
  }
}
