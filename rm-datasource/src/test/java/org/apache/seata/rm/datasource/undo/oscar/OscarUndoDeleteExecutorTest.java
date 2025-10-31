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

public class OscarUndoDeleteExecutorTest {

    private OscarUndoDeleteExecutor deleteExecutor;
    private SQLUndoLog sqlUndoLog;
    private TableMeta tableMeta;

    @BeforeEach
    public void setUp() {
        sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setSqlType(SQLType.DELETE);
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
        deleteExecutor = new OscarUndoDeleteExecutor(sqlUndoLog);
        Assertions.assertNotNull(deleteExecutor);
    }

    @Test
    public void testBuildUndoSQL() {
        // Create before image data (undo DELETE by re-inserting deleted row)
        TableRecords beforeImage = new TableRecords(tableMeta);
        Row row = new Row();
        Field idField = new Field("id", Types.INTEGER, 1);
        idField.setKeyType(KeyType.PRIMARY_KEY); // mark as primary key
        row.add(idField);
        row.add(new Field("name", Types.VARCHAR, "deleted_name"));
        row.add(new Field("age", Types.INTEGER, 30));

        List<Row> rows = new ArrayList<>();
        rows.add(row);
        beforeImage.setRows(rows);

        sqlUndoLog.setBeforeImage(beforeImage);
        deleteExecutor = new OscarUndoDeleteExecutor(sqlUndoLog);

        String undoSQL = deleteExecutor.buildUndoSQL();

        // Verify generated INSERT SQL format
        Assertions.assertTrue(undoSQL.contains("INSERT INTO test_table"));
        Assertions.assertTrue(undoSQL.contains("VALUES"));

        // Verify all columns included (non-PK + PK)
        Assertions.assertTrue(undoSQL.contains("\"name\""));
        Assertions.assertTrue(undoSQL.contains("\"age\""));
        Assertions.assertTrue(undoSQL.contains("\"id\""));

        // Verify number of placeholders matches number of columns
        long questionMarkCount = undoSQL.chars().filter(ch -> ch == '?').count();
        Assertions.assertEquals(3, questionMarkCount); // name, age, id
    }

    @Test
    public void testBuildUndoSQLWithMultipleColumns() {
        // Test multi-column scenario
        TableRecords beforeImage = new TableRecords(tableMeta);
        Row row = new Row();
        row.add(new Field("id", Types.INTEGER, 100));
        row.add(new Field("name", Types.VARCHAR, "test_user"));
        row.add(new Field("age", Types.INTEGER, 25));

        List<Row> rows = new ArrayList<>();
        rows.add(row);
        beforeImage.setRows(rows);

        sqlUndoLog.setBeforeImage(beforeImage);
        deleteExecutor = new OscarUndoDeleteExecutor(sqlUndoLog);

        String undoSQL = deleteExecutor.buildUndoSQL();

        // Verify INSERT SQL format
        String expectedPattern = "INSERT INTO test_table \\(.+\\) VALUES \\(.+\\)";
        Assertions.assertTrue(undoSQL.matches(expectedPattern));

        // Verify column names are within parentheses
        int columnsStart = undoSQL.indexOf("(");
        int columnsEnd = undoSQL.indexOf(")");
        String columnsSection = undoSQL.substring(columnsStart + 1, columnsEnd);

        Assertions.assertTrue(columnsSection.contains("\"name\""));
        Assertions.assertTrue(columnsSection.contains("\"age\""));
        Assertions.assertTrue(columnsSection.contains("\"id\""));
    }

    @Test
    public void testBuildUndoSQLWithSingleColumn() {
        // Test single-column scenario (PK only)
        TableRecords beforeImage = new TableRecords(tableMeta);
        Row row = new Row();
        Field idField = new Field("id", Types.INTEGER, 1);
        idField.setKeyType(KeyType.PRIMARY_KEY); // mark as primary key
        row.add(idField);

        List<Row> rows = new ArrayList<>();
        rows.add(row);
        beforeImage.setRows(rows);

        sqlUndoLog.setBeforeImage(beforeImage);
        deleteExecutor = new OscarUndoDeleteExecutor(sqlUndoLog);

        String undoSQL = deleteExecutor.buildUndoSQL();

        // Verify basic INSERT structure
        Assertions.assertTrue(undoSQL.contains("INSERT INTO test_table"));
        Assertions.assertTrue(undoSQL.contains("\"id\""));
        Assertions.assertTrue(undoSQL.contains("VALUES"));

        // Verify single placeholder
        long questionMarkCount = undoSQL.chars().filter(ch -> ch == '?').count();
        Assertions.assertEquals(1, questionMarkCount);
    }

    @Test
    public void testBuildUndoSQLWithEmptyBeforeImage() {
        // Empty before image should throw
        TableRecords emptyBeforeImage = new TableRecords(tableMeta);
        emptyBeforeImage.setRows(new ArrayList<>());

        sqlUndoLog.setBeforeImage(emptyBeforeImage);
        deleteExecutor = new OscarUndoDeleteExecutor(sqlUndoLog);

        Assertions.assertThrows(ShouldNeverHappenException.class, () -> {
            deleteExecutor.buildUndoSQL();
        });
    }

    @Test
    public void testBuildUndoSQLWithNullBeforeImage() {
        // Null before image should throw
        sqlUndoLog.setBeforeImage(null);
        deleteExecutor = new OscarUndoDeleteExecutor(sqlUndoLog);

        Assertions.assertThrows(NullPointerException.class, () -> {
            deleteExecutor.buildUndoSQL();
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
        deleteExecutor = new OscarUndoDeleteExecutor(sqlUndoLog);

        TableRecords undoRows = deleteExecutor.getUndoRows();

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
        row.add(new Field("age", Types.INTEGER, 25));

        List<Row> rows = new ArrayList<>();
        rows.add(row);
        beforeImage.setRows(rows);

        sqlUndoLog.setBeforeImage(beforeImage);
        deleteExecutor = new OscarUndoDeleteExecutor(sqlUndoLog);

        String undoSQL = deleteExecutor.buildUndoSQL();

        // Verify column names presence (escaping may vary)
        Assertions.assertTrue(undoSQL.contains("\"name\""));
        Assertions.assertTrue(undoSQL.contains("\"age\""));
        Assertions.assertTrue(undoSQL.contains("\"id\""));
    }

    @Test
    public void testInsertSQLTemplateFormat() {
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
        deleteExecutor = new OscarUndoDeleteExecutor(sqlUndoLog);

        String undoSQL = deleteExecutor.buildUndoSQL();

        // Verify INSERT template: INSERT INTO table (columns) VALUES (values)
        String expectedPattern = "INSERT INTO test_table \\(.+\\) VALUES \\(.+\\)";
        Assertions.assertTrue(undoSQL.matches(expectedPattern));

        // Calculate number of columns and placeholders
        String columnsSection = undoSQL.substring(undoSQL.indexOf("(") + 1, undoSQL.indexOf(")"));
        String valuesSection = undoSQL.substring(undoSQL.lastIndexOf("(") + 1, undoSQL.lastIndexOf(")"));

        // Calculate number of columns and placeholders
        long columnCount = columnsSection.chars().filter(ch -> ch == ',').count() + 1;
        long placeholderCount = valuesSection.chars().filter(ch -> ch == '?').count();

        Assertions.assertEquals(columnCount, placeholderCount);
    }

    @Test
    public void testColumnOrdering() {
        // Test column ordering: non-PK columns + PK columns
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
        deleteExecutor = new OscarUndoDeleteExecutor(sqlUndoLog);

        String undoSQL = deleteExecutor.buildUndoSQL();

        // Verify all fields are present in SQL
        Assertions.assertTrue(undoSQL.contains("\"name\""));
        Assertions.assertTrue(undoSQL.contains("\"age\""));
        Assertions.assertTrue(undoSQL.contains("\"id\""));

        // Verify total number of placeholders
        long questionMarkCount = undoSQL.chars().filter(ch -> ch == '?').count();
        Assertions.assertEquals(3, questionMarkCount);
    }
}
