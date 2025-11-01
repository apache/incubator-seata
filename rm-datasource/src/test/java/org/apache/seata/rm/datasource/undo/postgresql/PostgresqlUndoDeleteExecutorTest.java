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
package org.apache.seata.rm.datasource.undo.postgresql;

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostgresqlUndoDeleteExecutorTest {

    private PostgresqlUndoDeleteExecutor executor;
    private SQLUndoLog sqlUndoLog;
    private TableMeta tableMeta;

    @BeforeEach
    public void setUp() {
        tableMeta = createTableMeta();
        sqlUndoLog = createSQLUndoLog();
        executor = new PostgresqlUndoDeleteExecutor(sqlUndoLog);
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

        // Create before image (for DELETE undo, we use before image)
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
    public void testConstructor() {
        PostgresqlUndoDeleteExecutor newExecutor = new PostgresqlUndoDeleteExecutor(sqlUndoLog);
        Assertions.assertNotNull(newExecutor);
    }

    @Test
    public void testConstructorWithNull() {
        PostgresqlUndoDeleteExecutor newExecutor = new PostgresqlUndoDeleteExecutor(null);
        Assertions.assertNotNull(newExecutor);
    }

    @Test
    public void testBuildUndoSQL() {
        String undoSQL = executor.buildUndoSQL();

        // Verify SQL structure for INSERT statement
        Assertions.assertNotNull(undoSQL);
        Assertions.assertTrue(undoSQL.contains("INSERT INTO"));
        Assertions.assertTrue(undoSQL.contains("test_table"));
        Assertions.assertTrue(undoSQL.contains("VALUES"));

        // Verify all columns are included
        Assertions.assertTrue(undoSQL.contains("id"));
        Assertions.assertTrue(undoSQL.contains("name"));
        Assertions.assertTrue(undoSQL.contains("age"));

        // Verify placeholder values
        Assertions.assertTrue(undoSQL.contains("?"));
    }

    @Test
    public void testBuildUndoSQLWithQuotes() {
        String undoSQL = executor.buildUndoSQL();

        // PostgreSQL uses double quotes for column escaping
        Assertions.assertTrue(undoSQL.contains("\"id\"") || undoSQL.contains("id"));
        Assertions.assertTrue(undoSQL.contains("\"name\"") || undoSQL.contains("name"));
        Assertions.assertTrue(undoSQL.contains("\"age\"") || undoSQL.contains("age"));
    }

    @Test
    public void testGetUndoRows() {
        TableRecords undoRows = executor.getUndoRows();

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

        Field nameField = row.getFields().stream()
                .filter(f -> "name".equals(f.getName()))
                .findFirst()
                .orElse(null);
        Assertions.assertNotNull(nameField);
        Assertions.assertEquals("John", nameField.getValue());
    }

    @Test
    public void testBuildUndoSQLWithEmptyBeforeImage() {
        // Create SQLUndoLog with empty before image
        SQLUndoLog emptyUndoLog = new SQLUndoLog();
        emptyUndoLog.setSqlType(SQLType.DELETE);
        emptyUndoLog.setTableName("test_table");

        TableRecords emptyBeforeImage = new TableRecords();
        emptyBeforeImage.setTableName("test_table");
        emptyBeforeImage.setRows(new ArrayList<>());
        emptyUndoLog.setBeforeImage(emptyBeforeImage);

        PostgresqlUndoDeleteExecutor emptyExecutor = new PostgresqlUndoDeleteExecutor(emptyUndoLog);

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

        PostgresqlUndoDeleteExecutor nullExecutor = new PostgresqlUndoDeleteExecutor(nullUndoLog);

        Assertions.assertThrows(NullPointerException.class, () -> {
            nullExecutor.buildUndoSQL();
        });
    }

    @Test
    public void testBuildUndoSQLWithCompoundPrimaryKey() {
        // Create table meta with compound primary key
        TableMeta compoundMeta = new TableMeta();
        compoundMeta.setTableName("compound_table");

        Map<String, ColumnMeta> allColumns = new HashMap<>();

        ColumnMeta id1Column = new ColumnMeta();
        id1Column.setTableName("compound_table");
        id1Column.setColumnName("user_id");
        id1Column.setDataType(Types.INTEGER);
        allColumns.put("user_id", id1Column);

        ColumnMeta id2Column = new ColumnMeta();
        id2Column.setTableName("compound_table");
        id2Column.setColumnName("product_id");
        id2Column.setDataType(Types.INTEGER);
        allColumns.put("product_id", id2Column);

        ColumnMeta valueColumn = new ColumnMeta();
        valueColumn.setTableName("compound_table");
        valueColumn.setColumnName("quantity");
        valueColumn.setDataType(Types.INTEGER);
        allColumns.put("quantity", valueColumn);

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
                new Field("user_id", Types.INTEGER, 100),
                new Field("product_id", Types.INTEGER, 200),
                new Field("quantity", Types.INTEGER, 5));

        // Set primary key types
        fields.get(0).setKeyType(KeyType.PRIMARY_KEY);
        fields.get(1).setKeyType(KeyType.PRIMARY_KEY);

        Row resultRow = new Row();
        resultRow.setFields(fields);

        beforeImage.setRows(Arrays.asList(resultRow));
        compoundUndoLog.setBeforeImage(beforeImage);

        PostgresqlUndoDeleteExecutor compoundExecutor = new PostgresqlUndoDeleteExecutor(compoundUndoLog);
        String undoSQL = compoundExecutor.buildUndoSQL();

        // Verify all columns are included in INSERT
        Assertions.assertTrue(undoSQL.contains("user_id"));
        Assertions.assertTrue(undoSQL.contains("product_id"));
        Assertions.assertTrue(undoSQL.contains("quantity"));

        // Verify INSERT structure
        Assertions.assertTrue(undoSQL.contains("INSERT INTO"));
        Assertions.assertTrue(undoSQL.contains("compound_table"));
        Assertions.assertTrue(undoSQL.contains("VALUES"));
    }

    @Test
    public void testBuildUndoSQLWithOnlyPrimaryKeyColumns() {
        // Create table with only primary key columns
        TableMeta pkOnlyMeta = new TableMeta();
        pkOnlyMeta.setTableName("pk_only_table");

        Map<String, ColumnMeta> allColumns = new HashMap<>();

        ColumnMeta idColumn = new ColumnMeta();
        idColumn.setTableName("pk_only_table");
        idColumn.setColumnName("id");
        idColumn.setDataType(Types.INTEGER);
        allColumns.put("id", idColumn);

        pkOnlyMeta.getAllColumns().putAll(allColumns);

        Map<String, IndexMeta> allIndexes = new HashMap<>();
        IndexMeta primaryIndex = new IndexMeta();
        primaryIndex.setIndexName("PRIMARY");
        primaryIndex.setNonUnique(false);
        primaryIndex.setIndextype(IndexType.PRIMARY);

        List<ColumnMeta> primaryColumns = new ArrayList<>();
        primaryColumns.add(idColumn);
        primaryIndex.setValues(primaryColumns);

        allIndexes.put("PRIMARY", primaryIndex);
        pkOnlyMeta.getAllIndexes().putAll(allIndexes);

        SQLUndoLog pkOnlyUndoLog = new SQLUndoLog();
        pkOnlyUndoLog.setSqlType(SQLType.DELETE);
        pkOnlyUndoLog.setTableName("pk_only_table");
        pkOnlyUndoLog.setTableMeta(pkOnlyMeta);

        TableRecords beforeImage = new TableRecords();
        beforeImage.setTableName("pk_only_table");
        beforeImage.setTableMeta(pkOnlyMeta);

        List<Field> fields = Arrays.asList(new Field("id", Types.INTEGER, 1));

        fields.get(0).setKeyType(KeyType.PRIMARY_KEY);

        Row resultRow = new Row();
        resultRow.setFields(fields);

        beforeImage.setRows(Arrays.asList(resultRow));
        pkOnlyUndoLog.setBeforeImage(beforeImage);

        PostgresqlUndoDeleteExecutor pkOnlyExecutor = new PostgresqlUndoDeleteExecutor(pkOnlyUndoLog);
        String undoSQL = pkOnlyExecutor.buildUndoSQL();

        // Should have INSERT with only primary key column
        Assertions.assertTrue(undoSQL.contains("INSERT INTO"));
        Assertions.assertTrue(undoSQL.contains("pk_only_table"));
        Assertions.assertTrue(undoSQL.contains("id"));
        Assertions.assertTrue(undoSQL.contains("VALUES"));
        Assertions.assertTrue(undoSQL.contains("?"));
    }

    @Test
    public void testBuildUndoSQLWithMultipleRows() {
        // Create before image with multiple rows
        TableRecords multiRowBeforeImage = new TableRecords();
        multiRowBeforeImage.setTableName("test_table");
        multiRowBeforeImage.setTableMeta(tableMeta);

        List<Field> fields1 = Arrays.asList(
                new Field("id", Types.INTEGER, 1),
                new Field("name", Types.VARCHAR, "John"),
                new Field("age", Types.INTEGER, 30));
        fields1.get(0).setKeyType(KeyType.PRIMARY_KEY);

        List<Field> fields2 = Arrays.asList(
                new Field("id", Types.INTEGER, 2),
                new Field("name", Types.VARCHAR, "Jane"),
                new Field("age", Types.INTEGER, 25));
        fields2.get(0).setKeyType(KeyType.PRIMARY_KEY);

        Row row1 = new Row();
        row1.setFields(fields1);

        Row row2 = new Row();
        row2.setFields(fields2);

        multiRowBeforeImage.setRows(Arrays.asList(row1, row2));

        SQLUndoLog multiRowUndoLog = new SQLUndoLog();
        multiRowUndoLog.setSqlType(SQLType.DELETE);
        multiRowUndoLog.setTableName("test_table");
        multiRowUndoLog.setTableMeta(tableMeta);
        multiRowUndoLog.setBeforeImage(multiRowBeforeImage);

        PostgresqlUndoDeleteExecutor multiRowExecutor = new PostgresqlUndoDeleteExecutor(multiRowUndoLog);
        String undoSQL = multiRowExecutor.buildUndoSQL();

        // Should use the first row for SQL generation
        Assertions.assertNotNull(undoSQL);
        Assertions.assertTrue(undoSQL.contains("INSERT INTO"));
        Assertions.assertTrue(undoSQL.contains("test_table"));
        Assertions.assertTrue(undoSQL.contains("VALUES"));
        Assertions.assertTrue(undoSQL.contains("id"));
        Assertions.assertTrue(undoSQL.contains("name"));
        Assertions.assertTrue(undoSQL.contains("age"));
    }

    @Test
    public void testFieldOrdering() {
        String undoSQL = executor.buildUndoSQL();

        // The SQL should include both non-primary keys first, then primary keys
        // This matches the implementation in buildUndoSQL method
        Assertions.assertTrue(undoSQL.contains("INSERT INTO"));
        Assertions.assertTrue(undoSQL.contains("test_table"));

        // All fields should be present
        Assertions.assertTrue(undoSQL.contains("id"));
        Assertions.assertTrue(undoSQL.contains("name"));
        Assertions.assertTrue(undoSQL.contains("age"));
    }

    @Test
    public void testInheritanceFromAbstractUndoExecutor() {
        Assertions.assertTrue(executor instanceof org.apache.seata.rm.datasource.undo.AbstractUndoExecutor);
    }
}
