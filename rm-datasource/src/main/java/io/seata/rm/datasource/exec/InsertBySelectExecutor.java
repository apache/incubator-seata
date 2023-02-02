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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.seata.common.exception.NotSupportYetException;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.IOUtil;
import io.seata.common.util.StringUtils;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.sql.struct.ColumnMeta;
import io.seata.sqlparser.SQLInsertRecognizer;
import io.seata.sqlparser.SQLRecognizer;

/**
 * The type InsertBySelect executor.
 *
 * @param <T> the type parameter
 * @param <S> the type parameter
 * @author renliangyu857
 */
public class InsertBySelectExecutor<T, S extends Statement> extends BaseInsertExecutor<T, S> {
    private SQLInsertRecognizer sqlInsertRecognizer;
    /**
     * Instantiates a new Update executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizer     the sql recognizer
     */
    public InsertBySelectExecutor(StatementProxy<S> statementProxy, StatementCallback<T, S> statementCallback,
        SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
        sqlInsertRecognizer = (SQLInsertRecognizer) sqlRecognizer;
    }

    @Override
    protected TableRecords afterImage(TableRecords beforeImage) throws SQLException {
        return buildTableRecords(getPkValues());
    }

    @Override
    protected TableRecords buildTableRecords(Map<String, List<Object>> pkValuesMap) throws SQLException {
        String querySql = sqlInsertRecognizer.getSubQuerySql();
        if (StringUtils.isEmpty(querySql)) {
            return null;
        }
        ResultSet rs = null;
        try (PreparedStatement ps = statementProxy.getConnection().prepareStatement(querySql)) {
            rs = ps.executeQuery();
            return buildRecordsForInsertBySelect(getTableMeta(sqlInsertRecognizer.getTableName()), rs);
        } finally {
            IOUtil.close(rs);
        }
    }

    protected TableRecords buildRecordsForInsertBySelect(TableMeta tmeta, ResultSet resultSet) throws SQLException {
        TableRecords records = new TableRecords(tmeta);
        ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
        int columnCount = resultSetMetaData.getColumnCount();
        List<String> sqlRecognizerColumns = sqlInsertRecognizer.getInsertColumns();
        List<String> insertColumns = CollectionUtils.isEmpty(sqlRecognizerColumns) ? new ArrayList<>(tmeta.getAllColumns().keySet()) : sqlRecognizerColumns;
        while (resultSet.next()) {
            List<Field> fields = new ArrayList<>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                //select a,b,c from t select a1,b1,c1 from t1
                String colName = insertColumns.get(i - 1);
                checkDataType(tmeta,resultSetMetaData,i,colName);
                fields.add(TableRecords.parseResultSetField(tmeta,resultSet,i,colName));
            }

            Row row = new Row();
            row.setFields(fields);
            records.add(row);
        }
        return records;
    }

    private void checkDataType(TableMeta tmeta, ResultSetMetaData resultSetMetaData, int i, String colName) throws SQLException {
        ColumnMeta col = tmeta.getColumnMeta(colName);
        if (col.getDataType() != resultSetMetaData.getColumnType(i)) {
            throw new NotSupportYetException("the col[" + colName + "] data type is not consistent with source table");
        }
    }

    @Override
    public Map<String, List<Object>> getPkValues() throws SQLException {
        return null;
    }

    @Override
    public Map<String, List<Object>> getPkValuesByColumn() throws SQLException {
        return null;
    }
}
