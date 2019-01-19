/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.alibaba.fescar.rm.datasource.exec;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import com.alibaba.fescar.core.context.RootContext;
import com.alibaba.fescar.rm.datasource.ConnectionProxy;
import com.alibaba.fescar.rm.datasource.StatementProxy;
import com.alibaba.fescar.rm.datasource.plugin.Plugin;
import com.alibaba.fescar.rm.datasource.plugin.PluginContext;
import com.alibaba.fescar.rm.datasource.plugin.PluginManager;
import com.alibaba.fescar.rm.datasource.plugin.context.LockKeyBuildAfterContext;
import com.alibaba.fescar.rm.datasource.sql.SQLRecognizer;
import com.alibaba.fescar.rm.datasource.sql.SQLType;
import com.alibaba.fescar.rm.datasource.sql.struct.Field;
import com.alibaba.fescar.rm.datasource.sql.struct.TableMeta;
import com.alibaba.fescar.rm.datasource.sql.struct.TableMetaCache;
import com.alibaba.fescar.rm.datasource.sql.struct.TableRecords;
import com.alibaba.fescar.rm.datasource.undo.SQLUndoLog;

public abstract class BaseTransactionalExecutor<T, S extends Statement> implements Executor {

    protected StatementProxy<S> statementProxy;

    protected StatementCallback<T, S> statementCallback;

    protected SQLRecognizer sqlRecognizer;

    private TableMeta tableMeta;

    public BaseTransactionalExecutor(StatementProxy<S> statementProxy, StatementCallback<T, S> statementCallback, SQLRecognizer sqlRecognizer) {
        this.statementProxy = statementProxy;
        this.statementCallback = statementCallback;
        this.sqlRecognizer = sqlRecognizer;
    }

    @Override
    public Object execute(Object... args) throws Throwable {
        String xid = RootContext.getXID();
        statementProxy.getConnectionProxy().bind(xid);
        return doExecute(args);
    }

    protected abstract Object doExecute(Object... args) throws Throwable;

    protected String buildWhereConditionByPKs(List<Field> pkRows) throws SQLException {
        StringBuffer whereConditionAppender = new StringBuffer();
        for (int i = 0; i < pkRows.size(); i++) {
            Field field = pkRows.get(i);
            whereConditionAppender.append(field.getName() + " = ?");
            if (i < (pkRows.size() - 1)) {
                whereConditionAppender.append(" OR ");
            }
        }
        return whereConditionAppender.toString();
    }

    protected TableMeta getTableMeta() {
        if (tableMeta != null) {
            return tableMeta;
        }
        String tableName = sqlRecognizer.getTableName();
        List<String> sqlHints = sqlRecognizer.getSqlHints();
        tableMeta = TableMetaCache.getTableMeta(sqlHints, tableName, statementProxy.getConnectionProxy().getDataSourceProxy());
        return tableMeta;
    }

    protected void prepareUndoLog(TableRecords beforeImage, TableRecords afterImage) {
        ConnectionProxy connectionProxy = statementProxy.getConnectionProxy();
        SQLType sqlType = sqlRecognizer.getSQLType();

        TableRecords lockKeyRecords = afterImage;
        if (sqlType == SQLType.DELETE) {
            lockKeyRecords = beforeImage;
        }
        String lockKeys = buildLockKey(lockKeyRecords);
        connectionProxy.appendLockKey(lockKeys);

        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setSqlType(sqlType);
        sqlUndoLog.setSqlHints(sqlRecognizer.getSqlHints());
        sqlUndoLog.setTableName(sqlRecognizer.getTableName());
        sqlUndoLog.setBeforeImage(beforeImage);
        sqlUndoLog.setAfterImage(afterImage);
        connectionProxy.appendUndoItem(sqlUndoLog);
    }

    /**
     * ${tableName}:${pkV1},${pkV2},...
     *
     * @param rowsIncludingPK
     * @return
     */
    protected String buildLockKey(TableRecords rowsIncludingPK) {
        if (rowsIncludingPK.size() == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(rowsIncludingPK.getTableMeta().getTableName());
        sb.append(":");

        boolean flag = false;
        for (Field field : rowsIncludingPK.pkRows()) {
            if (flag) {
                sb.append(",");
            } else {
                flag = true;
            }
            sb.append(field.getValue());
        }
        PluginManager pluginManager = getPluginManager();
        String lockKeys = pluginManager.execLockKeyBuildAfter(sqlRecognizer.getSqlHints(), rowsIncludingPK, sb.toString());
        return lockKeys;
    }

    protected PluginManager getPluginManager() {
        ConnectionProxy connectionProxy = statementProxy.getConnectionProxy();
        PluginManager pluginManager = connectionProxy.getDataSourceProxy().getPluginManager();
        return pluginManager;
    }

    protected String prepareSql(String originSql) {
        List<String> hints = sqlRecognizer.getSqlHints();
        String sql = getPluginManager().execSqlBuildAfter(hints, originSql);
        return sql;
    }


}
