/*
 *  Copyright 1999-2019 Seata.io Group.
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
package io.seata.rm.datasource.exec.postgresql;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.loader.LoadLevel;
import io.seata.common.loader.Scope;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.exec.BaseInsertExecutor;
import io.seata.rm.datasource.exec.StatementCallback;
import io.seata.sqlparser.struct.ColumnMeta;
import io.seata.sqlparser.SQLInsertRecognizer;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.struct.Defaultable;
import io.seata.sqlparser.struct.Sequenceable;
import io.seata.sqlparser.struct.SqlMethodExpr;
import io.seata.sqlparser.struct.SqlSequenceExpr;
import io.seata.sqlparser.struct.SqlDefaultExpr;
import io.seata.sqlparser.util.ColumnUtils;
import io.seata.sqlparser.util.JdbcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The type Postgresql insert executor.
 *
 * @author jsbxyyx
 */
@LoadLevel(name = JdbcConstants.POSTGRESQL, scope = Scope.PROTOTYPE)
public class PostgresqlInsertExecutor extends BaseInsertExecutor implements Sequenceable, Defaultable {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresqlInsertExecutor.class);

    /**
     * Instantiates a new Abstract dml base executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizer     the sql recognizer
     */
    public PostgresqlInsertExecutor(StatementProxy statementProxy, StatementCallback statementCallback,
                                    SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
    }

    /**
     * 1. If the insert columns are not empty and do not contain any pk columns,
     * it means that there is no pk value in the insert rows, then all the pk values should come from auto-increment.
     * <p>
     * 2. The pk value exists in insert rows. The possible situations are:
     * <ul>
     *     <li>The insert columns are empty: all pk values can be obtained from insert rows</li>
     *     <li>The insert columns contain at least one pk column: first obtain the existing pk value from the insert rows, and other from auto-increment</li>
     * </ul>
     *
     * @return {@link Map}<{@link String}, {@link List}<{@link Object}>>
     * @throws SQLException the sql exception
     */
    @Override
    public Map<String, List<Object>> getPkValues() throws SQLException {
        List<String> pkColumnNameList = getTableMeta().getPrimaryKeyOnlyName();
        Map<String, List<Object>> pkValuesMap = new HashMap<>(pkColumnNameList.size());

        // first obtain the existing pk value from the insert rows (if exists)
        if (!containsColumns() || containsAnyPk()) {
            pkValuesMap.putAll(getPkValuesByColumn());
        }
        // other from auto-increment
        for (String columnName : pkColumnNameList) {
            if (!pkValuesMap.containsKey(columnName)) {
                pkValuesMap.put(columnName, getGeneratedKeys(columnName));
            }
        }
        return pkValuesMap;
    }

    /**
     * Whether the insert columns contain any pk columns
     *
     * @return true: contain at least one pk column. false: do not contain any pk columns
     */
    public boolean containsAnyPk() {
        SQLInsertRecognizer recognizer = (SQLInsertRecognizer)sqlRecognizer;
        List<String> insertColumns = recognizer.getInsertColumns();
        if (CollectionUtils.isEmpty(insertColumns)) {
            return false;
        }
        List<String> pkColumnNameList = getTableMeta().getPrimaryKeyOnlyName();
        if (CollectionUtils.isEmpty(pkColumnNameList)) {
            return false;
        }
        List<String> newColumns = ColumnUtils.delEscape(insertColumns, getDbType());
        return pkColumnNameList.stream().anyMatch(pkColumn -> newColumns.contains(pkColumn)
            || CollectionUtils.toUpperList(newColumns).contains(pkColumn.toUpperCase()));
    }

    @Override
    public Map<String, List<Object>> getPkValuesByColumn() throws SQLException {
        Map<String, List<Object>> pkValuesMap = parsePkValuesFromStatement();
        Set<String> keySet = pkValuesMap.keySet();
        for (String pkKey : keySet) {
            List<Object> pkValues = pkValuesMap.get(pkKey);
            for (int i = 0; i < pkValues.size(); i++) {
                if (!pkKey.isEmpty() && pkValues.get(i) instanceof SqlSequenceExpr) {
                    pkValues.set(i, getPkValuesBySequence((SqlSequenceExpr) pkValues.get(i), pkKey).get(0));
                } else if (!pkKey.isEmpty() && pkValues.get(i) instanceof SqlMethodExpr) {
                    pkValues.set(i, getGeneratedKeys(pkKey).get(0));
                } else if (!pkValues.isEmpty() && pkValues.get(i) instanceof SqlDefaultExpr) {
                    pkValues.set(i, getPkValuesByDefault(pkKey).get(0));
                }
            }
            pkValuesMap.put(pkKey, pkValues);
        }
        return pkValuesMap;
    }

    /**
     * get primary key values by default
     * @return the pk values
     * @throws SQLException the sql exception
     */
    @Override
    @Deprecated
    public List<Object> getPkValuesByDefault() throws SQLException {
        // current version 1.2 only support postgresql.
        Map<String, ColumnMeta> pkMetaMap = getTableMeta().getPrimaryKeyMap();
        ColumnMeta pkMeta = pkMetaMap.values().iterator().next();
        String columnDef = pkMeta.getColumnDef();
        // sample: nextval('test_id_seq'::regclass)
        String seq = org.apache.commons.lang.StringUtils.substringBetween(columnDef, "'", "'");
        String function = org.apache.commons.lang.StringUtils.substringBetween(columnDef, "", "(");
        if (StringUtils.isBlank(seq)) {
            throw new ShouldNeverHappenException("get primary key value failed, cause columnDef is " + columnDef);
        }
        return getPkValuesBySequence(new SqlSequenceExpr("'" + seq + "'", function));
    }

    /**
     * get primary key values by default
     *
     * @param pkKey the pk key
     * @return the primary values
     * @throws SQLException the sql exception
     */
    @Override
    public List<Object> getPkValuesByDefault(String pkKey) throws SQLException {
        // current version 1.2 only support postgresql.
        Map<String, ColumnMeta> pkMetaMap = getTableMeta().getPrimaryKeyMap();
        ColumnMeta pkMeta = pkMetaMap.values().iterator().next();
        String columnDef = pkMeta.getColumnDef();
        // sample: nextval('test_id_seq'::regclass)
        String seq = org.apache.commons.lang.StringUtils.substringBetween(columnDef, "'", "'");
        String function = org.apache.commons.lang.StringUtils.substringBetween(columnDef, "", "(");
        if (StringUtils.isBlank(seq)) {
            throw new ShouldNeverHappenException("get primary key value failed, cause columnDef is " + columnDef);
        }
        return getPkValuesBySequence(new SqlSequenceExpr("'" + seq + "'", function), pkKey);
    }

    @Override
    public String getSequenceSql(SqlSequenceExpr expr) {
        return "SELECT currval(" + expr.getSequence() + ")";
    }

}
