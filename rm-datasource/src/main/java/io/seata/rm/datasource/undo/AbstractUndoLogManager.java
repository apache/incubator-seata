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

import io.seata.common.Constants;
import io.seata.common.util.CollectionUtils;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ClientTableColumnsName;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.exception.TransactionException;
import io.seata.rm.datasource.ConnectionContext;
import io.seata.rm.datasource.ConnectionProxy;
import io.seata.rm.datasource.DataSourceProxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract undoLogManager
 *
 * @author Zhibei Hao
 * @date 2019/8/23
 */
public abstract class AbstractUndoLogManager {
  protected enum State {
    /** This state can be properly rolled back by services */
    Normal(0),
    /**
     * This state prevents the branch transaction from inserting undo_log after the global
     * transaction is rolled back.
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

  protected static final String UNDO_LOG_TABLE_NAME =
      ConfigurationFactory.getInstance()
          .getConfig(
              ConfigurationKeys.TRANSACTION_UNDO_LOG_TABLE,
              ConfigurationKeys.TRANSACTION_UNDO_LOG_DEFAULT_TABLE);

  protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractUndoLogManager.class);
  /** branch_id, xid, context, rollback_info, log_status, log_created, log_modified */
  protected static final String INSERT_UNDO_LOG_SQL =
      "INSERT INTO "
          + UNDO_LOG_TABLE_NAME
          + " ("
          + ClientTableColumnsName.UNDO_LOG_BRANCH_XID
          + ", "
          + ClientTableColumnsName.UNDO_LOG_XID
          + ", "
          + ClientTableColumnsName.UNDO_LOG_CONTEXT
          + ", "
          + ClientTableColumnsName.UNDO_LOG_ROLLBACK_INFO
          + ", "
          + ClientTableColumnsName.UNDO_LOG_LOG_STATUS
          + ", "
          + ClientTableColumnsName.UNDO_LOG_LOG_CREATED
          + ", "
          + ClientTableColumnsName.UNDO_LOG_LOG_MODIFIED
          + ")"
          + " VALUES (?, ?, ?, ?, ?, now(), now())";

  protected static final String DELETE_UNDO_LOG_SQL =
      "DELETE FROM "
          + UNDO_LOG_TABLE_NAME
          + " WHERE "
          + ClientTableColumnsName.UNDO_LOG_BRANCH_XID
          + " = ? AND "
          + ClientTableColumnsName.UNDO_LOG_XID
          + " = ?";

  protected static final String DELETE_UNDO_LOG_BY_CREATE_SQL =
      "DELETE FROM " + UNDO_LOG_TABLE_NAME + " WHERE log_created <= ? LIMIT ?";

  protected static final String SELECT_UNDO_LOG_SQL =
      "SELECT * FROM "
          + UNDO_LOG_TABLE_NAME
          + " WHERE "
          + ClientTableColumnsName.UNDO_LOG_BRANCH_XID
          + " = ? AND "
          + ClientTableColumnsName.UNDO_LOG_XID
          + " = ? FOR UPDATE";

  /**
   * Get Db Type
   *
   * @return dbType
   */
  public abstract String getDbType();

  /**
   * Assert DbType Is Supported.
   *
   * @param dbType
   */
  public abstract void assertDbSupport(String dbType);

  /**
   * Undo.
   *
   * @param dataSourceProxy the data source proxy
   * @param xid the xid
   * @param branchId the branch id
   * @throws TransactionException the transaction exception
   */
  public abstract void undo(DataSourceProxy dataSourceProxy, String xid, long branchId)
      throws TransactionException;

  /**
   * Delete UndoLog By LogCreated
   *
   * @param logCreated
   * @param dbType
   * @param limitRows
   * @param conn
   * @return
   * @throws SQLException
   */
  public abstract int deleteUndoLogByLogCreated(
      Date logCreated, String dbType, int limitRows, Connection conn) throws SQLException;

  /**
   * Get Current Serializer
   *
   * @return
   */
  public abstract String getCurrentSerializer();

  /**
   * Insert UndoLog With Normal
   *
   * @param xid
   * @param branchID
   * @param rollbackCtx
   * @param undoLogContent
   * @param conn
   * @throws SQLException
   */
  public abstract void insertUndoLogWithNormal(
      String xid, long branchID, String rollbackCtx, byte[] undoLogContent, Connection conn)
      throws SQLException;

  /**
   * Insert UndoLog With GlobalFinished
   *
   * @param xid
   * @param branchID
   * @param parser
   * @param conn
   * @throws SQLException
   */
  public abstract void insertUndoLogWithGlobalFinished(
      String xid, long branchID, UndoLogParser parser, Connection conn) throws SQLException;

  /**
   * Insert UndoLog
   *
   * @param xid
   * @param branchID
   * @param rollbackCtx
   * @param undoLogContent
   * @param state
   * @param conn
   * @throws SQLException
   */
  public abstract void insertUndoLog(
      String xid,
      long branchID,
      String rollbackCtx,
      byte[] undoLogContent,
      State state,
      Connection conn)
      throws SQLException;

  /**
   * Delete undo log.
   *
   * @param xid the xid
   * @param branchId the branch id
   * @param conn the conn
   * @throws SQLException the sql exception
   */
  protected void deleteUndoLog(String xid, long branchId, Connection conn) throws SQLException {
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
  public void batchDeleteUndoLog(Set<String> xids, Set<Long> branchIds, Connection conn)
      throws SQLException {
    int xidSize = xids.size();
    int branchIdSize = branchIds.size();
    String batchDeleteSql = toBatchDeleteUndoLogSql(xidSize, branchIdSize);
    PreparedStatement deletePST = null;
    try {
      deletePST = conn.prepareStatement(batchDeleteSql);
      int paramsIndex = 1;
      for (Long branchId : branchIds) {
        deletePST.setLong(paramsIndex++, branchId);
      }
      for (String xid : xids) {
        deletePST.setString(paramsIndex++, xid);
      }
      int deleteRows = deletePST.executeUpdate();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("batch delete undo log size " + deleteRows);
      }
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
   * Flush undo logs.
   *
   * @param cp the cp
   * @throws SQLException the sql exception
   */
  public void flushUndoLogs(ConnectionProxy cp) throws SQLException {
    assertDbSupport(cp.getDbType());

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

    insertUndoLogWithNormal(
        xid, branchID, buildContext(parser.getName()), undoLogContent, cp.getTargetConnection());
  }

  /**
   * Convert to Batch Delete UndoLog Sql
   *
   * @param xidSize
   * @param branchIdSize
   * @return
   */
  protected String toBatchDeleteUndoLogSql(int xidSize, int branchIdSize) {
    StringBuilder sqlBuilder = new StringBuilder(64);
    sqlBuilder
        .append("DELETE FROM ")
        .append(UNDO_LOG_TABLE_NAME)
        .append(" WHERE  " + ClientTableColumnsName.UNDO_LOG_BRANCH_XID + " IN ");
    appendInParam(branchIdSize, sqlBuilder);
    sqlBuilder.append(" AND " + ClientTableColumnsName.UNDO_LOG_XID + " IN ");
    appendInParam(xidSize, sqlBuilder);
    return sqlBuilder.toString();
  }

  /**
   * Append In Param
   *
   * @param size
   * @param sqlBuilder
   */
  protected void appendInParam(int size, StringBuilder sqlBuilder) {
    sqlBuilder.append(" (");
    for (int i = 0; i < size; i++) {
      sqlBuilder.append("?");
      if (i < (size - 1)) {
        sqlBuilder.append(",");
      }
    }
    sqlBuilder.append(") ");
  }

  /**
   * Judge Can Undo Or Not
   *
   * @param state
   * @return
   */
  protected boolean canUndo(int state) {
    return state == State.Normal.getValue();
  }

  /**
   * Build Context
   *
   * @param serializer
   * @return
   */
  protected String buildContext(String serializer) {
    Map<String, String> map = new HashMap<>();
    map.put(UndoLogConstants.SERIALIZER_KEY, serializer);
    return CollectionUtils.encodeMap(map);
  }

  /**
   * Parse Context
   *
   * @param data
   * @return
   */
  protected Map<String, String> parseContext(String data) {
    return CollectionUtils.decodeMap(data);
  }
}
