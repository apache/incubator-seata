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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

import com.alibaba.fescar.rm.datasource.sql.struct.Field;
import com.alibaba.fescar.rm.datasource.sql.struct.KeyType;
import com.alibaba.fescar.rm.datasource.sql.struct.Row;
import com.alibaba.fescar.rm.datasource.sql.struct.TableRecords;

/**
 * The type Abstract undo executor.
 */
public abstract class AbstractUndoExecutor {

    /**
     * The Sql undo log.
     */
    protected SQLUndoLog sqlUndoLog;

    /**
     * Build undo sql string.
     *
     * @return the string
     */
    protected abstract String buildUndoSQL();

    /**
     * Instantiates a new Abstract undo executor.
     *
     * @param sqlUndoLog the sql undo log
     */
    public AbstractUndoExecutor(SQLUndoLog sqlUndoLog) {
        this.sqlUndoLog = sqlUndoLog;
    }

    /**
     * Execute on.
     *
     * @param conn the conn
     * @throws SQLException the sql exception
     */
    public void executeOn(Connection conn) throws SQLException {
        dataValidation(conn);

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

    /**
     * Undo prepare.
     *
     * @param undoPST    the undo pst
     * @param undoValues the undo values
     * @param pkValue    the pk value
     * @throws SQLException the sql exception
     */
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

    /**
     * Gets undo rows.
     *
     * @return the undo rows
     */
    protected abstract TableRecords getUndoRows();

    /**
     * Data validation.
     *
     * @param conn the conn
     * @throws SQLException the sql exception
     */
    protected void dataValidation(Connection conn) throws SQLException {
        // Validate if data is dirty.
    }
}
