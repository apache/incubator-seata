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
package org.apache.seata.rm.datasource.undo.mysql;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;

import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.IdWorker;
import org.apache.seata.core.compressor.CompressorType;
import org.apache.seata.core.constants.ClientTableColumnsName;
import org.apache.seata.rm.datasource.undo.AbstractUndoLogManager;
import org.apache.seata.rm.datasource.undo.UndoLogConstants;
import org.apache.seata.rm.datasource.undo.UndoLogParser;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@LoadLevel(name = JdbcConstants.MYSQL)
public class MySQLUndoLogManager extends AbstractUndoLogManager {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * branch_id, xid, context, rollback_info, log_status, log_created, log_modified
     */
    private static final String INSERT_UNDO_LOG_SQL = "INSERT INTO " + UNDO_LOG_TABLE_NAME +
            " (" + ClientTableColumnsName.UNDO_LOG_BRANCH_XID + ", " + ClientTableColumnsName.UNDO_LOG_XID + ", "
            + ClientTableColumnsName.UNDO_LOG_CONTEXT + ", " + ClientTableColumnsName.UNDO_LOG_ROLLBACK_INFO + ", "
            + ClientTableColumnsName.UNDO_LOG_LOG_STATUS + ", " + ClientTableColumnsName.UNDO_LOG_LOG_CREATED + ", "
            + ClientTableColumnsName.UNDO_LOG_LOG_MODIFIED + ")"
            + " VALUES (?, ?, ?, ?, ?, now(6), now(6))";

    private static final String DELETE_UNDO_LOG_BY_CREATE_SQL = "DELETE FROM " + UNDO_LOG_TABLE_NAME +
            " WHERE " + ClientTableColumnsName.UNDO_LOG_LOG_CREATED + " <= ? LIMIT ?";

    private static final IdWorker ID_WORKER = new IdWorker(null);

    @Override
    public int deleteUndoLogByLogCreated(Date logCreated, int limitRows, Connection conn) throws SQLException {
        try (PreparedStatement deletePST = conn.prepareStatement(DELETE_UNDO_LOG_BY_CREATE_SQL)) {
            deletePST.setDate(1, new java.sql.Date(logCreated.getTime()));
            deletePST.setInt(2, limitRows);
            int deleteRows = deletePST.executeUpdate();
            if (logger.isDebugEnabled()) {
                logger.debug("batch delete undo log size {}", deleteRows);
            }
            return deleteRows;
        } catch (Exception e) {
            if (!(e instanceof SQLException)) {
                e = new SQLException(e);
            }
            throw (SQLException) e;
        }
    }

    @Override
    protected void insertUndoLogWithNormal(String xid, long branchId, String rollbackCtx, byte[] undoLogContent,
                                           Connection conn) throws SQLException {
        // 64MB * 0.45
        int limit = (int) (64 * 0.45 * 1024 * 1024);
        if (undoLogContent.length > limit) {
            final String subRollbackCtx = UndoLogConstants.BRANCH_ID_KEY + CollectionUtils.KV_SPLIT + branchId;
            int pos = 0;
            byte[] first = new byte[limit];
            StringBuilder subIdBuilder = new StringBuilder(36);
            while (pos < undoLogContent.length) {
                if (pos == 0) {
                    System.arraycopy(undoLogContent, pos, first, 0, first.length);
                    pos += first.length;
                } else {
                    byte[] bytes = new byte[Math.min(undoLogContent.length - pos, limit)];
                    System.arraycopy(undoLogContent, pos, bytes, 0, bytes.length);
                    long subId = ID_WORKER.nextId();
                    subIdBuilder.append(subId).append(UndoLogConstants.SUB_SPLIT_KEY);
                    insertUndoLog(xid, subId, subRollbackCtx, bytes, State.Normal, conn);
                    pos += bytes.length;
                }
            }
            Map<String, String> decodeMap = CollectionUtils.decodeMap(rollbackCtx);
            decodeMap.put(UndoLogConstants.SUB_ID_KEY, subIdBuilder.toString());
            String finalRollbackCtx = CollectionUtils.encodeMap(decodeMap);
            insertUndoLog(xid, branchId, finalRollbackCtx, first, State.Normal, conn);
        } else {
            insertUndoLog(xid, branchId, rollbackCtx, undoLogContent, State.Normal, conn);
        }
    }

    @Override
    protected void insertUndoLogWithGlobalFinished(String xid, long branchId, UndoLogParser parser, Connection conn) throws SQLException {
        insertUndoLog(xid, branchId, buildContext(parser.getName(), CompressorType.NONE), parser.getDefaultContent(), State.GlobalFinished, conn);
    }

    private void insertUndoLog(String xid, long branchId, String rollbackCtx, byte[] undoLogContent,
                               State state, Connection conn) throws SQLException {
        try (PreparedStatement pst = conn.prepareStatement(INSERT_UNDO_LOG_SQL)) {
            pst.setLong(1, branchId);
            pst.setString(2, xid);
            pst.setString(3, rollbackCtx);
            pst.setObject(4, new ByteArrayInputStream(undoLogContent));
            pst.setInt(5, state.getValue());
            pst.executeUpdate();
        } catch (Exception e) {
            if (!(e instanceof SQLException)) {
                e = new SQLException(e);
            }
            throw (SQLException) e;
        }
    }

}
