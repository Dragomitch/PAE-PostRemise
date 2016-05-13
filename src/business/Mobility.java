package business;

import business.dto.MobilityDto;
import business.exceptions.BusinessException;
import main.exceptions.FatalException;

public interface Mobility extends MobilityDto, MobilityChoice {

  /**
   * Indicates that a document has been filled in.
   * 
   * @param documentId the document id
   * @return true if the operation has been successfully completed. False if the document has
   *         already been filled in.
   * @throws IllegalArgumentException if the documentId is less or equals to zero
   * @throws BusinessException if the document doesn't exist in the mobility
   * @throws FatalException if the documents haven't been loaded
   */
  boolean fillInDocument(int documentId);

  /**
   * Checks if all documents are filled in.
   * 
   * @return true if all documents are filled in, false otherwise
   * @throws FatalException if the documents haven't been loaded
   */
  boolean allDocumentsFilledIn();

  /**
   * Checks if all departure documents are filled in.
   * 
   * @return true if all departure documents are filled in, false otherwise
   * @throws FatalException if the documents haven't been loaded
   */
  boolean allDepartureDocumentsFilledIn();

  /**
   * Checks if all return documents are filled in.
   * 
   * @return true if all return documents are filled in, false otherwise
   * @throws FatalException if the documents haven't been loaded
   */
  boolean allReturnDocumentsFilledIn();

  /**
   * Checks if all documents are filled in.
   * 
   * @throws BusinessException if all documents are not filled in
   * @throws FatalException if the documents haven't been loaded
   */
  void checkAllDocumentsFilledIn();

  /**
   * Checks if all departure documents are filled in.
   * 
   * @throws BusinessException if all departure documents are not filled in
   * @throws FatalException if the documents haven't been loaded
   */
  void checkAllDepartureDocumentsFilledIn();

  /**
   * Checks if all departure documents are filled in.
   * 
   * @throws BusinessException if all return documents are not filled in
   * @throws FatalException if the documents haven't been loaded
   */
  void checkAllReturnDocumentsFilledIn();

  /**
   * Checks if a mobility is cancelled.
   * 
   * @throws BusinessException if the mobility is cancelled
   */
  void checkNotCancelled();

  /**
   * Checks if a mobility is closed.
   * 
   * @throws BusinessException if the mobility is closed
   */
  void checkNotClosed();

  /**
   * Checks if a mobility is either cancelled or closed.
   * 
   * @throws BusinessException if the mobility is cancelled/closed
   */
  void checkNotCancelledAndNotClosed();

}
