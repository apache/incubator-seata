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

/**
 * The type Base transactional executor.
 *
 * @param <T> the type parameter
 * @param <S> the type parameter
 */
public abstract class BaseTransactionalExecutor<T, S extends Statement> implements Executor {

    /**
     * The Statement proxy.
     */
    protected StatementProxy<S> statementProxy;

    /**
     * The Statement callback.
     */
    protected StatementCallback<T, S> statementCallback;

    /**
     * The Sql recognizer.
     */
    protected SQLRecognizer sqlRecognizer;

    private TableMeta tableMeta;

    /**
     * Instantiates a new Base transactional executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizer     the sql recognizer
     */
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

    /**
     * Do execute object.
     *
     * @param args the args
     * @return the object
     * @throws Throwable the throwable
     */
    protected abstract Object doExecute(Object... args) throws Throwable;

    /**
     * Build where condition by p ks string.
     *
     * @param pkRows the pk rows
     * @return the string
     * @throws SQLException the sql exception
     */
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

    /**
     * Gets column name in sql.
     *
     * @param columnName the column name
     * @return the column name in sql
     */
    protected String getColumnNameInSQL(String columnName) {
        String tableAlias = sqlRecognizer.getTableAlias();
        if (tableAlias == null) {
            return columnName;
        } else {
            return tableAlias + "." + columnName;
        }
    }

    /**
     * Gets from table in sql.
     *
     * @return the from table in sql
     */
    protected String getFromTableInSQL() {
        String tableName = sqlRecognizer.getTableName();
        String tableAlias = sqlRecognizer.getTableAlias();
        if (tableAlias == null) {
            return tableName;
        } else {
            return tableName + " " + tableAlias;
        }
    }

    /**
     * Gets table meta.
     *
     * @return the table meta
     */
    protected TableMeta getTableMeta() {
        return getTableMeta(sqlRecognizer.getTableName());
    }

    /**
     * Gets table meta.
     *
     * @param tableName the table name
     * @return the table meta
     */
    protected TableMeta getTableMeta(String tableName) {
        if (tableMeta != null) {
            return tableMeta;
        }
        tableMeta = TableMetaCache.getTableMeta(statementProxy.getConnectionProxy().getDataSourceProxy(), tableName);
        return tableMeta;
    }

}
