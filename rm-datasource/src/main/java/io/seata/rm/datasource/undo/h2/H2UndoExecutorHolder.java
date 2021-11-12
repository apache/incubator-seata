package io.seata.rm.datasource.undo.h2;

import io.seata.common.loader.LoadLevel;
import io.seata.rm.datasource.undo.AbstractUndoExecutor;
import io.seata.rm.datasource.undo.SQLUndoLog;
import io.seata.rm.datasource.undo.UndoExecutorHolder;
import io.seata.sqlparser.util.JdbcConstants;

/**
 * The Type H2UndoExecutorHolder
 *
 * @author: hongyan
 */
@LoadLevel(name = JdbcConstants.H2)
public class H2UndoExecutorHolder implements UndoExecutorHolder {

    @Override
    public AbstractUndoExecutor getInsertExecutor(SQLUndoLog sqlUndoLog) {
        return new H2UndoInsertExecutor(sqlUndoLog);
    }

    @Override
    public AbstractUndoExecutor getUpdateExecutor(SQLUndoLog sqlUndoLog) {
        return new H2UndoUpdateExecutor(sqlUndoLog);
    }

    @Override
    public AbstractUndoExecutor getDeleteExecutor(SQLUndoLog sqlUndoLog) {
        return new H2UndoDeleteExecutor(sqlUndoLog);
    }
}

