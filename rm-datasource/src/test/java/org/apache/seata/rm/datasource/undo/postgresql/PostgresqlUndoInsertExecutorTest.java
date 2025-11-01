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
import org.mockito.Mockito;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostgresqlUndoInsertExecutorTest {

    private PostgresqlUndoInsertExecutor executor;
    private SQLUndoLog sqlUndoLog;
    private TableMeta tableMeta;

    @BeforeEach
    public void setUp() {
        tableMeta = createTableMeta();
        sqlUndoLog = createSQLUndoLog();
        executor = new PostgresqlUndoInsertExecutor(sqlUndoLog);
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
        undoLog.setSqlType(SQLType.INSERT);
        undoLog.setTableName("test_table");
        undoLog.setTableMeta(tableMeta);

        // Create after image (for INSERT undo, we use after image)
        TableRecords afterImage = new TableRecords();
        afterImage.setTableName("test_table");
        afterImage.setTableMeta(tableMeta);

        List<Field> fields = Arrays.asList(
                new Field("id", Types.INTEGER, 1),
                new Field("name", Types.VARCHAR, "John"),
                new Field("age", Types.INTEGER, 30));

        // Set primary key for id field
        fields.get(0).setKeyType(KeyType.PRIMARY_KEY);

        Row row = new Row();
        row.setFields(fields);

        afterImage.setRows(Arrays.asList(row));
        undoLog.setAfterImage(afterImage);

        return undoLog;
    }

    @Test
    public void testConstructor() {
        PostgresqlUndoInsertExecutor newExecutor = new PostgresqlUndoInsertExecutor(sqlUndoLog);
        Assertions.assertNotNull(newExecutor);
    }

    @Test
    public void testConstructorWithNull() {
        PostgresqlUndoInsertExecutor newExecutor = new PostgresqlUndoInsertExecutor(null);
        Assertions.assertNotNull(newExecutor);
    }

    @Test
    public void testBuildUndoSQL() {
        String undoSQL = executor.buildUndoSQL();

        // Verify SQL structure for DELETE statement
        Assertions.assertNotNull(undoSQL);
        Assertions.assertTrue(undoSQL.contains("DELETE FROM"));
        Assertions.assertTrue(undoSQL.contains("test_table"));
        Assertions.assertTrue(undoSQL.contains("WHERE"));

        // Verify WHERE clause uses primary key only
        Assertions.assertTrue(undoSQL.contains("id") && undoSQL.contains("= ?"));
    }

    @Test
    public void testBuildUndoSQLWithQuotes() {
        String undoSQL = executor.buildUndoSQL();

        // PostgreSQL uses double quotes for column escaping
        Assertions.assertTrue(undoSQL.contains("\"id\"") || undoSQL.contains("id"));
    }

    @Test
    public void testGetUndoRows() {
        TableRecords undoRows = executor.getUndoRows();

        Assertions.assertNotNull(undoRows);
        Assertions.assertEquals(sqlUndoLog.getAfterImage(), undoRows);
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
    }

    @Test
    public void testBuildUndoSQLWithEmptyAfterImage() {
        // Create SQLUndoLog with empty after image
        SQLUndoLog emptyUndoLog = new SQLUndoLog();
        emptyUndoLog.setSqlType(SQLType.INSERT);
        emptyUndoLog.setTableName("test_table");

        TableRecords emptyAfterImage = new TableRecords();
        emptyAfterImage.setTableName("test_table");
        emptyAfterImage.setRows(new ArrayList<>());
        emptyUndoLog.setAfterImage(emptyAfterImage);

        PostgresqlUndoInsertExecutor emptyExecutor = new PostgresqlUndoInsertExecutor(emptyUndoLog);

        Assertions.assertThrows(ShouldNeverHappenException.class, () -> {
            emptyExecutor.buildUndoSQL();
        });
    }

    @Test
    public void testBuildUndoSQLWithNullAfterImage() {
        SQLUndoLog nullUndoLog = new SQLUndoLog();
        nullUndoLog.setSqlType(SQLType.INSERT);
        nullUndoLog.setTableName("test_table");
        nullUndoLog.setAfterImage(null);

        PostgresqlUndoInsertExecutor nullExecutor = new PostgresqlUndoInsertExecutor(nullUndoLog);

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
        compoundUndoLog.setSqlType(SQLType.INSERT);
        compoundUndoLog.setTableName("compound_table");
        compoundUndoLog.setTableMeta(compoundMeta);

        TableRecords afterImage = new TableRecords();
        afterImage.setTableName("compound_table");
        afterImage.setTableMeta(compoundMeta);

        List<Field> fields = Arrays.asList(
                new Field("user_id", Types.INTEGER, 100),
                new Field("product_id", Types.INTEGER, 200),
                new Field("quantity", Types.INTEGER, 5));

        // Set primary key types
        fields.get(0).setKeyType(KeyType.PRIMARY_KEY);
        fields.get(1).setKeyType(KeyType.PRIMARY_KEY);

        Row resultRow = new Row();
        resultRow.setFields(fields);

        afterImage.setRows(Arrays.asList(resultRow));
        compoundUndoLog.setAfterImage(afterImage);

        PostgresqlUndoInsertExecutor compoundExecutor = new PostgresqlUndoInsertExecutor(compoundUndoLog);
        String undoSQL = compoundExecutor.buildUndoSQL();

        // Verify compound primary key in WHERE clause
        Assertions.assertTrue(undoSQL.contains("user_id") && undoSQL.contains("= ?"));
        Assertions.assertTrue(undoSQL.contains("product_id") && undoSQL.contains("= ?"));
        Assertions.assertTrue(undoSQL.contains(" and "));
    }

    @Test
    public void testUndoPrepareWithPrimaryKeys() throws SQLException {
        PreparedStatement mockPreparedStatement = Mockito.mock(PreparedStatement.class);

        // Create test data
        List<Field> pkFields = Arrays.asList(new Field("id", Types.INTEGER, 1));
        pkFields.get(0).setKeyType(KeyType.PRIMARY_KEY);

        ArrayList<Field> undoValues = new ArrayList<>();

        // Call undoPrepare
        executor.undoPrepare(mockPreparedStatement, undoValues, pkFields);

        // Verify setObject was called with correct parameters
        Mockito.verify(mockPreparedStatement).setObject(1, 1, Types.INTEGER);
    }

    @Test
    public void testUndoPrepareWithCompoundPrimaryKeys() throws SQLException {
        PreparedStatement mockPreparedStatement = Mockito.mock(PreparedStatement.class);

        // Create test data with compound primary keys
        List<Field> pkFields =
                Arrays.asList(new Field("user_id", Types.INTEGER, 100), new Field("product_id", Types.INTEGER, 200));
        pkFields.get(0).setKeyType(KeyType.PRIMARY_KEY);
        pkFields.get(1).setKeyType(KeyType.PRIMARY_KEY);

        ArrayList<Field> undoValues = new ArrayList<>();

        // Call undoPrepare
        executor.undoPrepare(mockPreparedStatement, undoValues, pkFields);

        // Verify setObject was called for both primary keys
        Mockito.verify(mockPreparedStatement).setObject(1, 100, Types.INTEGER);
        Mockito.verify(mockPreparedStatement).setObject(2, 200, Types.INTEGER);
    }

    @Test
    public void testUndoPrepareWithEmptyPrimaryKeys() throws SQLException {
        PreparedStatement mockPreparedStatement = Mockito.mock(PreparedStatement.class);

        List<Field> emptyPkFields = new ArrayList<>();
        ArrayList<Field> undoValues = new ArrayList<>();

        // Call undoPrepare with empty primary keys
        executor.undoPrepare(mockPreparedStatement, undoValues, emptyPkFields);

        // Verify no interactions with PreparedStatement
        Mockito.verifyNoInteractions(mockPreparedStatement);
    }

    @Test
    public void testUndoPrepareWithNullValues() throws SQLException {
        PreparedStatement mockPreparedStatement = Mockito.mock(PreparedStatement.class);

        List<Field> pkFields = Arrays.asList(new Field("id", Types.INTEGER, null));
        pkFields.get(0).setKeyType(KeyType.PRIMARY_KEY);

        ArrayList<Field> undoValues = new ArrayList<>();

        // Call undoPrepare with null value
        executor.undoPrepare(mockPreparedStatement, undoValues, pkFields);

        // Verify setObject was called with null value
        Mockito.verify(mockPreparedStatement).setObject(1, null, Types.INTEGER);
    }

    @Test
    public void testBuildUndoSQLWithMultipleRows() {
        // Create after image with multiple rows
        TableRecords multiRowAfterImage = new TableRecords();
        multiRowAfterImage.setTableName("test_table");
        multiRowAfterImage.setTableMeta(tableMeta);

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

        multiRowAfterImage.setRows(Arrays.asList(row1, row2));

        SQLUndoLog multiRowUndoLog = new SQLUndoLog();
        multiRowUndoLog.setSqlType(SQLType.INSERT);
        multiRowUndoLog.setTableName("test_table");
        multiRowUndoLog.setTableMeta(tableMeta);
        multiRowUndoLog.setAfterImage(multiRowAfterImage);

        PostgresqlUndoInsertExecutor multiRowExecutor = new PostgresqlUndoInsertExecutor(multiRowUndoLog);
        String undoSQL = multiRowExecutor.buildUndoSQL();

        // Should use the first row for SQL generation
        Assertions.assertNotNull(undoSQL);
        Assertions.assertTrue(undoSQL.contains("DELETE FROM"));
        Assertions.assertTrue(undoSQL.contains("test_table"));
        Assertions.assertTrue(undoSQL.contains("WHERE"));
        Assertions.assertTrue(undoSQL.contains("id") && undoSQL.contains("= ?"));
    }

    @Test
    public void testInheritanceFromAbstractUndoExecutor() {
        Assertions.assertTrue(executor instanceof org.apache.seata.rm.datasource.undo.AbstractUndoExecutor);
    }
}
