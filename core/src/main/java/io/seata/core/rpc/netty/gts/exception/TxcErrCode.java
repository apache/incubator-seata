package io.seata.core.rpc.netty.gts.exception;

/**
 * @author liusicheng
 */

public enum TxcErrCode {
    MergeMessageError("0330", "MergeMessageError", ""),
    MergeResultMessageError("0331", "MergeMessageError", ""),
    UnknownAppError("10000", "unknown error", "Unknown Internal Error");

    public String errCode;
    public String errMessage;
    public String errDispose;

    private TxcErrCode(String errCode, String errMessage, String errDispose) {
        this.errCode = errCode;
        this.errMessage = errMessage;
        this.errDispose = errDispose;
    }

    @Override
    public String toString() {
        return String.format("[%s] [%s] [%s]", this.errCode, this.errMessage, this.errDispose);
    }
}

