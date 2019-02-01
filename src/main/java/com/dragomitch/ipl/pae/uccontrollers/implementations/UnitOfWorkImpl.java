package com.dragomitch.ipl.pae.uccontrollers.implementations;

import com.dragomitch.ipl.pae.business.dto.Entity;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import com.dragomitch.ipl.pae.annotations.Inject;
import com.dragomitch.ipl.pae.exceptions.FatalException;
import com.dragomitch.ipl.pae.logging.LogManager;
import com.dragomitch.ipl.pae.persistence.AddressDao;
import com.dragomitch.ipl.pae.persistence.DalServices;
import com.dragomitch.ipl.pae.persistence.DaoClass;
import com.dragomitch.ipl.pae.persistence.DenialReasonDao;
import com.dragomitch.ipl.pae.persistence.MobilityChoiceDao;
import com.dragomitch.ipl.pae.persistence.MobilityDao;
import com.dragomitch.ipl.pae.persistence.MobilityDocumentDao;
import com.dragomitch.ipl.pae.persistence.NominatedStudentDao;
import com.dragomitch.ipl.pae.persistence.OptionDao;
import com.dragomitch.ipl.pae.persistence.PartnerDao;
import com.dragomitch.ipl.pae.persistence.PartnerOptionDao;
import com.dragomitch.ipl.pae.persistence.PaymentDao;
import com.dragomitch.ipl.pae.persistence.ProgrammeDao;
import com.dragomitch.ipl.pae.persistence.UserDao;
import com.dragomitch.ipl.pae.uccontrollers.UnitOfWork;

public class UnitOfWorkImpl implements UnitOfWork {

  private static Logger logger = LogManager.getLogger(UnitOfWork.class.getName());

  private DalServices dalServices;
  private Map<Class<?>, Object> daos;
  private ThreadLocal<Integer> transactionSemaphore;
  // Stores all the update operations to be made
  private ThreadLocal<Map<String, Entity>> updateOperations;
  // Stores the initial version of all entities to update
  private ThreadLocal<Map<String, Integer>> versionsCache;

  @Inject
  private UnitOfWorkImpl(AddressDao addressDao, DalServices dalServices,
      DenialReasonDao denialReason, MobilityChoiceDao mobilityChoiceDao, MobilityDao mobilityDao,
      MobilityDocumentDao mobilityDocumentDao, OptionDao optionDao, PaymentDao paymentDao,
      NominatedStudentDao nominatedStudentDao, PartnerDao partnerDao,
      PartnerOptionDao partnerOptionDao, ProgrammeDao programmeDao, UserDao userDao) {
    this.dalServices = dalServices;
    daos = new HashMap<Class<?>, Object>();
    daos.put(AddressDao.class, addressDao);
    daos.put(DenialReasonDao.class, denialReason);
    daos.put(MobilityChoiceDao.class, mobilityChoiceDao);
    daos.put(MobilityDao.class, mobilityDao);
    daos.put(MobilityDocumentDao.class, mobilityDocumentDao);
    daos.put(OptionDao.class, optionDao);
    daos.put(PaymentDao.class, paymentDao);
    daos.put(NominatedStudentDao.class, nominatedStudentDao);
    daos.put(PartnerDao.class, partnerDao);
    daos.put(PartnerOptionDao.class, partnerOptionDao);
    daos.put(ProgrammeDao.class, programmeDao);
    daos.put(UserDao.class, userDao);
    transactionSemaphore = new ThreadLocal<Integer>();
    updateOperations = new ThreadLocal<Map<String, Entity>>();
    versionsCache = new ThreadLocal<Map<String, Integer>>();
  }

  @Override
  public void startTransaction() {
    if (updateOperations.get() == null || versionsCache.get() == null) {
      updateOperations.set(new HashMap<String, Entity>());
      versionsCache.set(new HashMap<String, Integer>());
    }
    int semaphore;
    if (transactionSemaphore.get() == null) {
      semaphore = 0;
    } else {
      semaphore = transactionSemaphore.get();
    }
    if (semaphore == 0) {
      dalServices.openConnection();
      logger.finer("Opening connection");
      dalServices.startTransaction();
    }
    transactionSemaphore.set(++semaphore);
  }

  @Override
  public Entity update(Entity entity) {
    String objectId = getObjectId(entity);
    Entity inCache = updateOperations.get().get(objectId);
    if (inCache == null) {
      // If it is the first update operation, the version is saved in cache.
      versionsCache.get().put(objectId, entity.getVersion());
    }
    int initialVersion = versionsCache.get().get(objectId);
    updateOperations.get().put(objectId, entity);
    entity.setVersion(initialVersion + 1);
    return entity;
  }

  @Override
  public void commit() {
    int semaphore = transactionSemaphore.get();
    try {
      performUpdates();
    } catch (Exception ex) {
      throw ex;
    } finally {
      // Purge cache
      updateOperations.get().clear();
      versionsCache.get().clear();
    }
    if (semaphore == 1) {
      logger.finer("Committing changes");
      dalServices.commit();
      dalServices.closeConnection();
      logger.finer("Closing connection");
    }
    transactionSemaphore.set(--semaphore);
  }

  @Override
  public void rollback() {
    int semaphore = transactionSemaphore.get();
    if (semaphore == 1) {
      // Purge cache
      updateOperations.get().clear();
      versionsCache.get().clear();
      // Rollback procedure
      logger.info("Rolling back changes");
      dalServices.rollback();
      dalServices.closeConnection();
      logger.finer("Closing connection");
    }
    transactionSemaphore.set(--semaphore);
  }

  /**
   * Perform all update operations from the cache for calling thread.
   */
  private void performUpdates() {
    for (Entity entity : updateOperations.get().values()) {
      String objectId = getObjectId(entity);
      int initialVersion = versionsCache.get().get(objectId);
      entity.setVersion(initialVersion);
      Class<?> klass = entity.getClass().getAnnotation(DaoClass.class).value();
      Object dao = daos.get(klass);
      if (dao == null) {
        throw new FatalException("Couldn't find dao in local cache. Check the class dependencies"
            + " to see if the dependency has been correctly injected.");
      }
      logger.info("Performing updates on instance of " + klass.getName());
      try {
        Method method = null;
        for (Method curMethod : dao.getClass().getDeclaredMethods()) {
          if (curMethod.getName().equals("update")) {
            method = curMethod;
            method.setAccessible(true);
            logger.info(method.toString());
            break;
          }
        }
        if (method == null) {
          throw new FatalException("No such method.");
        }
        method.invoke(dao, entity);
      } catch (SecurityException ex) {
        throw new FatalException("Security Exception", ex);
      } catch (IllegalAccessException ex) {
        throw new FatalException("Illegal Access Exception", ex);
      } catch (IllegalArgumentException ex) {
        throw new FatalException("Illegal Argument Exception", ex);
      } catch (InvocationTargetException ex) {
        if (ex.getCause() instanceof RuntimeException) {
          throw (RuntimeException) ex.getCause();
        } else {
          throw new FatalException("Invocation Target Exeption", ex.getCause());
        }
      }
    }

  }

  /**
   * Return the object id : [runtime-type]_[id].
   *
   * @param entity the entity
   * @return the object id as a String
   */
  private String getObjectId(Entity entity) {
    return entity.getClass().getTypeName() + "_" + entity.getId();
  }

}
