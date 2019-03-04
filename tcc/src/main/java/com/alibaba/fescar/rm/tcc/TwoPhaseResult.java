package com.alibaba.fescar.rm.tcc;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zhangsen
 */
public class TwoPhaseResult {

    /**
     * 结果成功还是失败
     */
    private boolean             isSuccess;

    /**
     * 如果失败可以输出一些异常信息，供线上问题跟踪
     */
    private String              msg;

    /**
     * 这个是占位属性，业务同学不用关注，xts框架用来回传一些二阶段的信息
     */
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
