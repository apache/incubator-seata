/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.rm.datasource.undo.sqlserver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;

import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.core.compressor.CompressorType;
import org.apache.seata.core.constants.ClientTableColumnsName;
import org.apache.seata.rm.datasource.undo.AbstractUndoLogManager;
import org.apache.seata.rm.datasource.undo.UndoLogParser;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@LoadLevel(name = JdbcConstants.SQLSERVER)
public class SqlServerUndoLogManager extends AbstractUndoLogManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SqlServerUndoLogManager.class);

    /**
     * branch_id, xid, context, rollback_info, log_status, log_created, log_modified
     */
    private static final String INSERT_UNDO_LOG_SQL = "INSERT INTO " + UNDO_LOG_TABLE_NAME +
            " (" + ClientTableColumnsName.UNDO_LOG_BRANCH_XID + ", " + ClientTableColumnsName.UNDO_LOG_XID + ", "
            + ClientTableColumnsName.UNDO_LOG_CONTEXT + ", " + ClientTableColumnsName.UNDO_LOG_ROLLBACK_INFO + ", "
            + ClientTableColumnsName.UNDO_LOG_LOG_STATUS + ", " + ClientTableColumnsName.UNDO_LOG_LOG_CREATED + ", "
            + ClientTableColumnsName.UNDO_LOG_LOG_MODIFIED + ")"
            + " VALUES (?, ?, ?, ?, ?, SYSDATETIME(), SYSDATETIME())";

    private static final String DELETE_UNDO_LOG_BY_CREATE_SQL = "DELETE FROM " + UNDO_LOG_TABLE_NAME +
        " WHERE " + ClientTableColumnsName.UNDO_LOG_BRANCH_XID + "+" + ClientTableColumnsName.UNDO_LOG_XID +
        " IN ( SELECT TOP(?) " + ClientTableColumnsName.UNDO_LOG_BRANCH_XID + "+" + ClientTableColumnsName.UNDO_LOG_XID +
        " FROM " + UNDO_LOG_TABLE_NAME +
        " WHERE " + ClientTableColumnsName.UNDO_LOG_LOG_CREATED + " <= ? " +
        " ORDER BY " + ClientTableColumnsName.UNDO_LOG_LOG_CREATED + " ASC )";
    
    private static final String CHECK_UNDO_LOG_TABLE_EXIST_SQL = "SELECT TOP 1 1 FROM " + UNDO_LOG_TABLE_NAME;

    @Override
    protected void insertUndoLogWithGlobalFinished(String xid, long branchId, UndoLogParser undoLogParser, Connection conn) throws SQLException {
        insertUndoLog(xid, branchId, buildContext(undoLogParser.getName(), CompressorType.NONE), undoLogParser.getDefaultContent(),
                State.GlobalFinished, conn);
    }

    @Override
    protected void insertUndoLogWithNormal(String xid, long branchId, String rollbackCtx, byte[] undoLogContent, Connection conn) throws SQLException {
        insertUndoLog(xid, branchId, rollbackCtx, undoLogContent, State.Normal, conn);
    }

    @Override
    public int deleteUndoLogByLogCreated(Date logCreated, int limitRows, Connection conn) throws SQLException {
        try (PreparedStatement deletePST = conn.prepareStatement(DELETE_UNDO_LOG_BY_CREATE_SQL)) {
            deletePST.setInt(1, limitRows);
            deletePST.setDate(2, new java.sql.Date(logCreated.getTime()));
            int deleteRows = deletePST.executeUpdate();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("batch delete undo log size {}", deleteRows);
            }
            return deleteRows;
        } catch (Exception e) {
            if (!(e instanceof SQLException)) {
                e = new SQLException(e);
            }
            throw (SQLException) e;
        }
    }

    private void insertUndoLog(String xid, long branchId, String rollbackCtx, byte[] undoLogContent,
                               State state, Connection conn) throws SQLException {
        try (PreparedStatement pst = conn.prepareStatement(INSERT_UNDO_LOG_SQL)) {
            pst.setLong(1, branchId);
            pst.setString(2, xid);
            pst.setString(3, rollbackCtx);
            pst.setBytes(4, undoLogContent);
            pst.setInt(5, state.getValue());
            pst.executeUpdate();
        } catch (Exception e) {
            if (!(e instanceof SQLException)) {
                e = new SQLException(e);
            }
            throw (SQLException) e;
        }
    }

    @Override
    protected String buildSelectUndoSql() {
        return "SELECT * FROM " + UNDO_LOG_TABLE_NAME + " WITH(UPDLOCK) WHERE "
                + ClientTableColumnsName.UNDO_LOG_BRANCH_XID + " = ? AND " + ClientTableColumnsName.UNDO_LOG_XID + " = ?";
    }
    
    @Override
    protected String getCheckUndoLogTableExistSql() {
        return CHECK_UNDO_LOG_TABLE_EXIST_SQL;
    }
}
