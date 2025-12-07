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

import com.alibaba.druid.mock.MockPreparedStatement;
import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.rm.datasource.mock.MockConnection;
import org.apache.seata.rm.datasource.mock.MockDriver;
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

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SqlServerUndoInsertExecutorTest extends BaseExecutorTest {
    private static SqlServerUndoInsertExecutor executor;
    private SQLUndoLog sqlUndoLog;
    private TableMeta tableMeta;

    @BeforeAll
    public static void init() {
        TableMeta tableMeta = Mockito.mock(TableMeta.class);
        Mockito.when(tableMeta.getPrimaryKeyOnlyName()).thenReturn(Collections.singletonList("id"));
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
        Row row2 = new Row();
        addField(row2, "id", 1, "12345");
        addField(row2, "age", 1, "2");
        afterRows.add(row2);
        Row row3 = new Row();
        addField(row3, "id", 1, "12346");
        addField(row3, "age", 1, "2");
        afterRows.add(row3);
        afterImage.setRows(afterRows);

        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setSqlType(SQLType.INSERT);
        sqlUndoLog.setTableMeta(tableMeta);
        sqlUndoLog.setTableName("table_name");
        sqlUndoLog.setBeforeImage(beforeImage);
        sqlUndoLog.setAfterImage(afterImage);

        executor = new SqlServerUndoInsertExecutor(sqlUndoLog);
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

        List<Field> fields = Arrays.asList(new Field("id", Types.INTEGER, 1), new Field("name", Types.VARCHAR, "John"));

        // Set primary key for id field
        fields.get(0).setKeyType(KeyType.PRIMARY_KEY);

        Row row = new Row();
        row.setFields(fields);

        afterImage.setRows(Arrays.asList(row));
        undoLog.setAfterImage(afterImage);

        return undoLog;
    }

    @Test
    public void buildUndoSQL() {
        String sql = executor.buildUndoSQL().toUpperCase();
        Assertions.assertNotNull(sql);
        Assertions.assertTrue(sql.contains("DELETE"));
        Assertions.assertTrue(sql.contains("TABLE_NAME"));
        Assertions.assertTrue(sql.contains("ID"));
    }

    @Test
    public void getUndoRows() {
        Assertions.assertEquals(executor.getUndoRows(), executor.getSqlUndoLog().getAfterImage());
    }

    @Test
    public void undoPrepareTest() throws SQLException {
        String sql = executor.buildUndoSQL().toUpperCase();
        MockConnection connection = new MockConnection(new MockDriver(), "", null);
        MockPreparedStatement undoPST = (MockPreparedStatement) connection.prepareStatement(sql);

        List<Field> fieldList = new ArrayList<>();
        fieldList.add(new Field("id", 1, "12345"));
        executor.undoPrepare(undoPST, new ArrayList<>(), fieldList);
        Assertions.assertEquals(1, undoPST.getParameters().size());
    }

    @Test
    public void testBuildUndoSQL() {
        SqlServerUndoInsertExecutor insertExecutor = new SqlServerUndoInsertExecutor(sqlUndoLog);
        String undoSQL = insertExecutor.buildUndoSQL();

        // Verify SQL structure
        Assertions.assertNotNull(undoSQL);
        Assertions.assertTrue(undoSQL.contains("DELETE FROM"));
        Assertions.assertTrue(undoSQL.contains("WHERE"));

        // Verify table name
        Assertions.assertTrue(undoSQL.contains("test_table"));

        // Verify WHERE clause uses primary key
        Assertions.assertTrue(undoSQL.contains("id") && undoSQL.contains("= ?"));
    }

    @Test
    public void testGetUndoRows() {
        SqlServerUndoInsertExecutor insertExecutor = new SqlServerUndoInsertExecutor(sqlUndoLog);
        TableRecords undoRows = insertExecutor.getUndoRows();

        Assertions.assertNotNull(undoRows);
        Assertions.assertEquals(sqlUndoLog.getAfterImage(), undoRows);
        Assertions.assertEquals("test_table", undoRows.getTableName());
        Assertions.assertEquals(1, undoRows.getRows().size());

        Row row = undoRows.getRows().get(0);
        Assertions.assertEquals(2, row.getFields().size());

        Field idField = row.getFields().stream()
                .filter(f -> "id".equals(f.getName()))
                .findFirst()
                .orElse(null);
        Assertions.assertNotNull(idField);
        Assertions.assertEquals(1, idField.getValue());
        Assertions.assertEquals(KeyType.PRIMARY_KEY, idField.getKeyType());
    }

    @Test
    public void testBuildUndoSQLWithMultiplePrimaryKeys() {
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
        compoundUndoLog.setSqlType(SQLType.INSERT);
        compoundUndoLog.setTableName("compound_table");
        compoundUndoLog.setTableMeta(compoundMeta);

        TableRecords afterImage = new TableRecords();
        afterImage.setTableName("compound_table");
        afterImage.setTableMeta(compoundMeta);

        List<Field> fields = Arrays.asList(
                new Field("id1", Types.INTEGER, 1),
                new Field("id2", Types.INTEGER, 2),
                new Field("name", Types.VARCHAR, "test"));

        // Set primary key types
        fields.get(0).setKeyType(KeyType.PRIMARY_KEY);
        fields.get(1).setKeyType(KeyType.PRIMARY_KEY);

        Row resultRow = new Row();
        resultRow.setFields(fields);

        afterImage.setRows(Arrays.asList(resultRow));
        compoundUndoLog.setAfterImage(afterImage);

        SqlServerUndoInsertExecutor compoundExecutor = new SqlServerUndoInsertExecutor(compoundUndoLog);
        String undoSQL = compoundExecutor.buildUndoSQL();

        // Verify compound primary key in WHERE clause
        Assertions.assertTrue(undoSQL.contains("DELETE FROM"));
        Assertions.assertTrue(undoSQL.contains("id1") && undoSQL.contains("= ?"));
        Assertions.assertTrue(undoSQL.contains("id2") && undoSQL.contains("= ?"));
        Assertions.assertTrue(undoSQL.contains(" and "));
    }

    @Test
    public void testBuildUndoSQLWithEmptyAfterImage() {
        SQLUndoLog emptyUndoLog = new SQLUndoLog();
        emptyUndoLog.setSqlType(SQLType.INSERT);
        emptyUndoLog.setTableName("test_table");

        TableRecords emptyAfterImage = new TableRecords();
        emptyAfterImage.setTableName("test_table");
        emptyAfterImage.setRows(new ArrayList<>());
        emptyUndoLog.setAfterImage(emptyAfterImage);

        SqlServerUndoInsertExecutor emptyExecutor = new SqlServerUndoInsertExecutor(emptyUndoLog);

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

        SqlServerUndoInsertExecutor nullExecutor = new SqlServerUndoInsertExecutor(nullUndoLog);

        Assertions.assertThrows(NullPointerException.class, () -> {
            nullExecutor.buildUndoSQL();
        });
    }

    @Test
    public void testConstructor() {
        SqlServerUndoInsertExecutor insertExecutor = new SqlServerUndoInsertExecutor(sqlUndoLog);
        Assertions.assertNotNull(insertExecutor);
    }

    @Test
    public void testConstructorWithNull() {
        SqlServerUndoInsertExecutor insertExecutor = new SqlServerUndoInsertExecutor(null);
        Assertions.assertNotNull(insertExecutor);
    }

    @Test
    public void testInheritance() {
        SqlServerUndoInsertExecutor insertExecutor = new SqlServerUndoInsertExecutor(sqlUndoLog);

        // Verify inheritance hierarchy
        Assertions.assertTrue(insertExecutor instanceof BaseSqlServerUndoExecutor);
        Assertions.assertTrue(insertExecutor instanceof AbstractUndoExecutor);
    }

    @Test
    public void testSqlServerSpecificSql() {
        SqlServerUndoInsertExecutor insertExecutor = new SqlServerUndoInsertExecutor(sqlUndoLog);
        String undoSQL = insertExecutor.buildUndoSQL();

        // Verify SQL Server compatible syntax
        Assertions.assertNotNull(undoSQL);
        Assertions.assertTrue(undoSQL.toUpperCase().startsWith("DELETE"));

        // Should not contain database-specific syntax that would fail on SQL Server
        Assertions.assertFalse(undoSQL.contains("LIMIT"));
        Assertions.assertFalse(undoSQL.contains("ROWNUM"));
    }

    @Test
    public void testUndoPrepareWithMultiplePrimaryKeys() throws SQLException {
        // Create table with compound primary key
        TableMeta compoundMeta = new TableMeta();
        compoundMeta.setTableName("compound_table");

        Map<String, ColumnMeta> allColumns = new HashMap<>();

        ColumnMeta id1Column = new ColumnMeta();
        id1Column.setColumnName("id1");
        id1Column.setDataType(Types.INTEGER);
        allColumns.put("id1", id1Column);

        ColumnMeta id2Column = new ColumnMeta();
        id2Column.setColumnName("id2");
        id2Column.setDataType(Types.INTEGER);
        allColumns.put("id2", id2Column);

        compoundMeta.getAllColumns().putAll(allColumns);

        // Create compound primary key index
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

        SQLUndoLog compoundUndoLog = new SQLUndoLog();
        compoundUndoLog.setSqlType(SQLType.INSERT);
        compoundUndoLog.setTableName("compound_table");
        compoundUndoLog.setTableMeta(compoundMeta);

        TableRecords afterImage = new TableRecords();
        afterImage.setTableName("compound_table");
        afterImage.setTableMeta(compoundMeta);

        List<Field> fields = Arrays.asList(new Field("id1", Types.INTEGER, 1), new Field("id2", Types.INTEGER, 2));

        // Set primary key types
        fields.get(0).setKeyType(KeyType.PRIMARY_KEY);
        fields.get(1).setKeyType(KeyType.PRIMARY_KEY);

        Row resultRow = new Row();
        resultRow.setFields(fields);

        afterImage.setRows(Arrays.asList(resultRow));
        compoundUndoLog.setAfterImage(afterImage);

        SqlServerUndoInsertExecutor compoundExecutor = new SqlServerUndoInsertExecutor(compoundUndoLog);
        String sql = compoundExecutor.buildUndoSQL();

        MockConnection connection = new MockConnection(new MockDriver(), "", null);
        MockPreparedStatement undoPST = (MockPreparedStatement) connection.prepareStatement(sql);

        List<Field> pkFields = Arrays.asList(new Field("id1", Types.INTEGER, 1), new Field("id2", Types.INTEGER, 2));
        pkFields.get(0).setKeyType(KeyType.PRIMARY_KEY);
        pkFields.get(1).setKeyType(KeyType.PRIMARY_KEY);

        compoundExecutor.undoPrepare(undoPST, new ArrayList<>(), pkFields);

        // Should have 2 parameters for compound primary key
        Assertions.assertEquals(2, undoPST.getParameters().size());
    }
}
