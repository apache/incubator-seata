/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.rm.datasource.exec.sqlserver;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.loader.Scope;
import org.apache.seata.rm.datasource.StatementProxy;
import org.apache.seata.rm.datasource.exec.BaseInsertExecutor;
import org.apache.seata.rm.datasource.exec.StatementCallback;
import org.apache.seata.sqlparser.struct.ColumnMeta;
import org.apache.seata.sqlparser.SQLInsertRecognizer;
import org.apache.seata.sqlparser.SQLRecognizer;
import org.apache.seata.sqlparser.struct.Defaultable;
import org.apache.seata.sqlparser.struct.Null;
import org.apache.seata.sqlparser.struct.Sequenceable;
import org.apache.seata.sqlparser.struct.SqlDefaultExpr;
import org.apache.seata.sqlparser.struct.SqlMethodExpr;
import org.apache.seata.sqlparser.struct.SqlSequenceExpr;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type MS SqlServer insert executor.
 *
 */
@LoadLevel(name = JdbcConstants.SQLSERVER, scope = Scope.PROTOTYPE)
public class SqlServerInsertExecutor extends BaseInsertExecutor implements Sequenceable, Defaultable {
    private static final Logger LOGGER = LoggerFactory.getLogger(SqlServerInsertExecutor.class);

    /**
     * Instantiates a new Abstract dml base executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizer     the sql recognizer
     */
    public SqlServerInsertExecutor(StatementProxy statementProxy, StatementCallback statementCallback, SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
    }

    @Override
    public Map<String, List<Object>> getPkValues() throws SQLException {
        Map<String, List<Object>> pkValuesMap;
        boolean isContainsPk = containsPK();
        List<String> pkColumnNameList = getTableMeta().getPrimaryKeyOnlyName();

        if (pkColumnNameList.size() == 1) {
            //when there is only one pk in the table, which means only one column is used to form the primary key
            if (isContainsPk) {
                pkValuesMap = getPkValuesByColumn();
            } else if (containsColumns()) {
                String columnName = getTableMeta().getPrimaryKeyOnlyName().get(0);
                pkValuesMap = Collections.singletonMap(columnName, getGeneratedKeys());
            } else {
                pkValuesMap = getPkValuesWithNoColumn();
            }
        } else {
            //when there is a composite primary key
            throw new NotSupportYetException("composite primary key is not supported in sqlserver");
        }

        return pkValuesMap;
    }

    @Override
    public Map<String, List<Object>> getPkValuesByColumn() throws SQLException {
        Map<String, List<Object>> pkValuesMap = parsePkValuesFromStatement();
        Set<String> keySet = new HashSet<>(pkValuesMap.keySet());
        //auto increment
        for (String pkKey : keySet) {
            List<Object> pkValues = pkValuesMap.get(pkKey);
            //there is generally only one generation strategy for the primary key of the same table
            if (!pkValues.isEmpty() && pkValues.get(0) instanceof SqlSequenceExpr) {
                pkValuesMap.put(pkKey, getPkValuesBySequence((SqlSequenceExpr) pkValues.get(0)));
            } else if (!pkValues.isEmpty() && pkValues.get(0) instanceof SqlDefaultExpr) {
                //note that the DEFAULT keyword cannot be applied to the Identity column
                pkValuesMap.put(pkKey, getPkValuesByDefault());
            } else if (!pkValues.isEmpty() && pkValues.get(0) instanceof SqlMethodExpr) {
                pkValuesMap.put(pkKey, getGeneratedKeys());
            } else if (!pkValues.isEmpty() && pkValues.get(0) instanceof Null) {
                throw new NotSupportYetException("ms_sqlserver not support null");
            }
        }
        return pkValuesMap;
    }

    @Override
    public String getSequenceSql(SqlSequenceExpr expr) {
        return "SELECT current_value FROM sys.sequences WHERE name = " + expr.getSequence();
    }

    @Override
    public List<Object> getPkValuesByDefault() {
        //Get form the tableMetaData
        throw new NotSupportYetException("Default value is not yet supported");
    }

    @Override
    public List<Object> getPkValuesByDefault(String pkKey) throws SQLException {
        throw new NotSupportYetException("Default value with multi pkKey is not yet supported");
    }

    @Override
    public List<Object> getGeneratedKeys() throws SQLException {
        // PK is just auto generated
        ResultSet genKeys = statementProxy.getGeneratedKeys();
        List<Object> pkValues = new ArrayList<>();
        while (genKeys.next()) {
            Object v = genKeys.getObject(1);
            pkValues.add(v);
        }
        if (pkValues.isEmpty()) {
            throw new NotSupportYetException(String.format("not support sql [%s]", sqlRecognizer.getOriginalSQL()));
        }
        try {
            genKeys.beforeFirst();
        } catch (SQLException e) {
            LOGGER.warn("Fail to reset ResultSet cursor. can not get primary key value");
        }

        int updateCount = statementProxy.getUpdateCount();
        if (updateCount > 1 && pkValues.size() == 1) {
            //insert multiple rows of values at once, only the latest ID will be returned
            //just like 'insert into test values(?, ?), (?, ?),...'
            Map<String, ColumnMeta> primaryKeyMap = getTableMeta().getPrimaryKeyMap();
            ColumnMeta pkMeta = primaryKeyMap.values().iterator().next();
            if (!pkMeta.isAutoincrement()) {
                throw new SQLException("The primary key value is not isAutoincrement, which should not happen");
            }
            //get the increment
            int increment = 0;
            final String querySql = "SELECT IDENT_INCR('" + getTableMeta().getTableName() + "') As INCR";
            Connection conn = statementProxy.getConnection();
            try (Statement ps = conn.createStatement();
                 ResultSet incr = ps.executeQuery(querySql)) {
                if (incr.next()) {
                    increment = incr.getInt("INCR");
                }
            }
            if (increment < 1) {
                throw new SQLException("the increment for " + getTableMeta().getTableName() + " is illegal");
            }

            //The sqlserver driver uses SCOPE_IDENTITY() to get the primary key value,
            //and the return type of SCOPE_IDENTITY() is numeric(38,0)
            long lastPkValue;
            if (pkValues.get(0) instanceof BigDecimal) {
                lastPkValue = ((BigDecimal) pkValues.get(0)).longValue();
            } else {
                lastPkValue = (long) pkValues.get(0);
            }

            long beginAt = lastPkValue - (long) (updateCount - 1) * increment;
            pkValues = new ArrayList<>();
            for (int i = 0; i < updateCount; i++) {
                pkValues.add(beginAt);
                beginAt += increment;
            }
        }
        return pkValues;
    }

    private Map<String, List<Object>> getPkValuesWithNoColumn() throws SQLException {
        SQLInsertRecognizer recognizer = (SQLInsertRecognizer) sqlRecognizer;
        List<String> insertParamsValue = recognizer.getInsertParamsValue();
        boolean insertWithNoPkValue = insertParamsValue.isEmpty();
        if (!insertWithNoPkValue) {
            String paramsValue = insertParamsValue.get(0);
            String[] split = paramsValue.split(",");
            insertWithNoPkValue = getTableMeta().getAllColumns().size() > split.length;
        }

        if (insertWithNoPkValue) {
            //like 'insert into table_name values (args1, args2)' with no column_list and pkValue
            String columnName = getTableMeta().getPrimaryKeyOnlyName().get(0);
            return Collections.singletonMap(columnName, getGeneratedKeys());
        } else {
            return getPkValuesByColumn();
        }
    }
}
