package com.alibaba.fescar.rm.datasource.plugin.context;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fescar.rm.datasource.plugin.PluginConstants;
import com.alibaba.fescar.rm.datasource.plugin.PluginContext;

import java.util.ArrayList;
import java.util.List;


/**
 * 表元数据查询前处理插件,调用该插件获取最终的缓存键和元数据查询sql
 */
public class TableMetaBeforeContext extends PluginContext {

    private static final String ARG_KEY_SQL_HINTS = "sqlHints";
    private static final String ARG_KEY_TABLE_NAME = "tableName";

    public TableMetaBeforeContext(List<String> sqlHints, String tableName, String defaultCacheKey, String defaultMetaQuerySql) {
        this.setSqlHints(sqlHints);
        this.setTableName(tableName);
        this.setResultData(defaultCacheKey, defaultMetaQuerySql);
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

    public void setSqlHints(List<String> sqlHints) {
        this.args.put(ARG_KEY_SQL_HINTS, sqlHints);
    }

    public void setTableName(String tableName) {
        this.args.put(ARG_KEY_TABLE_NAME, tableName);
    }

    public String getTableName() {
        return (String) this.args.get(ARG_KEY_TABLE_NAME);
    }

    @Override
    public void setResult(Object result) {
        if (result != null && !(result instanceof JSONObject)) {
            throw new IllegalArgumentException("result must be JSONObject");
        }
        super.setResult(result);
    }

    public void setResultData(String cacheKey, String metaQuerySql) {
        JSONObject resultObject = new JSONObject();
        resultObject.fluentPut("cacheKey", cacheKey)
                .fluentPut("metaQuerySql", metaQuerySql)
        ;
        this.setResult(resultObject);
    }

    public String getResultForCacheKey() {
        JSONObject resultObject = (JSONObject) this.getResult();
        return resultObject.getString("cacheKey");
    }

    public String getResultForMetaQuerySql() {
        JSONObject resultObject = (JSONObject) this.getResult();
        return resultObject.getString("metaQuerySql");
    }
}
