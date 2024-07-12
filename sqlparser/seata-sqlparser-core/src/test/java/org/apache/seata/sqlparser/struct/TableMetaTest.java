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

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.seata.common.exception.NotSupportYetException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TableMetaTest {
    private TableMeta tableMeta;
    private TableMeta tableMeta2;

    @BeforeEach
    public void setUp() {
        tableMeta = new TableMeta();
        tableMeta.setTableName("tableName");

        ColumnMeta col1 = new ColumnMeta();
        col1.setColumnName("col1");
        col1.setOnUpdate(true);
        tableMeta.getAllColumns().put("col1", col1);

        ColumnMeta col2 = new ColumnMeta();
        col2.setColumnName("col2");
        tableMeta.getAllColumns().put("col2", col2);

        IndexMeta primaryIndexMeta = new IndexMeta();
        primaryIndexMeta.setIndextype(IndexType.PRIMARY);
        primaryIndexMeta.setValues(Arrays.asList(col1, col2));

        tableMeta.getAllIndexes().put("primary", primaryIndexMeta);

        tableMeta2 = new TableMeta();
        tableMeta2.setTableName("tableName");
        tableMeta2.getAllColumns().put("col1", col1);
        tableMeta2.getAllColumns().put("col2", col2);
        tableMeta2.getAllIndexes().put("primary", primaryIndexMeta);
    }

    @Test
    public void testSetGetTableName() {
        String tableName = "tableName";
        assertEquals(tableName, tableMeta.getTableName(), "Table name should match the value set");
    }

    @Test
    public void testSetGetCaseSensitive() {
        tableMeta.setCaseSensitive(true);
        assertTrue(tableMeta.isCaseSensitive(), "Case sensitivity should be true");
        tableMeta.setCaseSensitive(false);
        assertFalse(tableMeta.isCaseSensitive(), "Case sensitivity should be false");
    }

    @Test
    public void testGetColumnMeta() {
        ColumnMeta columnMeta = new ColumnMeta();
        tableMeta.getAllColumns().put("col1", columnMeta);

        assertEquals(columnMeta, tableMeta.getColumnMeta("col1"), "Should return the correct ColumnMeta object");
    }

    @Test
    public void testGetAllColumns() {
        ColumnMeta columnMeta1 = new ColumnMeta();
        ColumnMeta columnMeta2 = new ColumnMeta();
        tableMeta.getAllColumns().put("col1", columnMeta1);
        tableMeta.getAllColumns().put("col2", columnMeta2);

        Map<String, ColumnMeta> allColumns = tableMeta.getAllColumns();

        assertEquals(2, allColumns.size(), "Should return all columns added");
        assertTrue(allColumns.containsKey("col1"), "Should contain column 'col1'");
        assertTrue(allColumns.containsKey("col2"), "Should contain column 'col2'");
    }

    @Test
    public void testGetAllIndexes() {

        Map<String, IndexMeta> allIndexes = tableMeta.getAllIndexes();

        assertEquals(1, allIndexes.size(), "Should return all indexes added");
        assertTrue(allIndexes.containsKey("primary"), "Should contain index 'primary'");

    }

    @Test
    public void testGetPrimaryKeyMap() {
        Map<String, ColumnMeta> pkCol = tableMeta.getPrimaryKeyMap();

        assertEquals(2, pkCol.size());
        assertTrue(pkCol.containsKey("col1"));
        assertTrue(pkCol.containsKey("col2"));
    }

    @Test
    public void testGetPrimaryKeyMapNoPrimaryKey() {
        tableMeta.getAllIndexes().clear();

        NotSupportYetException exception = assertThrows(
                NotSupportYetException.class,
                () -> tableMeta.getPrimaryKeyMap());

        assertEquals(String.format("%s needs to contain the primary key.", tableMeta.getTableName()),
                exception.getMessage());
    }

    @Test
    public void testGetCaseInsensitivePKs() {
        tableMeta.getColumnMeta("col2".trim()).setColumnName("CoL2");
        Set<String> pks = tableMeta.getCaseInsensitivePKs();

        assertEquals(2, pks.size());
        assertTrue(pks.contains("col1"));
        assertTrue(pks.contains("CoL2"));

    }

    @Test
    public void testGetCaseInsensitivePKsNoPrimaryKey() {
        tableMeta.getAllIndexes().clear(); // Remove primary key

        NotSupportYetException exception = assertThrows(
                NotSupportYetException.class,
                () -> tableMeta.getCaseInsensitivePKs());

        assertEquals(String.format("%s needs to contain the primary key.", tableMeta.getTableName()),
                exception.getMessage());
    }

    @Test
    public void testGetPrimaryKeyOnlyName() {
        List<String> pksName = tableMeta.getPrimaryKeyOnlyName();

        assertEquals(2, pksName.size());
        assertTrue(pksName.contains("col1"));
        assertTrue(pksName.contains("col2"));

    }

    @Test
    public void testGetOnUpdateColumnsOnlyName() {
        List<String> onUpdateColumns = tableMeta.getOnUpdateColumnsOnlyName();
        List<String> expected = Arrays.asList("col1");

        assertEquals(expected.size(), onUpdateColumns.size());
        assertTrue(onUpdateColumns.containsAll(expected));
    }

    @Test
    public void testGetOnUpdateColumnsOnlyNameNoUpdates() {
        tableMeta.getAllColumns().values().forEach(col -> col.setOnUpdate(false));

        List<String> onUpdateColumns = tableMeta.getOnUpdateColumnsOnlyName();
        assertTrue(onUpdateColumns.isEmpty());
    }

    @Test
    public void testContainsPKWithNullList() {
        assertFalse(tableMeta.containsPK(null));
    }

    @Test
    public void testContainsPKWithNoPrimaryKey() {
        List<String> cols = Arrays.asList("col3", "col4");
        assertFalse(tableMeta.containsPK(cols));
    }

    @Test
    public void testContainsPKWithExactMatch() {
        List<String> cols = Arrays.asList("col1", "col2");
        assertTrue(tableMeta.containsPK(cols));
    }

    @Test
    public void testContainsPKWithCaseInsensitiveMatch() {
        List<String> cols = Arrays.asList("COL1", "COL2");
        assertTrue(tableMeta.containsPK(cols));
    }

    @Test
    public void testContainsPKWithNoMatch() {
        List<String> cols = Collections.singletonList("other");
        assertFalse(tableMeta.containsPK(cols));
    }

    @Test
    public void testEquals() {
        assertTrue(tableMeta.equals(tableMeta2));

        tableMeta2.setTableName("different_table");
        assertFalse(tableMeta.equals(tableMeta2));
    }

    @Test
    public void testHashCode() {
        assertEquals(tableMeta.hashCode(), tableMeta2.hashCode());

        tableMeta2.setTableName("different_table");
        assertNotEquals(tableMeta.hashCode(), tableMeta2.hashCode());
    }

}
