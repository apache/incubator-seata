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

import com.alibaba.druid.util.JdbcConstants;
import io.seata.common.exception.NotSupportYetException;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.rm.datasource.PreparedStatementProxy;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.sql.SQLInsertRecognizer;
import io.seata.rm.datasource.sql.SQLRecognizer;
import io.seata.rm.datasource.sql.struct.ColumnMeta;
import io.seata.rm.datasource.sql.struct.Null;
import io.seata.rm.datasource.sql.struct.SqlMethodExpr;
import io.seata.rm.datasource.sql.struct.SqlSequenceExpr;
import io.seata.rm.datasource.sql.struct.TableRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Insert executor.
 *
 * @param <T> the type parameter
 * @param <S> the type parameter
 * @author yuanguoyao
 */
public class InsertExecutor<T, S extends Statement> extends AbstractDMLBaseExecutor<T, S> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InsertExecutor.class);
    protected static final String ERR_SQL_STATE = "S1009";

    private static final String PLACEHOLDER = "?";

    /**
     * Instantiates a new Insert executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizer     the sql recognizer
     */
    public InsertExecutor(StatementProxy statementProxy, StatementCallback statementCallback,
                          SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
    }

    @Override
    protected TableRecords beforeImage() throws SQLException {
        return TableRecords.empty(getTableMeta());
    }

    @Override
    protected TableRecords afterImage(TableRecords beforeImage) throws SQLException {
        //Pk column exists or PK is just auto generated
        List<Object> pkValues = containsPK() ? getPkValuesByColumn() :
                (containsColumns() ? getPkValuesByAuto() : getPkValuesByColumn());

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

    protected boolean containsColumns() {
        SQLInsertRecognizer recognizer = (SQLInsertRecognizer) sqlRecognizer;
        List<String> insertColumns = recognizer.getInsertColumns();
        return insertColumns != null && !insertColumns.isEmpty();
    }

    protected List<Object> getPkValuesByColumn() throws SQLException {
        // insert values including PK
        SQLInsertRecognizer recognizer = (SQLInsertRecognizer) sqlRecognizer;
        final int pkIndex = getPkIndex();
        if (pkIndex == -1) {
            throw new ShouldNeverHappenException("pkIndex is " + pkIndex);
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
                        int finalPkIndex = pkIndex;
                        pkValues = insertRows.stream().map(insertRow -> insertRow.get(finalPkIndex)).collect(Collectors.toList());
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
                            for (Object obj : parameter) {
                                pkValues.add(obj);
                            }
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
            throw new NotSupportYetException("not support sql [" + sqlRecognizer.getOriginalSQL() + "]");
        }
        if (pkValues.size() > 0 && pkValues.get(0) instanceof SqlSequenceExpr) {
            pkValues = getPkValuesBySequence(pkValues.get(0));
        }
        // pk auto generated while single insert primary key is expression
        else if (pkValues.size() == 1 && pkValues.get(0) instanceof SqlMethodExpr) {
            pkValues = getPkValuesByAuto();
        }
        // pk auto generated while column exists and value is null
        else if (pkValues.size() > 0 && pkValues.get(0) instanceof Null) {
            pkValues = getPkValuesByAuto();
        }
        return pkValues;
    }

    protected List<Object> getPkValuesBySequence(Object expr) throws SQLException {

        // priority use getGeneratedKeys
        try {
            return oracleByAuto();
        } catch (NotSupportYetException | SQLException ignore) {
        }

        ResultSet genKeys = null;
        if (expr instanceof SqlSequenceExpr) {
            SqlSequenceExpr sequenceExpr = (SqlSequenceExpr) expr;
            final String sql = "SELECT " + sequenceExpr.getSequence() + ".currval FROM DUAL";
            LOGGER.warn("Fail to get auto-generated keys, use \'{}\' instead. Be cautious, statement could be polluted. Recommend you set the statement to return generated keys.", sql);
            genKeys = statementProxy.getConnection().createStatement().executeQuery(sql);
        } else {
            throw new NotSupportYetException(String.format("not support expr [%s]", expr.getClass().getName()));
        }
        List<Object> pkValues = new ArrayList<>();
        while (genKeys.next()) {
            Object v = genKeys.getObject(1);
            pkValues.add(v);
        }
        return pkValues;
    }

    protected List<Object> getPkValuesByAuto() throws SQLException {
        boolean oracle = StringUtils.equalsIgnoreCase(JdbcConstants.ORACLE, getDbType());
        if (oracle) {
            return oracleByAuto();
        }
        return defaultByAuto();
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
     * @return true support false not support
     */
    private boolean checkPkValues(List<Object> pkValues) {
        boolean pkParameterHasNull = false;
        boolean pkParameterHasNotNull = false;
        boolean pkParameterHasExpr = false;
        if (pkValues.size() == 1) {
            return true;
        }
        for (Object pkValue : pkValues) {
            if (pkValue instanceof Null) {
                pkParameterHasNull = true;
                continue;
            }
            pkParameterHasNotNull = true;
            if (pkValue instanceof SqlMethodExpr) {
                pkParameterHasExpr = true;
            }
        }
        if (pkParameterHasExpr) {
            return false;
        }
        if (pkParameterHasNull && pkParameterHasNotNull) {
            return false;
        }
        return true;
    }

    /**
     * default auto increment
     * @return the primary key value
     * @throws SQLException the SQL exception
     */
    private List<Object> defaultByAuto() throws SQLException {
        // PK is just auto generated
        Map<String, ColumnMeta> pkMetaMap = getTableMeta().getPrimaryKeyMap();
        if (pkMetaMap.size() != 1) {
            throw new NotSupportYetException();
        }
        ColumnMeta pkMeta = pkMetaMap.values().iterator().next();
        if (!pkMeta.isAutoincrement()) {
            throw new ShouldNeverHappenException();
        }

        ResultSet genKeys = null;
        try {
            genKeys = statementProxy.getTargetStatement().getGeneratedKeys();
        } catch (SQLException e) {
            // java.sql.SQLException: Generated keys not requested. You need to
            // specify Statement.RETURN_GENERATED_KEYS to
            // Statement.executeUpdate() or Connection.prepareStatement().
            if (ERR_SQL_STATE.equalsIgnoreCase(e.getSQLState())) {
                LOGGER.warn("Fail to get auto-generated keys, use \'SELECT LAST_INSERT_ID()\' instead. Be cautious, statement could be polluted. Recommend you set the statement to return generated keys.");
                genKeys = statementProxy.getTargetStatement().executeQuery("SELECT LAST_INSERT_ID()");
            } else {
                throw e;
            }
        }
        List<Object> pkValues = new ArrayList<>();
        while (genKeys.next()) {
            Object v = genKeys.getObject(1);
            pkValues.add(v);
        }
        try {
            genKeys.beforeFirst();
        } catch (SQLException e) {
            LOGGER.warn("Fail to reset ResultSet cursor. can not get primary key value");
        }
        return pkValues;
    }

    /**
     * oracle auto increment sequence
     * @return the primary key value
     * @throws SQLException the SQL exception
     */
    private List<Object> oracleByAuto() throws SQLException {
        Map<String, ColumnMeta> pkMetaMap = getTableMeta().getPrimaryKeyMap();
        if (pkMetaMap.size() != 1) {
            throw new NotSupportYetException();
        }
        ResultSet genKeys = null;
        try {
            genKeys = statementProxy.getTargetStatement().getGeneratedKeys();
        } catch (SQLException e) {
            throw e;
        }
        List<Object> pkValues = new ArrayList<>();
        while (genKeys.next()) {
            Object v = genKeys.getObject(1);
            pkValues.add(v);
        }
        if (pkValues.isEmpty()) {
            throw new NotSupportYetException("not support sql [" + sqlRecognizer.getOriginalSQL() + "]");
        }
        return pkValues;
    }

}
