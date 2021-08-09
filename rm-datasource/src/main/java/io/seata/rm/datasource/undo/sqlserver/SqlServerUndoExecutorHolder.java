package io.seata.rm.datasource.undo.sqlserver;

import io.seata.rm.datasource.undo.AbstractUndoExecutor;
import io.seata.rm.datasource.undo.SQLUndoLog;
import io.seata.rm.datasource.undo.UndoExecutorHolder;

/**
 * @author GoodBoyCoder
 * @date 2021-08-09
 */
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
