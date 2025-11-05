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

import org.apache.seata.rm.datasource.sql.struct.Field;
import org.apache.seata.rm.datasource.sql.struct.KeyType;
import org.apache.seata.rm.datasource.sql.struct.Row;
import org.apache.seata.rm.datasource.sql.struct.TableRecords;
import org.apache.seata.rm.datasource.undo.AbstractUndoExecutor;
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

public class BaseSqlServerUndoExecutorTest {

    private TestBaseSqlServerUndoExecutor executor;
    private SQLUndoLog sqlUndoLog;
    private TableMeta tableMeta;

    // Test implementation of the abstract class
    private static class TestBaseSqlServerUndoExecutor extends BaseSqlServerUndoExecutor {
        public TestBaseSqlServerUndoExecutor(SQLUndoLog sqlUndoLog) {
            super(sqlUndoLog);
        }

        @Override
        protected String buildUndoSQL() {
            return "TEST SQL";
        }

        @Override
        protected TableRecords getUndoRows() {
            return sqlUndoLog.getBeforeImage();
        }
    }

    @BeforeEach
    public void setUp() {
        tableMeta = createTableMeta();
        sqlUndoLog = createSQLUndoLog();
        executor = new TestBaseSqlServerUndoExecutor(sqlUndoLog);
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
        undoLog.setSqlType(SQLType.UPDATE);
        undoLog.setTableName("test_table");
        undoLog.setTableMeta(tableMeta);

        // Create before image
        TableRecords beforeImage = new TableRecords();
        beforeImage.setTableName("test_table");
        beforeImage.setTableMeta(tableMeta);

        List<Field> fields = Arrays.asList(new Field("id", Types.INTEGER, 1), new Field("name", Types.VARCHAR, "John"));

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
        TestBaseSqlServerUndoExecutor newExecutor = new TestBaseSqlServerUndoExecutor(sqlUndoLog);
        Assertions.assertNotNull(newExecutor);
    }

    @Test
    public void testConstructorWithNull() {
        TestBaseSqlServerUndoExecutor newExecutor = new TestBaseSqlServerUndoExecutor(null);
        Assertions.assertNotNull(newExecutor);
    }

    @Test
    public void testBuildCheckSql() {
        String tableName = "test_table";
        String whereCondition = "id = ?";

        String checkSql = executor.buildCheckSql(tableName, whereCondition);

        // Verify SQL Server specific check SQL with UPDLOCK hint
        Assertions.assertNotNull(checkSql);
        Assertions.assertTrue(checkSql.contains("SELECT * FROM"));
        Assertions.assertTrue(checkSql.contains(tableName));
        Assertions.assertTrue(checkSql.contains("WITH(UPDLOCK)"));
        Assertions.assertTrue(checkSql.contains("WHERE"));
        Assertions.assertTrue(checkSql.contains(whereCondition));

        // Verify exact format
        String expectedSql = "SELECT * FROM test_table WITH(UPDLOCK) WHERE id = ?";
        Assertions.assertEquals(expectedSql, checkSql);
    }

    @Test
    public void testBuildCheckSqlWithComplexWhereCondition() {
        String tableName = "user_orders";
        String whereCondition = "user_id = ? AND order_date > ?";

        String checkSql = executor.buildCheckSql(tableName, whereCondition);

        // Verify SQL Server specific check SQL
        Assertions.assertNotNull(checkSql);
        Assertions.assertTrue(checkSql.contains("SELECT * FROM"));
        Assertions.assertTrue(checkSql.contains(tableName));
        Assertions.assertTrue(checkSql.contains("WITH(UPDLOCK)"));
        Assertions.assertTrue(checkSql.contains("WHERE"));
        Assertions.assertTrue(checkSql.contains(whereCondition));

        String expectedSql = "SELECT * FROM user_orders WITH(UPDLOCK) WHERE user_id = ? AND order_date > ?";
        Assertions.assertEquals(expectedSql, checkSql);
    }

    @Test
    public void testBuildCheckSqlWithEmptyWhereCondition() {
        String tableName = "test_table";
        String whereCondition = "";

        String checkSql = executor.buildCheckSql(tableName, whereCondition);

        // Should still work with empty where condition
        Assertions.assertNotNull(checkSql);
        Assertions.assertTrue(checkSql.contains("SELECT * FROM"));
        Assertions.assertTrue(checkSql.contains(tableName));
        Assertions.assertTrue(checkSql.contains("WITH(UPDLOCK)"));
        Assertions.assertTrue(checkSql.contains("WHERE"));

        String expectedSql = "SELECT * FROM test_table WITH(UPDLOCK) WHERE ";
        Assertions.assertEquals(expectedSql, checkSql);
    }

    @Test
    public void testBuildCheckSqlWithNullWhereCondition() {
        String tableName = "test_table";
        String whereCondition = null;

        String checkSql = executor.buildCheckSql(tableName, whereCondition);

        // Should handle null where condition
        Assertions.assertNotNull(checkSql);
        Assertions.assertTrue(checkSql.contains("SELECT * FROM"));
        Assertions.assertTrue(checkSql.contains(tableName));
        Assertions.assertTrue(checkSql.contains("WITH(UPDLOCK)"));
        Assertions.assertTrue(checkSql.contains("WHERE"));

        String expectedSql = "SELECT * FROM test_table WITH(UPDLOCK) WHERE null";
        Assertions.assertEquals(expectedSql, checkSql);
    }

    @Test
    public void testBuildCheckSqlWithSpecialTableName() {
        String tableName = "[dbo].[user_table]";
        String whereCondition = "id = ?";

        String checkSql = executor.buildCheckSql(tableName, whereCondition);

        // Should work with SQL Server style table names
        Assertions.assertNotNull(checkSql);
        Assertions.assertTrue(checkSql.contains("SELECT * FROM"));
        Assertions.assertTrue(checkSql.contains(tableName));
        Assertions.assertTrue(checkSql.contains("WITH(UPDLOCK)"));

        String expectedSql = "SELECT * FROM [dbo].[user_table] WITH(UPDLOCK) WHERE id = ?";
        Assertions.assertEquals(expectedSql, checkSql);
    }

    @Test
    public void testInheritanceFromAbstractUndoExecutor() {
        // Verify inheritance chain
        Assertions.assertTrue(executor instanceof AbstractUndoExecutor);
        Assertions.assertTrue(executor instanceof BaseSqlServerUndoExecutor);
    }

    @Test
    public void testAbstractMethods() {
        // Verify that abstract methods are implemented in test class
        String undoSQL = executor.buildUndoSQL();
        Assertions.assertEquals("TEST SQL", undoSQL);

        TableRecords undoRows = executor.getUndoRows();
        Assertions.assertNotNull(undoRows);
        Assertions.assertEquals(sqlUndoLog.getBeforeImage(), undoRows);
    }

    @Test
    public void testSqlServerSpecificFeatures() {
        // Test that the buildCheckSql method provides SQL Server specific functionality
        String checkSql = executor.buildCheckSql("orders", "status = 'PENDING'");

        // SQL Server uses WITH(UPDLOCK) for row-level locking during SELECT
        Assertions.assertTrue(checkSql.contains("WITH(UPDLOCK)"));

        // This is different from other databases that might use different locking mechanisms
        Assertions.assertFalse(checkSql.contains("FOR UPDATE")); // MySQL/PostgreSQL style
        Assertions.assertFalse(checkSql.contains("WITH (ROWLOCK)")); // Alternative SQL Server syntax
    }

    @Test
    public void testBuildCheckSqlFormat() {
        // Test the exact format of the generated SQL
        String tableName = "products";
        String whereCondition = "category_id = ? AND price > ?";

        String checkSql = executor.buildCheckSql(tableName, whereCondition);

        // Verify the exact format matches SQL Server conventions
        String expectedPattern =
                "SELECT \\* FROM " + tableName + " WITH\\(UPDLOCK\\) WHERE " + whereCondition.replace("?", "\\?");
        Assertions.assertTrue(checkSql.matches(expectedPattern));

        // Verify no extra spaces or formatting issues
        Assertions.assertFalse(checkSql.contains("  ")); // No double spaces
        Assertions.assertTrue(checkSql.startsWith("SELECT * FROM"));
        Assertions.assertTrue(checkSql.contains(" WITH(UPDLOCK) WHERE "));
    }

    @Test
    public void testMultipleInstancesIndependence() {
        // Test that multiple instances work independently
        SQLUndoLog anotherUndoLog = new SQLUndoLog();
        anotherUndoLog.setSqlType(SQLType.DELETE);
        anotherUndoLog.setTableName("another_table");

        TestBaseSqlServerUndoExecutor anotherExecutor = new TestBaseSqlServerUndoExecutor(anotherUndoLog);

        // Both executors should work independently
        String checkSql1 = executor.buildCheckSql("table1", "id = 1");
        String checkSql2 = anotherExecutor.buildCheckSql("table2", "id = 2");

        Assertions.assertNotEquals(checkSql1, checkSql2);
        Assertions.assertTrue(checkSql1.contains("table1"));
        Assertions.assertTrue(checkSql2.contains("table2"));
        Assertions.assertTrue(checkSql1.contains("id = 1"));
        Assertions.assertTrue(checkSql2.contains("id = 2"));
    }
}
