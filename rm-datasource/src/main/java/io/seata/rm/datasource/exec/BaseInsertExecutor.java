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
package io.seata.rm.datasource.exec;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.seata.common.exception.NotSupportYetException;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.rm.datasource.PreparedStatementProxy;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.sql.struct.ColumnMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.sqlparser.SQLInsertRecognizer;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.struct.Null;
import io.seata.sqlparser.struct.SqlMethodExpr;
import io.seata.sqlparser.struct.SqlSequenceExpr;
import io.seata.sqlparser.util.JdbcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Base InsertExecutor
 * @author tt
 */
public abstract class BaseInsertExecutor<T, S extends Statement> extends AbstractDMLBaseExecutor<T, S> {

    protected static final String PLACEHOLDER = "?";

    /**
     * Instantiates a new Abstract dml base executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizer     the sql recognizer
     */
    public BaseInsertExecutor(StatementProxy<S> statementProxy, StatementCallback<T, S> statementCallback, SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
    }

    @Override
    protected TableRecords beforeImage() throws SQLException {
        return TableRecords.empty(getTableMeta());
    }

    @Override
    protected TableRecords afterImage(TableRecords beforeImage) throws SQLException {
        List<Object> pkValues = getPkValues();
        TableRecords afterImage = buildTableRecords(pkValues);
        if (afterImage == null) {
            throw new SQLException("Failed to build after-image for insert");
        }
        return afterImage;
    }

    protected boolean containsPK() {
        SQLInsertRecognizer recognizer = (SQLInsertRecognizer) sqlRecognizer;
        List<String> insertColumns = recognizer.getInsertColumns();
        if (CollectionUtils.isEmpty(insertColumns)) {
            return false;
        }
        return containsPK(insertColumns);
    }

    /**
     * judge sql specify column
     * @return true: contains column. false: not contains column.
     */
    protected boolean containsColumns() {
        SQLInsertRecognizer recognizer = (SQLInsertRecognizer) sqlRecognizer;
        List<String> insertColumns = recognizer.getInsertColumns();
        return insertColumns != null && !insertColumns.isEmpty();
    }

    /**
     * get primary key column index.
     * @return -1: not found.
     */
    protected int getPkIndex() {
        SQLInsertRecognizer recognizer = (SQLInsertRecognizer) sqlRecognizer;
        List<String> insertColumns = recognizer.getInsertColumns();
        if (CollectionUtils.isNotEmpty(insertColumns)) {
            final int insertColumnsSize = insertColumns.size();
            int pkIndex = -1;
            for (int paramIdx = 0; paramIdx < insertColumnsSize; paramIdx++) {
                if (equalsPK(insertColumns.get(paramIdx))) {
                    pkIndex = paramIdx;
                    break;
                }
            }
            return pkIndex;
        }
        int pkIndex = -1;
        Map<String, ColumnMeta> allColumns = getTableMeta().getAllColumns();
        for (Map.Entry<String, ColumnMeta> entry : allColumns.entrySet()) {
            pkIndex++;
            if (equalsPK(entry.getValue().getColumnName())) {
                break;
            }
        }
        return pkIndex;
    }

    /**
     * parse primary key value from statement.
     * @return
     */
    protected List<Object> parsePkValuesFromStatement() {
        // insert values including PK
        SQLInsertRecognizer recognizer = (SQLInsertRecognizer) sqlRecognizer;
        final int pkIndex = getPkIndex();
        if (pkIndex == -1) {
            throw new ShouldNeverHappenException(String.format("pkIndex is %d", pkIndex));
        }
        List<Object> pkValues = null;
        if (statementProxy instanceof PreparedStatementProxy) {
            PreparedStatementProxy preparedStatementProxy = (PreparedStatementProxy) statementProxy;

            List<List<Object>> insertRows = recognizer.getInsertRows();
            if (insertRows != null && !insertRows.isEmpty()) {
                ArrayList<Object>[] parameters = preparedStatementProxy.getParameters();
                final int rowSize = insertRows.size();

                if (rowSize == 1) {
                    Object pkValue = insertRows.get(0).get(pkIndex);
                    if (PLACEHOLDER.equals(pkValue)) {
                        pkValues = parameters[pkIndex];
                    } else {
                        pkValues = insertRows.stream().map(insertRow -> insertRow.get(pkIndex)).collect(Collectors.toList());
                    }
                } else {
                    int totalPlaceholderNum = -1;
                    pkValues = new ArrayList<>(rowSize);
                    for (int i = 0; i < rowSize; i++) {
                        List<Object> row = insertRows.get(i);
                        // oracle insert sql statement specify RETURN_GENERATED_KEYS will append :rowid on sql end
                        // insert parameter count will than the actual +1
                        if (row.isEmpty()) {
                            continue;
                        }
                        Object pkValue = row.get(pkIndex);
                        int currentRowPlaceholderNum = -1;
                        for (Object r : row) {
                            if (PLACEHOLDER.equals(r)) {
                                totalPlaceholderNum += 1;
                                currentRowPlaceholderNum += 1;
                            }
                        }
                        if (PLACEHOLDER.equals(pkValue)) {
                            int idx = pkIndex;
                            if (i != 0) {
                                idx = totalPlaceholderNum - currentRowPlaceholderNum + pkIndex;
                            }
                            ArrayList<Object> parameter = parameters[idx];
                            pkValues.addAll(parameter);
                        } else {
                            pkValues.add(pkValue);
                        }
                    }
                }
            }
        } else {
            List<List<Object>> insertRows = recognizer.getInsertRows();
            pkValues = new ArrayList<>(insertRows.size());
            for (List<Object> row : insertRows) {
                pkValues.add(row.get(pkIndex));
            }
        }
        if (pkValues == null) {
            throw new ShouldNeverHappenException();
        }
        boolean b = this.checkPkValues(pkValues);
        if (!b) {
            throw new NotSupportYetException(String.format("not support sql [%s]", sqlRecognizer.getOriginalSQL()));
        }
        return pkValues;
    }

    /**
     * get primary key values by default
     * @return
     * @throws SQLException
     */
    private List<Object> getPkValuesByDefault() throws SQLException {
        // current version 1.2 only support postgresql.
        // mysql default keyword the logic not support. (sample: insert into test(id, name) values(default, 'xx'))
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
     * get primary key values by sequence.
     * @param expr
     * @return
     * @throws SQLException
     */
    protected List<Object> getPkValuesBySequence(Object expr) throws SQLException {
        // priority use defaultGeneratedKeys
        List<Object> pkValues = null;
        try {
            pkValues = defaultGeneratedKeys();
        } catch (NotSupportYetException | SQLException ignore) {
        }

        if (!CollectionUtils.isEmpty(pkValues)) {
            return pkValues;
        }

        ResultSet genKeys;
        if (expr instanceof SqlSequenceExpr) {
            SqlSequenceExpr sequenceExpr = (SqlSequenceExpr) expr;
            String sql = "SELECT " + sequenceExpr.getSequence() + ".currval FROM DUAL";
            if (StringUtils.equalsIgnoreCase(JdbcConstants.POSTGRESQL, getDbType())) {
                sql = "SELECT currval(" + sequenceExpr.getSequence() + ")";
            }
            LOGGER.warn("Fail to get auto-generated keys, use '{}' instead. Be cautious, statement could be polluted. Recommend you set the statement to return generated keys.", sql);
            genKeys = statementProxy.getConnection().createStatement().executeQuery(sql);
        } else {
            throw new NotSupportYetException(String.format("not support expr [%s]", expr.getClass().getName()));
        }
        pkValues = new ArrayList<>();
        while (genKeys.next()) {
            Object v = genKeys.getObject(1);
            pkValues.add(v);
        }
        return pkValues;
    }

    protected List<Object> getPkValuesByAuto() throws SQLException {
        boolean mysql = StringUtils.equalsIgnoreCase(JdbcConstants.MYSQL, getDbType());
        if (mysql) {
            return mysqlGeneratedKeys();
        }
        return defaultGeneratedKeys();
    }

    /**
     * get pk index
     * @return -1 not found pk index
     */
    protected int getPkIndex() {
        SQLInsertRecognizer recognizer = (SQLInsertRecognizer) sqlRecognizer;
        List<String> insertColumns = recognizer.getInsertColumns();
        if (CollectionUtils.isNotEmpty(insertColumns)) {
            final int insertColumnsSize = insertColumns.size();
            int pkIndex = -1;
            for (int paramIdx = 0; paramIdx < insertColumnsSize; paramIdx++) {
                if (equalsPK(insertColumns.get(paramIdx))) {
                    pkIndex = paramIdx;
                    break;
                }
            }
            return pkIndex;
        }
        int pkIndex = -1;
        Map<String, ColumnMeta> allColumns = getTableMeta().getAllColumns();
        for (Map.Entry<String, ColumnMeta> entry : allColumns.entrySet()) {
            pkIndex++;
            if (equalsPK(entry.getValue().getColumnName())) {
                break;
            }
        }
        return pkIndex;
    }

    /**
     * check pk values
     * @param pkValues
     * @return true: support. false: not support.
     */
    protected boolean checkPkValues(List<Object> pkValues) {
        /*
        -----------------------------------------------
                  one    more
        null       O      O
        value      O      O
        method     X      X
        sequence   O      X
        -----------------------------------------------
                  null    value    method    sequence
        null       O        X         X         X
        value      X        O         X         X
        method     X        X         X         X
        sequence   X        X         X         X
        -----------------------------------------------
        */
        int n = 0, v = 0, m = 0, s = 0;
        for (Object pkValue : pkValues) {
            if (pkValue instanceof Null) {
                n++;
                continue;
            }
            if (pkValue instanceof SqlMethodExpr) {
                m++;
                break;
            }
            if (pkValue instanceof SqlSequenceExpr) {
                s++;
                continue;
            }
            v++;
        }
        // not support sql primary key is function.
        if (m > 0) {
            return false;
        }
        if (n > 0 && v == 0 && s == 0) {
            return true;
        }
        if (n == 0 && v > 0 && s == 0) {
            return true;
        }
        if (n == 0 && v == 0 && s == 1) {
            return true;
        }
        return false;
    }

    /**
     * get primary key values
     * @return
     */
    protected abstract List<Object> getPkValues() throws SQLException;

}
