package com.alibaba.fescar.rm.datasource.plugin;

import com.alibaba.fescar.rm.datasource.DataSourceProxy;

import java.util.HashMap;
import java.util.Map;

public class PluginContext {

    /**
     * DataSourceProxy
     */
    private DataSourceProxy dataSourceProxy;

    /**
     * 业务类型
     */
    private String action;
    /**
     * 业务处理相关参数
     */
    protected Map<String, Object> args = new HashMap<>();
    /**
     * proc执行前已经产生的业务结果对象
     */
    private Object result;


    public DataSourceProxy getDataSourceProxy() {
        return dataSourceProxy;
    }

    protected void setDataSourceProxy(DataSourceProxy dataSourceProxy) {
        this.dataSourceProxy = dataSourceProxy;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    public void setArgs(Map<String, Object> args) {
        this.args = args;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
