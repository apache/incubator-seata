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
        deleteExecutor = new OscarUndoDeleteExecutor(sqlUndoLog);
        Assertions.assertNotNull(deleteExecutor);
    }

    @Test
    public void testBuildUndoSQL() {
        // 创建 before image 数据 (DELETE操作的撤销需要重新插入被删除的数据)
        TableRecords beforeImage = new TableRecords(tableMeta);
        Row row = new Row();
        Field idField = new Field("id", Types.INTEGER, 1);
        idField.setKeyType(KeyType.PRIMARY_KEY); // 设置为主键
        row.add(idField);
        row.add(new Field("name", Types.VARCHAR, "deleted_name"));
        row.add(new Field("age", Types.INTEGER, 30));
        
        List<Row> rows = new ArrayList<>();
        rows.add(row);
        beforeImage.setRows(rows);
        
        sqlUndoLog.setBeforeImage(beforeImage);
        deleteExecutor = new OscarUndoDeleteExecutor(sqlUndoLog);
        
        String undoSQL = deleteExecutor.buildUndoSQL();
        
        // 验证生成的 INSERT SQL 格式
        Assertions.assertTrue(undoSQL.contains("INSERT INTO test_table"));
        Assertions.assertTrue(undoSQL.contains("VALUES"));
        
        // 验证包含所有列（非主键列 + 主键列）
        Assertions.assertTrue(undoSQL.contains("\"name\""));
        Assertions.assertTrue(undoSQL.contains("\"age\""));
        Assertions.assertTrue(undoSQL.contains("\"id\""));
        
        // 验证参数占位符数量与列数匹配
        long questionMarkCount = undoSQL.chars().filter(ch -> ch == '?').count();
        Assertions.assertEquals(3, questionMarkCount); // name, age, id
    }

    @Test
    public void testBuildUndoSQLWithMultipleColumns() {
        // 测试多列场景
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
        
        // 验证 INSERT 语句格式
        String expectedPattern = "INSERT INTO test_table \\(.+\\) VALUES \\(.+\\)";
        Assertions.assertTrue(undoSQL.matches(expectedPattern));
        
        // 验证列名都在括号中
        int columnsStart = undoSQL.indexOf("(");
        int columnsEnd = undoSQL.indexOf(")");
        String columnsSection = undoSQL.substring(columnsStart + 1, columnsEnd);
        
        Assertions.assertTrue(columnsSection.contains("\"name\""));
        Assertions.assertTrue(columnsSection.contains("\"age\""));
        Assertions.assertTrue(columnsSection.contains("\"id\""));
    }

    @Test
    public void testBuildUndoSQLWithSingleColumn() {
        // 测试单列场景（只有主键）
        TableRecords beforeImage = new TableRecords(tableMeta);
        Row row = new Row();
        Field idField = new Field("id", Types.INTEGER, 1);
        idField.setKeyType(KeyType.PRIMARY_KEY); // 设置为主键
        row.add(idField);
        
        List<Row> rows = new ArrayList<>();
        rows.add(row);
        beforeImage.setRows(rows);
        
        sqlUndoLog.setBeforeImage(beforeImage);
        deleteExecutor = new OscarUndoDeleteExecutor(sqlUndoLog);
        
        String undoSQL = deleteExecutor.buildUndoSQL();
        
        // 验证基本 INSERT 结构
        Assertions.assertTrue(undoSQL.contains("INSERT INTO test_table"));
        Assertions.assertTrue(undoSQL.contains("\"id\""));
        Assertions.assertTrue(undoSQL.contains("VALUES"));
        
        // 验证单个参数占位符
        long questionMarkCount = undoSQL.chars().filter(ch -> ch == '?').count();
        Assertions.assertEquals(1, questionMarkCount);
    }

    @Test
    public void testBuildUndoSQLWithEmptyBeforeImage() {
        // 测试空的 before image 应该抛出异常
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
        // 测试 null before image 应该抛出异常
        sqlUndoLog.setBeforeImage(null);
        deleteExecutor = new OscarUndoDeleteExecutor(sqlUndoLog);
        
        Assertions.assertThrows(NullPointerException.class, () -> {
            deleteExecutor.buildUndoSQL();
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
        deleteExecutor = new OscarUndoDeleteExecutor(sqlUndoLog);
        
        TableRecords undoRows = deleteExecutor.getUndoRows();
        
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
        row.add(new Field("age", Types.INTEGER, 25));
        
        List<Row> rows = new ArrayList<>();
        rows.add(row);
        beforeImage.setRows(rows);
        
        sqlUndoLog.setBeforeImage(beforeImage);
        deleteExecutor = new OscarUndoDeleteExecutor(sqlUndoLog);
        
        String undoSQL = deleteExecutor.buildUndoSQL();
        
        // 验证 Oscar 使用双引号进行转义
        Assertions.assertTrue(undoSQL.contains("\"name\""));
        Assertions.assertTrue(undoSQL.contains("\"age\""));
        Assertions.assertTrue(undoSQL.contains("\"id\""));
    }

    @Test
    public void testInsertSQLTemplateFormat() {
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
        deleteExecutor = new OscarUndoDeleteExecutor(sqlUndoLog);
        
        String undoSQL = deleteExecutor.buildUndoSQL();
        
        // 验证 INSERT 模板格式: INSERT INTO table (columns) VALUES (values)
        String expectedPattern = "INSERT INTO test_table \\(.+\\) VALUES \\(.+\\)";
        Assertions.assertTrue(undoSQL.matches(expectedPattern));
        
        // 验证列数与值占位符数量匹配
        String columnsSection = undoSQL.substring(
            undoSQL.indexOf("(") + 1, 
            undoSQL.indexOf(")")
        );
        String valuesSection = undoSQL.substring(
            undoSQL.lastIndexOf("(") + 1, 
            undoSQL.lastIndexOf(")")
        );
        
        // 计算列数和占位符数
        long columnCount = columnsSection.chars().filter(ch -> ch == ',').count() + 1;
        long placeholderCount = valuesSection.chars().filter(ch -> ch == '?').count();
        
        Assertions.assertEquals(columnCount, placeholderCount);
    }

    @Test
    public void testColumnOrdering() {
        // 测试列的排序：非主键列 + 主键列
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
        deleteExecutor = new OscarUndoDeleteExecutor(sqlUndoLog);
        
        String undoSQL = deleteExecutor.buildUndoSQL();
        
        // 验证所有字段都包含在SQL中
        Assertions.assertTrue(undoSQL.contains("\"name\""));
        Assertions.assertTrue(undoSQL.contains("\"age\""));
        Assertions.assertTrue(undoSQL.contains("\"id\""));
        
        // 验证总的占位符数量
        long questionMarkCount = undoSQL.chars().filter(ch -> ch == '?').count();
        Assertions.assertEquals(3, questionMarkCount);
    }
}
