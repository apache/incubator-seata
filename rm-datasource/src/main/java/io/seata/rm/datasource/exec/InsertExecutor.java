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

import io.seata.common.exception.NotSupportYetException;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.rm.datasource.PreparedStatementProxy;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.sql.SQLInsertRecognizer;
import io.seata.rm.datasource.sql.SQLRecognizer;
import io.seata.rm.datasource.sql.struct.ColumnMeta;
import io.seata.rm.datasource.sql.struct.Null;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The type Insert executor.
 *
 * @param <T> the type parameter
 * @param <S> the type parameter
 * @author yuanguoyao
 * @date 2019-03-21 21:30:02
 */
public class InsertExecutor<T, S extends Statement> extends AbstractDMLBaseExecutor<T, S> {

    protected static final String ERR_SQL_STATE = "S1009";

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
        List<Object> pkValues = containsPK() ? getPkValuesByColumn() : getPkValuesByAuto();

        TableRecords afterImage = getTableRecords(pkValues);

        if (afterImage == null) {
            throw new SQLException("Failed to build after-image for insert");
        }

        return afterImage;
    }

    protected boolean containsPK() {
        SQLInsertRecognizer recogizier = (SQLInsertRecognizer)sqlRecognizer;
        List<String> insertColumns = recogizier.getInsertColumns();
        TableMeta tmeta = getTableMeta();
        return tmeta.containsPK(insertColumns);
    }

    protected List<Object> getPkValuesByColumn() throws SQLException {
        // insert values including PK
        SQLInsertRecognizer recogizier = (SQLInsertRecognizer)sqlRecognizer;
        List<String> insertColumns = recogizier.getInsertColumns();
        String pk = getTableMeta().getPkName();
        List<Object> pkValues = null;
        for (int paramIdx = 0; paramIdx < insertColumns.size(); paramIdx++) {
            if (insertColumns.get(paramIdx).equalsIgnoreCase(pk)) {
                if (statementProxy instanceof PreparedStatementProxy) {
                    pkValues = ((PreparedStatementProxy)statementProxy).getParamsByIndex(paramIdx);
                } else {
                    List<List<Object>> insertRows = recogizier.getInsertRows();
                    pkValues = new ArrayList<>(insertRows.size());
                    for (List<Object> row : insertRows) {
                        pkValues.add(row.get(paramIdx));
                    }
                }
                break;
            }
        }
        if (pkValues == null) {
            throw new ShouldNeverHappenException();
        }
        //pk auto generated while column exists and value is null
        if (pkValues.size() == 1 && pkValues.get(0) instanceof Null) {
            pkValues = getPkValuesByAuto();
        }
        return pkValues;
    }


    protected List<Object> getPkValuesByAuto() throws SQLException {
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
        return pkValues;
    }

    protected TableRecords getTableRecords(List<Object> pkValues) throws SQLException {
        TableRecords afterImage;
        String pk = getTableMeta().getPkName();
        StringBuffer selectSQLAppender = new StringBuffer("SELECT * FROM " + getTableMeta().getTableName() + " WHERE ");
        for (int i = 1; i <= pkValues.size(); i++) {
            selectSQLAppender.append(pk + "=?");
            if (i < pkValues.size()) {
                selectSQLAppender.append(" OR ");
            }
        }
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = statementProxy.getConnection().prepareStatement(selectSQLAppender.toString());

            for (int i = 1; i <= pkValues.size(); i++) {
                ps.setObject(i, pkValues.get(i - 1));
            }

            rs = ps.executeQuery();
            afterImage = TableRecords.buildRecords(getTableMeta(), rs);

        } finally {
            if (rs != null) {
                rs.close();
            }
            if (ps != null) {
                ps.close();
            }
        }
        return afterImage;
    }
}
