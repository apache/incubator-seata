/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.rm.datasource.undo.sqlserver;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.rm.datasource.SqlGenerateUtils;
import org.apache.seata.rm.datasource.sql.struct.Field;
import org.apache.seata.rm.datasource.sql.struct.Row;
import org.apache.seata.rm.datasource.sql.struct.TableRecords;
import org.apache.seata.rm.datasource.undo.SQLUndoLog;
import org.apache.seata.sqlparser.util.JdbcConstants;


public class SqlServerUndoInsertExecutor extends BaseSqlServerUndoExecutor {
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
        return "DELETE FROM " + sqlUndoLog.getTableName() + " WHERE " + whereSql;
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
        }
    }
}
