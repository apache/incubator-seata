package com.alibaba.fescar.rm.datasource.plugin.context;

import com.alibaba.fescar.rm.datasource.plugin.PluginConstants;
import com.alibaba.fescar.rm.datasource.plugin.PluginContext;
import com.alibaba.fescar.rm.datasource.sql.struct.TableRecords;

import java.util.ArrayList;
import java.util.List;

/**
 * 锁key前处理插件,调用该插件获取最终的锁key字符串
 */
public class LockKeyBuildAfterContext extends PluginContext {

    private static final String ARG_KEY_TABLE_RECORDS = "tableRecords";
    private static final String ARG_KEY_SQL_HINTS = "sqlHints";

    public LockKeyBuildAfterContext(List<String> sqlHints, TableRecords tableRecords, String lockKey) {
        this.setSqlHints(sqlHints);
        this.setTableRecords(tableRecords);
        this.setResult(lockKey);
    }

    @Override
    public String getAction() {
        return PluginConstants.ACTION_LOCK_KEY_BUILD_AFTER;
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

    public TableRecords getTableRecords() {
        TableRecords tableRecords = (TableRecords) this.args.get(ARG_KEY_TABLE_RECORDS);
        return tableRecords;
    }

    public void setTableRecords(TableRecords tableRecords) {
        this.args.put(ARG_KEY_TABLE_RECORDS, tableRecords);
    }

    public String getResultLockKey() {
        return (String) getResult();
    }

}
