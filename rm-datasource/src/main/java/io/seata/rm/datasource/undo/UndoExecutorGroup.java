package io.seata.rm.datasource.undo;

public interface UndoExecutorGroup
{
  AbstractUndoExecutor getInsertExecutor(SQLUndoLog sqlUndoLog);

  AbstractUndoExecutor getUpdateExecutor(SQLUndoLog sqlUndoLog);

  AbstractUndoExecutor getDeleteExecutor(SQLUndoLog sqlUndoLog);

  String getDbType();
}
