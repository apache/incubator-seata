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

import org.apache.seata.rm.datasource.sql.struct.Field;
import org.apache.seata.rm.datasource.sql.struct.KeyType;
import org.apache.seata.rm.datasource.sql.struct.Row;
import org.apache.seata.rm.datasource.sql.struct.TableRecords;
import org.apache.seata.rm.datasource.undo.BaseExecutorTest;
import org.apache.seata.rm.datasource.undo.SQLUndoLog;
import org.apache.seata.sqlparser.SQLType;
import org.apache.seata.sqlparser.struct.TableMeta;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The type DmUndoInsertExecutor test.
 */
public class DmUndoInsertExecutorTest extends BaseExecutorTest {

    @Test
    public void buildUndoSQL() {
        DmUndoInsertExecutor executor = createExecutor();

        String sql = executor.buildUndoSQL();
        Assertions.assertNotNull(sql);
        Assertions.assertTrue(sql.contains("DELETE FROM"));
        Assertions.assertTrue(sql.contains("WHERE"));
        Assertions.assertTrue(sql.contains("\"id\" = ?"));
        Assertions.assertTrue(sql.contains("table_name"));
    }

    @Test
    public void getUndoRows() {
        DmUndoInsertExecutor executor = createExecutor();
        Assertions.assertEquals(executor.getUndoRows(), executor.getSqlUndoLog().getAfterImage());
    }

    @Test
    public void testInvalidUndoLog() {
        TableMeta tableMeta = Mockito.mock(TableMeta.class);
        Mockito.when(tableMeta.getPrimaryKeyOnlyName()).thenReturn(Arrays.asList("id"));
        Mockito.when(tableMeta.getTableName()).thenReturn("table_name");

        TableRecords afterImage = new TableRecords();
        afterImage.setTableName("table_name");
        afterImage.setTableMeta(tableMeta);
        afterImage.setRows(new ArrayList<>());

        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setSqlType(SQLType.INSERT);
        sqlUndoLog.setTableMeta(tableMeta);
        sqlUndoLog.setTableName("table_name");
        sqlUndoLog.setBeforeImage(new TableRecords());
        sqlUndoLog.setAfterImage(afterImage);

        DmUndoInsertExecutor executor = new DmUndoInsertExecutor(sqlUndoLog);

        Assertions.assertThrows(Exception.class, executor::buildUndoSQL);
    }

    @Test
    public void testCompositePrimaryKey() {
        DmUndoInsertExecutor executor = createExecutorWithCompositePrimaryKey();

        String sql = executor.buildUndoSQL();
        Assertions.assertNotNull(sql);
        Assertions.assertTrue(sql.contains("DELETE FROM"));
        Assertions.assertTrue(sql.contains("WHERE"));

        // Check that both primary key columns appear in the WHERE clause
        Assertions.assertTrue(sql.contains("id1"));
        Assertions.assertTrue(sql.contains("id2"));
        Assertions.assertTrue(sql.contains(" = ?"));
        Assertions.assertTrue(sql.contains(" and "));
    }

    @Test
    public void testUndoPrepare() throws SQLException {
        DmUndoInsertExecutor executor = createExecutor();
        PreparedStatement mockStatement = Mockito.mock(PreparedStatement.class);

        ArrayList<Field> undoValues = new ArrayList<>();
        List<Field> pkValueList = new ArrayList<>();
        Field pkField = new Field("id", 1, "12345");
        pkValueList.add(pkField);

        Assertions.assertDoesNotThrow(() -> executor.undoPrepare(mockStatement, undoValues, pkValueList));

        // Verify that setObject was called
        Mockito.verify(mockStatement, Mockito.times(1)).setObject(1, "12345", 1);
    }

    private DmUndoInsertExecutor createExecutor() {
        TableMeta tableMeta = Mockito.mock(TableMeta.class);
        Mockito.when(tableMeta.getPrimaryKeyOnlyName()).thenReturn(Arrays.asList("id"));
        Mockito.when(tableMeta.getTableName()).thenReturn("table_name");

        TableRecords beforeImage = new TableRecords();
        beforeImage.setTableName("table_name");
        beforeImage.setTableMeta(tableMeta);

        TableRecords afterImage = new TableRecords();
        afterImage.setTableName("table_name");
        afterImage.setTableMeta(tableMeta);
        List<Row> afterRows = new ArrayList<>();
        Row row0 = new Row();
        addField(row0, "id", 1, "12345");
        addField(row0, "age", 1, "1");
        afterRows.add(row0);
        Row row1 = new Row();
        addField(row1, "id", 1, "12346");
        addField(row1, "age", 1, "1");
        afterRows.add(row1);
        afterImage.setRows(afterRows);

        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setSqlType(SQLType.INSERT);
        sqlUndoLog.setTableMeta(tableMeta);
        sqlUndoLog.setTableName("table_name");
        sqlUndoLog.setBeforeImage(beforeImage);
        sqlUndoLog.setAfterImage(afterImage);

        return new DmUndoInsertExecutor(sqlUndoLog);
    }

    private DmUndoInsertExecutor createExecutorWithCompositePrimaryKey() {
        TableMeta tableMeta = Mockito.mock(TableMeta.class);
        Mockito.when(tableMeta.getPrimaryKeyOnlyName()).thenReturn(Arrays.asList("id1", "id2"));
        Mockito.when(tableMeta.getTableName()).thenReturn("table_name");

        TableRecords beforeImage = new TableRecords();
        beforeImage.setTableName("table_name");
        beforeImage.setTableMeta(tableMeta);

        TableRecords afterImage = new TableRecords();
        afterImage.setTableName("table_name");
        afterImage.setTableMeta(tableMeta);
        List<Row> afterRows = new ArrayList<>();
        Row row0 = new Row();

        // Manually create primary key fields since addField only sets "id" as primary key
        Field id1Field = new Field("id1", 1, "123");
        id1Field.setKeyType(KeyType.PRIMARY_KEY);
        row0.add(id1Field);

        Field id2Field = new Field("id2", 1, "456");
        id2Field.setKeyType(KeyType.PRIMARY_KEY);
        row0.add(id2Field);

        addField(row0, "age", 1, "25");
        afterRows.add(row0);
        afterImage.setRows(afterRows);

        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setSqlType(SQLType.INSERT);
        sqlUndoLog.setTableMeta(tableMeta);
        sqlUndoLog.setTableName("table_name");
        sqlUndoLog.setBeforeImage(beforeImage);
        sqlUndoLog.setAfterImage(afterImage);

        return new DmUndoInsertExecutor(sqlUndoLog);
    }
}
