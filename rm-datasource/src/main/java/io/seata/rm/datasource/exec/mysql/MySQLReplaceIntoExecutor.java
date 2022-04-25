package io.seata.rm.datasource.exec.mysql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import com.google.common.base.Joiner;
import io.seata.common.exception.NotSupportYetException;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.loader.LoadLevel;
import io.seata.common.loader.Scope;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.IOUtil;
import io.seata.common.util.StringUtils;
import io.seata.rm.datasource.ConnectionProxy;
import io.seata.rm.datasource.PreparedStatementProxy;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.exec.StatementCallback;
import io.seata.rm.datasource.sql.struct.ColumnMeta;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.undo.SQLUndoLog;
import io.seata.sqlparser.SQLInsertRecognizer;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.struct.Defaultable;
import io.seata.sqlparser.struct.Null;
import io.seata.sqlparser.util.JdbcConstants;

/**
 * @author jingliu_xiong@foxmail.com
 */
@LoadLevel(name = JdbcConstants.MYSQL, scope = Scope.PROTOTYPE)
public class MySQLReplaceIntoExecutor extends MySQLInsertOrUpdateExecutor implements Defaultable {

    private static final String COLUMN_SEPARATOR = "|";

    /**
     * is the inserted data exist
     */
    private boolean isExistFlag = false;

    public MySQLReplaceIntoExecutor(StatementProxy statementProxy, StatementCallback statementCallback, SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
    }

    public String getSelectSQL() {
        return super.getSelectSQL();
    }

    public ArrayList<List<Object>> getParamAppenderList() {
        return super.getParamAppenderList();
    }

    @Override
    protected Object executeAutoCommitFalse(Object[] args) throws Exception {
        TableRecords beforeImage = beforeImage();
        // insert rows are not existed
        if (CollectionUtils.isNotEmpty(beforeImage.getRows())) {
            isExistFlag = true;
        } else {
            beforeImage = TableRecords.empty(getTableMeta());
        }
        Object result = statementCallback.execute(statementProxy.getTargetStatement(), args);
        TableRecords afterImage = afterImage(beforeImage);
        prepareUndoLogAll(beforeImage, afterImage);
        return result;
    }

    /**
     * build a SQLUndoLog
     *
     * @param beforeImage the before image
     * @param afterImage  the after image
     */
    @Override
    protected void buildUndoItemAll(ConnectionProxy connectionProxy, TableRecords beforeImage, TableRecords afterImage) {
        if (!isExistFlag) {
            SQLUndoLog sqlUndoLog = buildUndoItem(SQLType.INSERT, TableRecords.empty(getTableMeta()), afterImage);
            connectionProxy.appendUndoLog(sqlUndoLog);
            return;
        }
        List<Row> beforeImageRows = beforeImage.getRows();
        List<String> befrePrimaryValues = new ArrayList<>();
        for (Row r : beforeImageRows) {
            String primaryValue = "";
            for (Field f: r.primaryKeys()) {
                primaryValue = primaryValue + f.getValue() + COLUMN_SEPARATOR;
            }
            befrePrimaryValues.add(primaryValue);
        }
        List<Row> insertRows = new ArrayList<>();
        List<Row> updateRows = new ArrayList<>();
        List<Row> afterImageRows = afterImage.getRows();
        for (Row r : afterImageRows) {
            String primaryValue = "";
            for (Field f: r.primaryKeys()) {
                primaryValue = primaryValue + f.getValue()  + COLUMN_SEPARATOR;
            }
            if (befrePrimaryValues.contains(primaryValue)) {
                updateRows.add(r);
            } else {
                insertRows.add(r);
            }
        }
        if (CollectionUtils.isNotEmpty(updateRows)) {
            TableRecords partAfterImage = new TableRecords(afterImage.getTableMeta());
            partAfterImage.setTableName(afterImage.getTableName());
            partAfterImage.setRows(updateRows);
            if (beforeImage.getRows().size() != partAfterImage.getRows().size()) {
                throw new ShouldNeverHappenException("Before image size is not equaled to after image size, probably because you updated the primary keys.");
            }
            connectionProxy.appendUndoLog(buildUndoItem(SQLType.UPDATE, beforeImage, partAfterImage));
        }
        if (CollectionUtils.isNotEmpty(insertRows)) {
            TableRecords partAfterImage = new TableRecords(afterImage.getTableMeta());
            partAfterImage.setTableName(afterImage.getTableName());
            partAfterImage.setRows(insertRows);
            connectionProxy.appendUndoLog(buildUndoItem(SQLType.INSERT, TableRecords.empty(getTableMeta()), partAfterImage));
        }
    }

    /**
     * build a SQLUndoLog
     *
     * @param sqlType
     * @param beforeImage
     * @param afterImage
     * @return sqlUndoLog the sql undo log
     */
    protected SQLUndoLog buildUndoItem(SQLType sqlType, TableRecords beforeImage, TableRecords afterImage) {
        String tableName = sqlRecognizer.getTableName();
        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setSqlType(sqlType);
        sqlUndoLog.setTableName(tableName);
        sqlUndoLog.setBeforeImage(beforeImage);
        sqlUndoLog.setAfterImage(afterImage);
        return sqlUndoLog;
    }

}
