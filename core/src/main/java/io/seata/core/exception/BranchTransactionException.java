package io.seata.core.exception;

/**
 * The type BranchTransaction exception.
 *
 * @author will
 */
public class BranchTransactionException extends TransactionException{

    /**
     * Instantiates a new Transaction exception.
     *
     * @param code the code
     */
    public BranchTransactionException(TransactionExceptionCode code) {
        super(code);
    }

    /**
     * Instantiates a new Transaction exception.
     *
     * @param code the code
     * @param cause the cause
     */
    public BranchTransactionException(TransactionExceptionCode code, Throwable cause) {
        super(code, cause);
    }

    /**
     * Instantiates a new Transaction exception.
     *
     * @param message the message
     */
    public BranchTransactionException(String message) {
        super(message);
    }

    /**
     * Instantiates a new Transaction exception.
     *
     * @param code the code
     * @param message the message
     */
    public BranchTransactionException(TransactionExceptionCode code, String message) {
        super(code, message);
    }

    /**
     * Instantiates a new Transaction exception.
     *
     * @param cause the cause
     */
    public BranchTransactionException(Throwable cause) {
        super(cause);
    }

    /**
     * Instantiates a new Transaction exception.
     *
     * @param message the message
     * @param cause the cause
     */
    public BranchTransactionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Instantiates a new Transaction exception.
     *
     * @param code the code
     * @param message the message
     * @param cause the cause
     */
    public BranchTransactionException(TransactionExceptionCode code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
