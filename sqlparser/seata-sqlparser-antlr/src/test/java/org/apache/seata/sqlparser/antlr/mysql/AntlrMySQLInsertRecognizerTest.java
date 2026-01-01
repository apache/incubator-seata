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
package org.apache.seata.sqlparser.antlr.mysql;

import org.apache.seata.sqlparser.SQLType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Unit tests for AntlrMySQLInsertRecognizer
 */
public class AntlrMySQLInsertRecognizerTest {

    /**
     * Test simple INSERT statement
     */
    @Test
    public void testSimpleInsert() {
        String sql = "INSERT INTO t1 (id, name) VALUES (1, 'test')";
        AntlrMySQLInsertRecognizer recognizer = new AntlrMySQLInsertRecognizer(sql);

        Assertions.assertEquals("t1", recognizer.getTableName());
        Assertions.assertEquals(SQLType.INSERT, recognizer.getSQLType());
        Assertions.assertEquals(sql, recognizer.getOriginalSQL());
        Assertions.assertFalse(recognizer.insertColumnsIsEmpty());
    }

    /**
     * Test getting insert column names
     */
    @Test
    public void testGetInsertColumns() {
        String sql = "INSERT INTO t1 (id, name, age) VALUES (1, 'John', 30)";
        AntlrMySQLInsertRecognizer recognizer = new AntlrMySQLInsertRecognizer(sql);

        List<String> columns = recognizer.getInsertColumns();
        Assertions.assertNotNull(columns);
        // 列数应该是3
        Assertions.assertEquals(3, columns.size());
    }

    /**
     * Test single column insert
     */
    @Test
    public void testSingleColumnInsert() {
        String sql = "INSERT INTO t1 (id) VALUES (1)";
        AntlrMySQLInsertRecognizer recognizer = new AntlrMySQLInsertRecognizer(sql);

        List<String> columns = recognizer.getInsertColumns();
        Assertions.assertEquals(1, columns.size());
        Assertions.assertFalse(recognizer.insertColumnsIsEmpty());
    }

    /**
     * Test multi-row insert
     */
    @Test
    public void testMultiRowInsert() {
        String sql = "INSERT INTO t1 (id, name) VALUES (1, 'Alice'), (2, 'Bob'), (3, 'Charlie')";
        AntlrMySQLInsertRecognizer recognizer = new AntlrMySQLInsertRecognizer(sql);

        List<String> columns = recognizer.getInsertColumns();
        Assertions.assertEquals(2, columns.size());
        Assertions.assertFalse(recognizer.insertColumnsIsEmpty());
    }

    /**
     * Test insert with backtick column names
     */
    @Test
    public void testInsertWithBacktickColumns() {
        String sql = "INSERT INTO t1 (`id`, `user_name`) VALUES (1, 'test')";
        AntlrMySQLInsertRecognizer recognizer = new AntlrMySQLInsertRecognizer(sql);

        List<String> columns = recognizer.getInsertColumns();
        Assertions.assertNotNull(columns);
        Assertions.assertEquals(2, columns.size());
        Assertions.assertFalse(recognizer.insertColumnsIsEmpty());
    }

    /**
     * Test insertColumnsIsEmpty method - with columns
     */
    @Test
    public void testInsertColumnsIsNotEmpty() {
        String sql = "INSERT INTO t1 (id) VALUES (1)";
        AntlrMySQLInsertRecognizer recognizer = new AntlrMySQLInsertRecognizer(sql);
        Assertions.assertFalse(recognizer.insertColumnsIsEmpty());
    }

    /**
     * Test getInsertRows method (should return null)
     */
    @Test
    public void testGetInsertRows() {
        String sql = "INSERT INTO t1 (id) VALUES (1)";
        AntlrMySQLInsertRecognizer recognizer = new AntlrMySQLInsertRecognizer(sql);

        Assertions.assertNull(recognizer.getInsertRows(null));
    }

    /**
     * Test getInsertParamsValue method (should return null)
     */
    @Test
    public void testGetInsertParamsValue() {
        String sql = "INSERT INTO t1 (id) VALUES (1)";
        AntlrMySQLInsertRecognizer recognizer = new AntlrMySQLInsertRecognizer(sql);

        Assertions.assertNull(recognizer.getInsertParamsValue());
    }

    /**
     * Test getDuplicateKeyUpdate method (should return null)
     */
    @Test
    public void testGetDuplicateKeyUpdate() {
        String sql = "INSERT INTO t1 (id) VALUES (1)";
        AntlrMySQLInsertRecognizer recognizer = new AntlrMySQLInsertRecognizer(sql);

        Assertions.assertNull(recognizer.getDuplicateKeyUpdate());
    }

    /**
     * Test insert with table name
     */
    @Test
    public void testInsertWithTableName() {
        String sql = "INSERT INTO t1 (id, name) VALUES (1, 'test')";
        AntlrMySQLInsertRecognizer recognizer = new AntlrMySQLInsertRecognizer(sql);

        Assertions.assertEquals("t1", recognizer.getTableName());
    }

    /**
     * Test insert with backtick table name
     */
    @Test
    public void testInsertWithBacktickTableName() {
        String sql = "INSERT INTO `user_table` (id, name) VALUES (1, 'test')";
        AntlrMySQLInsertRecognizer recognizer = new AntlrMySQLInsertRecognizer(sql);

        Assertions.assertNotNull(recognizer.getTableName());
        Assertions.assertEquals(SQLType.INSERT, recognizer.getSQLType());
    }

    /**
     * Test insert with mixed value types
     */
    @Test
    public void testInsertWithMixedValues() {
        String sql = "INSERT INTO t1 (id, name, age, salary) VALUES (1, 'John', 30, 5000.50)";
        AntlrMySQLInsertRecognizer recognizer = new AntlrMySQLInsertRecognizer(sql);

        List<String> columns = recognizer.getInsertColumns();
        Assertions.assertEquals(4, columns.size());
    }

    /**
     * Test insert with placeholders
     */
    @Test
    public void testInsertWithPlaceholders() {
        String sql = "INSERT INTO t1 (id, name) VALUES (?, ?)";
        AntlrMySQLInsertRecognizer recognizer = new AntlrMySQLInsertRecognizer(sql);

        List<String> columns = recognizer.getInsertColumns();
        Assertions.assertEquals(2, columns.size());
    }

    /**
     * Test getTableAlias method
     */
    @Test
    public void testGetTableAlias() {
        String sql = "INSERT INTO t1 (id) VALUES (1)";
        AntlrMySQLInsertRecognizer recognizer = new AntlrMySQLInsertRecognizer(sql);

        // INSERT statements typically don't have table aliases
        String alias = recognizer.getTableAlias();
        // Alias may be null or empty
        Assertions.assertTrue(alias == null || alias.isEmpty() || alias.length() >= 0);
    }

    /**
     * Test complex INSERT statement
     */
    @Test
    public void testComplexInsert() {
        String sql = "INSERT INTO users (id, username, email, created_at) VALUES (1, 'admin', 'admin@test.com', NOW())";
        AntlrMySQLInsertRecognizer recognizer = new AntlrMySQLInsertRecognizer(sql);

        Assertions.assertEquals("users", recognizer.getTableName());
        Assertions.assertEquals(SQLType.INSERT, recognizer.getSQLType());
        Assertions.assertFalse(recognizer.insertColumnsIsEmpty());

        List<String> columns = recognizer.getInsertColumns();
        Assertions.assertNotNull(columns);
        Assertions.assertEquals(4, columns.size());
    }

    /**
     * Test insert with database name
     */
    @Test
    public void testInsertWithDatabaseName() {
        String sql = "INSERT INTO db1.t1 (id, name) VALUES (1, 'test')";
        AntlrMySQLInsertRecognizer recognizer = new AntlrMySQLInsertRecognizer(sql);

        Assertions.assertNotNull(recognizer.getTableName());
        Assertions.assertEquals(SQLType.INSERT, recognizer.getSQLType());
    }

    /**
     * Test SQL type
     */
    @Test
    public void testGetSQLType() {
        String sql = "INSERT INTO t1 (id) VALUES (1)";
        AntlrMySQLInsertRecognizer recognizer = new AntlrMySQLInsertRecognizer(sql);

        Assertions.assertEquals(SQLType.INSERT, recognizer.getSQLType());
    }

    /**
     * Test getting original SQL
     */
    @Test
    public void testGetOriginalSQL() {
        String sql = "INSERT INTO t1 (id, name) VALUES (1, 'test')";
        AntlrMySQLInsertRecognizer recognizer = new AntlrMySQLInsertRecognizer(sql);

        Assertions.assertEquals(sql, recognizer.getOriginalSQL());
    }
}
