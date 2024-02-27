/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.seata.rm.datasource.undo.db2;


import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.rm.datasource.SqlGenerateUtils;
import org.apache.seata.rm.datasource.sql.struct.Field;
import org.apache.seata.rm.datasource.sql.struct.Row;
import org.apache.seata.rm.datasource.sql.struct.TableRecords;
import org.apache.seata.rm.datasource.undo.AbstractUndoExecutor;
import org.apache.seata.rm.datasource.undo.SQLUndoLog;
import org.apache.seata.sqlparser.util.JdbcConstants;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @author qingjiusanliangsan
 */
public class DB2UndoInsertExecutor extends AbstractUndoExecutor {
    /**
     * Instantiates a new DB2 undo insert executor.
     *
     * @param sqlUndoLog the sql undo log
     */
    public DB2UndoInsertExecutor(SQLUndoLog sqlUndoLog) {
        super(sqlUndoLog);
    }

    /**
     * Undo Inset.
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
        return generateDeleteSql(afterImageRows, afterImage);
    }

    @Override
    protected void undoPrepare(PreparedStatement undoPST, ArrayList<Field> undoValues, List<Field> pkValueList)
            throws SQLException {
        int undoIndex = 0;
        for (Field pkField : pkValueList) {
            undoIndex++;
            undoPST.setObject(undoIndex, pkField.getValue(), pkField.getType());
        }
    }

    @Override
    protected TableRecords getUndoRows() {
        return sqlUndoLog.getAfterImage();
    }

    private String generateDeleteSql(List<Row> rows, TableRecords afterImage) {
        List<String> pkNameList = getOrderedPkList(afterImage, rows.get(0), JdbcConstants.DB2).stream().map(
            Field::getName).collect(Collectors.toList());
        String whereSql = SqlGenerateUtils.buildWhereConditionByPKs(pkNameList, JdbcConstants.DB2);
        return "DELETE FROM " + sqlUndoLog.getTableName() + " WHERE " + whereSql;
    }
}

