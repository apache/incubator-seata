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
package org.apache.seata.rm.datasource.exec.db2;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.loader.Scope;
import org.apache.seata.common.util.IOUtil;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.rm.datasource.StatementProxy;
import org.apache.seata.rm.datasource.exec.BaseInsertExecutor;
import org.apache.seata.rm.datasource.exec.StatementCallback;
import org.apache.seata.sqlparser.SQLRecognizer;
import org.apache.seata.sqlparser.struct.ColumnMeta;
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
 * @author qingjiusanliangsan
 * @author GoodBoyCoder
 */
@LoadLevel(name = JdbcConstants.DB2, scope = Scope.PROTOTYPE)
public class DB2InsertExecutor extends BaseInsertExecutor implements Defaultable, Sequenceable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DB2InsertExecutor.class);

    /**
     * The cache of auto increment step of database
     * the key is the db's resource id
     * the value is the step
     */
    public static final Map<String, BigDecimal> RESOURCE_ID_STEP_CACHE = new ConcurrentHashMap<>(8);

    /**
     * Instantiates a new Abstract dml base executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizer     the sql recognizer
     */
    public DB2InsertExecutor(StatementProxy statementProxy, StatementCallback statementCallback,
                             SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
    }

    @Override
    public Map<String, List<Object>> getPkValues() throws SQLException {
        Map<String, List<Object>> pkValuesMap = null;
        Boolean isContainsPk = containsPK();
        List<String> pkColumnNameList = getTableMeta().getPrimaryKeyOnlyName();
        //when there is only one pk in the table
        if (pkColumnNameList.size() == 1) {
            if (isContainsPk) {
                pkValuesMap = getPkValuesByColumn();
            } else if (containsColumns()) {
                pkValuesMap = getPkValuesByAuto();
            } else {
                pkValuesMap = getPkValuesByColumn();
            }
        } else {
            throw new NotSupportYetException("composite primary key is not supported in db2");
        }
        return pkValuesMap;
    }

    public Map<String, List<Object>> getPkValuesByAuto() throws SQLException {
        // PK is just auto generated
        Map<String, List<Object>> pkValuesMap = new HashMap<>(8);
        Map<String, ColumnMeta> pkMetaMap = getTableMeta().getPrimaryKeyMap();
        String autoColumnName = null;
        for (Map.Entry<String, ColumnMeta> entry : pkMetaMap.entrySet()) {
            if (entry.getValue().isAutoincrement()) {
                autoColumnName = entry.getKey();
                break;
            }
        }
        if (StringUtils.isBlank(autoColumnName)) {
            throw new ShouldNeverHappenException("Auto-increment column should not be blank");
        }

        ResultSet genKeys = statementProxy.getGeneratedKeys();
        List<Object> pkValues = new ArrayList<>();
        while (genKeys.next()) {
            Object v = genKeys.getObject(1);
            pkValues.add(v);
        }

        if (pkValues.isEmpty()) {
            LOGGER.error("Fail to get auto-generated keys, use 'IDENTITY_VAL_LOCAL()' instead. Be cautious, " +
                    "statement could be polluted. Recommend you set the statement to return generated keys.");
            int updateCount = statementProxy.getUpdateCount();
            ResultSet firstId = genKeys = statementProxy.getTargetStatement().executeQuery("SELECT identity_val_local() FROM SYSIBM.SYSDUMMY1");

            if (!firstId.next()) {
                throw new ShouldNeverHappenException("Could not get insert primary key value");
            }

            // If there is batch insert
            // do auto increment base LAST_INSERT_ID and variable `auto_increment_increment`
            if (updateCount > 1 && canAutoIncrement(pkMetaMap)) {
                return autoGeneratePks(new BigDecimal(firstId.getString(1)), autoColumnName, updateCount);
            } else {
                pkValues.add(firstId.getObject(1));
            }
        }

        try {
            genKeys.beforeFirst();
        } catch (SQLException e) {
            LOGGER.warn("Fail to reset ResultSet cursor. can not get primary key value");
        }
        pkValuesMap.put(autoColumnName, pkValues);
        return pkValuesMap;
    }

    @Override
    public Map<String, List<Object>> getPkValuesByColumn() throws SQLException {
        Map<String, List<Object>> pkValuesMap = parsePkValuesFromStatement();
        Set<String> keySet = new HashSet<>(pkValuesMap.keySet());
        //auto increment
        for (String pkKey : keySet) {
            List<Object> pkValues = pkValuesMap.get(pkKey);
            // pk auto generated while single insert primary key is expression
            if (pkValues.size() == 1 && (pkValues.get(0) instanceof SqlMethodExpr)) {
                pkValuesMap.putAll(getPkValuesByAuto());
            }
            // pk auto generated while column exists and value is null
            else if (!pkValues.isEmpty() && pkValues.get(0) instanceof Null) {
                pkValuesMap.putAll(getPkValuesByAuto());
            }
            // pk auto generated while column exists and value is default
            else if (!pkValues.isEmpty() && pkValues.get(0) instanceof SqlDefaultExpr) {
                pkValuesMap.putAll(getPkValuesByAuto());
            }
            // pk auto generated while column exists and value gets from sequence
            else if (!pkValues.isEmpty() && pkValues.get(0) instanceof SqlSequenceExpr) {
                pkValuesMap.put(pkKey, getPkValuesBySequence((SqlSequenceExpr) pkValues.get(0)));
            }
        }
        return pkValuesMap;
    }

    @Override
    public List<Object> getPkValuesByDefault() {
        //Get form the tableMetaData
        throw new NotSupportYetException("Default value is not yet supported in db2");
    }

    @Override
    public List<Object> getPkValuesByDefault(String pkKey) throws SQLException {
        throw new NotSupportYetException("Default value with multi pkKey is not yet supported in db2");
    }

    protected Map<String, List<Object>> autoGeneratePks(BigDecimal cursor, String autoColumnName, Integer updateCount) throws SQLException {
        BigDecimal step;
        String resourceId = statementProxy.getConnectionProxy().getDataSourceProxy().getResourceId();
        if (RESOURCE_ID_STEP_CACHE.containsKey(resourceId)) {
            step = RESOURCE_ID_STEP_CACHE.get(resourceId);
        } else {
            step = getIncrementStep();
            RESOURCE_ID_STEP_CACHE.put(resourceId, step);
        }

        List<Object> pkValues = new ArrayList<>();
        for (int i = 0; i < updateCount; i++) {
            cursor = cursor.add(step);
            pkValues.add(cursor);
        }

        Map<String, List<Object>> pkValuesMap = new HashMap<>(1, 1.001f);
        pkValuesMap.put(autoColumnName, pkValues);
        return pkValuesMap;
    }

    protected boolean canAutoIncrement(Map<String, ColumnMeta> primaryKeyMap) {
        if (primaryKeyMap.size() != 1) {
            return false;
        }

        for (ColumnMeta pk : primaryKeyMap.values()) {
            return pk.isAutoincrement();
        }
        return false;
    }

    public BigDecimal getIncrementStep() throws SQLException {
        //Each row represents an identity column that is defined for a table.
        String sqlQuery = "select INCREMENT AS INCR from SYSCAT.COLIDENTATTRIBUTES WHERE TABSCHEMA = ? AND TABNAME = ?";
        ResultSet increment = null;
        try (PreparedStatement pst = statementProxy.getConnectionProxy().prepareStatement(sqlQuery)) {
            pst.setString(1, statementProxy.getConnectionProxy().getSchema());
            pst.setString(2, getTableMeta().getTableName());
            increment = pst.executeQuery();
            if (increment.next()) {
                return new BigDecimal(increment.getString("INCR"));
            }
        } finally {
            IOUtil.close(increment);
        }

        throw new ShouldNeverHappenException("could not get auto_increment step");
    }

    @Override
    public String getSequenceSql(SqlSequenceExpr expr) {
        return "VALUES PREVIOUS VALUE FOR " + expr.getSequence();
    }
}