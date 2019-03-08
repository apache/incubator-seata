package com.alibaba.fescar.rm.tcc;

import java.util.HashMap;
import java.util.Map;

/**
 * the TCC method result
 * @author zhangsen
 */
public class TwoPhaseResult {

    /**
     * is Success ?
     */
    private boolean             isSuccess = false;

    /**
     * result message
     */
    private String              msg;

    private Map<String, Object> context = new HashMap<String, Object>();

    public TwoPhaseResult(boolean isSuccess, String msg) {
        this.isSuccess = isSuccess;
        this.msg = msg;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean isSuccess) {
        this.isSuccess = isSuccess;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public Object getContext(String key) {
        return context.get(key);
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    public void addContext(String key, Object value) {
        context.put(key, value);
    }

    public String toString() {
        return String.valueOf(isSuccess);
    }
}
