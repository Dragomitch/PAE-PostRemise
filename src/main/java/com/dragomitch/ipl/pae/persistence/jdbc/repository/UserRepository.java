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
   * Changes the role of a user without touching its version (legacy behaviour of
   * {@code UserDao.promoteToProfessor}).
   *
   * @param id the user id
   * @param role the new role
   * @return the number of rows updated (0 or 1)
   */
  @Modifying
  @Query("UPDATE student_exchange_tools.users SET role = :role WHERE user_id = :id")
  int updateRole(@Param("id") int id, @Param("role") String role);
}
