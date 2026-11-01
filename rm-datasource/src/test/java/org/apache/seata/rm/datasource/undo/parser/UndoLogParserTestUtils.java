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
package org.apache.seata.rm.datasource.undo.parser;

import org.apache.seata.rm.datasource.sql.struct.Field;
import org.apache.seata.rm.datasource.sql.struct.Row;
import org.apache.seata.rm.datasource.sql.struct.TableRecords;
import org.apache.seata.rm.datasource.undo.BranchUndoLog;
import org.apache.seata.rm.datasource.undo.SQLUndoLog;
import org.apache.seata.rm.datasource.undo.UndoLogParser;
import org.apache.seata.sqlparser.SQLType;
import org.apache.seata.sqlparser.struct.TableMeta;

import java.util.ArrayList;
import java.util.List;

final class UndoLogParserTestUtils {

    private UndoLogParserTestUtils() {}

    /**
     * Encodes an undo log whose before image holds a single field with the given value, decodes it again and
     * returns the decoded field value.
     */
    static Object roundTripValue(UndoLogParser parser, int jdbcType, Object value) {
        Row row = new Row();
        row.add(new Field("col", jdbcType, value));
        List<Row> rows = new ArrayList<>();
        rows.add(row);

        TableRecords beforeImage = new TableRecords();
        beforeImage.setTableName("test_table");
        beforeImage.setTableMeta(new TableMeta());
        beforeImage.setRows(rows);

        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setSqlType(SQLType.UPDATE);
        sqlUndoLog.setTableName("test_table");
        sqlUndoLog.setBeforeImage(beforeImage);
        sqlUndoLog.setAfterImage(TableRecords.empty(new TableMeta()));

        List<SQLUndoLog> sqlUndoLogs = new ArrayList<>();
        sqlUndoLogs.add(sqlUndoLog);
        BranchUndoLog branchUndoLog = new BranchUndoLog();
        branchUndoLog.setXid("192.168.0.1:8091:123456");
        branchUndoLog.setBranchId(123457);
        branchUndoLog.setSqlUndoLogs(sqlUndoLogs);

        BranchUndoLog decoded = parser.decode(parser.encode(branchUndoLog));
        return decoded.getSqlUndoLogs()
                .get(0)
                .getBeforeImage()
                .getRows()
                .get(0)
                .getFields()
                .get(0)
                .getValue();
    }
}
