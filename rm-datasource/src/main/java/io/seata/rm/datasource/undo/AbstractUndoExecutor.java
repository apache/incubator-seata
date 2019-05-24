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
package io.seata.rm.datasource.undo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableRecords;

/**
 * The type Abstract undo executor.
 *
 * @author sharajava
 * @author Geng Zhang
 */
public abstract class AbstractUndoExecutor {

    /**
     * wrap sql with row records
     */
    public static class PreparedStatementParams {
        String sql;
        List<Row> rows;

        private PreparedStatementParams() {
        }

        public static PreparedStatementParams create(String sql, List<Row> rows) {
            PreparedStatementParams params = new PreparedStatementParams();
            params.sql = sql;
            params.rows = rows;
            return params;
        }
    }

    /**
     * The Sql undo log.
     */
    protected SQLUndoLog sqlUndoLog;

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
        if (sqlUndoLog.hasNotAffected()) {
            return;
        }

        assertConnectionNotDirty(conn);

        try {
            PreparedStatementParams params = buildUndoPreparedStatementParams();
            PreparedStatement undoPrepStmt = conn.prepareStatement(params.sql);
            // TODO batch update
            for (Row row : params.rows) {
                fillPreparedStatement(undoPrepStmt, row);
                undoPrepStmt.executeUpdate();
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
     * Data validation.
     *
     * @param conn the conn
     * @throws SQLException the sql exception
     */
    void assertConnectionNotDirty(Connection conn) throws SQLException {
        // Validate if data is dirty.
    }

    /**
     * build prepareStatement params of undo action
     *
     * @return
     */
    protected abstract PreparedStatementParams buildUndoPreparedStatementParams();

    /**
     * Fill column value to  PreparedStatement
     *
     * @param preparedStatement the undo statement
     * @throws SQLException the sql exception
     */
    void fillPreparedStatement(PreparedStatement preparedStatement, Row row) throws SQLException {
        List<Field> fields = row.getFields();
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            preparedStatement.setObject(i + 1, field.getValue(), field.getType());
        }
    }

    /**
     * Assert not empty and get first row
     *
     * @return
     */
    protected Row getRowDefinition() {
        List<Row> undoRows = getUndoRecords().getRows();
        if (undoRows == null || undoRows.size() == 0) {
            throw new ShouldNeverHappenException("Invalid UNDO LOG");
        }
        return undoRows.get(0);
    }

    /**
     * Get undo image
     *
     * @return
     */
    protected abstract TableRecords getUndoRecords();

}
