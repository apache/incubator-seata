package io.seata.rm.datasource.exec.oracle;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.exec.StatementCallback;
import io.seata.rm.datasource.exec.constant.SQLTypeConstant;
import io.seata.rm.datasource.exec.handler.AfterHandler;
import io.seata.rm.datasource.exec.handler.AfterHandlerFactory;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.SQLType;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author: lyx
 */
public class OracleInsertIgnoreExecutor extends OracleInsertExecutor {

    /**
     * before image sql and after image sql,condition is unique index
     */
    private String selectSQL;

    private AfterHandler afterHandler;

    public OracleInsertIgnoreExecutor(StatementProxy statementProxy, StatementCallback statementCallback, SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
        afterHandler = AfterHandlerFactory.getAfterHandler(SQLTypeConstant.INSERT_IGNORE);
    }

    @Override
    public TableRecords beforeImage() throws SQLException {
        TableMeta tableMeta = getTableMeta();
        // after image sql the same of before image
        if (StringUtils.isBlank(selectSQL)) {
            selectSQL = buildImageSQL(tableMeta);
        }
        if (CollectionUtils.isEmpty(paramAppenderMap)) {
            throw new ShouldNeverHappenException("can not find unique param,may be you should add the unique key what you expect to ignore " +
                    "when you use the IGNORE_ROW_ON_DUPKEY_INDEX hint");
        }
        return buildTableRecords2(tableMeta, selectSQL, new ArrayList<List<Object>>(paramAppenderMap.values()));
    }

    @Override
    protected TableRecords afterImage(TableRecords beforeImage) throws SQLException {
        return buildTableRecords2(getTableMeta(), selectSQL + afterHandler.buildAfterSelectSQL(beforeImage),
                new ArrayList<List<Object>>(paramAppenderMap.values()));
    }

    @Override
    protected Map<SQLType, List<Row>> buildUndoRow(TableRecords beforeImage, TableRecords afterImage) {
        return afterHandler.buildUndoRow(beforeImage, afterImage);
    }

}
