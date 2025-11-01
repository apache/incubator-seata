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

public class SqlServerUndoUpdateExecutorTest extends BaseExecutorTest {
    private static SqlServerUndoUpdateExecutor executor;
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
        sqlUndoLog.setSqlType(SQLType.UPDATE);
        sqlUndoLog.setTableMeta(tableMeta);
        sqlUndoLog.setTableName("table_name");
        sqlUndoLog.setBeforeImage(beforeImage);
        sqlUndoLog.setAfterImage(afterImage);

        executor = new SqlServerUndoUpdateExecutor(sqlUndoLog);
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
        undoLog.setSqlType(SQLType.UPDATE);
        undoLog.setTableName("test_table");
        undoLog.setTableMeta(tableMeta);

        // Create before image
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
        String sql = executor.buildUndoSQL().toLowerCase();
        Assertions.assertNotNull(sql);
        Assertions.assertTrue(sql.contains("update"));
        Assertions.assertTrue(sql.contains("id"));
        Assertions.assertTrue(sql.contains("age"));
    }

    @Test
    public void getUndoRows() {
        Assertions.assertEquals(executor.getUndoRows(), executor.getSqlUndoLog().getBeforeImage());
    }

    @Test
    public void testBuildUndoSQL() {
        SqlServerUndoUpdateExecutor updateExecutor = new SqlServerUndoUpdateExecutor(sqlUndoLog);
        String undoSQL = updateExecutor.buildUndoSQL();

        // Verify SQL structure
        Assertions.assertNotNull(undoSQL);
        Assertions.assertTrue(undoSQL.contains("UPDATE"));
        Assertions.assertTrue(undoSQL.contains("SET"));
        Assertions.assertTrue(undoSQL.contains("WHERE"));

        // Verify SET clause contains non-primary key fields
        Assertions.assertTrue(undoSQL.contains("name") && undoSQL.contains("= ?"));
        Assertions.assertTrue(undoSQL.contains("age") && undoSQL.contains("= ?"));

        // Verify WHERE clause uses primary key
        Assertions.assertTrue(undoSQL.contains("id") && undoSQL.contains("= ?"));

        // Ensure primary key is NOT in SET clause
        String setClause = undoSQL.substring(undoSQL.indexOf("SET") + 3, undoSQL.indexOf("WHERE"))
                .trim();
        Assertions.assertFalse(setClause.matches(".*\\bid\\s*=.*"));
    }

    @Test
    public void testBuildUndoSQLWithSqlServerSyntax() {
        SqlServerUndoUpdateExecutor updateExecutor = new SqlServerUndoUpdateExecutor(sqlUndoLog);
        String undoSQL = updateExecutor.buildUndoSQL();

        // SQL Server uses square brackets for column escaping (optional)
        // Verify basic SQL structure is correct
        Assertions.assertTrue(undoSQL.contains("test_table"));
        Assertions.assertTrue(undoSQL.contains("UPDATE"));
    }

    @Test
    public void testGetUndoRows() {
        SqlServerUndoUpdateExecutor updateExecutor = new SqlServerUndoUpdateExecutor(sqlUndoLog);
        TableRecords undoRows = updateExecutor.getUndoRows();

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
        SQLUndoLog emptyUndoLog = new SQLUndoLog();
        emptyUndoLog.setSqlType(SQLType.UPDATE);
        emptyUndoLog.setTableName("test_table");

        TableRecords emptyBeforeImage = new TableRecords();
        emptyBeforeImage.setTableName("test_table");
        emptyBeforeImage.setRows(new ArrayList<>());
        emptyUndoLog.setBeforeImage(emptyBeforeImage);

        SqlServerUndoUpdateExecutor emptyExecutor = new SqlServerUndoUpdateExecutor(emptyUndoLog);

        Assertions.assertThrows(ShouldNeverHappenException.class, () -> {
            emptyExecutor.buildUndoSQL();
        });
    }

    @Test
    public void testBuildUndoSQLWithNullBeforeImage() {
        SQLUndoLog nullUndoLog = new SQLUndoLog();
        nullUndoLog.setSqlType(SQLType.UPDATE);
        nullUndoLog.setTableName("test_table");
        nullUndoLog.setBeforeImage(null);

        SqlServerUndoUpdateExecutor nullExecutor = new SqlServerUndoUpdateExecutor(nullUndoLog);

        Assertions.assertThrows(NullPointerException.class, () -> {
            nullExecutor.buildUndoSQL();
        });
    }

    @Test
    public void testBuildUndoSQLWithCompoundPrimaryKey() {
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

        ColumnMeta valueColumn = new ColumnMeta();
        valueColumn.setTableName("compound_table");
        valueColumn.setColumnName("value");
        valueColumn.setDataType(Types.VARCHAR);
        allColumns.put("value", valueColumn);

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
        compoundUndoLog.setSqlType(SQLType.UPDATE);
        compoundUndoLog.setTableName("compound_table");
        compoundUndoLog.setTableMeta(compoundMeta);

        TableRecords beforeImage = new TableRecords();
        beforeImage.setTableName("compound_table");
        beforeImage.setTableMeta(compoundMeta);

        List<Field> fields = Arrays.asList(
                new Field("id1", Types.INTEGER, 1),
                new Field("id2", Types.INTEGER, 2),
                new Field("value", Types.VARCHAR, "test"));

        // Set primary key types
        fields.get(0).setKeyType(KeyType.PRIMARY_KEY);
        fields.get(1).setKeyType(KeyType.PRIMARY_KEY);

        Row resultRow = new Row();
        resultRow.setFields(fields);

        beforeImage.setRows(Arrays.asList(resultRow));
        compoundUndoLog.setBeforeImage(beforeImage);

        SqlServerUndoUpdateExecutor compoundExecutor = new SqlServerUndoUpdateExecutor(compoundUndoLog);
        String undoSQL = compoundExecutor.buildUndoSQL();

        // Verify compound primary key in WHERE clause
        Assertions.assertTrue(undoSQL.contains("id1") && undoSQL.contains("= ?"));
        Assertions.assertTrue(undoSQL.contains("id2") && undoSQL.contains("= ?"));
        Assertions.assertTrue(undoSQL.contains(" and "));

        // Verify only non-primary key in SET clause
        Assertions.assertTrue(undoSQL.contains("value") && undoSQL.contains("= ?"));

        // Extract SET clause to check primary keys are not in it
        String setClause = undoSQL.substring(undoSQL.indexOf("SET") + 3, undoSQL.indexOf("WHERE"))
                .trim();
        Assertions.assertFalse(setClause.matches(".*\\bid1\\s*=.*"));
        Assertions.assertFalse(setClause.matches(".*\\bid2\\s*=.*"));
    }

    @Test
    public void testConstructor() {
        SqlServerUndoUpdateExecutor updateExecutor = new SqlServerUndoUpdateExecutor(sqlUndoLog);
        Assertions.assertNotNull(updateExecutor);
    }

    @Test
    public void testConstructorWithNull() {
        SqlServerUndoUpdateExecutor updateExecutor = new SqlServerUndoUpdateExecutor(null);
        Assertions.assertNotNull(updateExecutor);
    }

    @Test
    public void testInheritance() {
        SqlServerUndoUpdateExecutor updateExecutor = new SqlServerUndoUpdateExecutor(sqlUndoLog);

        // Verify inheritance hierarchy
        Assertions.assertTrue(updateExecutor instanceof BaseSqlServerUndoExecutor);
        Assertions.assertTrue(updateExecutor instanceof AbstractUndoExecutor);
    }

    @Test
    public void testSqlServerSpecificSql() {
        SqlServerUndoUpdateExecutor updateExecutor = new SqlServerUndoUpdateExecutor(sqlUndoLog);
        String undoSQL = updateExecutor.buildUndoSQL();

        // Verify SQL Server compatible syntax
        Assertions.assertNotNull(undoSQL);
        Assertions.assertTrue(undoSQL.toUpperCase().startsWith("UPDATE"));

        // Should not contain database-specific syntax that would fail on SQL Server
        Assertions.assertFalse(undoSQL.contains("LIMIT"));
        Assertions.assertFalse(undoSQL.contains("ROWNUM"));
    }
}
