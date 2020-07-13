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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;

import com.google.common.collect.Lists;
import io.seata.common.exception.NotSupportYetException;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.CollectionUtils;
import io.seata.rm.datasource.ColumnUtils;
import io.seata.rm.datasource.PreparedStatementProxy;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.sql.struct.ColumnMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.sqlparser.SQLInsertRecognizer;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.struct.Null;
import io.seata.sqlparser.struct.Sequenceable;
import io.seata.sqlparser.struct.SqlDefaultExpr;
import io.seata.sqlparser.struct.SqlMethodExpr;
import io.seata.sqlparser.struct.SqlSequenceExpr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Base Insert Executor.
 * @author jsbxyyx
 */
public abstract class BaseInsertExecutor<T, S extends Statement> extends AbstractDMLBaseExecutor<T, S> implements InsertExecutor<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseInsertExecutor.class);

    protected static final String PLACEHOLDER = "?";

    /**
     * Instantiates a new Abstract dml base executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizer     the sql recognizer
     */
    public BaseInsertExecutor(StatementProxy<S> statementProxy, StatementCallback<T, S> statementCallback,
                              SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
    }

    @Override
    protected TableRecords beforeImage() throws SQLException {
        return TableRecords.empty(getTableMeta());
    }

    @Override
    protected TableRecords afterImage(TableRecords beforeImage) throws SQLException {
        Map<String,List<Object>> pkValues = getPkValues();
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
        return !((SQLInsertRecognizer) sqlRecognizer).insertColumnsIsEmpty();
    }

    /**
     * get pk index
     * @return the key is pk column name and the value is index of the pk column
     */
    protected Map<String,Integer> getPkIndex() {
        Map<String,Integer> pkIndexMap = new HashMap<>();
        SQLInsertRecognizer recognizer = (SQLInsertRecognizer) sqlRecognizer;
        List<String> insertColumns = recognizer.getInsertColumns();
        if (CollectionUtils.isNotEmpty(insertColumns)) {
            final int insertColumnsSize = insertColumns.size();
            for (int paramIdx = 0; paramIdx < insertColumnsSize; paramIdx++) {
                String sqlColumnName = insertColumns.get(paramIdx);
                if (containPK(sqlColumnName)) {
                    pkIndexMap.put(getStandardColumnName(sqlColumnName),paramIdx);
                }
            }
            return pkIndexMap;
        }
        int pkIndex = -1;
        Map<String, ColumnMeta> allColumns = getTableMeta().getAllColumns();
        for (Map.Entry<String, ColumnMeta> entry : allColumns.entrySet()) {
            pkIndex++;
            if (containPK(entry.getValue().getColumnName())) {
                pkIndexMap.put(ColumnUtils.delEscape(entry.getValue().getColumnName(),getDbType()),pkIndex);
            }
        }
        return pkIndexMap;
    }


    /**
     * parse primary key value from statement.
     * @return
     */
    protected Map<String,List<Object>> parsePkValuesFromStatement() {
        // insert values including PK
        SQLInsertRecognizer recognizer = (SQLInsertRecognizer) sqlRecognizer;
        final Map<String, Integer> pkIndexMap = getPkIndex();
        if (pkIndexMap.isEmpty()) {
            throw new ShouldNeverHappenException("pkIndex is not found");
        }
        Map<String,List<Object>> pkValuesMap = new HashMap<>();
        boolean ps = true;
        if (statementProxy instanceof PreparedStatementProxy) {
            PreparedStatementProxy preparedStatementProxy = (PreparedStatementProxy) statementProxy;

            List<List<Object>> insertRows = recognizer.getInsertRows(pkIndexMap.values());
            if (insertRows != null && !insertRows.isEmpty()) {
                Map<Integer,ArrayList<Object>> parameters = preparedStatementProxy.getParameters();
                final int rowSize = insertRows.size();
                int totalPlaceholderNum = -1;
                for (int i = 0; i < rowSize; i++) {
                    List<Object> row = insertRows.get(i);
                    // oracle insert sql statement specify RETURN_GENERATED_KEYS will append :rowid on sql end
                    // insert parameter count will than the actual +1
                    if (row.isEmpty()) {
                        continue;
                    }
                    int currentRowPlaceholderNum = -1;
                    for (Object r : row) {
                        if (PLACEHOLDER.equals(r)) {
                            totalPlaceholderNum += 1;
                            currentRowPlaceholderNum += 1;
                        }
                    }
                    for (String pkKey:pkIndexMap.keySet())
                    {
                        List pkValues = pkValuesMap.get(pkKey);
                        if (Objects.isNull(pkValues))
                        {
                            pkValues = new ArrayList(rowSize);
                        }
                        int pkIndex = pkIndexMap.get(pkKey);
                        Object pkValue = row.get(pkIndex);
                        if (PLACEHOLDER.equals(pkValue)) {
                            int currentRowNotPlaceholderNumBeforePkIndex = 0;
                            for (int n = 0, len = row.size(); n < len; n++) {
                                Object r = row.get(n);
                                if (n < pkIndex && !PLACEHOLDER.equals(r)) {
                                    currentRowNotPlaceholderNumBeforePkIndex++;
                                }
                            }
                            int idx = totalPlaceholderNum - currentRowPlaceholderNum + pkIndex - currentRowNotPlaceholderNumBeforePkIndex;
                            ArrayList<Object> parameter = parameters.get(idx + 1);
                            pkValues.addAll(parameter);
                        } else {
                            pkValues.add(pkValue);
                        }
                        if (!pkValuesMap.containsKey(ColumnUtils.delEscape(pkKey, getDbType())))
                        {
                            pkValuesMap.put(ColumnUtils.delEscape(pkKey, getDbType()), pkValues);
                        }
                    }
                }
            }
        } else {
            ps = false;
            List<List<Object>> insertRows = recognizer.getInsertRows(pkIndexMap.values());
            for (List<Object> row : insertRows) {
                for (String pkKey:pkIndexMap.keySet()) {
                    int pkIndex = pkIndexMap.get(pkKey);
                    List<Object> pkValues = pkValuesMap.get(pkKey);
                    if (Objects.isNull(pkValues)) {
                        pkValuesMap.put(ColumnUtils.delEscape(pkKey, getDbType()), Lists.newArrayList(row.get(pkIndex)));
                    } else {
                        pkValues.add(row.get(pkIndex));
                    }
                }
            }
        }
        if (pkValuesMap.isEmpty()) {
            throw new ShouldNeverHappenException();
        }
        boolean b = this.checkPkValues(pkValuesMap, ps);
        if (!b) {
            throw new NotSupportYetException(String.format("not support sql [%s]", sqlRecognizer.getOriginalSQL()));
        }
        return pkValuesMap;
    }

    /**
     * default get generated keys.
     * @return
     * @throws SQLException
     */
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
        return pkValues;
    }

    /**
     * the modify for test
     */
    protected List<Object> getPkValuesBySequence(SqlSequenceExpr expr) throws SQLException {
        List<Object> pkValues = null;
        try {
            pkValues = getGeneratedKeys();
        } catch (NotSupportYetException | SQLException ignore) {
        }

        if (!CollectionUtils.isEmpty(pkValues)) {
            return pkValues;
        }

        Sequenceable sequenceable = (Sequenceable) this;
        final String sql = sequenceable.getSequenceSql(expr);
        LOGGER.warn("Fail to get auto-generated keys, use '{}' instead. Be cautious, statement could be polluted. Recommend you set the statement to return generated keys.", sql);

        ResultSet genKeys;
        genKeys = statementProxy.getConnection().createStatement().executeQuery(sql);
        pkValues = new ArrayList<>();
        while (genKeys.next()) {
            Object v = genKeys.getObject(1);
            pkValues.add(v);
        }
        return pkValues;
    }

    /**
     * check pk values for multi Pk
     * At most one null per row.
     * Method is not allowed.
     * @param pkValues
     * @return
     */
    protected boolean checkPkValuesForMultiPk(Map<String,List<Object>> pkValues) {
        Set<String> pkNames = pkValues.keySet();
        if (pkNames.isEmpty())
        {
            throw new ShouldNeverHappenException();
        }
        int rowSize = pkValues.get(pkNames.iterator().next()).size();
        for (int i = 0;i < rowSize; i++)
        {
            int n = 0;
            int m = 0;
            for (String name : pkNames)
            {
                Object pkValue = pkValues.get(name).get(i);
                if (pkValue instanceof Null) {
                    n++;
                }
                if (pkValue instanceof SqlMethodExpr) {
                    m++;
                }
            }
            if (n > 1)
            {
                return false;
            }
            if (m > 0)
            {
                return false;
            }
        }
        return true;
    }

    protected boolean checkPkValues(Map<String,List<Object>> pkValues, boolean ps) {
        Set<String> pkNames = pkValues.keySet();
        if (pkNames.size() == 1) {
            return checkPkValuesForSinglePk(pkValues.get(pkNames.iterator().next()),ps);
        }
        else {
            return checkPkValuesForMultiPk(pkValues);
        }
    }

    /**
     * check pk values for single pk
     * @param pkValues
     * @param ps true: is prepared statement. false: normal statement.
     * @return true: support. false: not support.
     */
    protected boolean checkPkValuesForSinglePk(List<Object> pkValues, boolean ps) {
        /*
        ps = true
        -----------------------------------------------
                  one    more
        null       O      O
        value      O      O
        method     O      O
        sequence   O      O
        default    O      O
        -----------------------------------------------
        ps = false
        -----------------------------------------------
                  one    more
        null       O      X
        value      O      O
        method     X      X
        sequence   O      X
        default    O      X
        -----------------------------------------------
        */
        int n = 0, v = 0, m = 0, s = 0, d = 0;
        for (Object pkValue : pkValues) {
            if (pkValue instanceof Null) {
                n++;
                continue;
            }
            if (pkValue instanceof SqlMethodExpr) {
                m++;
                continue;
            }
            if (pkValue instanceof SqlSequenceExpr) {
                s++;
                continue;
            }
            if (pkValue instanceof SqlDefaultExpr) {
                d++;
                continue;
            }
            v++;
        }

        if (!ps) {
            if (m > 0) {
                return false;
            }
            if (n == 1 && v == 0 && m == 0 && s == 0 && d == 0) {
                return true;
            }
            if (n == 0 && v > 0 && m == 0 && s == 0 && d == 0) {
                return true;
            }
            if (n == 0 && v == 0 && m == 0 && s == 1 && d == 0) {
                return true;
            }
            if (n == 0 && v == 0 && m == 0 && s == 0 && d == 1) {
                return true;
            }
            return false;
        }

        if (n > 0 && v == 0 && m == 0 && s == 0 && d == 0) {
            return true;
        }
        if (n == 0 && v > 0 && m == 0 && s == 0 && d == 0) {
            return true;
        }
        if (n == 0 && v == 0 && m > 0 && s == 0 && d == 0) {
            return true;
        }
        if (n == 0 && v == 0 && m == 0 && s > 0 && d == 0) {
            return true;
        }
        if (n == 0 && v == 0 && m == 0 && s == 0 && d > 0) {
            return true;
        }
        return false;
    }

}
