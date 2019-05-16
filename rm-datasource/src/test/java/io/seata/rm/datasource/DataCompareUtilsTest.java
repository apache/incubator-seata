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
package io.seata.rm.datasource;

import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Geng Zhang
 */
public class DataCompareUtilsTest {

    @Test
    public void isFieldEquals() {
        Field field0 = new Field("name", 0, "111");
        Field field1 = new Field("name", 1, "111");
        Field field2 = new Field("name", 0, "222");
        Field field3 = new Field("age", 0, "222");
        Field field4 = new Field("name", 0, null);

        Assertions.assertFalse(DataCompareUtils.isFieldEquals(field0, null));
        Assertions.assertFalse(DataCompareUtils.isFieldEquals(null, field0));
        Assertions.assertFalse(DataCompareUtils.isFieldEquals(field0, field1));
        Assertions.assertFalse(DataCompareUtils.isFieldEquals(field0, field2));
        Assertions.assertFalse(DataCompareUtils.isFieldEquals(field0, field3));
        Assertions.assertFalse(DataCompareUtils.isFieldEquals(field0, field4));

        Field field10 = new Field("Name", 0, "111");
        Field field11 = new Field("Name", 0, null);
        Assertions.assertTrue(DataCompareUtils.isFieldEquals(field0, field10));
        Assertions.assertTrue(DataCompareUtils.isFieldEquals(field4, field11));
    }

    @Test
    public void isRecordsEquals() {
        TableMeta tableMeta = Mockito.mock(TableMeta.class);
        Mockito.when(tableMeta.getPkName()).thenReturn("pk");
        Mockito.when(tableMeta.getTableName()).thenReturn("table_name");

        TableRecords beforeImage = new TableRecords();
        beforeImage.setTableName("table_name");
        beforeImage.setTableMeta(tableMeta);

        List<Row> rows = new ArrayList<>();
        Row row = new Row();
        Field field01 = addField(row,"pk", 1, "12345");
        Field field02 = addField(row,"age", 1, "18");
        rows.add(row);
        beforeImage.setRows(rows);

        Assertions.assertFalse(DataCompareUtils.isRecordsEquals(beforeImage, null));
        Assertions.assertFalse(DataCompareUtils.isRecordsEquals(null, beforeImage));

        TableRecords afterImage = new TableRecords();
        afterImage.setTableName("table_name1"); // wrong table name
        afterImage.setTableMeta(tableMeta);

        Assertions.assertFalse(DataCompareUtils.isRecordsEquals(beforeImage, afterImage));
        afterImage.setTableName("table_name");

        Assertions.assertFalse(DataCompareUtils.isRecordsEquals(beforeImage, afterImage));

        List<Row> rows2 = new ArrayList<>();
        Row row2 = new Row();
        Field field11 = addField(row2,"pk", 1, "12345");
        Field field12 = addField(row2,"age", 1, "18");
        rows2.add(row2);
        afterImage.setRows(rows2);
        Assertions.assertTrue(DataCompareUtils.isRecordsEquals(beforeImage, afterImage));

        field11.setValue("23456");
        Assertions.assertFalse(DataCompareUtils.isRecordsEquals(beforeImage, afterImage));
        field11.setValue("12345");

        field12.setName("sex");
        Assertions.assertFalse(DataCompareUtils.isRecordsEquals(beforeImage, afterImage));
        field12.setName("age");

        field12.setValue("19");
        Assertions.assertFalse(DataCompareUtils.isRecordsEquals(beforeImage, afterImage));
        field12.setName("18");

        Field field3 = new Field("pk", 1, "12346");
        Row row3 = new Row();
        row3.add(field3);
        rows2.add(row3);
        Assertions.assertFalse(DataCompareUtils.isRecordsEquals(beforeImage, afterImage));
        

        beforeImage.setRows(new ArrayList<>());
        afterImage.setRows(new ArrayList<>());
        Assertions.assertTrue(DataCompareUtils.isRecordsEquals(beforeImage, afterImage));
    }
    
    private Field addField(Row row, String name, int type, Object value){
        Field field = new Field(name, type, value);
        row.add(field);
        return field;
    }

    @Test
    public void isRowsEquals() {
        TableMeta tableMeta = Mockito.mock(TableMeta.class);
        Mockito.when(tableMeta.getPkName()).thenReturn("pk");
        Mockito.when(tableMeta.getTableName()).thenReturn("table_name");

        List<Row> rows = new ArrayList<>();
        Field field = new Field("pk", 1, "12345");
        Row row = new Row();
        row.add(field);
        rows.add(row);

        Assertions.assertFalse(DataCompareUtils.isRowsEquals(tableMeta, rows, null));
        Assertions.assertFalse(DataCompareUtils.isRowsEquals(tableMeta, null, rows));

        List<Row> rows2 = new ArrayList<>();
        Field field2 = new Field("pk", 1, "12345");
        Row row2 = new Row();
        row2.add(field2);
        rows2.add(row2);
        Assertions.assertTrue(DataCompareUtils.isRowsEquals(tableMeta, rows, rows2));

        field.setValue("23456");
        Assertions.assertFalse(DataCompareUtils.isRowsEquals(tableMeta, rows, rows2));
        field.setValue("12345");

        Field field3 = new Field("pk", 1, "12346");
        Row row3 = new Row();
        row3.add(field3);
        rows2.add(row3);
        Assertions.assertFalse(DataCompareUtils.isRowsEquals(tableMeta, rows, rows2));
    }
}