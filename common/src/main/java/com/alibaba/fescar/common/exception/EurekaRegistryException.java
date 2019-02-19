package com.alibaba.fescar.common.exception;

/**
 * eureka registry exception
 */
public class EurekaRegistryException extends RuntimeException {
    /**
     * eureka registry exception.
     */
    public EurekaRegistryException() {
        super();
    }

    /**
     * eureka registry exception.
     *
     * @param message the message
     */
    public EurekaRegistryException(String message) {
        super(message);
    }

    /**
     * eureka registry exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public EurekaRegistryException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * eureka registry exception.
     *
     * @param cause the cause
     */
    public EurekaRegistryException(Throwable cause) {
        super(cause);
    }
}
