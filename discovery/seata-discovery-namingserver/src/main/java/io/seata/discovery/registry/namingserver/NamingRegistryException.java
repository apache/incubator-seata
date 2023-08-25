package io.seata.discovery.registry.namingserver;

public class NamingRegistryException extends RuntimeException{

    /**
     * naming registry exception.
     *
     * @param message the message
     */
    public NamingRegistryException(String message) {
        super(message);
    }

    /**
     * naming registry exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public NamingRegistryException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * naming registry exception.
     *
     * @param cause the cause
     */
    public NamingRegistryException(Throwable cause) {
        super(cause);
    }

}
