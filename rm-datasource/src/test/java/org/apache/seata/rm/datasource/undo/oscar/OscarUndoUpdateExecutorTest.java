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
import org.apache.seata.rm.datasource.sql.struct.Field;
import org.apache.seata.rm.datasource.sql.struct.KeyType;
import org.apache.seata.rm.datasource.sql.struct.Row;
import org.apache.seata.rm.datasource.sql.struct.TableRecords;
import org.apache.seata.rm.datasource.undo.SQLUndoLog;
import org.apache.seata.sqlparser.SQLType;
import org.apache.seata.sqlparser.struct.ColumnMeta;
import org.apache.seata.sqlparser.struct.IndexMeta;
import org.apache.seata.sqlparser.struct.IndexType;
import org.apache.seata.sqlparser.struct.TableMeta;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OscarUndoUpdateExecutorTest {

    private OscarUndoUpdateExecutor updateExecutor;
    private SQLUndoLog sqlUndoLog;
    private TableMeta tableMeta;

    @BeforeEach
    public void setUp() {
        sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setSqlType(SQLType.UPDATE);
        sqlUndoLog.setTableName("test_table");

        tableMeta = createTableMeta();
        sqlUndoLog.setTableMeta(tableMeta);
    }

    private TableMeta createTableMeta() {
        TableMeta meta = new TableMeta();
        meta.setTableName("test_table");

        // Create column metadata
        Map<String, ColumnMeta> allColumns = new HashMap<>();

        ColumnMeta idColumn = new ColumnMeta();
        idColumn.setTableName("test_table");
        idColumn.setColumnName("id");
        idColumn.setDataType(Types.INTEGER);
        idColumn.setColumnSize(11);
        allColumns.put("id", idColumn);

        ColumnMeta nameColumn = new ColumnMeta();
        nameColumn.setTableName("test_table");
        nameColumn.setColumnName("name");
        nameColumn.setDataType(Types.VARCHAR);
        nameColumn.setColumnSize(255);
        allColumns.put("name", nameColumn);

        ColumnMeta ageColumn = new ColumnMeta();
        ageColumn.setTableName("test_table");
        ageColumn.setColumnName("age");
        ageColumn.setDataType(Types.INTEGER);
        ageColumn.setColumnSize(11);
        allColumns.put("age", ageColumn);

        meta.getAllColumns().putAll(allColumns);

        // Create primary key index
        Map<String, IndexMeta> allIndexes = new HashMap<>();
        IndexMeta primaryIndex = new IndexMeta();
        primaryIndex.setIndexName("PRIMARY");
        primaryIndex.setNonUnique(false);
        primaryIndex.setIndextype(IndexType.PRIMARY);

        List<ColumnMeta> primaryColumns = new ArrayList<>();
        primaryColumns.add(idColumn);
        primaryIndex.setValues(primaryColumns);

        allIndexes.put("PRIMARY", primaryIndex);
        meta.getAllIndexes().putAll(allIndexes);

        return meta;
    }

    @Test
    public void testConstructor() {
        updateExecutor = new OscarUndoUpdateExecutor(sqlUndoLog);
        Assertions.assertNotNull(updateExecutor);
    }

    @Test
    public void testBuildUndoSQL() {
        // Create before image data
        TableRecords beforeImage = new TableRecords(tableMeta);
        Row row = new Row();
        Field idField = new Field("id", Types.INTEGER, 1);
        idField.setKeyType(KeyType.PRIMARY_KEY); // mark as primary key
        row.add(idField);
        row.add(new Field("name", Types.VARCHAR, "old_name"));
        row.add(new Field("age", Types.INTEGER, 25));

        List<Row> rows = new ArrayList<>();
        rows.add(row);
        beforeImage.setRows(rows);

        sqlUndoLog.setBeforeImage(beforeImage);
        updateExecutor = new OscarUndoUpdateExecutor(sqlUndoLog);

        String undoSQL = updateExecutor.buildUndoSQL();

        // Verify generated SQL format
        Assertions.assertTrue(undoSQL.contains("UPDATE test_table"));
        Assertions.assertTrue(undoSQL.contains("SET"));
        Assertions.assertTrue(undoSQL.contains("WHERE"));

        // Verify updates include non-PK columns (escaping may vary)
        Assertions.assertTrue(undoSQL.contains("name") && undoSQL.contains("= ?"));
        Assertions.assertTrue(undoSQL.contains("age") && undoSQL.contains("= ?"));

        // Verify WHERE contains PK condition
        Assertions.assertTrue(undoSQL.contains("id") && undoSQL.contains("= ?"));

        // Verify SET clause does not include PK column (regex without quoting)
        Assertions.assertFalse(undoSQL.matches(".*SET.*\\bid\\s*=.*"));
    }

    @Test
    public void testBuildUndoSQLWithSingleNonPkColumn() {
        // Create scenario with only one non-PK column
        TableRecords beforeImage = new TableRecords(tableMeta);
        Row row = new Row();
        Field idField = new Field("id", Types.INTEGER, 1);
        idField.setKeyType(KeyType.PRIMARY_KEY); // mark as primary key
        row.add(idField);
        row.add(new Field("name", Types.VARCHAR, "test_name"));

        List<Row> rows = new ArrayList<>();
        rows.add(row);
        beforeImage.setRows(rows);

        sqlUndoLog.setBeforeImage(beforeImage);
        updateExecutor = new OscarUndoUpdateExecutor(sqlUndoLog);

        String undoSQL = updateExecutor.buildUndoSQL();

        // Verify SQL structure
        Assertions.assertTrue(undoSQL.contains("UPDATE test_table"));
        Assertions.assertTrue(undoSQL.contains("SET") && undoSQL.contains("name") && undoSQL.contains("= ?"));
        Assertions.assertTrue(undoSQL.contains("WHERE") && undoSQL.contains("id") && undoSQL.contains("= ?"));
    }

    @Test
    public void testBuildUndoSQLWithMultipleNonPkColumns() {
        // Create scenario with multiple non-PK columns
        TableRecords beforeImage = new TableRecords(tableMeta);
        Row row = new Row();
        Field idField = new Field("id", Types.INTEGER, 1);
        idField.setKeyType(KeyType.PRIMARY_KEY); // mark as primary key
        row.add(idField);
        row.add(new Field("name", Types.VARCHAR, "test_name"));
        row.add(new Field("age", Types.INTEGER, 30));

        List<Row> rows = new ArrayList<>();
        rows.add(row);
        beforeImage.setRows(rows);

        sqlUndoLog.setBeforeImage(beforeImage);
        updateExecutor = new OscarUndoUpdateExecutor(sqlUndoLog);

        String undoSQL = updateExecutor.buildUndoSQL();

        // Verify all non-PK columns included
        Assertions.assertTrue(undoSQL.contains("name") && undoSQL.contains("= ?"));
        Assertions.assertTrue(undoSQL.contains("age") && undoSQL.contains("= ?"));

        // Verify comma separation in SET clause
        Assertions.assertTrue(undoSQL.contains("name") && undoSQL.contains("age") && undoSQL.contains(","));
    }

    @Test
    public void testBuildUndoSQLWithEmptyBeforeImage() {
        // Empty before image should throw
        TableRecords emptyBeforeImage = new TableRecords(tableMeta);
        emptyBeforeImage.setRows(new ArrayList<>());

        sqlUndoLog.setBeforeImage(emptyBeforeImage);
        updateExecutor = new OscarUndoUpdateExecutor(sqlUndoLog);

        Assertions.assertThrows(ShouldNeverHappenException.class, () -> {
            updateExecutor.buildUndoSQL();
        });
    }

    @Test
    public void testBuildUndoSQLWithNullBeforeImage() {
        // Null before image should throw
        sqlUndoLog.setBeforeImage(null);
        updateExecutor = new OscarUndoUpdateExecutor(sqlUndoLog);

        Assertions.assertThrows(NullPointerException.class, () -> {
            updateExecutor.buildUndoSQL();
        });
    }

    @Test
    public void testGetUndoRows() {
        // Create before image data
        TableRecords beforeImage = new TableRecords(tableMeta);
        Row row = new Row();
        Field idField = new Field("id", Types.INTEGER, 1);
        idField.setKeyType(KeyType.PRIMARY_KEY); // mark as primary key
        row.add(idField);
        row.add(new Field("name", Types.VARCHAR, "test_name"));

        List<Row> rows = new ArrayList<>();
        rows.add(row);
        beforeImage.setRows(rows);

        sqlUndoLog.setBeforeImage(beforeImage);
        updateExecutor = new OscarUndoUpdateExecutor(sqlUndoLog);

        TableRecords undoRows = updateExecutor.getUndoRows();

        // Verify it returns before image
        Assertions.assertSame(beforeImage, undoRows);
        Assertions.assertEquals(1, undoRows.getRows().size());
        // Find field values via field list
        Row resultRow = undoRows.getRows().get(0);
        Field idFields = resultRow.getFields().stream()
                .filter(f -> "id".equals(f.getName()))
                .findFirst()
                .orElse(null);
        Field nameField = resultRow.getFields().stream()
                .filter(f -> "name".equals(f.getName()))
                .findFirst()
                .orElse(null);
        Assertions.assertNotNull(idFields);
        Assertions.assertNotNull(nameField);
        Assertions.assertEquals(1, idFields.getValue());
        Assertions.assertEquals("test_name", nameField.getValue());
    }

    @Test
    public void testOscarSpecificEscaping() {
        // Test Oscar-specific column escaping
        TableRecords beforeImage = new TableRecords(tableMeta);
        Row row = new Row();
        Field idField = new Field("id", Types.INTEGER, 1);
        idField.setKeyType(KeyType.PRIMARY_KEY); // mark as primary key
        row.add(idField);
        row.add(new Field("name", Types.VARCHAR, "test"));

        List<Row> rows = new ArrayList<>();
        rows.add(row);
        beforeImage.setRows(rows);

        sqlUndoLog.setBeforeImage(beforeImage);
        updateExecutor = new OscarUndoUpdateExecutor(sqlUndoLog);

        String undoSQL = updateExecutor.buildUndoSQL();

        // Verify column names presence (escaping may vary)
        Assertions.assertTrue(undoSQL.contains("name"));
        Assertions.assertTrue(undoSQL.contains("id"));
    }

    @Test
    public void testSQLTemplateFormat() {
        TableRecords beforeImage = new TableRecords(tableMeta);
        Row row = new Row();
        Field idField = new Field("id", Types.INTEGER, 1);
        idField.setKeyType(KeyType.PRIMARY_KEY); // mark as primary key
        row.add(idField);
        row.add(new Field("name", Types.VARCHAR, "test"));
        row.add(new Field("age", Types.INTEGER, 25));

        List<Row> rows = new ArrayList<>();
        rows.add(row);
        beforeImage.setRows(rows);

        sqlUndoLog.setBeforeImage(beforeImage);
        updateExecutor = new OscarUndoUpdateExecutor(sqlUndoLog);

        String undoSQL = updateExecutor.buildUndoSQL();

        // Verify SQL template: UPDATE table SET columns WHERE conditions
        String expectedPattern = "UPDATE test_table SET .+ WHERE .+";
        Assertions.assertTrue(undoSQL.matches(expectedPattern));

        // Verify number of placeholders
        long questionMarkCount = undoSQL.chars().filter(ch -> ch == '?').count();
        Assertions.assertTrue(questionMarkCount >= 3); // at least placeholders for name, age, id
    }
}
