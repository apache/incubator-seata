package io.seata.namingserver;

public class ClusterNotFoundException extends RuntimeException{

    /**
     * cluster not found exception.
     *
     * @param message the message
     */
    public ClusterNotFoundException(String message) {
        super(message);
    }

    /**
     * cluster not found exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public ClusterNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * cluster not found exception.
     *
     * @param cause the cause
     */
    public ClusterNotFoundException(Throwable cause) {
        super(cause);
    }

}
