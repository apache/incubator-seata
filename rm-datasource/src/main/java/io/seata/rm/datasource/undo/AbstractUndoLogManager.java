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

import com.alibaba.druid.util.JdbcConstants;
import io.seata.common.Constants;
import io.seata.common.exception.NotSupportYetException;
import io.seata.common.util.BlobUtils;
import io.seata.common.util.CollectionUtils;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ClientTableColumnsName;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.exception.BranchTransactionException;
import io.seata.core.exception.TransactionException;
import io.seata.rm.datasource.ConnectionContext;
import io.seata.rm.datasource.ConnectionProxy;
import io.seata.rm.datasource.DataSourceProxy;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableMetaCache;
import io.seata.rm.datasource.sql.struct.TableMetaCacheOracle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.seata.core.exception.TransactionExceptionCode.BranchRollbackFailed_Retriable;

/**
 * @author jsbxyyx
 * @date 2019/09/07
 */
public abstract class AbstractUndoLogManager implements UndoLogManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractUndoLogManager.class);

    protected enum State {
        /**
         * This state can be properly rolled back by services
         */
        Normal(0),
        /**
         * This state prevents the branch transaction from inserting undo_log after the global transaction is rolled
         * back.
         */
        GlobalFinished(1);

        private int value;

        State(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    protected static final String UNDO_LOG_TABLE_NAME = ConfigurationFactory.getInstance()
            .getConfig(ConfigurationKeys.TRANSACTION_UNDO_LOG_TABLE, ConfigurationKeys.TRANSACTION_UNDO_LOG_DEFAULT_TABLE);

    protected static final String SELECT_UNDO_LOG_SQL = "SELECT * FROM " + UNDO_LOG_TABLE_NAME +
            " WHERE " + ClientTableColumnsName.UNDO_LOG_BRANCH_XID + " = ? AND " + ClientTableColumnsName.UNDO_LOG_XID + " = ? FOR UPDATE";

    protected static final String DELETE_UNDO_LOG_SQL = "DELETE FROM " + UNDO_LOG_TABLE_NAME +
            " WHERE " + ClientTableColumnsName.UNDO_LOG_BRANCH_XID + " = ? AND " + ClientTableColumnsName.UNDO_LOG_XID + " = ?";

    private static final ThreadLocal<String> SERIALIZER_LOCAL = new ThreadLocal<>();

    public static String getCurrentSerializer() {
        return SERIALIZER_LOCAL.get();
    }

    public static void setCurrentSerializer(String serializer) {
        SERIALIZER_LOCAL.set(serializer);
    }

    public static void removeCurrentSerializer() {
        SERIALIZER_LOCAL.remove();
    }

    /**
     * get db type
     * @return the db type
     */
    public abstract String getDbType();

    /**
     * Delete undo log.
     *
     * @param xid      the xid
     * @param branchId the branch id
     * @param conn     the conn
     * @throws SQLException the sql exception
     */
    @Override
    public void deleteUndoLog(String xid, long branchId, Connection conn) throws SQLException {
        PreparedStatement deletePST = null;
        try {
            deletePST = conn.prepareStatement(DELETE_UNDO_LOG_SQL);
            deletePST.setLong(1, branchId);
            deletePST.setString(2, xid);
            deletePST.executeUpdate();
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

    /**
     * batch Delete undo log.
     *
     * @param xids
     * @param branchIds
     * @param conn
     */
    @Override
    public void batchDeleteUndoLog(Set<String> xids, Set<Long> branchIds, Connection conn) throws SQLException {
        if (CollectionUtils.isEmpty(xids) || CollectionUtils.isEmpty(branchIds)) {
            return;
        }
        int xidSize = xids.size();
        int branchIdSize = branchIds.size();
        String batchDeleteSql = toBatchDeleteUndoLogSql(xidSize, branchIdSize);
        PreparedStatement deletePST = null;
        try {
            deletePST = conn.prepareStatement(batchDeleteSql);
            int paramsIndex = 1;
            for (Long branchId : branchIds) {
                deletePST.setLong(paramsIndex++,branchId);
            }
            for (String xid: xids){
                deletePST.setString(paramsIndex++, xid);
            }
            int deleteRows = deletePST.executeUpdate();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("batch delete undo log size " + deleteRows);
            }
        }catch (Exception e){
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

    protected static String toBatchDeleteUndoLogSql(int xidSize, int branchIdSize) {
        StringBuilder sqlBuilder = new StringBuilder(64);
        sqlBuilder.append("DELETE FROM ")
                .append(UNDO_LOG_TABLE_NAME)
                .append(" WHERE  " + ClientTableColumnsName.UNDO_LOG_BRANCH_XID + " IN ");
        appendInParam(branchIdSize, sqlBuilder);
        sqlBuilder.append(" AND " + ClientTableColumnsName.UNDO_LOG_XID + " IN ");
        appendInParam(xidSize, sqlBuilder);
        return sqlBuilder.toString();
    }

    protected static void appendInParam(int size, StringBuilder sqlBuilder) {
        sqlBuilder.append(" (");
        for (int i = 0; i < size; i++) {
            sqlBuilder.append("?");
            if (i < (size - 1)) {
                sqlBuilder.append(",");
            }
        }
        sqlBuilder.append(") ");
    }

    protected static boolean canUndo(int state) {
        return state == State.Normal.getValue();
    }

    protected String buildContext(String serializer) {
        Map<String, String> map = new HashMap<>();
        map.put(UndoLogConstants.SERIALIZER_KEY, serializer);
        return CollectionUtils.encodeMap(map);
    }

    protected Map<String, String> parseContext(String data) {
        return CollectionUtils.decodeMap(data);
    }

    /**
     * Flush undo logs.
     *
     * @param cp the cp
     * @throws SQLException the sql exception
     */
    @Override
    public void flushUndoLogs(ConnectionProxy cp) throws SQLException {
        ConnectionContext connectionContext = cp.getContext();
        String xid = connectionContext.getXid();
        long branchID = connectionContext.getBranchId();

        BranchUndoLog branchUndoLog = new BranchUndoLog();
        branchUndoLog.setXid(xid);
        branchUndoLog.setBranchId(branchID);
        branchUndoLog.setSqlUndoLogs(connectionContext.getUndoItems());

        UndoLogParser parser = UndoLogParserFactory.getInstance();
        byte[] undoLogContent = parser.encode(branchUndoLog);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Flushing UNDO LOG: {}", new String(undoLogContent, Constants.DEFAULT_CHARSET));
        }

        insertUndoLogWithNormal(xid, branchID, buildContext(parser.getName()), undoLogContent,
                cp.getTargetConnection());
    }

    /**
     *
     * Undo.
     *
     * @param dataSourceProxy the data source proxy
     * @param xid             the xid
     * @param branchId        the branch id
     * @throws TransactionException the transaction exception
     */
    @Override
    public void undo(DataSourceProxy dataSourceProxy, String xid, long branchId) throws TransactionException {
        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement selectPST = null;
        boolean originalAutoCommit = true;

        for (; ; ) {
            try {
                conn = dataSourceProxy.getPlainConnection();

                // The entire undo process should run in a local transaction.
                if (originalAutoCommit = conn.getAutoCommit()) {
                    conn.setAutoCommit(false);
                }

                // Find UNDO LOG
                selectPST = conn.prepareStatement(SELECT_UNDO_LOG_SQL);
                selectPST.setLong(1, branchId);
                selectPST.setString(2, xid);
                rs = selectPST.executeQuery();

                boolean exists = false;
                while (rs.next()) {
                    exists = true;

                    // It is possible that the server repeatedly sends a rollback request to roll back
                    // the same branch transaction to multiple processes,
                    // ensuring that only the undo_log in the normal state is processed.
                    int state = rs.getInt(ClientTableColumnsName.UNDO_LOG_LOG_STATUS);
                    if (!canUndo(state)) {
                        if (LOGGER.isInfoEnabled()) {
                            LOGGER.info("xid {} branch {}, ignore {} undo_log",
                                    xid, branchId, state);
                        }
                        return;
                    }

                    String contextString = rs.getString(ClientTableColumnsName.UNDO_LOG_CONTEXT);
                    Map<String, String> context = parseContext(contextString);
                    Blob b = rs.getBlob(ClientTableColumnsName.UNDO_LOG_ROLLBACK_INFO);
                    byte[] rollbackInfo = BlobUtils.blob2Bytes(b);

                    String serializer = context == null ? null : context.get(UndoLogConstants.SERIALIZER_KEY);
                    UndoLogParser parser = serializer == null ? UndoLogParserFactory.getInstance() :
                            UndoLogParserFactory.getInstance(serializer);
                    BranchUndoLog branchUndoLog = parser.decode(rollbackInfo);

                    try {
                        // put serializer name to local
                        setCurrentSerializer(parser.getName());
                        List<SQLUndoLog> sqlUndoLogs = branchUndoLog.getSqlUndoLogs();
                        if (sqlUndoLogs.size() > 1) {
                            Collections.reverse(sqlUndoLogs);
                        }
                        for (SQLUndoLog sqlUndoLog : sqlUndoLogs) {
                            TableMeta tableMeta;
                            if (JdbcConstants.ORACLE.equalsIgnoreCase(dataSourceProxy.getDbType())) {
                                tableMeta = TableMetaCacheOracle.getTableMeta(dataSourceProxy, sqlUndoLog.getTableName());
                            } else if (JdbcConstants.MYSQL.equalsIgnoreCase(dataSourceProxy.getDbType())) {
                                tableMeta = TableMetaCache.getTableMeta(dataSourceProxy, sqlUndoLog.getTableName());
                            } else {
                                throw new NotSupportYetException("not support dbType[" + dataSourceProxy.getDbType() + "]");
                            }
                            sqlUndoLog.setTableMeta(tableMeta);
                            AbstractUndoExecutor undoExecutor = UndoExecutorFactory.getUndoExecutor(
                                    dataSourceProxy.getDbType(),
                                    sqlUndoLog);
                            undoExecutor.executeOn(conn);
                        }
                    } finally {
                        // remove serializer name
                        removeCurrentSerializer();
                    }
                }

                // If undo_log exists, it means that the branch transaction has completed the first phase,
                // we can directly roll back and clean the undo_log
                // Otherwise, it indicates that there is an exception in the branch transaction,
                // causing undo_log not to be written to the database.
                // For example, the business processing timeout, the global transaction is the initiator rolls back.
                // To ensure data consistency, we can insert an undo_log with GlobalFinished state
                // to prevent the local transaction of the first phase of other programs from being correctly submitted.
                // See https://github.com/seata/seata/issues/489

                if (exists) {
                    deleteUndoLog(xid, branchId, conn);
                    conn.commit();
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("xid {} branch {}, undo_log deleted with {}",
                                xid, branchId, State.GlobalFinished.name());
                    }
                } else {
                    insertUndoLogWithGlobalFinished(xid, branchId, UndoLogParserFactory.getInstance(), conn);
                    conn.commit();
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("xid {} branch {}, undo_log added with {}",
                                xid, branchId, State.GlobalFinished.name());
                    }
                }

                return;
            } catch (SQLIntegrityConstraintViolationException e) {
                // Possible undo_log has been inserted into the database by other processes, retrying rollback undo_log
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("xid {} branch {}, undo_log inserted, retry rollback",
                            xid, branchId);
                }
            } catch (Throwable e) {
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException rollbackEx) {
                        LOGGER.warn("Failed to close JDBC resource while undo ... ", rollbackEx);
                    }
                }
                throw new BranchTransactionException(BranchRollbackFailed_Retriable,
                        String.format("Branch session rollback failed and try again later xid = %s branchId = %s %s", xid, branchId, e.getMessage()), e);

            } finally {
                try {
                    if (rs != null) {
                        rs.close();
                    }
                    if (selectPST != null) {
                        selectPST.close();
                    }
                    if (conn != null) {
                        if (originalAutoCommit) {
                            conn.setAutoCommit(true);
                        }
                        conn.close();
                    }
                } catch (SQLException closeEx) {
                    LOGGER.warn("Failed to close JDBC resource while undo ... ", closeEx);
                }
            }
        }
    }

    /**
     *  insert uodo log when global finished
     * @param xid  the xid
     * @param branchId the branchId
     * @param undoLogParser the undoLogParse
     * @param conn sql connection
     * @throws SQLException
     */
    protected abstract void insertUndoLogWithGlobalFinished(String xid, long branchId, UndoLogParser undoLogParser, Connection conn) throws SQLException;

    /**
     *  insert uodo log when normal
     * @param xid  the xid
     * @param branchId the branchId
     * @param rollbackCtx the rollbackContext
     * @param undoLogContent the undoLogContent
     * @param conn sql connection
     * @throws SQLException
     */
    protected abstract void insertUndoLogWithNormal(String xid, long branchId, String rollbackCtx, byte[] undoLogContent, Connection conn) throws SQLException;
}
