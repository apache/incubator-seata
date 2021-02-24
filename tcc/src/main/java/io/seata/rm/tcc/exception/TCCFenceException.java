package io.seata.rm.tcc.exception;

import io.seata.common.exception.FrameworkErrorCode;
import io.seata.common.exception.FrameworkException;

/**
 * TCC Fence Exception
 *
 * @author cebbank
 */
public class TCCFenceException extends FrameworkException {

    public TCCFenceException() {
    }

    public TCCFenceException(FrameworkErrorCode err) {
        super(err);
    }

    public TCCFenceException(String msg) {
        super(msg);
    }

    public TCCFenceException(String msg, FrameworkErrorCode errCode) {
        super(msg, errCode);
    }

    public TCCFenceException(Throwable cause, String msg, FrameworkErrorCode errCode) {
        super(cause, msg, errCode);
    }

    public TCCFenceException(Throwable th) {
        super(th);
    }

    public TCCFenceException(Throwable th, String msg) {
        super(th, msg);
    }

}
