package com.dragomitch.ipl.pae.persistence.implementations;

import com.dragomitch.ipl.pae.exceptions.FatalException;

import java.util.ConcurrentModificationException;
import java.util.function.Supplier;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.conversion.DbActionExecutionException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Keeps the contract of the DAO interfaces for the DAOs implemented with Spring Data JDBC
 * repositories or {@code JdbcClient}, which report errors as {@link DataAccessException}s:
 * <ul>
 *   <li>a DAO is only called inside a transaction ({@code @Transactional} use case), otherwise
 *       {@link IllegalStateException}, like {@link DalBackendServices};</li>
 *   <li>a stale version ({@link OptimisticLockingFailureException}, raised by Spring Data for
 *       {@code @Version} aggregates) is a {@link ConcurrentModificationException};</li>
 *   <li>any other database error is a {@link FatalException}, including the
 *       {@link DbActionExecutionException} in which Spring Data wraps the failure of an insert or
 *       an update.</li>
 * </ul>
 */
final class DataAccess {

  private DataAccess() {
  }

  /**
   * Runs a DAO operation with the translations above.
   *
   * @param work the operation
   * @param <T> the type of its result
   * @return what {@code work} returned
   */
  static <T> T call(Supplier<T> work) {
    if (!TransactionSynchronizationManager.isActualTransactionActive()) {
      throw new IllegalStateException(
          "DAO called outside a transaction: annotate the calling service with @Transactional");
    }
    try {
      return work.get();
    } catch (OptimisticLockingFailureException ex) {
      ConcurrentModificationException stale = new ConcurrentModificationException(ex.getMessage());
      stale.initCause(ex);
      throw stale;
    } catch (DataAccessException | DbActionExecutionException ex) {
      throw new FatalException(FatalException.DATABASE_ERROR_MSG, ex);
    }
  }

  /**
   * A reference to another aggregate, {@code null} for a {@code null} id: the database, not the
   * mapping, then rejects a missing mandatory reference (as a {@link FatalException}).
   *
   * @param id the id of the referenced aggregate, possibly {@code null}
   * @param <T> the type of the referenced aggregate
   * @param <I> the type of its id
   * @return the reference or {@code null}
   */
  static <T, I> AggregateReference<T, I> reference(I id) {
    return id == null ? null : AggregateReference.to(id);
  }

  /**
   * Same as {@link #call(Supplier)} for an operation without result.
   *
   * @param work the operation
   */
  static void run(Runnable work) {
    call(() -> {
      work.run();
      return null;
    });
  }
}
