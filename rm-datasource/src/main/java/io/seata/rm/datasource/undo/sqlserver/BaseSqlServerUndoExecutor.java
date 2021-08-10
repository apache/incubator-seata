package io.seata.rm.datasource.undo.sqlserver;

import io.seata.rm.datasource.undo.AbstractUndoExecutor;
import io.seata.rm.datasource.undo.SQLUndoLog;

/**
 * @author GoodBoyCoder
 * @date 2021-07-26
 */
public abstract class BaseSqlServerUndoExecutor extends AbstractUndoExecutor {
    /**
     * template of check sql
     * TODO support multiple primary key
     */
    private static final String CHECK_SQL_TEMPLATE_SQLSERVER = "SELECT * FROM %s WITH(UPDLOCK) WHERE %s";

    /**
     * Instantiates a new Abstract undo executor.
     *
     * @param sqlUndoLog the sql undo log
     */
    public BaseSqlServerUndoExecutor(SQLUndoLog sqlUndoLog) {
        super(sqlUndoLog);
    }

    @Override
    protected String buildCheckSql(String tableName, String whereCondition) {
        return String.format(CHECK_SQL_TEMPLATE_SQLSERVER, tableName, whereCondition);
    }
}
