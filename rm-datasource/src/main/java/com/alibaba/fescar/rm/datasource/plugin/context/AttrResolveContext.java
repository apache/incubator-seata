package com.alibaba.fescar.rm.datasource.plugin.context;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fescar.rm.datasource.plugin.PluginConstants;
import com.alibaba.fescar.rm.datasource.plugin.PluginContext;

import java.util.ArrayList;
import java.util.List;

/**
 * 运行时特性解析插件
 */
public class AttrResolveContext extends PluginContext {

    private static final String ARG_KEY_SQL_HINTS = "sqlHints";
    private static final String ARG_KEY_SQL_Text = "sqlText";
    private static final String ATTR_KEY_SAVEPOINT_SUPPORT = "savepoint_support";

    public AttrResolveContext(List<String> sqlHints, String sqlText) {
        this.setSqlHints(sqlHints);
        this.setSqlText(sqlText);
        this.setResult(new JSONObject());
    }

    @Override
    public String getAction() {
        return PluginConstants.ACTION_SQL_BUILD_AFTER;
    }

    @Override
    public void setAction(String action) {

    }

    public List<String> getSqlHints() {
        List<String> sqlHints = (List<String>) this.args.get(ARG_KEY_SQL_HINTS);
        if (sqlHints == null) {
            return new ArrayList<>();
        }
        return sqlHints;
    }

    public void setSqlText(String sqlText) {
        this.args.put(ARG_KEY_SQL_Text, sqlText);
    }

    public String getSqlText() {
        return (String) this.args.get(ARG_KEY_SQL_Text);
    }

    public void setSqlHints(List<String> sqlHints) {
        this.args.put(ARG_KEY_SQL_HINTS, sqlHints);
    }

    @Override
    public void setResult(Object result) {
        if (result != null && !(result instanceof JSONObject)) {
            throw new IllegalArgumentException("result must be JSONObject");
        }
        super.setResult(result);
    }

    public void setResultData(Boolean savepointSupport) {
        JSONObject resultObject = new JSONObject();
        resultObject.fluentPut(ATTR_KEY_SAVEPOINT_SUPPORT, savepointSupport);
        this.setResult(resultObject);
    }

    /**
     * 是否支持savepoint操作
     *
     * @return
     */
    public Boolean getAttrForSavepointSupport() {
        JSONObject resultObject = (JSONObject) this.getResult();
        return resultObject.getBooleanValue(ATTR_KEY_SAVEPOINT_SUPPORT);
    }

}
