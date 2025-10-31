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
import java.util.Arrays;
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
        
        ColumnMeta ageColumn = new ColumnMeta();
        ageColumn.setTableName("test_table");
        ageColumn.setColumnName("age");
        ageColumn.setDataType(Types.INTEGER);
        ageColumn.setColumnSize(11);
        allColumns.put("age", ageColumn);
        
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
        updateExecutor = new OscarUndoUpdateExecutor(sqlUndoLog);
        Assertions.assertNotNull(updateExecutor);
    }

    @Test
    public void testBuildUndoSQL() {
        // 创建 before image 数据
        TableRecords beforeImage = new TableRecords(tableMeta);
        Row row = new Row();
        Field idField = new Field("id", Types.INTEGER, 1);
        idField.setKeyType(KeyType.PRIMARY_KEY); // 设置为主键
        row.add(idField);
        row.add(new Field("name", Types.VARCHAR, "old_name"));
        row.add(new Field("age", Types.INTEGER, 25));
        
        List<Row> rows = new ArrayList<>();
        rows.add(row);
        beforeImage.setRows(rows);
        
        sqlUndoLog.setBeforeImage(beforeImage);
        updateExecutor = new OscarUndoUpdateExecutor(sqlUndoLog);
        
        String undoSQL = updateExecutor.buildUndoSQL();
        
        // 验证生成的 SQL 格式
        Assertions.assertTrue(undoSQL.contains("UPDATE test_table"));
        Assertions.assertTrue(undoSQL.contains("SET"));
        Assertions.assertTrue(undoSQL.contains("WHERE"));
        
        // 验证包含非主键列的更新
        Assertions.assertTrue(undoSQL.contains("\"name\" = ?"));
        Assertions.assertTrue(undoSQL.contains("\"age\" = ?"));
        
        // 验证包含主键条件
        Assertions.assertTrue(undoSQL.contains("\"id\" = ?"));
        
        // 验证不包含主键列在SET子句中
        Assertions.assertFalse(undoSQL.matches(".*SET.*\"id\"\\s*=.*"));
    }

    @Test
    public void testBuildUndoSQLWithSingleNonPkColumn() {
        // 创建只有一个非主键列的场景
        TableRecords beforeImage = new TableRecords(tableMeta);
        Row row = new Row();
        Field idField = new Field("id", Types.INTEGER, 1);
        idField.setKeyType(KeyType.PRIMARY_KEY); // 设置为主键
        row.add(idField);
        row.add(new Field("name", Types.VARCHAR, "test_name"));
        
        List<Row> rows = new ArrayList<>();
        rows.add(row);
        beforeImage.setRows(rows);
        
        sqlUndoLog.setBeforeImage(beforeImage);
        updateExecutor = new OscarUndoUpdateExecutor(sqlUndoLog);
        
        String undoSQL = updateExecutor.buildUndoSQL();
        
        // 验证 SQL 结构
        Assertions.assertTrue(undoSQL.contains("UPDATE test_table"));
        Assertions.assertTrue(undoSQL.contains("SET \"name\" = ?"));
        Assertions.assertTrue(undoSQL.contains("WHERE \"id\" = ?"));
    }

    @Test
    public void testBuildUndoSQLWithMultipleNonPkColumns() {
        // 创建多个非主键列的场景
        TableRecords beforeImage = new TableRecords(tableMeta);
        Row row = new Row();
        Field idField = new Field("id", Types.INTEGER, 1);
        idField.setKeyType(KeyType.PRIMARY_KEY); // 设置为主键
        row.add(idField);
        row.add(new Field("name", Types.VARCHAR, "test_name"));
        row.add(new Field("age", Types.INTEGER, 30));
        
        List<Row> rows = new ArrayList<>();
        rows.add(row);
        beforeImage.setRows(rows);
        
        sqlUndoLog.setBeforeImage(beforeImage);
        updateExecutor = new OscarUndoUpdateExecutor(sqlUndoLog);
        
        String undoSQL = updateExecutor.buildUndoSQL();
        
        // 验证包含所有非主键列
        Assertions.assertTrue(undoSQL.contains("\"name\" = ?"));
        Assertions.assertTrue(undoSQL.contains("\"age\" = ?"));
        
        // 验证SET子句中的逗号分隔
        Assertions.assertTrue(undoSQL.contains("\"name\" = ?, \"age\" = ?") || 
                           undoSQL.contains("\"age\" = ?, \"name\" = ?"));
    }

    @Test
    public void testBuildUndoSQLWithEmptyBeforeImage() {
        // 测试空的 before image 应该抛出异常
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
        // 测试 null before image 应该抛出异常
        sqlUndoLog.setBeforeImage(null);
        updateExecutor = new OscarUndoUpdateExecutor(sqlUndoLog);
        
        Assertions.assertThrows(NullPointerException.class, () -> {
            updateExecutor.buildUndoSQL();
        });
    }

    @Test
    public void testGetUndoRows() {
        // 创建 before image 数据
        TableRecords beforeImage = new TableRecords(tableMeta);
        Row row = new Row();
        Field idField = new Field("id", Types.INTEGER, 1);
        idField.setKeyType(KeyType.PRIMARY_KEY); // 设置为主键
        row.add(idField);
        row.add(new Field("name", Types.VARCHAR, "test_name"));
        
        List<Row> rows = new ArrayList<>();
        rows.add(row);
        beforeImage.setRows(rows);
        
        sqlUndoLog.setBeforeImage(beforeImage);
        updateExecutor = new OscarUndoUpdateExecutor(sqlUndoLog);
        
        TableRecords undoRows = updateExecutor.getUndoRows();
        
        // 验证返回的是 before image
        Assertions.assertSame(beforeImage, undoRows);
        Assertions.assertEquals(1, undoRows.getRows().size());
        // 通过字段列表查找字段值
        Row resultRow = undoRows.getRows().get(0);
        Field idFields = resultRow.getFields().stream().filter(f -> "id".equals(f.getName())).findFirst().orElse(null);
        Field nameField = resultRow.getFields().stream().filter(f -> "name".equals(f.getName())).findFirst().orElse(null);
        Assertions.assertNotNull(idFields);
        Assertions.assertNotNull(nameField);
        Assertions.assertEquals(1, idFields.getValue());
        Assertions.assertEquals("test_name", nameField.getValue());
    }

    @Test
    public void testOscarSpecificEscaping() {
        // 测试 Oscar 数据库特定的列名转义
        TableRecords beforeImage = new TableRecords(tableMeta);
        Row row = new Row();
        Field idField = new Field("id", Types.INTEGER, 1);
        idField.setKeyType(KeyType.PRIMARY_KEY); // 设置为主键
        row.add(idField);
        row.add(new Field("name", Types.VARCHAR, "test"));
        
        List<Row> rows = new ArrayList<>();
        rows.add(row);
        beforeImage.setRows(rows);
        
        sqlUndoLog.setBeforeImage(beforeImage);
        updateExecutor = new OscarUndoUpdateExecutor(sqlUndoLog);
        
        String undoSQL = updateExecutor.buildUndoSQL();
        
        // 验证 Oscar 使用双引号进行转义
        Assertions.assertTrue(undoSQL.contains("\"name\""));
        Assertions.assertTrue(undoSQL.contains("\"id\""));
    }

    @Test
    public void testSQLTemplateFormat() {
        TableRecords beforeImage = new TableRecords(tableMeta);
        Row row = new Row();
        Field idField = new Field("id", Types.INTEGER, 1);
        idField.setKeyType(KeyType.PRIMARY_KEY); // 设置为主键
        row.add(idField);
        row.add(new Field("name", Types.VARCHAR, "test"));
        row.add(new Field("age", Types.INTEGER, 25));
        
        List<Row> rows = new ArrayList<>();
        rows.add(row);
        beforeImage.setRows(rows);
        
        sqlUndoLog.setBeforeImage(beforeImage);
        updateExecutor = new OscarUndoUpdateExecutor(sqlUndoLog);
        
        String undoSQL = updateExecutor.buildUndoSQL();
        
        // 验证 SQL 模板格式: UPDATE table SET columns WHERE conditions
        String expectedPattern = "UPDATE test_table SET .+ WHERE .+";
        Assertions.assertTrue(undoSQL.matches(expectedPattern));
        
        // 验证参数占位符数量
        long questionMarkCount = undoSQL.chars().filter(ch -> ch == '?').count();
        Assertions.assertTrue(questionMarkCount >= 3); // 至少有 name, age, id 三个参数
    }
}
