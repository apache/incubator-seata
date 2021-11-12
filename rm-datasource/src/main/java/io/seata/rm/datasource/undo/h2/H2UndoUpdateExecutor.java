package io.seata.rm.datasource.undo.h2;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.CollectionUtils;
import io.seata.rm.datasource.ColumnUtils;
import io.seata.rm.datasource.SqlGenerateUtils;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.undo.AbstractUndoExecutor;
import io.seata.rm.datasource.undo.SQLUndoLog;
import io.seata.sqlparser.util.JdbcConstants;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The type h2 sql undo update executor.
 *
 * @author hongyan
 */
public class H2UndoUpdateExecutor extends AbstractUndoExecutor {

    /**
     * UPDATE a SET x = ?, y = ?, z = ? WHERE pk1 in (?) pk2 in (?)
     */
    private static final String UPDATE_SQL_TEMPLATE = "UPDATE %s SET %s WHERE %s ";

    /**
     * Undo Update.
     *
     * @return sql
     */
    @Override
    protected String buildUndoSQL() {
        TableRecords beforeImage = sqlUndoLog.getBeforeImage();
        List<Row> beforeImageRows = beforeImage.getRows();
        if (CollectionUtils.isEmpty(beforeImageRows)) {
            throw new ShouldNeverHappenException("Invalid UNDO LOG"); // TODO
        }
        Row row = beforeImageRows.get(0);

        List<Field> nonPkFields = row.nonPrimaryKeys();
        // update sql undo log before image all field come from table meta. need add escape.
        // see BaseTransactionalExecutor#buildTableRecords
        String updateColumns = nonPkFields.stream().map(
                field -> ColumnUtils.addEscape(field.getName(), JdbcConstants.H2) + " = ?").collect(
                Collectors.joining(", "));

        List<String> pkNameList = getOrderedPkList(beforeImage, row, JdbcConstants.H2).stream().map(e -> e.getName())
                .collect(Collectors.toList());
        String whereSql = SqlGenerateUtils.buildWhereConditionByPKs(pkNameList, JdbcConstants.H2);

        return String.format(UPDATE_SQL_TEMPLATE, sqlUndoLog.getTableName(), updateColumns, whereSql);
    }

    /**
     * Instantiates a new h2 sql undo update executor.
     *
     * @param sqlUndoLog the sql undo log
     */
    public H2UndoUpdateExecutor(SQLUndoLog sqlUndoLog) {
        super(sqlUndoLog);
    }

    @Override
    protected TableRecords getUndoRows() {
        return sqlUndoLog.getBeforeImage();
    }
}
