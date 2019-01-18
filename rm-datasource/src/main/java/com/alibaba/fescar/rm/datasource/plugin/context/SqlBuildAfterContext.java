package com.alibaba.fescar.rm.datasource.plugin.context;

import com.alibaba.fescar.rm.datasource.plugin.PluginConstants;
import com.alibaba.fescar.rm.datasource.plugin.PluginContext;

import java.util.ArrayList;
import java.util.List;


public class SqlBuildAfterContext extends PluginContext {

    private static final String ARG_KEY_SQL_HINTS = "sqlHints";

    public SqlBuildAfterContext(List<String> sqlHints, String originSql) {
        this.setSqlHints(sqlHints);
        this.setResult(originSql);
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


}
