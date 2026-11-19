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
package org.apache.seata.rm.datasource.undo.clickhouse;

import org.apache.seata.rm.datasource.sql.struct.Field;
import org.apache.seata.rm.datasource.sql.struct.Row;
import org.apache.seata.rm.datasource.sql.struct.TableMeta;
import org.apache.seata.rm.datasource.sql.struct.TableRecords;
import org.apache.seata.rm.datasource.undo.SQLUndoLog;
import org.apache.seata.sqlparser.struct.ColumnMeta;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClickhouseUndoDeleteExecutorTest {

    @Test
    public void testBuildUndoSQL() {
        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setTableName("t1");
        TableMeta tableMeta = Mockito.mock(TableMeta.class);
        Mockito.when(tableMeta.getTableName()).thenReturn("t1");

        ColumnMeta pkMeta = new ColumnMeta();
        pkMeta.setColumnName("id");
        pkMeta.setDataType(Types.INTEGER);
        Mockito.when(tableMeta.getPrimaryKeyOnlyName()).thenReturn(Arrays.asList("id"));

        TableRecords beforeImage = new TableRecords();
        beforeImage.setTableName("t1");
        beforeImage.setTableMeta(tableMeta);

        Row row = new Row();
        Field pkField = new Field("id", Types.INTEGER, 1);
        Field nameField = new Field("name", Types.VARCHAR, "test_deleted");
        row.add(pkField);
        row.add(nameField);

        List<Row> rows = new ArrayList<>();
        rows.add(row);
        beforeImage.setRows(rows);
        sqlUndoLog.setBeforeImage(beforeImage);

        ClickhouseUndoDeleteExecutor executor = new ClickhouseUndoDeleteExecutor(sqlUndoLog);
        String sql = executor.buildUndoSQL();
        Assertions.assertEquals("INSERT INTO t1 (name, id) VALUES (?, ?)", sql.trim());
    }
}
