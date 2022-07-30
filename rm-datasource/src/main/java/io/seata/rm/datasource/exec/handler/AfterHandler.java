package io.seata.rm.datasource.exec.handler;

import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.sqlparser.SQLType;

import java.util.List;
import java.util.Map;

/**
 * @author: lyx
 */
public interface AfterHandler {
    /**
     * build after select SQL to append in the SQL when build in before image
     *
     * @param beforeImage beforeImage
     * @return select SQL
     */
    String buildAfterSelectSQL(TableRecords beforeImage);

    /**
     * Gets build undo row
     *
     * @param beforeImage before image
     * @param afterImage  after image
     * @return Map<SQLType, List < Row>>
     */
    Map<SQLType, List<Row>> buildUndoRow(TableRecords beforeImage, TableRecords afterImage);
}
