package com.alibaba.fescar.common.exception;

/**
 * the store exception
 *
 * @author zhangsen
 * @data 2019 /4/2
 */
public class StoreException extends FrameworkException {

    /**
     * Instantiates a new Store exception.
     */
    public StoreException() {
    }

    /**
     * Instantiates a new Store exception.
     *
     * @param err the err
     */
    public StoreException(FrameworkErrorCode err) {
        super(err);
    }

    /**
     * Instantiates a new Store exception.
     *
     * @param msg the msg
     */
    public StoreException(String msg) {
        super(msg);
    }

    /**
     * Instantiates a new Store exception.
     *
     * @param msg     the msg
     * @param errCode the err code
     */
    public StoreException(String msg, FrameworkErrorCode errCode) {
        super(msg, errCode);
    }

    /**
     * Instantiates a new Store exception.
     *
     * @param cause   the cause
     * @param msg     the msg
     * @param errCode the err code
     */
    public StoreException(Throwable cause, String msg, FrameworkErrorCode errCode) {
        super(cause, msg, errCode);
    }

    /**
     * Instantiates a new Store exception.
     *
     * @param th the th
     */
    public StoreException(Throwable th) {
        super(th);
    }

    /**
     * Instantiates a new Store exception.
     *
     * @param th  the th
     * @param msg the msg
     */
    public StoreException(Throwable th, String msg) {
        super(th, msg);
    }
}
