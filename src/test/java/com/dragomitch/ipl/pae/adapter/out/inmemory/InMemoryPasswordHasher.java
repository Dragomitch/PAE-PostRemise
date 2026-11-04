package com.dragomitch.ipl.pae.adapter.out.inmemory;

import com.dragomitch.ipl.pae.domain.port.out.PasswordHasher;

/**
 * A plain {@link PasswordHasher} for the unit tests: no BCrypt, no salt, a fixed prefix that looks
 * like the start of a BCrypt hash ($2) so that the tests asserting the stored value is a hash stay
 * unchanged.
 */
public class InMemoryPasswordHasher implements PasswordHasher {

  private static final String PREFIX = "$2test$";

  @Override
  public String hash(String rawPassword) {
    return PREFIX + rawPassword;
  }

  @Override
  public boolean matches(String rawPassword, String hash) {
    return hash(rawPassword).equals(hash);
  }
}
