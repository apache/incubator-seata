package com.alibaba.fescar.server.store;

public enum  FlushDiskMode {
    SYNC_MODEL("sync"),
    ASYNC_MODEL("async");

    private String modeStr;

    FlushDiskMode(String modeStr) {
        this.modeStr = modeStr;
    }

    public static FlushDiskMode findDiskMode(String modeStr) {
        if (SYNC_MODEL.modeStr.equals(modeStr)){
            return SYNC_MODEL;
        }
        return ASYNC_MODEL;
    }
}
