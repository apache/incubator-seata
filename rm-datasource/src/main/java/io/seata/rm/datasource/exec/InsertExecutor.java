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
import java.util.*;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import io.seata.common.exception.NotSupportYetException;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.rm.datasource.ColumnUtils;

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
    public InsertExecutor(StatementProxy<S> statementProxy, StatementCallback<T,S> statementCallback,
                          SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
    }

    @Override
    protected TableRecords beforeImage() throws SQLException {
        return TableRecords.empty(getTableMeta());
    }

    @Override
    protected TableRecords afterImage(TableRecords beforeImage) throws SQLException {
        SQLInsertRecognizer recognizer = (SQLInsertRecognizer) sqlRecognizer;
        //Pk column exists or PK is just auto generated
        Map<String,List<Object>> pkValuesMap = null;
        List<String> pkColumnNameList=getTableMeta().getPrimaryKeyOnlyName();
        // Not support statement batch insert sql
        List<List<Object>> insertRows=recognizer.getInsertRows();
        boolean moreThanOneRow = Objects.nonNull(insertRows)&&!insertRows.isEmpty()&&insertRows.size()>1;
        boolean isStatement=!(statementProxy instanceof PreparedStatementProxy);
        Boolean isContainsPk=containsPK();
        if (isStatement&&moreThanOneRow)
        {
            //Because we can't obtain the auto increment value if the program run batch insert sql in the form of statement.
            //We can't rollback without the auto increment value.
            throw new NotSupportYetException("Not support statement batch insert sql");
        }
        //when there is only one pk in the table
        if(getTableMeta().getPrimaryKeyOnlyName().size()==1) {
            if(isContainsPk) {
                pkValuesMap=getPkValuesByColumn();
            }
            else if(containsColumns()){
                pkValuesMap=getPkValuesByAuto();
            }
            else{
                pkValuesMap=getPkValuesByColumn();
            }
        }
        else{
            //when there is multiple pk in the table
            //1、all pk columns are filled value.
            //2、the auto increment pk column value is null, and other pk value are not null.
            pkValuesMap=getPkValuesByColumn();

            for(String columnName:pkColumnNameList)
            {
                if(!pkValuesMap.containsKey(columnName))
                {
                    ColumnMeta pkColumnMeta=getTableMeta().getColumnMeta(columnName);
                    if(Objects.nonNull(pkColumnMeta)&&pkColumnMeta.isAutoincrement())
                    {
                        //3、the auto increment pk column is not exits in sql, and other pk are exits also the value is not null.
                        pkValuesMap.putAll(getPkValuesByAuto());
                    }
                }
            }
        }


        TableRecords afterImage = buildTableRecords(pkValuesMap);

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

    /**
     * get pks value
     * @return the key of map is pk column name, the value of map is insert value associate with pk column.
     * @throws SQLException
     */
    protected Map<String,List<Object>> getPkValuesByColumn() throws SQLException {
        // insert values including PK
        SQLInsertRecognizer recognizer = (SQLInsertRecognizer) sqlRecognizer;
        final Map<String, Integer> pkIndexMap = getPkIndex();
        if (pkIndexMap.isEmpty()) {
            throw new ShouldNeverHappenException("pkIndex is not found");
        }
        Map<String,List<Object>> pkValuesMap = new HashMap<>(3);

        if (statementProxy instanceof PreparedStatementProxy) {
            PreparedStatementProxy preparedStatementProxy = (PreparedStatementProxy) statementProxy;

            List<List<Object>> insertRows = recognizer.getInsertRows();
            if (insertRows != null && !insertRows.isEmpty()) {
                ArrayList<Object>[] parameters = preparedStatementProxy.getParameters();
                final int rowSize = insertRows.size();

                if (rowSize == 1) {
                    for(String pkKey:pkIndexMap.keySet())
                    {
                        int pkIndex=pkIndexMap.get(pkKey);
                        List<Object> pkValues=null;
                        Object pkValue = insertRows.get(0).get(pkIndex);
                        if (PLACEHOLDER.equals(pkValue)) {
                            pkValues = parameters[pkIndex];
                        } else {
                            int finalPkIndex = pkIndex;
                            pkValues = insertRows.stream().map(insertRow -> insertRow.get(finalPkIndex)).collect(Collectors.toList());
                        }
                        pkValuesMap.put(ColumnUtils.delEscape(pkKey, getDbType()), pkValues);
                    }
                } else {
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
                        for(String pkKey:pkIndexMap.keySet())
                        {
                            Object pkValue = row.get(pkIndexMap.get(pkKey));
                            List<Object> pkValues=pkValuesMap.get(pkKey);
                            if(Objects.isNull(pkValues))
                            {
                                pkValues=new ArrayList<>();
                            }
                            if (PLACEHOLDER.equals(pkValue)) {
                                int idx = pkIndexMap.get(pkKey);
                                if (i != 0) {
                                    idx = totalPlaceholderNum - currentRowPlaceholderNum + pkIndexMap.get(pkKey);
                                }
                                ArrayList<Object> parameter = parameters[idx];
                                pkValues.addAll(parameter);
                            } else {
                                pkValues.add(pkValue);
                            }
                            pkValuesMap.put(ColumnUtils.delEscape(pkKey, getDbType()), pkValues);
                        }
                    }
                }
            }
        } else {
            List<List<Object>> insertRows = recognizer.getInsertRows();
            for (List<Object> row : insertRows) {
                for(String pkKey:pkIndexMap.keySet())
                {
                    int pkIndex=pkIndexMap.get(pkKey);
                    List<Object> pkValues=pkValuesMap.get(pkKey);
                    if(Objects.isNull(pkValues)) {
                        pkValuesMap.put(ColumnUtils.delEscape(pkKey, getDbType()), Lists.newArrayList(row.get(pkIndex)));
                    }
                    else {
                        pkValues.add(row.get(pkIndex));
                    }

                }
            }
        }
        if (pkValuesMap.isEmpty()) {
            throw new ShouldNeverHappenException();
        }
        Set<String> keySet=new HashSet<>(pkValuesMap.keySet());
        //auto increment
        for(String pkKey:keySet)
        {
            List<Object> pkValues=pkValuesMap.get(pkKey);
            boolean b = this.checkPkValues(pkValues);
            if (!b) {
                throw new NotSupportYetException("not support sql [" + sqlRecognizer.getOriginalSQL() + "]");
            }
            if (!pkValuesMap.isEmpty() && pkValues.get(0) instanceof SqlSequenceExpr) {
                pkValues = getPkValuesBySequence(pkValues.get(0));
                pkValuesMap.put(ColumnUtils.delEscape(pkKey, getDbType()),pkValues);
            }
            // pk auto generated while single insert primary key is expression
            else if (pkValues.size() == 1 && pkValues.get(0) instanceof SqlMethodExpr) {
                pkValuesMap.putAll(getPkValuesByAuto());
            }
            // pk auto generated while column exists and value is null
            else if (pkValues.size() > 0 && pkValues.get(0) instanceof Null) {
                pkValuesMap.putAll(getPkValuesByAuto());
            }
        }

        return pkValuesMap;
    }

    protected List<Object> getPkValuesBySequence(Object expr) throws SQLException {

        // priority use getGeneratedKeys
        try {
            return oracleByAuto();
        } catch (NotSupportYetException | SQLException ignore) {
        }

        ResultSet genKeys;
        if (expr instanceof SqlSequenceExpr) {
            SqlSequenceExpr sequenceExpr = (SqlSequenceExpr) expr;
            final String sql = "SELECT " + sequenceExpr.getSequence() + ".currval FROM DUAL";
            LOGGER.warn("Fail to get auto-generated keys, use '{}' instead. Be cautious, statement could be polluted. Recommend you set the statement to return generated keys.", sql);
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

    protected Map<String,List<Object>> getPkValuesByAuto() throws SQLException {
        Map<String,List<Object>> pkValuesMap =new HashMap<>();
        boolean oracle = StringUtils.equalsIgnoreCase(JdbcConstants.ORACLE, getDbType());
        Map<String, ColumnMeta> pkMetaMap = getTableMeta().getPrimaryKeyMap();
        for(String pkColumnName: pkMetaMap.keySet())
        {
            List<Object> valueList=null;
            if (oracle) {
                valueList=oracleByAuto();
            }
            else {
                if(!pkMetaMap.get(pkColumnName).isAutoincrement())
                {
                    continue;
                }
                valueList=defaultByAuto();
            }
            pkValuesMap.put(pkColumnName,valueList);
        }
        if(pkValuesMap.isEmpty())
        {
            throw new ShouldNeverHappenException();
        }
        return pkValuesMap;
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
                if (containPK(insertColumns.get(paramIdx))) {
                    pkIndexMap.put(insertColumns.get(paramIdx),paramIdx);
                }
            }
            return pkIndexMap;
        }
        int pkIndex = -1;
        Map<String, ColumnMeta> allColumns = getTableMeta().getAllColumns();
        for (Map.Entry<String, ColumnMeta> entry : allColumns.entrySet()) {
            pkIndex++;
            if (containPK(entry.getValue().getColumnName())) {
                pkIndexMap.put(entry.getValue().getColumnName(),pkIndex);
            }
        }
        return pkIndexMap;
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
        return !pkParameterHasNull || !pkParameterHasNotNull;
    }

    /**
     * default auto increment
     * @return the primary key value
     * @throws SQLException the SQL exception
     */
    private List<Object> defaultByAuto() throws SQLException {
        // PK is just auto generated
        List<Object> pkValues = new ArrayList<>();
        Map<String, ColumnMeta> pkMetaMap = getTableMeta().getPrimaryKeyMap();
        if (pkMetaMap.size() == 0) {
            throw new NotSupportYetException();
        }
        String autoColumnName =null;
        for(String pkKey:pkMetaMap.keySet()){
            if(pkMetaMap.get(pkKey).isAutoincrement())
            {
                autoColumnName=pkKey;
                break;
            }
        }
        if (Objects.isNull(autoColumnName)) {
            throw new ShouldNeverHappenException();
        }
        ColumnMeta pkMeta = pkMetaMap.get(autoColumnName);
        if(Objects.isNull(pkMeta)) {
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
                LOGGER.warn("Fail to get auto-generated keys, use 'SELECT LAST_INSERT_ID()' instead. Be cautious, statement could be polluted. Recommend you set the statement to return generated keys.");
                genKeys = statementProxy.getTargetStatement().executeQuery("SELECT LAST_INSERT_ID()");
            } else {
                throw e;
            }
        }
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
        ResultSet genKeys = statementProxy.getTargetStatement().getGeneratedKeys();
        List<Object> pkValues = new ArrayList<>();
        while (genKeys.next()) {
            Object v = genKeys.getObject(1);
            pkValues.add(v);
        }
        if (pkValues.isEmpty()) {
            throw new NotSupportYetException(String.format("not support sql [%s]", sqlRecognizer.getOriginalSQL()));
        }
        return pkValues;
    }

}
