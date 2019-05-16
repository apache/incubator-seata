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
package io.seata.rm.datasource.undo;

import io.seata.rm.datasource.sql.SQLType;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.KeyType;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class AbstractUndoExecutorTest extends BaseExecutorTest{

    @Test
    public void dataValidationUpdate() throws SQLException {
        TableMeta tableMeta = Mockito.mock(TableMeta.class);
        Mockito.when(tableMeta.getPkName()).thenReturn("id");
        Mockito.when(tableMeta.getTableName()).thenReturn("table_name");

        TableRecords beforeImage = new TableRecords();
        beforeImage.setTableName("table_name");
        beforeImage.setTableMeta(tableMeta);

        List<Row> beforeRows = new ArrayList<>();
        Row row0 = new Row();
        Field field01 = addField(row0, "id", 1, "12345");
        Field field02 = addField(row0, "age", 1, "1");
        beforeRows.add(row0);
        Row row1 = new Row();
        Field field11 = addField(row1, "id", 1, "12346");
        Field field12 = addField(row1, "age", 1, "1");
        beforeRows.add(row1);
        beforeImage.setRows(beforeRows);

        TableRecords afterImage = new TableRecords();
        afterImage.setTableName("table_name");
        afterImage.setTableMeta(tableMeta);

        List<Row> afterRows = new ArrayList<>();
        Row row2 = new Row();
        Field field21 = addField(row2, "id", 1, "12345");
        Field field22 = addField(row2, "age", 1, "2");
        afterRows.add(row2);
        Row row3 = new Row();
        Field field31 = addField(row3, "id", 1, "12346");
        Field field32 = addField(row3, "age", 1, "2");
        afterRows.add(row3);
        afterImage.setRows(afterRows);

        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setSqlType(SQLType.UPDATE);
        sqlUndoLog.setTableMeta(tableMeta);
        sqlUndoLog.setTableName("table_name");
        sqlUndoLog.setBeforeImage(beforeImage);
        sqlUndoLog.setAfterImage(afterImage);

        TableRecords currentRecords = new TableRecords();
        currentRecords.setTableName("table_name");
        currentRecords.setTableMeta(tableMeta);

        List<Row> currentRows = new ArrayList<>();
        Row rows4 = new Row();
        Field field41 = addField(rows4, "id", 1, "12345");
        Field field42 = addField(rows4, "age", 1, "2");
        currentRows.add(rows4);
        Row row5 = new Row();
        Field field51 = addField(row5, "id", 1, "12346");
        Field field52 = addField(row5, "age", 1, "2");
        currentRows.add(row5);
        currentRecords.setRows(currentRows);

        TestUndoExecutor executor = new TestUndoExecutor(sqlUndoLog, false);
        Connection connection = Mockito.mock(Connection.class);
        TestUndoExecutor spy = Mockito.spy(executor);

        Mockito.doReturn(currentRecords).when(spy).queryCurrentRecords(connection);

        // case1: normal case  before:1 -> after:2 -> current:2 
        Assertions.assertTrue(spy.dataValidation(connection));

        // case2: dirty data   before:1 -> after:2 -> current:3
        field52.setValue("3");
        try {
            spy.dataValidation(connection);
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof SQLException);
        } finally {
            field52.setValue("2");
        }

        // case 3: before == current before:1 -> after:2 -> current:1
        field42.setValue("1");
        field52.setValue("1");
        try {
            Assertions.assertFalse(spy.dataValidation(connection));
        } finally {
            field42.setValue("2");
            field52.setValue("2");
        }

        // case 4: before == after
        field22.setValue("1");
        field32.setValue("1");
        try {
            Assertions.assertFalse(spy.dataValidation(connection));
        } finally {
            field22.setValue("2");
            field32.setValue("2");
        }
    }

    @Test
    public void dataValidationInsert() throws SQLException {
        TableMeta tableMeta = Mockito.mock(TableMeta.class);
        Mockito.when(tableMeta.getPkName()).thenReturn("id");
        Mockito.when(tableMeta.getTableName()).thenReturn("table_name");

        TableRecords beforeImage = new TableRecords();
        beforeImage.setTableName("table_name");
        beforeImage.setTableMeta(tableMeta);

        List<Row> beforeRows = new ArrayList<>();
        beforeImage.setRows(beforeRows);

        TableRecords afterImage = new TableRecords();
        afterImage.setTableName("table_name");
        afterImage.setTableMeta(tableMeta);

        List<Row> afterRows = new ArrayList<>();
        Row row2 = new Row();
        Field field21 = addField(row2, "id", 1, "12345");
        Field field22 = addField(row2, "age", 1, "2");
        afterRows.add(row2);
        Row row3 = new Row();
        Field field31 = addField(row3, "id", 1, "12346");
        Field field32 = addField(row3, "age", 1, "2");
        afterRows.add(row3);
        afterImage.setRows(afterRows);

        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setSqlType(SQLType.UPDATE);
        sqlUndoLog.setTableMeta(tableMeta);
        sqlUndoLog.setTableName("table_name");
        sqlUndoLog.setBeforeImage(beforeImage);
        sqlUndoLog.setAfterImage(afterImage);

        TableRecords currentRecords = new TableRecords();
        currentRecords.setTableName("table_name");
        currentRecords.setTableMeta(tableMeta);

        List<Row> currentRows = new ArrayList<>();
        Row rows4 = new Row();
        Field field41 = addField(rows4, "id", 1, "12345");
        Field field42 = addField(rows4, "age", 1, "2");
        currentRows.add(rows4);
        Row row5 = new Row();
        Field field51 = addField(row5, "id", 1, "12346");
        Field field52 = addField(row5, "age", 1, "2");
        currentRows.add(row5);
        currentRecords.setRows(currentRows);

        TestUndoExecutor executor = new TestUndoExecutor(sqlUndoLog, false);
        Connection connection = Mockito.mock(Connection.class);
        TestUndoExecutor spy = Mockito.spy(executor);
        
        Mockito.doReturn(currentRecords).when(spy).queryCurrentRecords(connection);

        // case1: normal case  before:0 -> after:2 -> current:2 
        Assertions.assertTrue(spy.dataValidation(connection));

        // case2: dirty data   before:0 -> after:2 -> current:2' 
        field52.setValue("3");
        try {
            Assertions.assertTrue(spy.dataValidation(connection));
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof SQLException);
        } finally {
            field52.setValue("2");
        }

        // case3: before == current   before:0 -> after:2 -> current:2' 
        currentRows.clear();
        Assertions.assertFalse(spy.dataValidation(connection));

        // case 4: before == after
        afterRows.clear();
        Assertions.assertFalse(spy.dataValidation(connection));
    }

    @Test
    public void dataValidationDelete() throws SQLException {
        TableMeta tableMeta = Mockito.mock(TableMeta.class);
        Mockito.when(tableMeta.getPkName()).thenReturn("id");
        Mockito.when(tableMeta.getTableName()).thenReturn("table_name");

        TableRecords beforeImage = new TableRecords();
        beforeImage.setTableName("table_name");
        beforeImage.setTableMeta(tableMeta);

        List<Row> beforeRows = new ArrayList<>();
        Row row0 = new Row();
        Field field01 = addField(row0, "id", 1, "12345");
        Field field02 = addField(row0, "age", 1, "2");
        beforeRows.add(row0);
        Row row1 = new Row();
        Field field11 = addField(row1, "id", 1, "12346");
        Field field12 = addField(row1, "age", 1, "2");
        beforeRows.add(row1);
        beforeImage.setRows(beforeRows);

        TableRecords afterImage = new TableRecords();
        afterImage.setTableName("table_name");
        afterImage.setTableMeta(tableMeta);

        List<Row> afterRows = new ArrayList<>();
        afterImage.setRows(afterRows);

        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setSqlType(SQLType.UPDATE);
        sqlUndoLog.setTableMeta(tableMeta);
        sqlUndoLog.setTableName("table_name");
        sqlUndoLog.setBeforeImage(beforeImage);
        sqlUndoLog.setAfterImage(afterImage);

        TableRecords currentRecords = new TableRecords();
        currentRecords.setTableName("table_name");
        currentRecords.setTableMeta(tableMeta);

        List<Row> currentRows = new ArrayList<>();
        currentRecords.setRows(currentRows);

        TestUndoExecutor executor = new TestUndoExecutor(sqlUndoLog, true);
        Connection connection = Mockito.mock(Connection.class);
        TestUndoExecutor spy = Mockito.spy(executor);

        Mockito.doReturn(currentRecords).when(spy).queryCurrentRecords(connection);

        // case1: normal case  before:2 -> after:0 -> current:0
        Assertions.assertTrue(spy.dataValidation(connection));

        // case2: dirty data   before:2 -> after:0 -> current:1
        Row rows4 = new Row();
        Field field41 = addField(rows4, "id", 1, "12345");
        Field field42 = addField(rows4, "age", 1, "2");
        currentRows.add(rows4);
        try {
            Assertions.assertTrue(spy.dataValidation(connection));
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof SQLException);
        }

        // case3: before == current   before:2 -> after:0 -> current:2
        Row row5 = new Row();
        Field field51 = addField(row5, "id", 1, "12346");
        Field field52 = addField(row5, "age", 1, "2");
        currentRows.add(row5);
        Assertions.assertFalse(spy.dataValidation(connection));

        // case 4: before == after  before:2 -> after:2 -> current:2
        Row row2 = new Row();
        Field field21 = addField(row2, "id", 1, "12345");
        Field field22 = addField(row2, "age", 1, "2");
        afterRows.add(row2);
        Row row3 = new Row();
        Field field31 = addField(row3, "id", 1, "12346");
        Field field32 = addField(row3, "age", 1, "2");
        afterRows.add(row3);
        Assertions.assertFalse(spy.dataValidation(connection));
    }
    
    @Test
    public void testParsePK(){
        TableMeta tableMeta = Mockito.mock(TableMeta.class);
        Mockito.when(tableMeta.getPkName()).thenReturn("id");
        Mockito.when(tableMeta.getTableName()).thenReturn("table_name");

        TableRecords beforeImage = new TableRecords();
        beforeImage.setTableName("table_name");
        beforeImage.setTableMeta(tableMeta);

        List<Row> beforeRows = new ArrayList<>();
        Row row0 = new Row();
        Field field01 = addField(row0, "id", 1, "12345");
        Field field02 = addField(row0, "age", 1, "2");
        beforeRows.add(row0);
        Row row1 = new Row();
        Field field11 = addField(row1, "id", 1, "12346");
        Field field12 = addField(row1, "age", 1, "2");
        beforeRows.add(row1);
        beforeImage.setRows(beforeRows);

        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setSqlType(SQLType.UPDATE);
        sqlUndoLog.setTableMeta(tableMeta);
        sqlUndoLog.setTableName("table_name");
        sqlUndoLog.setBeforeImage(beforeImage);
        sqlUndoLog.setAfterImage(null);
        
        TestUndoExecutor executor = new TestUndoExecutor(sqlUndoLog, true);
        Object[] pkValues = executor.parsePkValues(beforeImage);
        Assertions.assertEquals(2, pkValues.length);
    }
}

class TestUndoExecutor extends AbstractUndoExecutor {
    private boolean isDelete;
    public TestUndoExecutor(SQLUndoLog sqlUndoLog, boolean isDelete) {
        super(sqlUndoLog);
        this.isDelete = isDelete;
    }

    @Override
    protected String buildUndoSQL() {
        return null;
    }

    @Override
    protected TableRecords getUndoRows() {
        return isDelete ? sqlUndoLog.getBeforeImage() : sqlUndoLog.getAfterImage();
    }
}