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
package org.apache.seata.rm.datasource.undo.oscar;

import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.rm.datasource.sql.struct.Field;
import org.apache.seata.rm.datasource.sql.struct.Row;
import org.apache.seata.rm.datasource.sql.struct.TableRecords;
import org.apache.seata.rm.datasource.undo.AbstractUndoExecutor;
import org.apache.seata.rm.datasource.undo.SQLUndoLog;
import org.apache.seata.sqlparser.util.ColumnUtils;
import org.apache.seata.sqlparser.util.JdbcConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The type oscar undo delete executor.
 *
 */
public class OscarUndoDeleteExecutor extends AbstractUndoExecutor {

    /**
     * INSERT INTO a (x, y, z, pk) VALUES (?, ?, ?, ?)
     */
    private static final String INSERT_SQL_TEMPLATE = "INSERT INTO %s (%s) VALUES (%s)";

    /**
     * Instantiates a new oscar undo delete executor.
     *
     * @param sqlUndoLog the sql undo log
     */
    public OscarUndoDeleteExecutor(SQLUndoLog sqlUndoLog) {
        super(sqlUndoLog);
    }

    @Override
    protected String buildUndoSQL() {
        TableRecords beforeImage = sqlUndoLog.getBeforeImage();
        List<Row> beforeImageRows = beforeImage.getRows();
        if (CollectionUtils.isEmpty(beforeImageRows)) {
            throw new ShouldNeverHappenException("Invalid UNDO LOG");
        }
        Row row = beforeImageRows.get(0);
        List<Field> fields = new ArrayList<>(row.nonPrimaryKeys());
        fields.addAll(getOrderedPkList(beforeImage,row,JdbcConstants.OSCAR));

        // delete sql undo log before image all field come from table meta, need add escape.
        // see BaseTransactionalExecutor#buildTableRecords
        String insertColumns = fields.stream()
            .map(field -> ColumnUtils.addEscape(field.getName(), JdbcConstants.OSCAR))
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
