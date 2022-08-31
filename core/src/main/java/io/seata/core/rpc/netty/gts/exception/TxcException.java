package io.seata.core.rpc.netty.gts.exception;

import io.seata.core.rpc.netty.gts.message.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

public class TxcException extends RuntimeException {
    private static final Logger LOGGER = LoggerFactory.getLogger(TxcException.class);
    private static final long serialVersionUID = 5531074229174745826L;
    private final int result;
    private final TxcErrCode errcode;

    public TxcException(int result, String msg) {
        this((Throwable)null, msg, result, TxcErrCode.UnknownAppError);
    }

    public TxcException() {
        this(TxcErrCode.UnknownAppError);
    }

    public TxcException(TxcErrCode err) {
        this(err.errMessage, err);
    }

    public TxcException(String msg) {
        this(msg, TxcErrCode.UnknownAppError);
    }

    public TxcException(String msg, TxcErrCode errCode) {
        this((Throwable)null, msg, ResultCode.SYSTEMERROR.getValue(), errCode);
    }

    public TxcException(Throwable th, String msg, TxcErrCode errCode) {
        this(th, msg, ResultCode.SYSTEMERROR.getValue(), errCode);
    }

    public TxcException(Throwable th) {
        this(th, th.getMessage());
    }

    public TxcException(Throwable th, String msg) {
        this(th, msg, ResultCode.SYSTEMERROR.getValue(), TxcErrCode.UnknownAppError);
    }

    public TxcException(Throwable cause, String msg, int result, TxcErrCode errCode) {
        super(msg, cause);
        this.result = result;
        this.errcode = errCode;
    }

    public int getResult() {
        return this.result;
    }

    public TxcErrCode getErrcode() {
        return this.errcode;
    }

    public static TxcException nestedException(Throwable e) {
        return nestedException("", e);
    }

    public static TxcException nestedException(String msg, Throwable e) {
        return e instanceof TxcException ? (TxcException)e : new TxcException(e, msg);
    }

    public static SQLException nestedSQLException(Throwable e) {
        return nestedSQLException("", e);
    }

    public static SQLException nestedSQLException(String msg, Throwable e) {
        LOGGER.error("Gts error code is" + msg + ", error message is " + e.getMessage(), e);
        return e instanceof SQLException ? (SQLException)e : new SQLException(e);
    }
}
