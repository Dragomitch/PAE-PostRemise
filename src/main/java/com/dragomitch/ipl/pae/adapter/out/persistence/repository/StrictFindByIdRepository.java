package com.dragomitch.ipl.pae.adapter.out.persistence.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.FindOption;

import java.util.Optional;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.util.Assert;

/**
 * Base class of the Spring Data repositories ({@link JpaRepositoriesConfig}): a database failure
 * while loading an entity by its id is thrown, like any other database failure, instead of being
 * read as "not found".
 *
 * <p>Hibernate 7's {@code EntityManager.find(Class, Object)}, which {@link SimpleJpaRepository}
 * calls, catches a generic JDBC or connection error of the {@code SELECT} once it has marked the
 * transaction for rollback, logs it and returns {@code null} (HHH-12472). An adapter would then
 * answer "unknown id" (a 404, or a 409 for an update) for what is a 500. The JPA 3.2
 * {@code find(Class, Object, FindOption...)} loads the entity in the same way (persistence context
 * first) and lets the error through, so that the adapters translate it into a
 * {@code DataAccessException} ({@code RepositoryErrorHandlingIT}).
 *
 * <p>None of the repositories declares a lock mode or query hints for {@code findById}, which
 * {@link SimpleJpaRepository} would otherwise apply.
 *
 * @param <T> the entity type
 * @param <I> the id type
 */
public class StrictFindByIdRepository<T, I> extends SimpleJpaRepository<T, I> {

  private static final FindOption[] NO_OPTIONS = {};

  private final EntityManager entityManager;

  /**
   * Creates the repository of an entity.
   *
   * @param entityInformation the metadata of the entity
   * @param entityManager the shared entity manager
   */
  public StrictFindByIdRepository(JpaEntityInformation<T, ?> entityInformation,
      EntityManager entityManager) {
    super(entityInformation, entityManager);
    this.entityManager = entityManager;
  }

  @Override
  public Optional<T> findById(I id) {
    Assert.notNull(id, "The given id must not be null");
    return Optional.ofNullable(find(entityManager, getDomainClass(), id));
  }

  /**
   * Loads an entity by its id, throwing a database failure instead of returning {@code null}:
   * the replacement of {@code entityManager.find(type, id)} (see the class comment).
   *
   * @param entityManager the entity manager
   * @param type the entity class
   * @param id the id
   * @param <T> the entity type
   * @return the entity, or {@code null} when there is none with this id
   */
  public static <T> T find(EntityManager entityManager, Class<T> type, Object id) {
    return entityManager.find(type, id, NO_OPTIONS);
  }
}
