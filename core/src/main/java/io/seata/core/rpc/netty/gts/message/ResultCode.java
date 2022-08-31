package io.seata.core.rpc.netty.gts.message;

public enum ResultCode {
    OK(1),
    SYSTEMERROR(0),
    LOGICERROR(2);

    private int value;

    private ResultCode(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }
}