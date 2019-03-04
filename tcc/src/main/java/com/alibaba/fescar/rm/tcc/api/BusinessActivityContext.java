package com.alibaba.fescar.rm.tcc.api;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fescar.common.Constants;

/**
 * 主事务记录上下文信息
 * @author zhangsen
 */
public class BusinessActivityContext implements Serializable {

    /**  */
    private static final long   serialVersionUID = 6539226288677737992L;

    private Map<String, Object> context          = new HashMap<String, Object>();

    public BusinessActivityContext() {
    }

    public BusinessActivityContext(Map<String, Object> context) {
        this.context = context;
    }

    public void addContext(String key, Object value) {
        context.put(key, value);
    }

    /**
     * 获取本次分布式事务的开启时间
     * @return
     */
    public Long fetchStartTime() {
        return (Long) context.get(Constants.START_TIME);
    }

    /**
     * 获取应用自定义的参数
     */
    public Object getContext(String key){
        return context.get(key);
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    public String toString() {
        return context.toString();
    }

}
