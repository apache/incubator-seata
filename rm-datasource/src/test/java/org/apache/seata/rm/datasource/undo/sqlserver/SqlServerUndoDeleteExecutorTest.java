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
package org.apache.seata.rm.datasource.undo.sqlserver;

import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.rm.datasource.sql.struct.Field;
import org.apache.seata.rm.datasource.sql.struct.KeyType;
import org.apache.seata.rm.datasource.sql.struct.Row;
import org.apache.seata.rm.datasource.sql.struct.TableRecords;
import org.apache.seata.rm.datasource.undo.AbstractUndoExecutor;
import org.apache.seata.rm.datasource.undo.BaseExecutorTest;
import org.apache.seata.rm.datasource.undo.SQLUndoLog;
import org.apache.seata.sqlparser.SQLType;
import org.apache.seata.sqlparser.struct.ColumnMeta;
import org.apache.seata.sqlparser.struct.IndexMeta;
import org.apache.seata.sqlparser.struct.IndexType;
import org.apache.seata.sqlparser.struct.TableMeta;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SqlServerUndoDeleteExecutorTest extends BaseExecutorTest {
    private static SqlServerUndoDeleteExecutor executor;
    private SQLUndoLog sqlUndoLog;
    private TableMeta tableMeta;

    @BeforeAll
    public static void init() {
        TableMeta tableMeta = Mockito.mock(TableMeta.class);
        Mockito.when(tableMeta.getPrimaryKeyOnlyName()).thenReturn(Arrays.asList(new String[] {"id"}));
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
        List<Row> afterRows = new ArrayList<>();
        afterImage.setRows(afterRows);

        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setSqlType(SQLType.UPDATE);
        sqlUndoLog.setTableMeta(tableMeta);
        sqlUndoLog.setTableName("table_name");
        sqlUndoLog.setBeforeImage(beforeImage);
        sqlUndoLog.setAfterImage(afterImage);

        executor = new SqlServerUndoDeleteExecutor(sqlUndoLog);
    }

    @BeforeEach
    public void setUp() {
        tableMeta = createTableMeta();
        sqlUndoLog = createSQLUndoLog();
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

    private SQLUndoLog createSQLUndoLog() {
        SQLUndoLog undoLog = new SQLUndoLog();
        undoLog.setSqlType(SQLType.DELETE);
        undoLog.setTableName("test_table");
        undoLog.setTableMeta(tableMeta);

        // Create before image (for DELETE undo, we use before image to restore data)
        TableRecords beforeImage = new TableRecords();
        beforeImage.setTableName("test_table");
        beforeImage.setTableMeta(tableMeta);

        List<Field> fields = Arrays.asList(
                new Field("id", Types.INTEGER, 1),
                new Field("name", Types.VARCHAR, "John"),
                new Field("age", Types.INTEGER, 30));

        // Set primary key for id field
        fields.get(0).setKeyType(KeyType.PRIMARY_KEY);

        Row row = new Row();
        row.setFields(fields);

        beforeImage.setRows(Arrays.asList(row));
        undoLog.setBeforeImage(beforeImage);

        return undoLog;
    }

    @Test
    public void buildUndoSQL() {
        String sql = executor.buildUndoSQL().toUpperCase();
        Assertions.assertNotNull(sql);
        Assertions.assertTrue(sql.contains("INSERT"));
        Assertions.assertTrue(sql.contains("TABLE_NAME"));
        Assertions.assertTrue(sql.contains("ID"));
    }

    @Test
    public void getUndoRows() {
        Assertions.assertEquals(executor.getUndoRows(), executor.getSqlUndoLog().getBeforeImage());
    }

    @Test
    public void testBuildUndoSQL() {
        SqlServerUndoDeleteExecutor deleteExecutor = new SqlServerUndoDeleteExecutor(sqlUndoLog);
        String undoSQL = deleteExecutor.buildUndoSQL();

        // Verify SQL structure
        Assertions.assertNotNull(undoSQL);
        Assertions.assertTrue(undoSQL.contains("INSERT INTO"));
        Assertions.assertTrue(undoSQL.contains("VALUES"));

        // Verify table name
        Assertions.assertTrue(undoSQL.contains("test_table"));

        // Verify all fields are included (both PK and non-PK)
        Assertions.assertTrue(undoSQL.contains("id"));
        Assertions.assertTrue(undoSQL.contains("name"));
        Assertions.assertTrue(undoSQL.contains("age"));
    }

    @Test
    public void testGetUndoRows() {
        SqlServerUndoDeleteExecutor deleteExecutor = new SqlServerUndoDeleteExecutor(sqlUndoLog);
        TableRecords undoRows = deleteExecutor.getUndoRows();

        Assertions.assertNotNull(undoRows);
        Assertions.assertEquals(sqlUndoLog.getBeforeImage(), undoRows);
        Assertions.assertEquals("test_table", undoRows.getTableName());
        Assertions.assertEquals(1, undoRows.getRows().size());

        Row row = undoRows.getRows().get(0);
        Assertions.assertEquals(3, row.getFields().size());

        Field idField = row.getFields().stream()
                .filter(f -> "id".equals(f.getName()))
                .findFirst()
                .orElse(null);
        Assertions.assertNotNull(idField);
        Assertions.assertEquals(1, idField.getValue());
        Assertions.assertEquals(KeyType.PRIMARY_KEY, idField.getKeyType());

        Field nameField = row.getFields().stream()
                .filter(f -> "name".equals(f.getName()))
                .findFirst()
                .orElse(null);
        Assertions.assertNotNull(nameField);
        Assertions.assertEquals("John", nameField.getValue());
    }

    @Test
    public void testBuildUndoSQLWithMultipleFields() {
        SqlServerUndoDeleteExecutor deleteExecutor = new SqlServerUndoDeleteExecutor(sqlUndoLog);
        String undoSQL = deleteExecutor.buildUndoSQL();

        // Verify INSERT statement includes all fields
        Assertions.assertTrue(undoSQL.contains("INSERT INTO"));
        Assertions.assertTrue(undoSQL.contains("test_table"));

        // Should include all fields in column list
        String columnsPart = undoSQL.substring(undoSQL.indexOf("("), undoSQL.indexOf(")") + 1);
        Assertions.assertTrue(columnsPart.contains("id"));
        Assertions.assertTrue(columnsPart.contains("name"));
        Assertions.assertTrue(columnsPart.contains("age"));

        // Should have VALUES clause with proper number of placeholders
        Assertions.assertTrue(undoSQL.contains("VALUES"));
        long questionMarkCount = undoSQL.chars().filter(ch -> ch == '?').count();
        Assertions.assertEquals(3, questionMarkCount); // 3 fields
    }

    @Test
    public void testBuildUndoSQLWithCompoundPrimaryKey() {
        // Create table with compound primary key
        TableMeta compoundMeta = new TableMeta();
        compoundMeta.setTableName("compound_table");

        Map<String, ColumnMeta> allColumns = new HashMap<>();

        ColumnMeta id1Column = new ColumnMeta();
        id1Column.setTableName("compound_table");
        id1Column.setColumnName("id1");
        id1Column.setDataType(Types.INTEGER);
        allColumns.put("id1", id1Column);

        ColumnMeta id2Column = new ColumnMeta();
        id2Column.setTableName("compound_table");
        id2Column.setColumnName("id2");
        id2Column.setDataType(Types.INTEGER);
        allColumns.put("id2", id2Column);

        ColumnMeta nameColumn = new ColumnMeta();
        nameColumn.setTableName("compound_table");
        nameColumn.setColumnName("name");
        nameColumn.setDataType(Types.VARCHAR);
        allColumns.put("name", nameColumn);

        compoundMeta.getAllColumns().putAll(allColumns);

        // Create compound primary key
        Map<String, IndexMeta> allIndexes = new HashMap<>();
        IndexMeta primaryIndex = new IndexMeta();
        primaryIndex.setIndexName("PRIMARY");
        primaryIndex.setNonUnique(false);
        primaryIndex.setIndextype(IndexType.PRIMARY);

        List<ColumnMeta> primaryColumns = new ArrayList<>();
        primaryColumns.add(id1Column);
        primaryColumns.add(id2Column);
        primaryIndex.setValues(primaryColumns);

        allIndexes.put("PRIMARY", primaryIndex);
        compoundMeta.getAllIndexes().putAll(allIndexes);

        // Create SQL undo log with compound key
        SQLUndoLog compoundUndoLog = new SQLUndoLog();
        compoundUndoLog.setSqlType(SQLType.DELETE);
        compoundUndoLog.setTableName("compound_table");
        compoundUndoLog.setTableMeta(compoundMeta);

        TableRecords beforeImage = new TableRecords();
        beforeImage.setTableName("compound_table");
        beforeImage.setTableMeta(compoundMeta);

        List<Field> fields = Arrays.asList(
                new Field("id1", Types.INTEGER, 1),
                new Field("id2", Types.INTEGER, 2),
                new Field("name", Types.VARCHAR, "test"));

        // Set primary key types
        fields.get(0).setKeyType(KeyType.PRIMARY_KEY);
        fields.get(1).setKeyType(KeyType.PRIMARY_KEY);

        Row resultRow = new Row();
        resultRow.setFields(fields);

        beforeImage.setRows(Arrays.asList(resultRow));
        compoundUndoLog.setBeforeImage(beforeImage);

        SqlServerUndoDeleteExecutor compoundExecutor = new SqlServerUndoDeleteExecutor(compoundUndoLog);
        String undoSQL = compoundExecutor.buildUndoSQL();

        // Verify INSERT statement includes all fields including compound keys
        Assertions.assertTrue(undoSQL.contains("INSERT INTO"));
        Assertions.assertTrue(undoSQL.contains("compound_table"));
        Assertions.assertTrue(undoSQL.contains("id1"));
        Assertions.assertTrue(undoSQL.contains("id2"));
        Assertions.assertTrue(undoSQL.contains("name"));
        Assertions.assertTrue(undoSQL.contains("VALUES"));

        long questionMarkCount = undoSQL.chars().filter(ch -> ch == '?').count();
        Assertions.assertEquals(3, questionMarkCount); // 3 fields
    }

    @Test
    public void testBuildUndoSQLWithEmptyBeforeImage() {
        SQLUndoLog emptyUndoLog = new SQLUndoLog();
        emptyUndoLog.setSqlType(SQLType.DELETE);
        emptyUndoLog.setTableName("test_table");

        TableRecords emptyBeforeImage = new TableRecords();
        emptyBeforeImage.setTableName("test_table");
        emptyBeforeImage.setRows(new ArrayList<>());
        emptyUndoLog.setBeforeImage(emptyBeforeImage);

        SqlServerUndoDeleteExecutor emptyExecutor = new SqlServerUndoDeleteExecutor(emptyUndoLog);

        Assertions.assertThrows(ShouldNeverHappenException.class, () -> {
            emptyExecutor.buildUndoSQL();
        });
    }

    @Test
    public void testBuildUndoSQLWithNullBeforeImage() {
        SQLUndoLog nullUndoLog = new SQLUndoLog();
        nullUndoLog.setSqlType(SQLType.DELETE);
        nullUndoLog.setTableName("test_table");
        nullUndoLog.setBeforeImage(null);

        SqlServerUndoDeleteExecutor nullExecutor = new SqlServerUndoDeleteExecutor(nullUndoLog);

        Assertions.assertThrows(NullPointerException.class, () -> {
            nullExecutor.buildUndoSQL();
        });
    }

    @Test
    public void testConstructor() {
        SqlServerUndoDeleteExecutor deleteExecutor = new SqlServerUndoDeleteExecutor(sqlUndoLog);
        Assertions.assertNotNull(deleteExecutor);
    }

    @Test
    public void testConstructorWithNull() {
        SqlServerUndoDeleteExecutor deleteExecutor = new SqlServerUndoDeleteExecutor(null);
        Assertions.assertNotNull(deleteExecutor);
    }

    @Test
    public void testInheritance() {
        SqlServerUndoDeleteExecutor deleteExecutor = new SqlServerUndoDeleteExecutor(sqlUndoLog);

        // Verify inheritance hierarchy
        Assertions.assertTrue(deleteExecutor instanceof BaseSqlServerUndoExecutor);
        Assertions.assertTrue(deleteExecutor instanceof AbstractUndoExecutor);
    }

    @Test
    public void testSqlServerSpecificSql() {
        SqlServerUndoDeleteExecutor deleteExecutor = new SqlServerUndoDeleteExecutor(sqlUndoLog);
        String undoSQL = deleteExecutor.buildUndoSQL();

        // Verify SQL Server compatible syntax
        Assertions.assertNotNull(undoSQL);
        Assertions.assertTrue(undoSQL.toUpperCase().startsWith("INSERT"));

        // Should not contain database-specific syntax that would fail on SQL Server
        Assertions.assertFalse(undoSQL.contains("RETURNING"));
        Assertions.assertFalse(undoSQL.contains("ON DUPLICATE KEY"));
    }

    @Test
    public void testBuildUndoSQLWithMultipleRows() {
        // Create undo log with multiple rows
        TableRecords multiRowBeforeImage = new TableRecords();
        multiRowBeforeImage.setTableName("test_table");
        multiRowBeforeImage.setTableMeta(tableMeta);

        List<Row> rows = new ArrayList<>();

        // First row
        List<Field> fields1 = Arrays.asList(
                new Field("id", Types.INTEGER, 1),
                new Field("name", Types.VARCHAR, "John"),
                new Field("age", Types.INTEGER, 30));
        fields1.get(0).setKeyType(KeyType.PRIMARY_KEY);
        Row row1 = new Row();
        row1.setFields(fields1);
        rows.add(row1);

        // Second row
        List<Field> fields2 = Arrays.asList(
                new Field("id", Types.INTEGER, 2),
                new Field("name", Types.VARCHAR, "Jane"),
                new Field("age", Types.INTEGER, 25));
        fields2.get(0).setKeyType(KeyType.PRIMARY_KEY);
        Row row2 = new Row();
        row2.setFields(fields2);
        rows.add(row2);

        multiRowBeforeImage.setRows(rows);

        SQLUndoLog multiRowUndoLog = new SQLUndoLog();
        multiRowUndoLog.setSqlType(SQLType.DELETE);
        multiRowUndoLog.setTableName("test_table");
        multiRowUndoLog.setTableMeta(tableMeta);
        multiRowUndoLog.setBeforeImage(multiRowBeforeImage);

        SqlServerUndoDeleteExecutor multiRowExecutor = new SqlServerUndoDeleteExecutor(multiRowUndoLog);
        String undoSQL = multiRowExecutor.buildUndoSQL();

        // Verify SQL structure for multiple rows
        Assertions.assertNotNull(undoSQL);
        Assertions.assertTrue(undoSQL.contains("INSERT INTO"));

        // For multiple rows, there should be multiple INSERT statements or VALUES clauses
        // The exact format depends on the implementation
        Assertions.assertTrue(undoSQL.contains("test_table"));

        long questionMarkCount = undoSQL.chars().filter(ch -> ch == '?').count();
        Assertions.assertEquals(3, questionMarkCount);
    }
}
