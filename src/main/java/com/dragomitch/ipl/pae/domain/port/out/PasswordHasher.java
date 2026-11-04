package com.dragomitch.ipl.pae.domain.port.out;

/**
 * Hashes the passwords of the users and checks a password against a stored hash. The hashes are
 * BCrypt today; the port hides the library so that the use cases know none.
 */
public interface PasswordHasher {

  /**
   * Hashes a password. The hash is salted: hashing the same password twice gives two different
   * hashes.
   *
   * @param rawPassword the password in clear text
   * @return the hash to store
   */
  String hash(String rawPassword);

  /**
   * Checks a password against a stored hash.
   *
   * @param rawPassword the password in clear text
   * @param hash a hash produced by {@link #hash(String)}
   * @return true if the hash is the one of the password
   */
  boolean matches(String rawPassword, String hash);
}
