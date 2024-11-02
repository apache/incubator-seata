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
package org.apache.seata.sqlparser.struct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ColumnMetaTest {

    @Test
    public void testEqualsAndHashCode() {
        // Create two instances with identical properties
        ColumnMeta column1 = new ColumnMeta();
        ColumnMeta column2 = new ColumnMeta();

        // Test equality for two newly created instances
        assertTrue(column1.equals(column2));
        assertEquals(column1.hashCode(), column2.hashCode());

        // Modify some properties of column1
        column1.setTableName("table1");
        column1.setColumnName("column1");
        column1.setDataType(1);

        // Test inequality after modifying properties
        assertFalse(column1.equals(column2));
        assertNotEquals(column1.hashCode(), column2.hashCode());

        // Create a copy of column1 with the same properties
        ColumnMeta column3 = new ColumnMeta();
        column3.setTableName("table1");
        column3.setColumnName("column1");
        column3.setDataType(1);

        // Test equality with the copy
        assertTrue(column1.equals(column3));
        assertEquals(column1.hashCode(), column3.hashCode());

    }

    @Test
    public void testAutoincrement() {
        ColumnMeta column = new ColumnMeta();

        // Test default value
        assertFalse(column.isAutoincrement());

        // Set autoincrement to YES
        column.setIsAutoincrement("YES");
        assertTrue(column.isAutoincrement());

        // Set autoincrement to NO
        column.setIsAutoincrement("NO");
        assertFalse(column.isAutoincrement());
    }

    @Test
    public void testGetTableCat() {
        ColumnMeta columnMeta = new ColumnMeta();
        columnMeta.setTableCat("tableCat");
        assertEquals(columnMeta.getTableCat(), "tableCat".trim());
    }

    @Test
    public void testSetGetColumnName() {
        ColumnMeta columnMeta = new ColumnMeta();
        columnMeta.setColumnName("columnName");
        assertEquals("columnName".trim(), columnMeta.getColumnName());
    }

    @Test
    public void testSetGetDataType() {
        ColumnMeta columnMeta = new ColumnMeta();
        columnMeta.setDataType(2);
        assertEquals(2, columnMeta.getDataType());
    }

    @Test
    public void testSetGetDataTypeName() {
        ColumnMeta columnMeta = new ColumnMeta();
        columnMeta.setDataTypeName("dataTypeName");
        assertEquals("dataTypeName".trim(), columnMeta.getDataTypeName());
    }

    @Test
    public void testSetGetColumnSize() {
        ColumnMeta columnMeta = new ColumnMeta();
        columnMeta.setColumnSize(2);
        assertEquals(2, columnMeta.getColumnSize());
    }

    @Test
    public void testSetGetDecimalDigits() {
        ColumnMeta columnMeta = new ColumnMeta();
        columnMeta.setDecimalDigits(2);
        assertEquals(2, columnMeta.getDecimalDigits());
    }

    @Test
    public void testSetGetNumPrecRadix() {
        ColumnMeta columnMeta = new ColumnMeta();
        columnMeta.setNumPrecRadix(10);
        assertEquals(10, columnMeta.getNumPrecRadix());
    }

    @Test
    public void testSetGetNullAble() {
        ColumnMeta columnMeta = new ColumnMeta();
        columnMeta.setNullAble(5);
        assertEquals(5, columnMeta.getNullAble());
    }

    @Test
    public void testSetGetRemarks() {
        ColumnMeta columnMeta = new ColumnMeta();
        columnMeta.setRemarks("remarks");
        assertEquals("remarks".trim(), columnMeta.getRemarks());
    }

    @Test
    public void testSetGetColumnDef() {
        ColumnMeta columnMeta = new ColumnMeta();
        columnMeta.setColumnDef("columnDef");
        assertEquals("columnDef", columnMeta.getColumnDef());
    }

    @Test
    public void testSetGetSqlDataType() {
        ColumnMeta columnMeta = new ColumnMeta();
        columnMeta.setSqlDataType(1);
        assertEquals(1, columnMeta.getSqlDataType());
    }

    @Test
    public void testSetGetSqlDatetimeSub() {
        ColumnMeta columnMeta = new ColumnMeta();
        columnMeta.setSqlDatetimeSub(2);
        assertEquals(2, columnMeta.getSqlDatetimeSub());
    }

    @Test
    public void testSetGetCharOctetLength() {
        ColumnMeta columnMeta = new ColumnMeta();
        Object charOctetLength = 255;
        columnMeta.setCharOctetLength(charOctetLength);
        assertEquals(charOctetLength, columnMeta.getCharOctetLength());
    }

    @Test
    public void testSetGetOrdinalPosition() {
        ColumnMeta columnMeta = new ColumnMeta();
        columnMeta.setOrdinalPosition(3);
        assertEquals(3, columnMeta.getOrdinalPosition());
    }

    @Test
    public void testSetGetIsOnUpdate() {
        ColumnMeta columnMeta = new ColumnMeta();
        columnMeta.setOnUpdate(true);
        assertTrue(columnMeta.isOnUpdate());
    }

    @Test
    public void testSetGetIsCaseSensitive() {
        ColumnMeta columnMeta = new ColumnMeta();
        columnMeta.setCaseSensitive(true);
        assertTrue(columnMeta.isCaseSensitive());
    }

}
