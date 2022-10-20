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
package io.seata.rm.datasource.exec.mysql;

import io.seata.common.exception.NotSupportYetException;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.LowerCaseLinkHashMap;
import io.seata.rm.datasource.ConnectionProxy;
import io.seata.rm.datasource.PreparedStatementProxy;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.exec.StatementCallback;
import io.seata.rm.datasource.sql.SQLVisitorFactory;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableMetaCacheFactory;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.sqlparser.SQLInsertRecognizer;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.struct.Defaultable;
import io.seata.sqlparser.util.ColumnUtils;
import io.seata.sqlparser.util.JdbcConstants;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author: lyx
 */
public class MySQLInsertSelectExecutor extends MySQLInsertOnDuplicateUpdateExecutor implements Defaultable {

    private static final String SELECT = "SELECT";

    private static final String FOR_UPDATE = " FOR UPDATE";

    /**
     * insert recognizer from sql
     */
    private SQLInsertRecognizer insertRecognizer;

    public MySQLInsertSelectExecutor(StatementProxy statementProxy, StatementCallback statementCallback, SQLRecognizer sqlRecognizer) throws SQLException {
        super(statementProxy, statementCallback, sqlRecognizer);
        createInsertRecognizer();
    }

    public void createInsertRecognizer() throws SQLException {
        SQLInsertRecognizer recognizer = (SQLInsertRecognizer) sqlRecognizer;
        // get the sql after insert
        String querySQL = recognizer.getQuerySQL();
        insertRecognizer = doCreateInsertRecognizer(querySQL);
    }

    @Override
    public TableRecords beforeImage() throws SQLException {
        TableMeta tableMeta = getTableMeta();
        SQLInsertRecognizer recognizer = (SQLInsertRecognizer) sqlRecognizer;
        // when insertRecognizer is null, the select sql have not value
        if (Objects.isNull(insertRecognizer)) {
            return TableRecords.empty(tableMeta);
        }
        // oracle insert select can not get pk from drive,so should get uk when build image sql
        if (recognizer.isIgnore() || CollectionUtils.isNotEmpty(recognizer.getDuplicateKeyUpdate())
                || JdbcConstants.ORACLE.equals(getDbType())) {
            if (io.seata.common.util.StringUtils.isBlank(selectSQL)) {
                selectSQL = buildImageSQL(tableMeta);
            }
            if (CollectionUtils.isEmpty(paramAppenderMap)) {
                throw new NotSupportYetException("can not find unique param,may be you should add unique key when use the sqlType of" +
                        " on duplicate key update or insert select");
            }
            return buildTableRecords2(tableMeta, selectSQL, new ArrayList<>(paramAppenderMap.values()), Collections.emptyList());
        }
        return TableRecords.empty(tableMeta);
    }

    @Override
    public TableRecords afterImage(TableRecords beforeImage) throws SQLException {
        TableMeta tableMeta = getTableMeta();
        SQLInsertRecognizer recognizer = (SQLInsertRecognizer) sqlRecognizer;
        if (Objects.isNull(insertRecognizer)) {
            return TableRecords.empty(tableMeta);
        }
        if (recognizer.isIgnore() || CollectionUtils.isNotEmpty(recognizer.getDuplicateKeyUpdate())
                || JdbcConstants.ORACLE.equals(getDbType())) {
            return super.afterImage(beforeImage);
        }
        Map<String, List<Object>> pkValues = getPkValues();
        TableRecords afterImage = buildTableRecords(pkValues);
        if (afterImage == null) {
            throw new SQLException("Failed to build after-image for insert");
        }
        return afterImage;
    }

    @Override
    protected void buildUndoItemAll(ConnectionProxy connectionProxy, TableRecords beforeImage, TableRecords afterImage) {
        SQLInsertRecognizer recognizer = (SQLInsertRecognizer) sqlRecognizer;
        if (CollectionUtils.isNotEmpty(recognizer.getDuplicateKeyUpdate())) {
            super.buildUndoItemAll(connectionProxy, beforeImage, afterImage);
        } else {
            Map<SQLType, List<Row>> updateAndInsertRow = getUpdateAndInsertRow(beforeImage, afterImage);
            List<Row> insertRows = updateAndInsertRow.get(SQLType.INSERT);
            if (CollectionUtils.isNotEmpty(insertRows)) {
                TableRecords partAfterImage = new TableRecords(afterImage.getTableMeta());
                partAfterImage.setTableName(afterImage.getTableName());
                partAfterImage.setRows(insertRows);
                connectionProxy.appendUndoLog(buildUndoItem(SQLType.INSERT, TableRecords.empty(getTableMeta()), partAfterImage));
            }
        }
    }

    /**
     * from the sql type to get insert rows
     * unless select insert,other in {@link SQLInsertRecognizer#getInsertRows(Collection)}
     *
     * @param primaryKeyIndex the primary key index
     * @return the insert rows
     */
    @Override
    public List<List<Object>> getInsertRows(Collection primaryKeyIndex) {
        return Objects.nonNull(insertRecognizer) ? insertRecognizer.getInsertRows(primaryKeyIndex) : Collections.emptyList();
    }

    /**
     * from the sql type to get insert params values
     * unless select insert,other in {@link SQLInsertRecognizer#getInsertParamsValue}
     *
     * @return the insert params values
     */
    @Override
    public List<String> getInsertParamsValue() {
        return Objects.nonNull(insertRecognizer) ? insertRecognizer.getInsertParamsValue() : Collections.emptyList();
    }

    @Override
    public Map<String, ArrayList<Object>> buildImageParameters(SQLInsertRecognizer recognizer) {
        List<String> insertParamsList = getInsertParamsValue();
        List<String> insertColumns = Optional.ofNullable(recognizer.getInsertColumns()).map(list -> list.stream()
                .map(column -> ColumnUtils.delEscape(column, getDbType())).collect(Collectors.toList())).orElse(null);
        if (CollectionUtils.isEmpty(insertColumns)) {
            insertColumns = getTableMeta(recognizer.getTableName()).getDefaultTableColumn();
        }
        Map<String, ArrayList<Object>> imageParameterMap = new LowerCaseLinkHashMap<>(insertColumns.size(), 1);

        for (String insertParams : insertParamsList) {
            String[] insertParamsArray = insertParams.split(",");
            for (int i = 0; i < insertColumns.size(); i++) {
                String m = ColumnUtils.delEscape(insertColumns.get(i), getDbType());
                String params = insertParamsArray[i];
                ArrayList<Object> imageListTemp = imageParameterMap.computeIfAbsent(m, k -> new ArrayList<>());
                imageListTemp.add(params.trim());
                imageParameterMap.put(m, imageListTemp);
            }
        }
        return imageParameterMap;
    }

    /**
     * create the real insert recognizer
     *
     * @param querySQL the sql after insert
     * @throws SQLException
     */
    protected SQLInsertRecognizer doCreateInsertRecognizer(String querySQL) throws SQLException {
        Map<Integer, ArrayList<Object>> parameters = ((PreparedStatementProxy) statementProxy).getParameters();
        List<SQLRecognizer> sqlRecognizers = SQLVisitorFactory.get(querySQL + FOR_UPDATE, getDbType());
        SQLRecognizer selectRecognizer = sqlRecognizers.get(0);
        ConnectionProxy connectionProxy = statementProxy.getConnectionProxy();
        TableMeta selectTableMeta = TableMetaCacheFactory.getTableMetaCache(connectionProxy.getDbType())
                .getTableMeta(connectionProxy.getTargetConnection(), selectRecognizer.getTableName(), connectionProxy.getDataSourceProxy().getResourceId());
        // use query SQL to get values from database
        TableRecords tableRecords = buildTableRecords2(selectTableMeta, querySQL, new ArrayList<>(parameters.values()), Collections.emptyList());
        if (CollectionUtils.isNotEmpty(tableRecords.getRows())) {
            StringBuilder valuesSQL = new StringBuilder();
            // build values sql
            valuesSQL.append(" VALUES");
            tableRecords.getRows().forEach(row -> {
                List<Object> values = row.getFields().stream().map(Field::getValue)
                        .map(value -> Objects.isNull(value) ? null : value).collect(Collectors.toList());
                valuesSQL.append("(");
                for (Object value : values) {
                    if (Objects.isNull(value)) {
                        valuesSQL.append((String) null);
                    } else {
                        valuesSQL.append(value);
                    }
                    valuesSQL.append(",");
                }
                valuesSQL.insert(valuesSQL.length() - 1, ")");
            });
            valuesSQL.deleteCharAt(valuesSQL.length() - 1);
            List<SQLRecognizer> insertSQLRecognizers = SQLVisitorFactory.get(formatOriginSQL(valuesSQL.toString()), getDbType());
            if (CollectionUtils.isEmpty(insertSQLRecognizers)) {
                throw new NotSupportYetException("can not support the sql type together with select");
            }
            return (SQLInsertRecognizer) insertSQLRecognizers.get(0);
        }
        return null;
    }

    /**
     * format origin sql
     *
     * @param valueSQL the value after insert sql
     * @return eg: insert into test values(1,1)
     */
    private String formatOriginSQL(String valueSQL) {
        String tableName = this.sqlRecognizer.getTableName().toUpperCase();
        String originalSQL = this.sqlRecognizer.getOriginalSQL().toUpperCase();
        int index = originalSQL.indexOf(SELECT);
        if (tableName.equalsIgnoreCase(SELECT)) {
            // choose the next select
            index = originalSQL.indexOf(SELECT, index + SELECT.length());
        }
        if (index == -1) {
            throw new ShouldNeverHappenException("may be the query sql is not a select SQL");
        }
        return this.sqlRecognizer.getOriginalSQL().substring(0, index) + valueSQL;
    }
}
