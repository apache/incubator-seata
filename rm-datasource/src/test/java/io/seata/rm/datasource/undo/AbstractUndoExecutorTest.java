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
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Geng Zhang
 */
public class AbstractUndoExecutorTest extends BaseH2Test {

    @Test
    public void dataValidationUpdate() throws SQLException {
        execSQL("INSERT INTO table_name(id, name) VALUES (12345,'aaa');");
        execSQL("INSERT INTO table_name(id, name) VALUES (12346,'aaa');");

        TableRecords beforeImage = execQuery(tableMeta, "SELECT * FROM table_name WHERE id IN (12345, 12346);");

        execSQL("update table_name set name = 'xxx' where id in (12345, 12346);");

        TableRecords afterImage = execQuery(tableMeta, "SELECT * FROM table_name WHERE id IN (12345, 12346);");

        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setSqlType(SQLType.UPDATE);
        sqlUndoLog.setTableMeta(tableMeta);
        sqlUndoLog.setTableName("table_name");
        sqlUndoLog.setBeforeImage(beforeImage);
        sqlUndoLog.setAfterImage(afterImage);

        TestUndoExecutor spy = new TestUndoExecutor(sqlUndoLog, false);

        // case1: normal case  before:aaa -> after:xxx -> current:xxx
        Assertions.assertTrue(spy.dataValidationAndGoOn(connection));

        // case2: dirty data   before:aaa -> after:xxx -> current:yyy
        execSQL("update table_name set name = 'yyy' where id in (12345, 12346);");
        try {
            spy.dataValidationAndGoOn(connection);
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof SQLException);
        }

        // case 3: before == current before:aaa -> after:xxx -> current:aaa
        execSQL("update table_name set name = 'aaa' where id in (12345, 12346);");
        Assertions.assertFalse(spy.dataValidationAndGoOn(connection));

        // case 4: before == after   before:aaa -> after:aaa
        afterImage = execQuery(tableMeta, "SELECT * FROM table_name WHERE id IN (12345, 12346);");
        sqlUndoLog.setAfterImage(afterImage);
        Assertions.assertFalse(spy.dataValidationAndGoOn(connection));
    }

    @Test
    public void dataValidationInsert() throws SQLException {
        TableRecords beforeImage = execQuery(tableMeta, "SELECT * FROM table_name WHERE id IN (12345, 12346);");

        execSQL("INSERT INTO table_name(id, name) VALUES (12345,'aaa');");
        execSQL("INSERT INTO table_name(id, name) VALUES (12346,'aaa');");

        TableRecords afterImage = execQuery(tableMeta, "SELECT * FROM table_name WHERE id IN (12345, 12346);");

        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setSqlType(SQLType.INSERT);
        sqlUndoLog.setTableMeta(tableMeta);
        sqlUndoLog.setTableName("table_name");
        sqlUndoLog.setBeforeImage(beforeImage);
        sqlUndoLog.setAfterImage(afterImage);

        TestUndoExecutor spy = new TestUndoExecutor(sqlUndoLog, false);

        // case1: normal case  before:0 -> after:2 -> current:2 
        Assertions.assertTrue(spy.dataValidationAndGoOn(connection));

        // case2: dirty data   before:0 -> after:2 -> current:2' 
        execSQL("update table_name set name = 'yyy' where id in (12345, 12346);");
        try {
            Assertions.assertTrue(spy.dataValidationAndGoOn(connection));
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof SQLException);
        }

        // case3: before == current   before:0 -> after:2 -> current:0
        execSQL("delete from table_name where id in (12345, 12346);");
        Assertions.assertFalse(spy.dataValidationAndGoOn(connection));

        // case 4: before == after   before:0 -> after:0
        afterImage = execQuery(tableMeta, "SELECT * FROM table_name WHERE id IN (12345, 12346);");
        sqlUndoLog.setAfterImage(afterImage);
        Assertions.assertFalse(spy.dataValidationAndGoOn(connection));
    }

    @Test
    public void dataValidationDelete() throws SQLException {
        execSQL("INSERT INTO table_name(id, name) VALUES (12345,'aaa');");
        execSQL("INSERT INTO table_name(id, name) VALUES (12346,'aaa');");

        TableRecords beforeImage = execQuery(tableMeta, "SELECT * FROM table_name WHERE id IN (12345, 12346);");

        execSQL("delete from table_name where id in (12345, 12346);");

        TableRecords afterImage = execQuery(tableMeta, "SELECT * FROM table_name WHERE id IN (12345, 12346);");

        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setSqlType(SQLType.INSERT);
        sqlUndoLog.setTableMeta(tableMeta);
        sqlUndoLog.setTableName("table_name");
        sqlUndoLog.setBeforeImage(beforeImage);
        sqlUndoLog.setAfterImage(afterImage);

        TestUndoExecutor spy = new TestUndoExecutor(sqlUndoLog, true);

        // case1: normal case  before:2 -> after:0 -> current:0
        Assertions.assertTrue(spy.dataValidationAndGoOn(connection));

        // case2: dirty data   before:2 -> after:0 -> current:1
        execSQL("INSERT INTO table_name(id, name) VALUES (12345,'aaa');");
        try {
            Assertions.assertTrue(spy.dataValidationAndGoOn(connection));
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof SQLException);
        }

        // case3: before == current   before:2 -> after:0 -> current:2
        execSQL("INSERT INTO table_name(id, name) VALUES (12346,'aaa');");
        Assertions.assertFalse(spy.dataValidationAndGoOn(connection));

        // case 4: before == after  before:2 -> after:2
        afterImage = execQuery(tableMeta, "SELECT * FROM table_name WHERE id IN (12345, 12346);");
        sqlUndoLog.setAfterImage(afterImage);
        Assertions.assertFalse(spy.dataValidationAndGoOn(connection));
    }

    @Test
    public void testParsePK() {
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
