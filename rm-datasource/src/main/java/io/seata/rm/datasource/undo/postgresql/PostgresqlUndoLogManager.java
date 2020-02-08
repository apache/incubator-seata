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
package io.seata.rm.datasource.undo.postgresql;

import io.seata.common.loader.LoadLevel;
import io.seata.core.constants.ClientTableColumnsName;
import io.seata.rm.datasource.undo.AbstractUndoLogManager;
import io.seata.rm.datasource.undo.UndoLogParser;
import io.seata.sqlparser.util.JdbcConstants;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author japsercloud
 */
@LoadLevel(name = JdbcConstants.POSTGRESQL)
public class PostgresqlUndoLogManager extends AbstractUndoLogManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresqlUndoLogManager.class);

    private static final String INSERT_UNDO_LOG_SQL = "INSERT INTO " + UNDO_LOG_TABLE_NAME + "\n" +
        "\t(id,branch_id, xid,context, rollback_info, log_status, log_created, log_modified)\n" +
        "VALUES (nextval('undo_log_id_seq'),?, ?,?, ?, ?, now(), now())";

    private static final String DELETE_UNDO_LOG_BY_CREATE_SQL = "DELETE FROM " + UNDO_LOG_TABLE_NAME + " WHERE ID IN (" +
        "SELECT ID FROM " + UNDO_LOG_TABLE_NAME + " WHERE LOG_CREATED <= ? LIMIT ?" +
        ")";

    @Override
    public int deleteUndoLogByLogCreated(Date logCreated, int limitRows, Connection conn) throws SQLException {
        PreparedStatement deletePST = null;
        try {
            deletePST = conn.prepareStatement(DELETE_UNDO_LOG_BY_CREATE_SQL);
            deletePST.setDate(1, new java.sql.Date(logCreated.getTime()));
            deletePST.setInt(2, limitRows);
            int deleteRows = deletePST.executeUpdate();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("batch delete undo log size " + deleteRows);
            }
            return deleteRows;
        } catch (Exception e) {
            if (!(e instanceof SQLException)) {
                e = new SQLException(e);
            }
            throw (SQLException) e;
        } finally {
            if (deletePST != null) {
                deletePST.close();
            }
        }
    }

    @Override
    protected void insertUndoLogWithNormal(String xid, long branchID, String rollbackCtx,
        byte[] undoLogContent, Connection conn) throws SQLException {
        insertUndoLog(xid, branchID, rollbackCtx, undoLogContent, State.Normal, conn);
    }

    @Override
    protected byte[] getRollbackInfo(ResultSet rs) throws SQLException {
        byte[] rollbackInfo = rs.getBytes(ClientTableColumnsName.UNDO_LOG_ROLLBACK_INFO);
        return rollbackInfo;
    }

    @Override
    protected void insertUndoLogWithGlobalFinished(String xid, long branchId, UndoLogParser parser,
        Connection conn) throws SQLException {
        insertUndoLog(xid, branchId, buildContext(parser.getName()),
            parser.getDefaultContent(), State.GlobalFinished, conn);
    }

    private void insertUndoLog(String xid, long branchID, String rollbackCtx,
        byte[] undoLogContent, State state, Connection conn) throws SQLException {
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement(INSERT_UNDO_LOG_SQL);
            pst.setLong(1, branchID);
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
        } finally {
            if (pst != null) {
                pst.close();
            }
        }
    }
}
