/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.rm.datasource.undo.mysql;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.fescar.common.exception.ShouldNeverHappenException;
import com.alibaba.fescar.rm.datasource.sql.struct.Field;
import com.alibaba.fescar.rm.datasource.sql.struct.KeyType;
import com.alibaba.fescar.rm.datasource.sql.struct.Row;
import com.alibaba.fescar.rm.datasource.sql.struct.TableRecords;
import com.alibaba.fescar.rm.datasource.undo.AbstractUndoExecutor;
import com.alibaba.fescar.rm.datasource.undo.SQLUndoLog;

/**
 * The type My sql undo insert executor.
 */
public class MySQLUndoInsertExecutor extends AbstractUndoExecutor {

    @Override
    protected String buildUndoSQL() {
        TableRecords afterImage = sqlUndoLog.getAfterImage();
        List<Row> afterImageRows = afterImage.getRows();
        if (afterImageRows == null || afterImageRows.size() == 0) {
            throw new ShouldNeverHappenException("Invalid UNDO LOG");
        }
        Row row = afterImageRows.get(0);
        StringBuffer mainSQL = new StringBuffer("DELETE FROM " + sqlUndoLog.getTableName());
        StringBuffer where = new StringBuffer(" WHERE ");
        boolean first = true;
        for (Field field : row.getFields()) {
            if (field.getKeyType() == KeyType.PrimaryKey) {
                where.append(field.getName() + " = ? ");
            }

        }
        return mainSQL.append(where).toString();
    }

    @Override
    protected void undoPrepare(PreparedStatement undoPST, ArrayList<Field> undoValues, Field pkValue) throws SQLException {
        undoPST.setObject(1, pkValue.getValue(), pkValue.getType());
    }

    /**
     * Instantiates a new My sql undo insert executor.
     *
     * @param sqlUndoLog the sql undo log
     */
    public MySQLUndoInsertExecutor(SQLUndoLog sqlUndoLog) {
        super(sqlUndoLog);
    }

    @Override
    protected TableRecords getUndoRows() {
        return sqlUndoLog.getAfterImage();
    }
}
