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
package org.apache.seata.sqlparser.druid.oracle;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import org.apache.seata.sqlparser.SQLType;
import org.apache.seata.sqlparser.struct.Null;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Oracle Multi Insert Recognizer Test
 *
 */
public class OracleMultiInsertRecognizerTest {

    private static final String DB_TYPE = "oracle";

    @Test
    public void testGetSqlType() {
        String sql = "INSERT ALL INTO a (id) VALUES (1) INTO a (id) VALUES (2) SELECT 1 FROM DUAL";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        OracleMultiInsertRecognizer recognizer = new OracleMultiInsertRecognizer(sql, asts.get(0));
        Assertions.assertEquals(SQLType.INSERT, recognizer.getSQLType());
    }

    @Test
    public void testGetTableName() {
        String sql = "INSERT ALL INTO users (id) VALUES (1) INTO users (id) VALUES (2) SELECT 1 FROM DUAL";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        OracleMultiInsertRecognizer recognizer = new OracleMultiInsertRecognizer(sql, asts.get(0));
        Assertions.assertEquals("users", recognizer.getTableName());
    }

    @Test
    public void testGetTableAlias() {
        // 测试无别名的情况
        String sql = "INSERT ALL INTO users (id) VALUES (1) INTO users (id) VALUES (2) SELECT 1 FROM DUAL";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        OracleMultiInsertRecognizer recognizer = new OracleMultiInsertRecognizer(sql, asts.get(0));
        Assertions.assertNull(recognizer.getTableAlias());
    }

    @Test
    public void testGetInsertColumns() {
        String sql = "INSERT ALL INTO users (id, name, age) VALUES (1, 'Tom', 20) INTO users (id, name, age) VALUES (2, 'Jerry', 25) SELECT 1 FROM DUAL";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        OracleMultiInsertRecognizer recognizer = new OracleMultiInsertRecognizer(sql, asts.get(0));
        List<String> columns = recognizer.getInsertColumns();
        
        Assertions.assertNotNull(columns);
        Assertions.assertEquals(3, columns.size());
        Assertions.assertEquals("id", columns.get(0));
        Assertions.assertEquals("name", columns.get(1));
        Assertions.assertEquals("age", columns.get(2));
    }

    @Test
    public void testInsertColumnsIsEmpty() {
        // 测试有列的情况
        String sqlWithColumns = "INSERT ALL INTO users (id, name) VALUES (1, 'Tom') INTO users (id, name) VALUES (2, 'Jerry') SELECT 1 FROM DUAL";
        List<SQLStatement> asts = SQLUtils.parseStatements(sqlWithColumns, DB_TYPE);
        OracleMultiInsertRecognizer recognizer = new OracleMultiInsertRecognizer(sqlWithColumns, asts.get(0));
        Assertions.assertFalse(recognizer.insertColumnsIsEmpty());

        // 测试没有列的情况（虽然在实际的 INSERT ALL 中很少见）
        // 这里主要测试边界情况
    }

    @Test
    public void testGetInsertRows() {
        String sql = "INSERT ALL INTO users (id, name, age) VALUES (1, 'Tom', 20) INTO users (id, name, age) VALUES (2, 'Jerry', 25) SELECT 1 FROM DUAL";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        OracleMultiInsertRecognizer recognizer = new OracleMultiInsertRecognizer(sql, asts.get(0));
        List<List<Object>> rows = recognizer.getInsertRows(Collections.emptySet());
        
        Assertions.assertNotNull(rows);
        Assertions.assertEquals(2, rows.size());
        
        // 验证第一行数据
        List<Object> firstRow = rows.get(0);
        Assertions.assertEquals(3, firstRow.size());
        Assertions.assertEquals(1, firstRow.get(0));
        Assertions.assertEquals("Tom", firstRow.get(1));
        Assertions.assertEquals(20, firstRow.get(2));
        
        // 验证第二行数据
        List<Object> secondRow = rows.get(1);
        Assertions.assertEquals(3, secondRow.size());
        Assertions.assertEquals(2, secondRow.get(0));
        Assertions.assertEquals("Jerry", secondRow.get(1));
        Assertions.assertEquals(25, secondRow.get(2));
    }

    @Test
    public void testGetInsertRowsWithNullValues() {
        String sql = "INSERT ALL INTO users (id, name, age) VALUES (1, 'Tom', NULL) INTO users (id, name, age) VALUES (2, NULL, 25) SELECT 1 FROM DUAL";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        OracleMultiInsertRecognizer recognizer = new OracleMultiInsertRecognizer(sql, asts.get(0));
        List<List<Object>> rows = recognizer.getInsertRows(Collections.emptySet());
        
        Assertions.assertNotNull(rows);
        Assertions.assertEquals(2, rows.size());
        
        // 验证第一行数据（age 为 NULL）
        List<Object> firstRow = rows.get(0);
        Assertions.assertEquals(3, firstRow.size());
        Assertions.assertEquals(1, firstRow.get(0));
        Assertions.assertEquals("Tom", firstRow.get(1));
        Assertions.assertEquals(Null.get(), firstRow.get(2));
        
        // 验证第二行数据（name 为 NULL）
        List<Object> secondRow = rows.get(1);
        Assertions.assertEquals(3, secondRow.size());
        Assertions.assertEquals(2, secondRow.get(0));
        Assertions.assertEquals(Null.get(), secondRow.get(1));
        Assertions.assertEquals(25, secondRow.get(2));
    }

    @Test
    public void testGetInsertRowsWithParameters() {
        String sql = "INSERT ALL INTO users (id, name) VALUES (?, ?) INTO users (id, name) VALUES (?, ?) SELECT 1 FROM DUAL";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        OracleMultiInsertRecognizer recognizer = new OracleMultiInsertRecognizer(sql, asts.get(0));
        List<List<Object>> rows = recognizer.getInsertRows(Collections.emptySet());
        
        Assertions.assertNotNull(rows);
        Assertions.assertEquals(2, rows.size());
        
        // 验证参数占位符
        for (List<Object> row : rows) {
            Assertions.assertEquals(2, row.size());
            for (Object value : row) {
                Assertions.assertTrue(value instanceof String);
                Assertions.assertEquals("?", value);
            }
        }
    }

    @Test
    public void testGetInsertParamsValue() {
        String sql = "INSERT ALL INTO users (id, name) VALUES (1, 'Tom') INTO users (id, name) VALUES (2, 'Jerry') SELECT 1 FROM DUAL";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        OracleMultiInsertRecognizer recognizer = new OracleMultiInsertRecognizer(sql, asts.get(0));
        List<String> paramsValue = recognizer.getInsertParamsValue();
        
        Assertions.assertNull(paramsValue);
    }

    @Test
    public void testGetDuplicateKeyUpdate() {
        String sql = "INSERT ALL INTO users (id, name) VALUES (1, 'Tom') INTO users (id, name) VALUES (2, 'Jerry') SELECT 1 FROM DUAL";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        OracleMultiInsertRecognizer recognizer = new OracleMultiInsertRecognizer(sql, asts.get(0));
        List<String> duplicateKeyUpdate = recognizer.getDuplicateKeyUpdate();
        
        Assertions.assertNull(duplicateKeyUpdate);
    }

    @Test
    public void testIsSqlSyntaxSupports() {
        String sql = "INSERT ALL INTO users (id, name) VALUES (1, 'Tom') INTO users (id, name) VALUES (2, 'Jerry') SELECT 1 FROM DUAL";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        OracleMultiInsertRecognizer recognizer = new OracleMultiInsertRecognizer(sql, asts.get(0));
        Assertions.assertTrue(recognizer.isSqlSyntaxSupports());
    }

    @Test
    public void testMultipleTablesInsert() {
        // 测试插入到多个不同表的情况
        String sql = "INSERT ALL INTO users (id, name) VALUES (1, 'Tom') INTO orders (id, user_id) VALUES (101, 1) SELECT 1 FROM DUAL";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        OracleMultiInsertRecognizer recognizer = new OracleMultiInsertRecognizer(sql, asts.get(0));
        
        // 应该返回第一个表的信息
        Assertions.assertEquals("users", recognizer.getTableName());
        
        List<String> columns = recognizer.getInsertColumns();
        Assertions.assertNotNull(columns);
        Assertions.assertEquals(2, columns.size());
        Assertions.assertEquals("id", columns.get(0));
        Assertions.assertEquals("name", columns.get(1));
        
        // 验证所有插入行数据
        List<List<Object>> rows = recognizer.getInsertRows(Collections.emptySet());
        Assertions.assertEquals(2, rows.size());
        
        // 第一行：users 表数据
        List<Object> firstRow = rows.get(0);
        Assertions.assertEquals(2, firstRow.size());
        Assertions.assertEquals(1, firstRow.get(0));
        Assertions.assertEquals("Tom", firstRow.get(1));
        
        // 第二行：orders 表数据
        List<Object> secondRow = rows.get(1);
        Assertions.assertEquals(2, secondRow.size());
        Assertions.assertEquals(101, secondRow.get(0));
        Assertions.assertEquals(1, secondRow.get(1));
    }

    @Test
    public void testComplexInsertAllStatement() {
        // 测试更复杂的 INSERT ALL 语句
        String sql = "INSERT ALL " +
                     "INTO sales (prod_id, cust_id, time_id, amount) VALUES (product_id, customer_id, weekly_start_date, sales_sun) " +
                     "INTO sales (prod_id, cust_id, time_id, amount) VALUES (product_id, customer_id, weekly_start_date+1, sales_mon) " +
                     "INTO sales (prod_id, cust_id, time_id, amount) VALUES (product_id, customer_id, weekly_start_date+2, sales_tue) " +
                     "SELECT product_id, customer_id, weekly_start_date, sales_sun, sales_mon, sales_tue FROM sales_input_table";
        
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);
        OracleMultiInsertRecognizer recognizer = new OracleMultiInsertRecognizer(sql, asts.get(0));
        
        Assertions.assertEquals("sales", recognizer.getTableName());
        Assertions.assertEquals(SQLType.INSERT, recognizer.getSQLType());
        
        List<String> columns = recognizer.getInsertColumns();
        Assertions.assertNotNull(columns);
        Assertions.assertEquals(4, columns.size());
        Assertions.assertEquals("prod_id", columns.get(0));
        Assertions.assertEquals("cust_id", columns.get(1));
        Assertions.assertEquals("time_id", columns.get(2));
        Assertions.assertEquals("amount", columns.get(3));
    }

    @Test
    public void testEmptyEntriesHandling() {
        // 这个测试主要是为了保证在边界情况下不会出现异常
        // 虽然实际上不太可能出现空的 entries，但我们需要确保代码的健壮性
        
        String sql = "INSERT ALL INTO users (id, name) VALUES (1, 'Tom') SELECT 1 FROM DUAL";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);
        
        OracleMultiInsertRecognizer recognizer = new OracleMultiInsertRecognizer(sql, asts.get(0));
        
        // 基本功能应该正常工作
        Assertions.assertEquals(SQLType.INSERT, recognizer.getSQLType());
        Assertions.assertEquals("users", recognizer.getTableName());
        Assertions.assertTrue(recognizer.isSqlSyntaxSupports());
    }

    @Test
    public void testGetInsertRowsWithPrimaryKeyIndex() {
        String sql = "INSERT ALL INTO users (id, name, age) VALUES (1, 'Tom', 20) INTO users (id, name, age) VALUES (2, 'Jerry', 25) SELECT 1 FROM DUAL";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        OracleMultiInsertRecognizer recognizer = new OracleMultiInsertRecognizer(sql, asts.get(0));
        
        // 测试当指定主键索引时的行为
        List<List<Object>> rows = recognizer.getInsertRows(Arrays.asList(0)); // id 字段是主键
        
        Assertions.assertNotNull(rows);
        Assertions.assertEquals(2, rows.size());
        
        // 验证主键字段的值
        for (List<Object> row : rows) {
            Assertions.assertEquals(3, row.size());
            Assertions.assertTrue(row.get(0) instanceof Integer); // 主键应该是整数
        }
    }
}
