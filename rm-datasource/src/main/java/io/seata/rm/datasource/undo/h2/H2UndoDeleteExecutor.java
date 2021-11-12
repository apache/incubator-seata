package io.seata.rm.datasource.undo.h2;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.CollectionUtils;
import io.seata.rm.datasource.ColumnUtils;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.undo.AbstractUndoExecutor;
import io.seata.rm.datasource.undo.SQLUndoLog;
import io.seata.sqlparser.util.JdbcConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The type h2 sql undo delete executor.
 *
 * @author hongyan
 */
public class H2UndoDeleteExecutor extends AbstractUndoExecutor {

    /**
     * Instantiates a new h2 sql undo delete executor.
     *
     * @param sqlUndoLog the sql undo log
     */
    public H2UndoDeleteExecutor(SQLUndoLog sqlUndoLog) {
        super(sqlUndoLog);
    }

    /**
     * INSERT INTO a (x, y, z, pk) VALUES (?, ?, ?, ?)
     */
    private static final String INSERT_SQL_TEMPLATE = "INSERT INTO %s (%s) VALUES (%s)";

    /**
     * Undo delete.
     *
     * Notice: PK is at last one.
     * @see AbstractUndoExecutor#undoPrepare
     *
     * @return sql
     */
    @Override
    protected String buildUndoSQL() {
        TableRecords beforeImage = sqlUndoLog.getBeforeImage();
        List<Row> beforeImageRows = beforeImage.getRows();
        if (CollectionUtils.isEmpty(beforeImageRows)) {
            throw new ShouldNeverHappenException("Invalid UNDO LOG");
        }
        Row row = beforeImageRows.get(0);
        List<Field> fields = new ArrayList<>(row.nonPrimaryKeys());
        fields.addAll(getOrderedPkList(beforeImage,row, JdbcConstants.H2));

        // delete sql undo log before image all field come from table meta, need add escape.
        // see BaseTransactionalExecutor#buildTableRecords
        String insertColumns = fields.stream()
                .map(field -> ColumnUtils.addEscape(field.getName(), JdbcConstants.H2))
                .collect(Collectors.joining(", "));
        String insertValues = fields.stream().map(field -> "?")
                .collect(Collectors.joining(", "));

        return String.format(INSERT_SQL_TEMPLATE, sqlUndoLog.getTableName(), insertColumns, insertValues);
    }

    @Override
    protected TableRecords getUndoRows() {
        return sqlUndoLog.getBeforeImage();
    }
}
