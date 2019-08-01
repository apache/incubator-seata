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
import io.seata.core.exception.TransactionException;
import io.seata.rm.datasource.ConnectionContext;
import io.seata.rm.datasource.ConnectionProxy;
import io.seata.rm.datasource.DataSourceProxy;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableMetaCacheOracle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import static io.seata.core.exception.TransactionExceptionCode.BranchRollbackFailed_Retriable;

/**
 * The type Undo log manager.
 *
 * @author ccg
 * @date 2019/3/25
 */
public final class UndoLogManagerOracle {

    private static final Logger LOGGER = LoggerFactory.getLogger(UndoLogManagerOracle.class);
    private static final ThreadLocal<String> SERIALIZER_LOCAL = new ThreadLocal<>();
    private static String UNDO_LOG_TABLE_NAME = "undo_log";
    private static String INSERT_UNDO_LOG_SQL = "INSERT INTO " + UNDO_LOG_TABLE_NAME + "\n" +
            "\t(id,branch_id, xid,context, rollback_info, log_status, log_created, log_modified)\n" +
            "VALUES (UNDO_LOG_SEQ.nextval,?, ?, ?,?, ?, sysdate, sysdate)";
    private static String DELETE_UNDO_LOG_SQL = "DELETE FROM " + UNDO_LOG_TABLE_NAME + "\n" +
            "\tWHERE branch_id = ? AND xid = ?";

    private static String SELECT_UNDO_LOG_SQL = "SELECT * FROM " + UNDO_LOG_TABLE_NAME
            + " WHERE branch_id = ? AND xid = ? FOR UPDATE";

    private UndoLogManagerOracle() {

    }

    /**
     * Flush undo logs.
     *
     * @param cp the cp
     * @throws SQLException the sql exception
     */
    public static void flushUndoLogs(ConnectionProxy cp) throws SQLException {
        assertDbSupport(cp.getDbType());

        ConnectionContext connectionContext = cp.getContext();
        String xid = connectionContext.getXid();
        long branchId = connectionContext.getBranchId();

        BranchUndoLog branchUndoLog = new BranchUndoLog();
        branchUndoLog.setXid(xid);
        branchUndoLog.setBranchId(branchId);
        branchUndoLog.setSqlUndoLogs(connectionContext.getUndoItems());

        UndoLogParser parser = UndoLogParserFactory.getInstance();
        byte[] undoLogContent = parser.encode(branchUndoLog);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Flushing UNDO LOG: {}", new String(undoLogContent, Constants.DEFAULT_CHARSET));
        }

        insertUndoLogWithNormal(xid, branchId, buildContext(parser.getName()), undoLogContent, cp.getTargetConnection());
    }

    private static void assertDbSupport(String dbType) {
        if (!JdbcConstants.ORACLE.equalsIgnoreCase(dbType)) {
            throw new NotSupportYetException("DbType[" + dbType + "] is not support yet!");
        }
    }

    /**
     * Undo.
     *
     * @param dataSourceProxy the data source proxy
     * @param xid             the xid
     * @param branchId        the branch id
     * @throws TransactionException the transaction exception
     */
    public static void undo(DataSourceProxy dataSourceProxy, String xid, long branchId) throws TransactionException {
        assertDbSupport(dataSourceProxy.getDbType());

        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement selectPST = null;

        for (; ; ) {
            try {
                conn = dataSourceProxy.getPlainConnection();

                // The entire undo process should run in a local transaction.
                conn.setAutoCommit(false);

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
                    int state = rs.getInt("log_status");
                    if (!canUndo(state)) {
                        if (LOGGER.isInfoEnabled()) {
                            LOGGER.info("xid {} branch {}, ignore {} undo_log",
                                    xid, branchId, state);
                        }
                        return;
                    }

                    String contextString = rs.getString("context");
                    Map<String, String> context = parseContext(contextString);
                    Blob b = rs.getBlob("rollback_info");
                    byte[] rollbackInfo = BlobUtils.blob2Bytes(b);

                    String serializer = context == null ? null : context.get(UndoLogConstants.SERIALIZER_KEY);
                    UndoLogParser parser = serializer == null ? UndoLogParserFactory.getInstance() :
                            UndoLogParserFactory.getInstance(serializer);
                    BranchUndoLog branchUndoLog = parser.decode(rollbackInfo);

                    try {
                        // put serializer name to local
                        SERIALIZER_LOCAL.set(parser.getName());

                        for (SQLUndoLog sqlUndoLog : branchUndoLog.getSqlUndoLogs()) {
                            TableMeta tableMeta = TableMetaCacheOracle.getTableMeta(dataSourceProxy, sqlUndoLog.getTableName());
                            sqlUndoLog.setTableMeta(tableMeta);
                            AbstractUndoExecutor undoExecutor = UndoExecutorFactory.getUndoExecutor(dataSourceProxy.getDbType(),
                                    sqlUndoLog);
                            undoExecutor.executeOn(conn);
                        }
                    } finally {
                        // remove serializer name
                        SERIALIZER_LOCAL.remove();
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
                    LOGGER.info("xid {} branch {}, undo_log deleted with {}",
                            xid, branchId, State.GlobalFinished.name());
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
                throw new TransactionException(BranchRollbackFailed_Retriable, String.format("%s/%s", branchId, xid), e);

            } finally {
                try {
                    if (rs != null) {
                        rs.close();
                    }
                    if (selectPST != null) {
                        selectPST.close();
                    }
                    if (conn != null) {
                        conn.close();
                    }
                } catch (SQLException closeEx) {
                    LOGGER.warn("Failed to close JDBC resource while undo ... ", closeEx);
                }
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
    public static void batchDeleteUndoLog(Set<String> xids, Set<Long> branchIds, Connection conn) throws SQLException {
        if (CollectionUtils.isEmpty(xids) || CollectionUtils.isEmpty(branchIds)) {
            return;
        }
        int xidSize = xids.size();
        int branchIdSize = branchIds.size();
        String batchDeleteSql = toBatchDeleteUndoLogSql(xidSize, branchIdSize);
        try (PreparedStatement deletePst = conn.prepareStatement(batchDeleteSql)) {
            int paramsIndex = 1;
            for (Long branchId : branchIds) {
                deletePst.setLong(paramsIndex++, branchId);
            }
            for (String xid : xids) {
                deletePst.setString(paramsIndex++, xid);
            }
            int deleteRows = deletePst.executeUpdate();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("batch delete undo log size " + deleteRows);
            }
        } catch (Exception e) {
            if (!(e instanceof SQLException)) {
                e = new SQLException(e);
            }
            throw (SQLException) e;
        }

    }

    protected static String toBatchDeleteUndoLogSql(int xidSize, int branchIdSize) {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("DELETE FROM ")
                .append(UNDO_LOG_TABLE_NAME)
                .append(" WHERE  branch_id IN ");
        appendInParam(branchIdSize, sqlBuilder);
        sqlBuilder.append(" AND xid IN ");
        appendInParam(xidSize, sqlBuilder);
        return sqlBuilder.toString();
    }

    protected static void appendInParam(int size, StringBuilder sqlBuilder) {
        sqlBuilder.append(" (");
        StringJoiner joiner = new StringJoiner(",");
        for (int i = 0; i < size; i++) {
            joiner.add("?");
        }
        sqlBuilder.append(joiner.toString());
        sqlBuilder.append(") ");
    }

    /**
     * Delete undo log.
     *
     * @param xid      the xid
     * @param branchId the branch id
     * @param conn     the conn
     * @throws SQLException the sql exception
     */
    public static void deleteUndoLog(String xid, long branchId, Connection conn) throws SQLException {
        try (PreparedStatement deletePst = conn.prepareStatement(DELETE_UNDO_LOG_SQL)) {
            deletePst.setLong(1, branchId);
            deletePst.setString(2, xid);
            deletePst.executeUpdate();
        } catch (Exception e) {
            if (!(e instanceof SQLException)) {
                e = new SQLException(e);
            }
            throw (SQLException) e;
        }
    }

    public static String getCurrentSerializer() {
        return SERIALIZER_LOCAL.get();
    }

    private static void insertUndoLogWithNormal(String xid, long branchId, String rollbackCtx,
                                                byte[] undoLogContent, Connection conn) throws SQLException {
        insertUndoLog(xid, branchId, rollbackCtx, undoLogContent, State.Normal, conn);
    }

    private static void insertUndoLogWithGlobalFinished(String xid, long branchId, UndoLogParser parser,
                                                        Connection conn) throws SQLException {
        insertUndoLog(xid, branchId, buildContext(parser.getName()), parser.getDefaultContent(), State.GlobalFinished, conn);
    }

    private static void insertUndoLog(String xid, long branchId, String rollbackCtx,
                                      byte[] undoLogContent, State state, Connection conn) throws SQLException {
        try (PreparedStatement pst = conn.prepareStatement(INSERT_UNDO_LOG_SQL)) {
            pst.setLong(1, branchId);
            pst.setString(2, xid);
            pst.setString(3, rollbackCtx);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(undoLogContent);
            pst.setBlob(4, inputStream);
            pst.setInt(5, state.getValue());
            pst.executeUpdate();
        } catch (Exception e) {
            if (!(e instanceof SQLException)) {
                e = new SQLException(e);
            }
            throw (SQLException) e;
        }
    }

    private static boolean canUndo(int state) {
        return state == State.Normal.getValue();
    }

    private static String buildContext(String serializer) {
        Map<String, String> map = new HashMap<>();
        map.put(UndoLogConstants.SERIALIZER_KEY, serializer);
        return CollectionUtils.encodeMap(map);
    }

    private static Map<String, String> parseContext(String data) {
        return CollectionUtils.decodeMap(data);
    }

    private enum State {
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

}
