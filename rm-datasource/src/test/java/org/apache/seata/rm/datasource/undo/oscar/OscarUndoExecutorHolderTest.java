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

public class OscarUndoExecutorHolderTest {

    private OscarUndoExecutorHolder executorHolder;
    private SQLUndoLog sqlUndoLog;
    private TableMeta tableMeta;

    @BeforeEach
    public void setUp() {
        executorHolder = new OscarUndoExecutorHolder();
        tableMeta = createTableMeta();
        
        sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setTableName("test_table");
        sqlUndoLog.setTableMeta(tableMeta);
    }

    private TableMeta createTableMeta() {
        TableMeta meta = new TableMeta();
        meta.setTableName("test_table");
        
        // 创建列元数据
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
        
        // 创建主键索引
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
        
        // 验证返回的是正确的执行器类型
        Assertions.assertNotNull(executor);
        Assertions.assertInstanceOf(OscarUndoInsertExecutor.class, executor);
        
        // 验证执行器确实是不同的实例（每次调用都创建新实例）
        AbstractUndoExecutor anotherExecutor = executorHolder.getInsertExecutor(sqlUndoLog);
        Assertions.assertNotSame(executor, anotherExecutor);
        Assertions.assertInstanceOf(OscarUndoInsertExecutor.class, anotherExecutor);
    }

    @Test
    public void testGetUpdateExecutor() {
        sqlUndoLog.setSqlType(SQLType.UPDATE);
        
        AbstractUndoExecutor executor = executorHolder.getUpdateExecutor(sqlUndoLog);
        
        // 验证返回的是正确的执行器类型
        Assertions.assertNotNull(executor);
        Assertions.assertInstanceOf(OscarUndoUpdateExecutor.class, executor);
        
        // 验证执行器确实是不同的实例（每次调用都创建新实例）
        AbstractUndoExecutor anotherExecutor = executorHolder.getUpdateExecutor(sqlUndoLog);
        Assertions.assertNotSame(executor, anotherExecutor);
        Assertions.assertInstanceOf(OscarUndoUpdateExecutor.class, anotherExecutor);
    }

    @Test
    public void testGetDeleteExecutor() {
        sqlUndoLog.setSqlType(SQLType.DELETE);
        
        AbstractUndoExecutor executor = executorHolder.getDeleteExecutor(sqlUndoLog);
        
        // 验证返回的是正确的执行器类型
        Assertions.assertNotNull(executor);
        Assertions.assertInstanceOf(OscarUndoDeleteExecutor.class, executor);
        
        // 验证执行器确实是不同的实例（每次调用都创建新实例）
        AbstractUndoExecutor anotherExecutor = executorHolder.getDeleteExecutor(sqlUndoLog);
        Assertions.assertNotSame(executor, anotherExecutor);
        Assertions.assertInstanceOf(OscarUndoDeleteExecutor.class, anotherExecutor);
    }

    @Test
    public void testAllExecutorsWithNullSQLUndoLog() {
        // 测试传入 null 参数的情况 - 构造函数接受null，但后续使用会有问题
        AbstractUndoExecutor insertExecutor = executorHolder.getInsertExecutor(null);
        AbstractUndoExecutor updateExecutor = executorHolder.getUpdateExecutor(null);
        AbstractUndoExecutor deleteExecutor = executorHolder.getDeleteExecutor(null);
        
        // 验证执行器被创建，但使用时会有问题
        Assertions.assertNotNull(insertExecutor);
        Assertions.assertNotNull(updateExecutor);
        Assertions.assertNotNull(deleteExecutor);
        
        // 验证类型正确
        Assertions.assertInstanceOf(OscarUndoInsertExecutor.class, insertExecutor);
        Assertions.assertInstanceOf(OscarUndoUpdateExecutor.class, updateExecutor);
        Assertions.assertInstanceOf(OscarUndoDeleteExecutor.class, deleteExecutor);
    }

    @Test
    public void testExecutorWithDifferentSQLUndoLogs() {
        // 创建不同的 SQLUndoLog 实例
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
        
        // 验证不同的 SQLUndoLog 可以正确创建对应的执行器
        AbstractUndoExecutor insertExecutor = executorHolder.getInsertExecutor(insertUndoLog);
        AbstractUndoExecutor updateExecutor = executorHolder.getUpdateExecutor(updateUndoLog);
        AbstractUndoExecutor deleteExecutor = executorHolder.getDeleteExecutor(deleteUndoLog);
        
        Assertions.assertInstanceOf(OscarUndoInsertExecutor.class, insertExecutor);
        Assertions.assertInstanceOf(OscarUndoUpdateExecutor.class, updateExecutor);
        Assertions.assertInstanceOf(OscarUndoDeleteExecutor.class, deleteExecutor);
        
        // 验证每个执行器都是独立的实例
        Assertions.assertNotSame(insertExecutor, updateExecutor);
        Assertions.assertNotSame(insertExecutor, deleteExecutor);
        Assertions.assertNotSame(updateExecutor, deleteExecutor);
    }

    @Test
    public void testExecutorInheritance() {
        // 验证所有返回的执行器都继承自 AbstractUndoExecutor
        sqlUndoLog.setSqlType(SQLType.INSERT);
        AbstractUndoExecutor insertExecutor = executorHolder.getInsertExecutor(sqlUndoLog);
        
        sqlUndoLog.setSqlType(SQLType.UPDATE);
        AbstractUndoExecutor updateExecutor = executorHolder.getUpdateExecutor(sqlUndoLog);
        
        sqlUndoLog.setSqlType(SQLType.DELETE);
        AbstractUndoExecutor deleteExecutor = executorHolder.getDeleteExecutor(sqlUndoLog);
        
        // 验证继承关系
        Assertions.assertTrue(insertExecutor instanceof AbstractUndoExecutor);
        Assertions.assertTrue(updateExecutor instanceof AbstractUndoExecutor);
        Assertions.assertTrue(deleteExecutor instanceof AbstractUndoExecutor);
    }

    @Test
    public void testExecutorCreationConsistency() {
        // 测试多次调用相同方法的一致性
        sqlUndoLog.setSqlType(SQLType.INSERT);
        
        // 多次创建 INSERT 执行器
        AbstractUndoExecutor executor1 = executorHolder.getInsertExecutor(sqlUndoLog);
        AbstractUndoExecutor executor2 = executorHolder.getInsertExecutor(sqlUndoLog);
        AbstractUndoExecutor executor3 = executorHolder.getInsertExecutor(sqlUndoLog);
        
        // 验证都是正确的类型
        Assertions.assertInstanceOf(OscarUndoInsertExecutor.class, executor1);
        Assertions.assertInstanceOf(OscarUndoInsertExecutor.class, executor2);
        Assertions.assertInstanceOf(OscarUndoInsertExecutor.class, executor3);
        
        // 验证都是独立的实例
        Assertions.assertNotSame(executor1, executor2);
        Assertions.assertNotSame(executor2, executor3);
        Assertions.assertNotSame(executor1, executor3);
    }

    @Test
    public void testExecutorFactoryPattern() {
        // 验证工厂模式的正确实现
        sqlUndoLog.setSqlType(SQLType.UPDATE);
        
        // 测试工厂方法返回的执行器能够正常工作
        AbstractUndoExecutor updateExecutor = executorHolder.getUpdateExecutor(sqlUndoLog);
        
        // 验证执行器可以访问传入的 SQLUndoLog
        Assertions.assertNotNull(updateExecutor);
        
        // 验证执行器的类型和包名正确，表明工厂方法正常工作
        Assertions.assertEquals("OscarUndoUpdateExecutor", updateExecutor.getClass().getSimpleName());
        Assertions.assertTrue(updateExecutor.getClass().getName().contains("oscar"));
    }

    @Test
    public void testOscarSpecificExecutors() {
        // 验证创建的都是 Oscar 特定的执行器实现
        sqlUndoLog.setSqlType(SQLType.INSERT);
        AbstractUndoExecutor insertExecutor = executorHolder.getInsertExecutor(sqlUndoLog);
        
        sqlUndoLog.setSqlType(SQLType.UPDATE);
        AbstractUndoExecutor updateExecutor = executorHolder.getUpdateExecutor(sqlUndoLog);
        
        sqlUndoLog.setSqlType(SQLType.DELETE);
        AbstractUndoExecutor deleteExecutor = executorHolder.getDeleteExecutor(sqlUndoLog);
        
        // 验证类名包含 Oscar 前缀，确保是 Oscar 特定的实现
        Assertions.assertTrue(insertExecutor.getClass().getSimpleName().startsWith("Oscar"));
        Assertions.assertTrue(updateExecutor.getClass().getSimpleName().startsWith("Oscar"));
        Assertions.assertTrue(deleteExecutor.getClass().getSimpleName().startsWith("Oscar"));
        
        // 验证包名正确
        String expectedPackage = "org.apache.seata.rm.datasource.undo.oscar";
        Assertions.assertEquals(expectedPackage, insertExecutor.getClass().getPackage().getName());
        Assertions.assertEquals(expectedPackage, updateExecutor.getClass().getPackage().getName());
        Assertions.assertEquals(expectedPackage, deleteExecutor.getClass().getPackage().getName());
    }
}
