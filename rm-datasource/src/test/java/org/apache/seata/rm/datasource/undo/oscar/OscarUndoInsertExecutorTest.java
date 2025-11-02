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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class OscarUndoInsertExecutorTest {

    private OscarUndoInsertExecutor insertExecutor;
    private SQLUndoLog sqlUndoLog;
    private TableMeta tableMeta;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setSqlType(SQLType.INSERT);
        sqlUndoLog.setTableName("test_table");

        tableMeta = createTableMeta();
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

    @Test
    public void testConstructor() {
        insertExecutor = new OscarUndoInsertExecutor(sqlUndoLog);
        Assertions.assertNotNull(insertExecutor);
    }

    @Test
    public void testBuildUndoSQL() {
        // Create after image data (undo INSERT by deleting inserted rows)
        TableRecords afterImage = new TableRecords(tableMeta);
        Row row = new Row();
        Field idField = new Field("id", Types.INTEGER, 1);
        idField.setKeyType(KeyType.PRIMARY_KEY); // mark as primary key
        row.add(idField);
        row.add(new Field("name", Types.VARCHAR, "inserted_name"));
        row.add(new Field("age", Types.INTEGER, 25));

        List<Row> rows = new ArrayList<>();
        rows.add(row);
        afterImage.setRows(rows);

        sqlUndoLog.setAfterImage(afterImage);
        insertExecutor = new OscarUndoInsertExecutor(sqlUndoLog);

        String undoSQL = insertExecutor.buildUndoSQL();

        // Verify generated DELETE SQL format
        Assertions.assertTrue(undoSQL.contains("DELETE FROM test_table"));
        Assertions.assertTrue(undoSQL.contains("WHERE"));

        // Verify WHERE contains PK condition (DELETE only needs PK)
        Assertions.assertTrue(undoSQL.contains("id") && undoSQL.contains("= ?"));

        // DELETE WHERE clause should not include non-PK columns
        String whereClause = undoSQL.substring(undoSQL.indexOf("WHERE"));
        Assertions.assertFalse(whereClause.contains("name"));
        Assertions.assertFalse(whereClause.contains("age"));
    }

    @Test
    public void testBuildUndoSQLWithMultiplePrimaryKeys() {
        // Create table meta with composite primary key
        TableMeta compoundPkMeta = createCompoundPrimaryKeyTableMeta();
        sqlUndoLog.setTableMeta(compoundPkMeta);

        TableRecords afterImage = new TableRecords(compoundPkMeta);
        Row row = new Row();
        Field userIdField = new Field("user_id", Types.INTEGER, 1);
        userIdField.setKeyType(KeyType.PRIMARY_KEY); // mark as primary key
        Field productIdField = new Field("product_id", Types.INTEGER, 2);
        productIdField.setKeyType(KeyType.PRIMARY_KEY); // mark as primary key
        row.add(userIdField);
        row.add(productIdField);
        row.add(new Field("quantity", Types.INTEGER, 5));

        List<Row> rows = new ArrayList<>();
        rows.add(row);
        afterImage.setRows(rows);

        sqlUndoLog.setAfterImage(afterImage);
        insertExecutor = new OscarUndoInsertExecutor(sqlUndoLog);

        String undoSQL = insertExecutor.buildUndoSQL();

        // Verify it contains all PK conditions (escaping may vary)
        Assertions.assertTrue(undoSQL.contains("DELETE FROM test_table"));
        Assertions.assertTrue(undoSQL.contains("user_id") && undoSQL.contains("= ?"));
        Assertions.assertTrue(undoSQL.contains("product_id") && undoSQL.contains("= ?"));

        // Verify PK conditions are joined by AND
        Assertions.assertTrue(undoSQL.contains(" and "));
    }

    private TableMeta createCompoundPrimaryKeyTableMeta() {
        TableMeta meta = new TableMeta();
        meta.setTableName("test_table");

        Map<String, ColumnMeta> allColumns = new HashMap<>();

        ColumnMeta userIdColumn = new ColumnMeta();
        userIdColumn.setTableName("test_table");
        userIdColumn.setColumnName("user_id");
        userIdColumn.setDataType(Types.INTEGER);
        allColumns.put("user_id", userIdColumn);

        ColumnMeta productIdColumn = new ColumnMeta();
        productIdColumn.setTableName("test_table");
        productIdColumn.setColumnName("product_id");
        productIdColumn.setDataType(Types.INTEGER);
        allColumns.put("product_id", productIdColumn);

        ColumnMeta quantityColumn = new ColumnMeta();
        quantityColumn.setTableName("test_table");
        quantityColumn.setColumnName("quantity");
        quantityColumn.setDataType(Types.INTEGER);
        allColumns.put("quantity", quantityColumn);

        meta.getAllColumns().putAll(allColumns);

        // Create composite primary key index
        Map<String, IndexMeta> allIndexes = new HashMap<>();
        IndexMeta primaryIndex = new IndexMeta();
        primaryIndex.setIndexName("PRIMARY");
        primaryIndex.setNonUnique(false);
        primaryIndex.setIndextype(IndexType.PRIMARY);

        List<ColumnMeta> primaryColumns = new ArrayList<>();
        primaryColumns.add(userIdColumn);
        primaryColumns.add(productIdColumn);
        primaryIndex.setValues(primaryColumns);

        allIndexes.put("PRIMARY", primaryIndex);
        meta.getAllIndexes().putAll(allIndexes);

        return meta;
    }

    @Test
    public void testBuildUndoSQLWithEmptyAfterImage() {
        // Empty after image should throw
        TableRecords emptyAfterImage = new TableRecords(tableMeta);
        emptyAfterImage.setRows(new ArrayList<>());

        sqlUndoLog.setAfterImage(emptyAfterImage);
        insertExecutor = new OscarUndoInsertExecutor(sqlUndoLog);

        Assertions.assertThrows(ShouldNeverHappenException.class, () -> {
            insertExecutor.buildUndoSQL();
        });
    }

    @Test
    public void testBuildUndoSQLWithNullAfterImage() {
        // Null after image should throw
        sqlUndoLog.setAfterImage(null);
        insertExecutor = new OscarUndoInsertExecutor(sqlUndoLog);

        Assertions.assertThrows(NullPointerException.class, () -> {
            insertExecutor.buildUndoSQL();
        });
    }

    @Test
    public void testGetUndoRows() {
        // 创建 after image 数据
        TableRecords afterImage = new TableRecords(tableMeta);
        Row row = new Row();
        Field idField = new Field("id", Types.INTEGER, 1);
        idField.setKeyType(KeyType.PRIMARY_KEY); // 设置为主键
        row.add(idField);
        row.add(new Field("name", Types.VARCHAR, "test_name"));

        List<Row> rows = new ArrayList<>();
        rows.add(row);
        afterImage.setRows(rows);

        sqlUndoLog.setAfterImage(afterImage);
        insertExecutor = new OscarUndoInsertExecutor(sqlUndoLog);

        TableRecords undoRows = insertExecutor.getUndoRows();

        // Verify it returns after image
        Assertions.assertSame(afterImage, undoRows);
        Assertions.assertEquals(1, undoRows.getRows().size());
        // Find field values via field list
        Row resultRow = undoRows.getRows().get(0);
        Field idFields = resultRow.getFields().stream()
                .filter(f -> "id".equals(f.getName()))
                .findFirst()
                .orElse(null);
        Field nameField = resultRow.getFields().stream()
                .filter(f -> "name".equals(f.getName()))
                .findFirst()
                .orElse(null);
        Assertions.assertNotNull(idFields);
        Assertions.assertNotNull(nameField);
        Assertions.assertEquals(1, idFields.getValue());
        Assertions.assertEquals("test_name", nameField.getValue());
    }

    @Test
    public void testUndoPrepare() throws SQLException {
        // Test the special behavior of undoPrepare
        TableRecords afterImage = new TableRecords(tableMeta);
        Row row = new Row();
        Field idField = new Field("id", Types.INTEGER, 1);
        idField.setKeyType(KeyType.PRIMARY_KEY); // 设置为主键
        row.add(idField);
        row.add(new Field("name", Types.VARCHAR, "test_name"));
        row.add(new Field("age", Types.INTEGER, 25));

        List<Row> rows = new ArrayList<>();
        rows.add(row);
        afterImage.setRows(rows);

        sqlUndoLog.setAfterImage(afterImage);
        insertExecutor = new OscarUndoInsertExecutor(sqlUndoLog);

        // Mock PK field list (PK only)
        ArrayList<Field> undoValues = new ArrayList<>();
        List<Field> pkValueList = new ArrayList<>();
        Field pkField = new Field("id", Types.INTEGER, 1);
        pkField.setKeyType(KeyType.PRIMARY_KEY);
        pkValueList.add(pkField);

        // Invoke undoPrepare
        insertExecutor.undoPrepare(mockPreparedStatement, undoValues, pkValueList);

        // Verify only PK parameters are set
        verify(mockPreparedStatement, times(1)).setObject(eq(1), eq(1), eq(Types.INTEGER));
    }

    @Test
    public void testUndoPrepareWithMultiplePrimaryKeys() throws SQLException {
        // Test undoPrepare with composite primary key
        TableMeta compoundPkMeta = createCompoundPrimaryKeyTableMeta();
        sqlUndoLog.setTableMeta(compoundPkMeta);

        TableRecords afterImage = new TableRecords(compoundPkMeta);
        Row row = new Row();
        Field userIdField2 = new Field("user_id", Types.INTEGER, 1);
        userIdField2.setKeyType(KeyType.PRIMARY_KEY); // 设置为主键
        Field productIdField2 = new Field("product_id", Types.INTEGER, 2);
        productIdField2.setKeyType(KeyType.PRIMARY_KEY); // 设置为主键
        row.add(userIdField2);
        row.add(productIdField2);
        row.add(new Field("quantity", Types.INTEGER, 5));

        List<Row> rows = new ArrayList<>();
        rows.add(row);
        afterImage.setRows(rows);

        sqlUndoLog.setAfterImage(afterImage);
        insertExecutor = new OscarUndoInsertExecutor(sqlUndoLog);

        // Mock composite PK field list
        ArrayList<Field> undoValues = new ArrayList<>();
        List<Field> pkValueList = new ArrayList<>();
        Field userIdPkField = new Field("user_id", Types.INTEGER, 1);
        userIdPkField.setKeyType(KeyType.PRIMARY_KEY);
        Field productIdPkField = new Field("product_id", Types.INTEGER, 2);
        productIdPkField.setKeyType(KeyType.PRIMARY_KEY);
        pkValueList.add(userIdPkField);
        pkValueList.add(productIdPkField);

        // Invoke undoPrepare
        insertExecutor.undoPrepare(mockPreparedStatement, undoValues, pkValueList);

        // Verify all PK parameters are set
        verify(mockPreparedStatement, times(1)).setObject(eq(1), eq(1), eq(Types.INTEGER));
        verify(mockPreparedStatement, times(1)).setObject(eq(2), eq(2), eq(Types.INTEGER));
    }

    @Test
    public void testOscarSpecificEscaping() {
        // Test Oscar-specific column escaping
        TableRecords afterImage = new TableRecords(tableMeta);
        Row row = new Row();
        Field idField = new Field("id", Types.INTEGER, 1);
        idField.setKeyType(KeyType.PRIMARY_KEY); // mark as primary key
        row.add(idField);
        row.add(new Field("name", Types.VARCHAR, "test"));

        List<Row> rows = new ArrayList<>();
        rows.add(row);
        afterImage.setRows(rows);

        sqlUndoLog.setAfterImage(afterImage);
        insertExecutor = new OscarUndoInsertExecutor(sqlUndoLog);

        String undoSQL = insertExecutor.buildUndoSQL();

        // Verify column name presence (escaping may vary)
        Assertions.assertTrue(undoSQL.contains("id"));

        // DELETE 语句只在WHERE子句中包含主键列
        String whereClause = undoSQL.substring(undoSQL.indexOf("WHERE"));
        Assertions.assertTrue(whereClause.contains("id"));
    }

    @Test
    public void testDeleteSQLTemplateFormat() {
        TableRecords afterImage = new TableRecords(tableMeta);
        Row row = new Row();
        Field idField = new Field("id", Types.INTEGER, 1);
        idField.setKeyType(KeyType.PRIMARY_KEY); // 设置为主键
        row.add(idField);
        row.add(new Field("name", Types.VARCHAR, "test"));

        List<Row> rows = new ArrayList<>();
        rows.add(row);
        afterImage.setRows(rows);

        sqlUndoLog.setAfterImage(afterImage);
        insertExecutor = new OscarUndoInsertExecutor(sqlUndoLog);

        String undoSQL = insertExecutor.buildUndoSQL();

        // Verify DELETE template: DELETE FROM table WHERE conditions
        String expectedPattern = "DELETE FROM test_table WHERE .+";
        Assertions.assertTrue(undoSQL.matches(expectedPattern));

        // Verify number of placeholders equals number of PKs
        long questionMarkCount = undoSQL.chars().filter(ch -> ch == '?').count();
        Assertions.assertEquals(1, questionMarkCount); // 只有一个主键
    }

    @Test
    public void testGenerateDeleteSqlMethod() {
        // Test internal generateDeleteSql logic
        TableRecords afterImage = new TableRecords(tableMeta);
        Row row = new Row();
        Field idField = new Field("id", Types.INTEGER, 100);
        idField.setKeyType(KeyType.PRIMARY_KEY); // 设置为主键
        row.add(idField);
        row.add(new Field("name", Types.VARCHAR, "test_data"));
        row.add(new Field("age", Types.INTEGER, 30));

        List<Row> rows = new ArrayList<>();
        rows.add(row);
        afterImage.setRows(rows);

        sqlUndoLog.setAfterImage(afterImage);
        insertExecutor = new OscarUndoInsertExecutor(sqlUndoLog);

        String undoSQL = insertExecutor.buildUndoSQL();

        // Verify DELETE statement only contains PK conditions
        Assertions.assertTrue(undoSQL.contains("DELETE FROM test_table"));
        Assertions.assertTrue(undoSQL.contains("WHERE") && undoSQL.contains("id") && undoSQL.contains("= ?"));

        // Verify non-PK columns are not included
        String whereClause = undoSQL.substring(undoSQL.indexOf("WHERE"));
        Assertions.assertFalse(whereClause.contains("name"));
        Assertions.assertFalse(whereClause.contains("age"));
    }
}
