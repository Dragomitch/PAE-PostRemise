package com.dragomitch.ipl.pae.adapter.out.persistence.repository;

import com.dragomitch.ipl.pae.adapter.out.persistence.entity.UserEntity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, Integer> {

  @EntityGraph(attributePaths = "option")
  Optional<UserEntity> findWithOptionById(int id);

  @EntityGraph(attributePaths = "option")
  List<UserEntity> findAllWithOptionByOrderById();

  @EntityGraph(attributePaths = "option")
  Optional<UserEntity> findFirstWithOptionByUsernameOrderById(String username);

  @EntityGraph(attributePaths = "option")
  Optional<UserEntity> findFirstWithOptionByEmailOrderById(String email);

  @EntityGraph(attributePaths = "option")
  Optional<UserEntity> findFirstWithOptionByLastNameOrderById(String lastName);

  @EntityGraph(attributePaths = "option")
  Optional<UserEntity> findFirstWithOptionByFirstNameOrderById(String firstName);

  @EntityGraph(attributePaths = "option")
  Optional<UserEntity> findFirstWithOptionByRoleOrderById(String role);

  /**
   * Gives the user the role if its version is still {@code expectedVersion}, and increments the
   * version.
   *
   * @return the number of users changed: 0 for an unknown user or a stale version
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("UPDATE UserEntity u SET u.role = :role, u.version = u.version + 1"
      + " WHERE u.id = :id AND u.version = :expectedVersion")
  int updateRoleById(@Param("id") int id, @Param("role") String role,
      @Param("expectedVersion") int expectedVersion);

  /** Like {@link #updateRoleById}, for the user with that username. */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("UPDATE UserEntity u SET u.role = :role, u.version = u.version + 1"
      + " WHERE u.username = :username AND u.version = :expectedVersion")
  int updateRoleByUsername(@Param("username") String username, @Param("role") String role,
      @Param("expectedVersion") int expectedVersion);
}
