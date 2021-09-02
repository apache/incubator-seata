package io.seata.rm.datasource.undo.sqlserver;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.CollectionUtils;
import io.seata.rm.datasource.SqlGenerateUtils;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.undo.SQLUndoLog;
import io.seata.sqlparser.util.JdbcConstants;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author GoodBoyCoder
 */
public class SqlServerUndoInsertExecutor extends BaseSqlServerUndoExecutor {
    /**
     * DELETE FROM a WHERE pk = ?
     */
    private static final String DELETE_SQL_TEMPLATE = "DELETE FROM %s WHERE %s ";

    /**
     * Instantiates a new sqlserver undo insert executor.
     *
     * @param sqlUndoLog the sql undo log
     */
    public SqlServerUndoInsertExecutor(SQLUndoLog sqlUndoLog) {
        super(sqlUndoLog);
    }

    @Override
    protected String buildUndoSQL() {
        TableRecords afterImage = sqlUndoLog.getAfterImage();
        List<Row> afterImageRows = afterImage.getRows();
        if (CollectionUtils.isEmpty(afterImageRows)) {
            throw new ShouldNeverHappenException("Invalid UNDO LOG");
        }
        return generateDeleteSql(afterImageRows, afterImage);
    }

    private String generateDeleteSql(List<Row> rows, TableRecords afterImage) {
        List<String> pkNameList = getOrderedPkList(afterImage, rows.get(0), JdbcConstants.SQLSERVER)
                .stream()
                .map(Field::getName)
                .collect(Collectors.toList());
        String whereSql = SqlGenerateUtils.buildWhereConditionByPKs(pkNameList, JdbcConstants.SQLSERVER);
        return String.format(DELETE_SQL_TEMPLATE, sqlUndoLog.getTableName(), whereSql);
    }

    @Override
    protected TableRecords getUndoRows() {
        return sqlUndoLog.getAfterImage();
    }

    @Override
    protected void undoPrepare(PreparedStatement undoPST, ArrayList<Field> undoValues, List<Field> pkValueList) throws SQLException {
        //The purpose to override is because only the primary key value is needed to locate data
        int undoIndex = 0;
        for (Field pkField : pkValueList) {
            undoIndex++;
            undoPST.setObject(undoIndex, pkField.getValue(), pkField.getType());
            System.out.println(undoIndex);
        }
    }
}
