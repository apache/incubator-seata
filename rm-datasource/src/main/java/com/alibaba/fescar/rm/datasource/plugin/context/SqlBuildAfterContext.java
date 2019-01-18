package com.alibaba.fescar.rm.datasource.plugin.context;

import com.alibaba.fescar.rm.datasource.plugin.PluginConstants;
import com.alibaba.fescar.rm.datasource.plugin.PluginContext;
import com.alibaba.fescar.rm.datasource.undo.SQLUndoLog;

import java.util.Collections;
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
        return (List<String>) this.args.getOrDefault(ARG_KEY_SQL_HINTS, Collections.emptyList());
    }

    public void setSqlHints(List<String> sqlHints) {
        this.args.put(ARG_KEY_SQL_HINTS, sqlHints);
    }


}
