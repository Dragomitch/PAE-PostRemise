package com.dragomitch.ipl.pae.persistence.jdbc.repository;

import com.dragomitch.ipl.pae.persistence.jdbc.entity.UserEntity;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * Users. Inserts and version-checked updates go through {@code JdbcAggregateOperations}
 * ({@code insert}/{@code update}): {@code save} would decide between them from the version.
 */
public interface UserRepository extends ListCrudRepository<UserEntity, Integer> {

  /**
   * Changes the role of the user having that id and increments its version, if its version is
   * still {@code expectedVersion} (optimistic locking outside of the aggregate: only the role and
   * the version are written).
   *
   * @param id the user id
   * @param role the new role
   * @param expectedVersion the version the caller read
   * @return the number of rows updated: 0 for an unknown id or a stale version, else 1
   */
  @Modifying
  @Query("""
      UPDATE student_exchange_tools.users SET role = :role, version = version + 1
       WHERE user_id = :id AND version = :expectedVersion""")
  int updateRoleById(@Param("id") int id, @Param("role") String role,
      @Param("expectedVersion") int expectedVersion);

  /**
   * Same as {@link #updateRoleById} for the user having that (case-sensitive) username.
   *
   * @param username the username
   * @param role the new role
   * @param expectedVersion the version the caller read
   * @return the number of rows updated: 0 for an unknown username or a stale version, else 1
   */
  @Modifying
  @Query("""
      UPDATE student_exchange_tools.users SET role = :role, version = version + 1
       WHERE username = :username AND version = :expectedVersion""")
  int updateRoleByUsername(@Param("username") String username, @Param("role") String role,
      @Param("expectedVersion") int expectedVersion);
}
