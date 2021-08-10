package io.seata.rm.datasource.undo.sqlserver;

import io.seata.common.loader.LoadLevel;
import io.seata.rm.datasource.undo.AbstractUndoExecutor;
import io.seata.rm.datasource.undo.SQLUndoLog;
import io.seata.rm.datasource.undo.UndoExecutorHolder;
import io.seata.sqlparser.util.JdbcConstants;

/**
 * @author GoodBoyCoder
 */
@LoadLevel(name = JdbcConstants.SQLSERVER)
public class SqlServerUndoExecutorHolder implements UndoExecutorHolder {
    @Override
    public AbstractUndoExecutor getInsertExecutor(SQLUndoLog sqlUndoLog) {
        return null;
    }

    @Override
    public AbstractUndoExecutor getUpdateExecutor(SQLUndoLog sqlUndoLog) {
        return new SqlServerUndoUpdateExecutor(sqlUndoLog);
    }

    @Override
    public AbstractUndoExecutor getDeleteExecutor(SQLUndoLog sqlUndoLog) {
        return null;
    }
}
