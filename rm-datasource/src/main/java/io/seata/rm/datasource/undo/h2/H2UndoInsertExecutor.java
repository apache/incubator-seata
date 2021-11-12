package io.seata.rm.datasource.undo.h2;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.CollectionUtils;
import io.seata.rm.datasource.SqlGenerateUtils;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.undo.AbstractUndoExecutor;
import io.seata.rm.datasource.undo.SQLUndoLog;
import io.seata.sqlparser.util.JdbcConstants;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The type h2 sql undo insert executor.
 *
 * @author hongyan
 */
public class H2UndoInsertExecutor extends AbstractUndoExecutor {

    /**
     * DELETE FROM a WHERE pk = ?
     */
    private static final String DELETE_SQL_TEMPLATE = "DELETE FROM %s WHERE %s ";

    /**
     * Undo Insert.
     *
     * @return sql
     */
    @Override
    protected String buildUndoSQL() {
        TableRecords afterImage = sqlUndoLog.getAfterImage();
        List<Row> afterImageRows = afterImage.getRows();
        if (CollectionUtils.isEmpty(afterImageRows)) {
            throw new ShouldNeverHappenException("Invalid UNDO LOG");
        }
        return generateDeleteSql(afterImageRows,afterImage);
    }

    @Override
    protected void undoPrepare(PreparedStatement undoPST, ArrayList<Field> undoValues, List<Field> pkValueList)
            throws SQLException {
        int undoIndex = 0;
        for (Field pkField:pkValueList) {
            undoIndex++;
            undoPST.setObject(undoIndex, pkField.getValue(), pkField.getType());
        }
    }

    private String generateDeleteSql(List<Row> rows, TableRecords afterImage) {
        List<String> pkNameList = getOrderedPkList(afterImage, rows.get(0), JdbcConstants.H2).stream().map(
                e -> e.getName()).collect(Collectors.toList());
        String whereSql = SqlGenerateUtils.buildWhereConditionByPKs(pkNameList, JdbcConstants.H2);
        return String.format(DELETE_SQL_TEMPLATE, sqlUndoLog.getTableName(), whereSql);
    }

    /**
     * Instantiates a new My sql undo insert executor.
     *
     * @param sqlUndoLog the sql undo log
     */
    public H2UndoInsertExecutor(SQLUndoLog sqlUndoLog) {
        super(sqlUndoLog);
    }

    @Override
    protected TableRecords getUndoRows() {
        return sqlUndoLog.getAfterImage();
    }
}