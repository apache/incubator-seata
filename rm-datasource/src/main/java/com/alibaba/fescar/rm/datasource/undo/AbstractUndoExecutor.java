/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.rm.datasource.undo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.fescar.rm.datasource.sql.struct.*;

public abstract class AbstractUndoExecutor {

    protected SQLUndoLog sqlUndoLog;

    protected abstract String buildUndoSQL();

    public AbstractUndoExecutor(SQLUndoLog sqlUndoLog) {
        this.sqlUndoLog = sqlUndoLog;
    }

    public void executeOn(Connection conn, boolean force) throws SQLException {
        if (!force) {
            dataValidation(conn);
        }

        try {
            String undoSQL = buildUndoSQL();

            PreparedStatement undoPST = conn.prepareStatement(undoSQL);

            TableRecords undoRows = getUndoRows();

            for (Row undoRow : undoRows.getRows()) {
                ArrayList<Field> undoValues = new ArrayList<>();
                Field pkValue = null;
                for (Field field : undoRow.getFields()) {
                    if (field.getKeyType() == KeyType.PrimaryKey) {
                        pkValue = field;
                    } else {
                        undoValues.add(field);
                    }
                }

                undoPrepare(undoPST, undoValues, pkValue);

                undoPST.executeUpdate();
            }

        } catch (Exception ex) {
            if (ex instanceof SQLException) {
                throw (SQLException) ex;
            } else {
                throw new SQLException(ex);
            }

        }

    }

    protected void undoPrepare(PreparedStatement undoPST, ArrayList<Field> undoValues, Field pkValue) throws SQLException {
        int undoIndex = 0;
        for (Field undoValue : undoValues) {
            undoIndex++;
            undoPST.setObject(undoIndex, undoValue.getValue(), undoValue.getType());
        }
        // PK is at last one.
        // INSERT INTO a (x, y, z, pk) VALUES (?, ?, ?, ?)
        // UPDATE a SET x=?, y=?, z=? WHERE pk = ?
        // DELETE FROM a WHERE pk = ?
        undoIndex++;
        undoPST.setObject(undoIndex, pkValue.getValue(), pkValue.getType());
    }

    protected abstract TableRecords getUndoRows();

    // Validate if data is dirty.
    protected void dataValidation(Connection conn) throws SQLException {
        String tableName = sqlUndoLog.getTableName();
        List<Field> pkRows = sqlUndoLog.getAfterImage().pkRows();
        String querySQL = buildQueryCurrentImageSQL(tableName, pkRows);
        TableRecords currentImage = getCurrentImage(conn, querySQL, pkRows);
        currentImage.setTableName(tableName);
        if (!currentImage.equals(sqlUndoLog.getAfterImage()))
            throw new RuntimeException("rollback failed! data is dirty!");
    }

    protected String buildQueryCurrentImageSQL(String tableName, List<Field> pkRows) {
        StringBuilder sb = new StringBuilder("SELECT * FROM ");
        sb.append(tableName).append(" WHERE ");
        buildWhereConditionByPKs(sb, pkRows);
        return sb.toString();
    }

    protected void buildWhereConditionByPKs(StringBuilder sb, List<Field> pkRows) {
        for (int index = 0; index < pkRows.size(); index++) {
            Field field = pkRows.get(index);
            sb.append(field.getName() + " = ?");
            if (index < (pkRows.size() - 1)) {
                sb.append(" OR ");
            }
        }
    }

    protected TableRecords getCurrentImage(Connection conn, String querySQL,
                                           List<Field> paramList) throws SQLException {
        if (paramList == null || paramList.isEmpty()) {
            return new TableRecords();
        }

        TableRecords currentRecords;
        try (PreparedStatement ps = conn.prepareStatement(querySQL)) {
            for (int i = 0; i < paramList.size(); i++) {
                ps.setObject((i + 1), paramList.get(i));
            }
            ResultSet rs = ps.executeQuery();
            currentRecords = buildTableRecordsFromResultSet(rs);
        }

        return currentRecords;
    }

    private TableRecords buildTableRecordsFromResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        int columnCount = rsmd.getColumnCount();
        TableRecords tableRecords = new TableRecords();
        while (rs.next()) {
            Row row = new Row();
            for (int index = 1; index <= columnCount; ++index) {
                String name = rsmd.getCatalogName(index).toUpperCase();
                int type = rsmd.getColumnType(index);
                Object value = rs.getObject(name);
                if (value == null) continue;    // skip Null
                Field field = new Field(name, type, value);
                row.add(field);
            }
            tableRecords.add(row);
        }
        return tableRecords;
    }

}
