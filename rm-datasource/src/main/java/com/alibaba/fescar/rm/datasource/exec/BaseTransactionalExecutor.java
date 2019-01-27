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
import com.alibaba.fescar.rm.datasource.StatementProxy;
import com.alibaba.fescar.rm.datasource.sql.SQLRecognizer;
import com.alibaba.fescar.rm.datasource.sql.struct.Field;
import com.alibaba.fescar.rm.datasource.sql.struct.TableMeta;
import com.alibaba.fescar.rm.datasource.sql.struct.TableMetaCache;

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
            whereConditionAppender.append(getColumnNameInSQL(field.getName()) + " = ?");
            if (i < (pkRows.size() - 1)) {
                whereConditionAppender.append(" OR ");
            }
        }
        return whereConditionAppender.toString();
    }

    protected String getColumnNameInSQL(String columnName) {
        String tableAlias = sqlRecognizer.getTableAlias();
        if (tableAlias == null) {
            return columnName;
        } else {
            return tableAlias + "." + columnName;
        }
    }

    protected String getFromTableInSQL() {
        String tableName = sqlRecognizer.getTableName();
        String tableAlias = sqlRecognizer.getTableAlias();
        if (tableAlias == null) {
            return tableName;
        } else {
            return tableName + " " + tableAlias;
        }
    }

    protected TableMeta getTableMeta() {
        return getTableMeta(sqlRecognizer.getTableName());
    }

    protected TableMeta getTableMeta(String tableName) {
        if (tableMeta != null) {
            return tableMeta;
        }
        tableMeta = TableMetaCache.getTableMeta(statementProxy.getConnectionProxy().getDataSourceProxy(), tableName);
        return tableMeta;
    }

}
