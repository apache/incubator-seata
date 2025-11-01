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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostgresqlUndoExecutorHolderTest {

    private PostgresqlUndoExecutorHolder executorHolder;
    private SQLUndoLog sqlUndoLog;
    private TableMeta tableMeta;

    @BeforeEach
    public void setUp() {
        executorHolder = new PostgresqlUndoExecutorHolder();
        tableMeta = createTableMeta();

        sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setTableName("test_table");
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
        Assertions.assertNotNull(executorHolder);
    }

    @Test
    public void testGetInsertExecutor() {
        sqlUndoLog.setSqlType(SQLType.INSERT);

        AbstractUndoExecutor executor = executorHolder.getInsertExecutor(sqlUndoLog);

        // Verify correct executor type returned
        Assertions.assertNotNull(executor);
        Assertions.assertInstanceOf(PostgresqlUndoInsertExecutor.class, executor);

        // Verify a new instance is created each time
        AbstractUndoExecutor anotherExecutor = executorHolder.getInsertExecutor(sqlUndoLog);
        Assertions.assertNotSame(executor, anotherExecutor);
        Assertions.assertInstanceOf(PostgresqlUndoInsertExecutor.class, anotherExecutor);
    }

    @Test
    public void testGetUpdateExecutor() {
        sqlUndoLog.setSqlType(SQLType.UPDATE);

        AbstractUndoExecutor executor = executorHolder.getUpdateExecutor(sqlUndoLog);

        // Verify correct executor type returned
        Assertions.assertNotNull(executor);
        Assertions.assertInstanceOf(PostgresqlUndoUpdateExecutor.class, executor);

        // Verify a new instance is created each time
        AbstractUndoExecutor anotherExecutor = executorHolder.getUpdateExecutor(sqlUndoLog);
        Assertions.assertNotSame(executor, anotherExecutor);
        Assertions.assertInstanceOf(PostgresqlUndoUpdateExecutor.class, anotherExecutor);
    }

    @Test
    public void testGetDeleteExecutor() {
        sqlUndoLog.setSqlType(SQLType.DELETE);

        AbstractUndoExecutor executor = executorHolder.getDeleteExecutor(sqlUndoLog);

        // Verify correct executor type returned
        Assertions.assertNotNull(executor);
        Assertions.assertInstanceOf(PostgresqlUndoDeleteExecutor.class, executor);

        // Verify a new instance is created each time
        AbstractUndoExecutor anotherExecutor = executorHolder.getDeleteExecutor(sqlUndoLog);
        Assertions.assertNotSame(executor, anotherExecutor);
        Assertions.assertInstanceOf(PostgresqlUndoDeleteExecutor.class, anotherExecutor);
    }

    @Test
    public void testAllExecutorsWithNullSQLUndoLog() {
        // Passing null: constructors accept null but usage may fail later
        AbstractUndoExecutor insertExecutor = executorHolder.getInsertExecutor(null);
        AbstractUndoExecutor updateExecutor = executorHolder.getUpdateExecutor(null);
        AbstractUndoExecutor deleteExecutor = executorHolder.getDeleteExecutor(null);

        // Verify executors are created
        Assertions.assertNotNull(insertExecutor);
        Assertions.assertNotNull(updateExecutor);
        Assertions.assertNotNull(deleteExecutor);

        // Verify types are correct
        Assertions.assertInstanceOf(PostgresqlUndoInsertExecutor.class, insertExecutor);
        Assertions.assertInstanceOf(PostgresqlUndoUpdateExecutor.class, updateExecutor);
        Assertions.assertInstanceOf(PostgresqlUndoDeleteExecutor.class, deleteExecutor);
    }

    @Test
    public void testExecutorWithDifferentSQLUndoLogs() {
        // Create different SQLUndoLog instances
        SQLUndoLog insertUndoLog = new SQLUndoLog();
        insertUndoLog.setSqlType(SQLType.INSERT);
        insertUndoLog.setTableName("insert_table");
        insertUndoLog.setTableMeta(tableMeta);

        SQLUndoLog updateUndoLog = new SQLUndoLog();
        updateUndoLog.setSqlType(SQLType.UPDATE);
        updateUndoLog.setTableName("update_table");
        updateUndoLog.setTableMeta(tableMeta);

        SQLUndoLog deleteUndoLog = new SQLUndoLog();
        deleteUndoLog.setSqlType(SQLType.DELETE);
        deleteUndoLog.setTableName("delete_table");
        deleteUndoLog.setTableMeta(tableMeta);

        // Verify different SQLUndoLog create corresponding executors
        AbstractUndoExecutor insertExecutor = executorHolder.getInsertExecutor(insertUndoLog);
        AbstractUndoExecutor updateExecutor = executorHolder.getUpdateExecutor(updateUndoLog);
        AbstractUndoExecutor deleteExecutor = executorHolder.getDeleteExecutor(deleteUndoLog);

        Assertions.assertInstanceOf(PostgresqlUndoInsertExecutor.class, insertExecutor);
        Assertions.assertInstanceOf(PostgresqlUndoUpdateExecutor.class, updateExecutor);
        Assertions.assertInstanceOf(PostgresqlUndoDeleteExecutor.class, deleteExecutor);

        // Verify each executor is an independent instance
        Assertions.assertNotSame(insertExecutor, updateExecutor);
        Assertions.assertNotSame(insertExecutor, deleteExecutor);
        Assertions.assertNotSame(updateExecutor, deleteExecutor);
    }

    @Test
    public void testExecutorInheritance() {
        // Verify all returned executors extend AbstractUndoExecutor
        sqlUndoLog.setSqlType(SQLType.INSERT);
        AbstractUndoExecutor insertExecutor = executorHolder.getInsertExecutor(sqlUndoLog);

        sqlUndoLog.setSqlType(SQLType.UPDATE);
        AbstractUndoExecutor updateExecutor = executorHolder.getUpdateExecutor(sqlUndoLog);

        sqlUndoLog.setSqlType(SQLType.DELETE);
        AbstractUndoExecutor deleteExecutor = executorHolder.getDeleteExecutor(sqlUndoLog);

        // Verify inheritance
        Assertions.assertTrue(insertExecutor instanceof AbstractUndoExecutor);
        Assertions.assertTrue(updateExecutor instanceof AbstractUndoExecutor);
        Assertions.assertTrue(deleteExecutor instanceof AbstractUndoExecutor);
    }

    @Test
    public void testExecutorCreationConsistency() {
        // Verify consistency when calling the same method multiple times
        sqlUndoLog.setSqlType(SQLType.INSERT);

        // Create INSERT executors multiple times
        AbstractUndoExecutor executor1 = executorHolder.getInsertExecutor(sqlUndoLog);
        AbstractUndoExecutor executor2 = executorHolder.getInsertExecutor(sqlUndoLog);
        AbstractUndoExecutor executor3 = executorHolder.getInsertExecutor(sqlUndoLog);

        // Verify correct types
        Assertions.assertInstanceOf(PostgresqlUndoInsertExecutor.class, executor1);
        Assertions.assertInstanceOf(PostgresqlUndoInsertExecutor.class, executor2);
        Assertions.assertInstanceOf(PostgresqlUndoInsertExecutor.class, executor3);

        // Verify they are independent instances
        Assertions.assertNotSame(executor1, executor2);
        Assertions.assertNotSame(executor2, executor3);
        Assertions.assertNotSame(executor1, executor3);
    }

    @Test
    public void testExecutorFactoryPattern() {
        // Verify factory pattern works correctly
        sqlUndoLog.setSqlType(SQLType.UPDATE);

        // Verify factory-created executor works
        AbstractUndoExecutor updateExecutor = executorHolder.getUpdateExecutor(sqlUndoLog);

        // Verify executor is created and accessible
        Assertions.assertNotNull(updateExecutor);

        // Verify class name and package indicate correct implementation
        Assertions.assertEquals(
                "PostgresqlUndoUpdateExecutor", updateExecutor.getClass().getSimpleName());
        Assertions.assertTrue(updateExecutor.getClass().getName().contains("postgresql"));
    }

    @Test
    public void testPostgresqlSpecificExecutors() {
        // Verify all created executors are PostgreSQL-specific implementations
        sqlUndoLog.setSqlType(SQLType.INSERT);
        AbstractUndoExecutor insertExecutor = executorHolder.getInsertExecutor(sqlUndoLog);

        sqlUndoLog.setSqlType(SQLType.UPDATE);
        AbstractUndoExecutor updateExecutor = executorHolder.getUpdateExecutor(sqlUndoLog);

        sqlUndoLog.setSqlType(SQLType.DELETE);
        AbstractUndoExecutor deleteExecutor = executorHolder.getDeleteExecutor(sqlUndoLog);

        // Verify class names contain Postgresql prefix
        Assertions.assertTrue(insertExecutor.getClass().getSimpleName().startsWith("Postgresql"));
        Assertions.assertTrue(updateExecutor.getClass().getSimpleName().startsWith("Postgresql"));
        Assertions.assertTrue(deleteExecutor.getClass().getSimpleName().startsWith("Postgresql"));

        // Verify package names are correct
        String expectedPackage = "org.apache.seata.rm.datasource.undo.postgresql";
        Assertions.assertEquals(
                expectedPackage, insertExecutor.getClass().getPackage().getName());
        Assertions.assertEquals(
                expectedPackage, updateExecutor.getClass().getPackage().getName());
        Assertions.assertEquals(
                expectedPackage, deleteExecutor.getClass().getPackage().getName());
    }

    @Test
    public void testUndoExecutorHolderInterface() {
        // Verify that the holder implements UndoExecutorHolder interface
        Assertions.assertTrue(executorHolder instanceof org.apache.seata.rm.datasource.undo.UndoExecutorHolder);
    }

    @Test
    public void testLoadLevelAnnotation() {
        // Verify that the class has LoadLevel annotation for PostgreSQL
        org.apache.seata.common.loader.LoadLevel loadLevel =
                PostgresqlUndoExecutorHolder.class.getAnnotation(org.apache.seata.common.loader.LoadLevel.class);

        Assertions.assertNotNull(loadLevel);
        Assertions.assertEquals("postgresql", loadLevel.name());
    }

    @Test
    public void testExecutorFactoryBehavior() {
        // Test that the factory creates different executor types based on SQL type
        SQLUndoLog insertLog = new SQLUndoLog();
        insertLog.setSqlType(SQLType.INSERT);
        insertLog.setTableMeta(tableMeta);

        SQLUndoLog updateLog = new SQLUndoLog();
        updateLog.setSqlType(SQLType.UPDATE);
        updateLog.setTableMeta(tableMeta);

        SQLUndoLog deleteLog = new SQLUndoLog();
        deleteLog.setSqlType(SQLType.DELETE);
        deleteLog.setTableMeta(tableMeta);

        // Get executors for different SQL types
        AbstractUndoExecutor insertExecutor = executorHolder.getInsertExecutor(insertLog);
        AbstractUndoExecutor updateExecutor = executorHolder.getUpdateExecutor(updateLog);
        AbstractUndoExecutor deleteExecutor = executorHolder.getDeleteExecutor(deleteLog);

        // Verify each returns a different executor type
        Assertions.assertInstanceOf(PostgresqlUndoInsertExecutor.class, insertExecutor);
        Assertions.assertInstanceOf(PostgresqlUndoUpdateExecutor.class, updateExecutor);
        Assertions.assertInstanceOf(PostgresqlUndoDeleteExecutor.class, deleteExecutor);

        // Verify all are different classes
        Assertions.assertNotEquals(insertExecutor.getClass(), updateExecutor.getClass());
        Assertions.assertNotEquals(insertExecutor.getClass(), deleteExecutor.getClass());
        Assertions.assertNotEquals(updateExecutor.getClass(), deleteExecutor.getClass());
    }
}
