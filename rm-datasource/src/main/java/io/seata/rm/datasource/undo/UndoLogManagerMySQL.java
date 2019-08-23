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

import static io.seata.core.exception.TransactionExceptionCode.BranchRollbackFailed_Retriable;

import com.alibaba.druid.util.JdbcConstants;
import io.seata.common.exception.NotSupportYetException;
import io.seata.common.util.BlobUtils;
import io.seata.core.constants.ClientTableColumnsName;
import io.seata.core.exception.TransactionException;
import io.seata.rm.datasource.DataSourceProxy;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableMetaCache;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * The type Undo log manager.
 *
 * @author sharajava
 * @author Geng Zhang
 */
public final class UndoLogManagerMySQL extends AbstractUndoLogManager {
  private static final ThreadLocal<String> SERIALIZER_LOCAL = new ThreadLocal<>();

  @Override
  public String getDbType() {
    return "mysql";
  }

  @Override
  public void assertDbSupport(String dbType) {
    if (!JdbcConstants.MYSQL.equals(dbType)) {
      throw new NotSupportYetException("DbType[" + dbType + "] is not support yet!");
    }
  }

  /**
   * Undo.
   *
   * @param dataSourceProxy the data source proxy
   * @param xid the xid
   * @param branchId the branch id
   * @throws TransactionException the transaction exception
   */
  @Override
  public void undo(DataSourceProxy dataSourceProxy, String xid, long branchId)
      throws TransactionException {
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
          int state = rs.getInt(ClientTableColumnsName.UNDO_LOG_LOG_STATUS);
          if (!canUndo(state)) {
            if (LOGGER.isInfoEnabled()) {
              LOGGER.info("xid {} branch {}, ignore {} undo_log", xid, branchId, state);
            }
            return;
          }

          String contextString = rs.getString(ClientTableColumnsName.UNDO_LOG_CONTEXT);
          Map<String, String> context = parseContext(contextString);
          Blob b = rs.getBlob(ClientTableColumnsName.UNDO_LOG_ROLLBACK_INFO);
          byte[] rollbackInfo = BlobUtils.blob2Bytes(b);

          String serializer = context == null ? null : context.get(UndoLogConstants.SERIALIZER_KEY);
          UndoLogParser parser =
              serializer == null
                  ? UndoLogParserFactory.getInstance()
                  : UndoLogParserFactory.getInstance(serializer);
          BranchUndoLog branchUndoLog = parser.decode(rollbackInfo);

          try {
            // put serializer name to local
            SERIALIZER_LOCAL.set(parser.getName());
            List<SQLUndoLog> sqlUndoLogs = branchUndoLog.getSqlUndoLogs();
            if (sqlUndoLogs.size() > 1) {
              Collections.reverse(sqlUndoLogs);
            }
            for (SQLUndoLog sqlUndoLog : sqlUndoLogs) {
              TableMeta tableMeta =
                  TableMetaCache.getTableMeta(dataSourceProxy, sqlUndoLog.getTableName());
              sqlUndoLog.setTableMeta(tableMeta);
              AbstractUndoExecutor undoExecutor =
                  UndoExecutorFactory.getUndoExecutor(dataSourceProxy.getDbType(), sqlUndoLog);
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
        // For example, the business processing timeout, the global transaction is the initiator
        // rolls back.
        // To ensure data consistency, we can insert an undo_log with GlobalFinished state
        // to prevent the local transaction of the first phase of other programs from being
        // correctly submitted.
        // See https://github.com/seata/seata/issues/489

        if (exists) {
          deleteUndoLog(xid, branchId, conn);
          conn.commit();
          if (LOGGER.isInfoEnabled()) {
            LOGGER.info(
                "xid {} branch {}, undo_log deleted with {}",
                xid,
                branchId,
                State.GlobalFinished.name());
          }
        } else {
          insertUndoLogWithGlobalFinished(xid, branchId, UndoLogParserFactory.getInstance(), conn);
          conn.commit();
          if (LOGGER.isInfoEnabled()) {
            LOGGER.info(
                "xid {} branch {}, undo_log added with {}",
                xid,
                branchId,
                State.GlobalFinished.name());
          }
        }

        return;
      } catch (SQLIntegrityConstraintViolationException e) {
        // Possible undo_log has been inserted into the database by other processes, retrying
        // rollback undo_log
        if (LOGGER.isInfoEnabled()) {
          LOGGER.info("xid {} branch {}, undo_log inserted, retry rollback", xid, branchId);
        }
      } catch (Throwable e) {
        if (conn != null) {
          try {
            conn.rollback();
          } catch (SQLException rollbackEx) {
            LOGGER.warn("Failed to close JDBC resource while undo ... ", rollbackEx);
          }
        }
        throw new TransactionException(
            BranchRollbackFailed_Retriable,
            String.format("%s/%s %s", branchId, xid, e.getMessage()),
            e);

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

  @Override
  public int deleteUndoLogByLogCreated(
      Date logCreated, String dbType, int limitRows, Connection conn) throws SQLException {
    assertDbSupport(dbType);
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
  public String getCurrentSerializer() {
    return SERIALIZER_LOCAL.get();
  }

  @Override
  public void insertUndoLogWithNormal(
      String xid, long branchID, String rollbackCtx, byte[] undoLogContent, Connection conn)
      throws SQLException {
    insertUndoLog(xid, branchID, rollbackCtx, undoLogContent, State.Normal, conn);
  }

  @Override
  public void insertUndoLogWithGlobalFinished(
      String xid, long branchID, UndoLogParser parser, Connection conn) throws SQLException {
    insertUndoLog(
        xid,
        branchID,
        buildContext(parser.getName()),
        parser.getDefaultContent(),
        State.GlobalFinished,
        conn);
  }

  @Override
  public void insertUndoLog(
      String xid,
      long branchID,
      String rollbackCtx,
      byte[] undoLogContent,
      State state,
      Connection conn)
      throws SQLException {
    PreparedStatement pst = null;
    try {
      pst = conn.prepareStatement(INSERT_UNDO_LOG_SQL);
      pst.setLong(1, branchID);
      pst.setString(2, xid);
      pst.setString(3, rollbackCtx);
      pst.setBlob(4, BlobUtils.bytes2Blob(undoLogContent));
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
