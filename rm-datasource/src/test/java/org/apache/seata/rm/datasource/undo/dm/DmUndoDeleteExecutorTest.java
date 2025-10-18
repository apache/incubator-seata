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
package org.apache.seata.rm.datasource.undo.dm;

import org.apache.seata.rm.datasource.sql.struct.Row;
import org.apache.seata.rm.datasource.sql.struct.TableRecords;
import org.apache.seata.rm.datasource.undo.BaseExecutorTest;
import org.apache.seata.rm.datasource.undo.SQLUndoLog;
import org.apache.seata.sqlparser.SQLType;
import org.apache.seata.sqlparser.struct.ColumnMeta;
import org.apache.seata.sqlparser.struct.TableMeta;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The type DmUndoDeleteExecutor test.
 */
public class DmUndoDeleteExecutorTest extends BaseExecutorTest {

    @Test
    public void buildUndoSQL() {
        DmUndoDeleteExecutor executor = createExecutor();

        String sql = executor.buildUndoSQL();
        Assertions.assertNotNull(sql);
        Assertions.assertTrue(sql.contains("INSERT INTO"));
        Assertions.assertTrue(sql.contains("\"id\""));
        Assertions.assertTrue(sql.contains("\"age\""));
        Assertions.assertTrue(sql.contains("table_name"));
    }

    @Test
    public void buildUndoSQLWithAutoIncrement() {
        DmUndoDeleteExecutor executor = createExecutorWithAutoIncrement();

        String sql = executor.buildUndoSQL();
        Assertions.assertNotNull(sql);
        Assertions.assertTrue(sql.contains("SET IDENTITY_INSERT"));
        Assertions.assertTrue(sql.contains("INSERT INTO"));
        Assertions.assertTrue(sql.contains("\"id\""));
        Assertions.assertTrue(sql.contains("\"age\""));
        Assertions.assertTrue(sql.contains("table_name"));
    }

    @Test
    public void getUndoRows() {
        DmUndoDeleteExecutor executor = createExecutor();
        Assertions.assertEquals(executor.getUndoRows(), executor.getSqlUndoLog().getBeforeImage());
    }

    @Test
    public void testInvalidUndoLog() {
        TableMeta tableMeta = Mockito.mock(TableMeta.class);
        Mockito.when(tableMeta.getPrimaryKeyOnlyName()).thenReturn(Arrays.asList("id"));
        Mockito.when(tableMeta.getTableName()).thenReturn("table_name");

        TableRecords beforeImage = new TableRecords();
        beforeImage.setTableName("table_name");
        beforeImage.setTableMeta(tableMeta);
        beforeImage.setRows(new ArrayList<>());

        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setSqlType(SQLType.DELETE);
        sqlUndoLog.setTableMeta(tableMeta);
        sqlUndoLog.setTableName("table_name");
        sqlUndoLog.setBeforeImage(beforeImage);
        sqlUndoLog.setAfterImage(new TableRecords());

        DmUndoDeleteExecutor executor = new DmUndoDeleteExecutor(sqlUndoLog);

        Assertions.assertThrows(Exception.class, executor::buildUndoSQL);
    }

    private DmUndoDeleteExecutor createExecutor() {
        TableMeta tableMeta = Mockito.mock(TableMeta.class);
        Mockito.when(tableMeta.getPrimaryKeyOnlyName()).thenReturn(Arrays.asList("id"));
        Mockito.when(tableMeta.getTableName()).thenReturn("table_name");

        TableRecords beforeImage = new TableRecords();
        beforeImage.setTableName("table_name");
        beforeImage.setTableMeta(tableMeta);
        List<Row> beforeRows = new ArrayList<>();
        Row row0 = new Row();
        addField(row0, "id", 1, "12345");
        addField(row0, "age", 1, "1");
        beforeRows.add(row0);
        Row row1 = new Row();
        addField(row1, "id", 1, "12346");
        addField(row1, "age", 1, "1");
        beforeRows.add(row1);
        beforeImage.setRows(beforeRows);

        TableRecords afterImage = new TableRecords();
        afterImage.setTableName("table_name");
        afterImage.setTableMeta(tableMeta);

        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setSqlType(SQLType.DELETE);
        sqlUndoLog.setTableMeta(tableMeta);
        sqlUndoLog.setTableName("table_name");
        sqlUndoLog.setBeforeImage(beforeImage);
        sqlUndoLog.setAfterImage(afterImage);

        return new DmUndoDeleteExecutor(sqlUndoLog);
    }

    private DmUndoDeleteExecutor createExecutorWithAutoIncrement() {
        TableMeta tableMeta = Mockito.mock(TableMeta.class);
        Mockito.when(tableMeta.getPrimaryKeyOnlyName()).thenReturn(Arrays.asList("id"));
        Mockito.when(tableMeta.getTableName()).thenReturn("table_name");

        ColumnMeta idColumnMeta = Mockito.mock(ColumnMeta.class);
        Mockito.when(idColumnMeta.isAutoincrement()).thenReturn(true);
        Mockito.when(tableMeta.getColumnMeta("id")).thenReturn(idColumnMeta);

        TableRecords beforeImage = new TableRecords();
        beforeImage.setTableName("table_name");
        beforeImage.setTableMeta(tableMeta);
        List<Row> beforeRows = new ArrayList<>();
        Row row0 = new Row();
        addField(row0, "id", 1, "12345");
        addField(row0, "age", 1, "1");
        beforeRows.add(row0);
        beforeImage.setRows(beforeRows);

        TableRecords afterImage = new TableRecords();
        afterImage.setTableName("table_name");
        afterImage.setTableMeta(tableMeta);

        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setSqlType(SQLType.DELETE);
        sqlUndoLog.setTableMeta(tableMeta);
        sqlUndoLog.setTableName("table_name");
        sqlUndoLog.setBeforeImage(beforeImage);
        sqlUndoLog.setAfterImage(afterImage);

        return new DmUndoDeleteExecutor(sqlUndoLog);
    }
}
