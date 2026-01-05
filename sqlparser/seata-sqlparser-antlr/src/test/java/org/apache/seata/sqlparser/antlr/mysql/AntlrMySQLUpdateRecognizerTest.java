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
 * Unit tests for AntlrMySQLUpdateRecognizer
 */
public class AntlrMySQLUpdateRecognizerTest {

    /**
     * Test simple UPDATE statement
     */
    @Test
    public void testSimpleUpdate() {
        String sql = "UPDATE t1 SET name = 'John' WHERE id = 1";
        AntlrMySQLUpdateRecognizer recognizer = new AntlrMySQLUpdateRecognizer(sql);

        Assertions.assertEquals("t1", recognizer.getTableName());
        Assertions.assertEquals("id = 1", recognizer.getWhereCondition());
        Assertions.assertEquals(SQLType.UPDATE, recognizer.getSQLType());
        Assertions.assertEquals(sql, recognizer.getOriginalSQL());
    }

    /**
     * Test getting update columns
     */
    @Test
    public void testGetUpdateColumns() {
        String sql = "UPDATE t1 SET name = 'John', age = 30 WHERE id = 1";
        AntlrMySQLUpdateRecognizer recognizer = new AntlrMySQLUpdateRecognizer(sql);

        List<String> columns = recognizer.getUpdateColumns();
        Assertions.assertNotNull(columns);
        Assertions.assertEquals(2, columns.size());
        Assertions.assertTrue(columns.contains("name"));
        Assertions.assertTrue(columns.contains("age"));
    }

    /**
     * Test getting update values
     */
    @Test
    public void testGetUpdateValues() {
        String sql = "UPDATE t1 SET name = 'John', age = 30 WHERE id = 1";
        AntlrMySQLUpdateRecognizer recognizer = new AntlrMySQLUpdateRecognizer(sql);

        List<Object> values = recognizer.getUpdateValues();
        Assertions.assertNotNull(values);
        Assertions.assertEquals(2, values.size());
    }

    /**
     * Test single column update
     */
    @Test
    public void testSingleColumnUpdate() {
        String sql = "UPDATE t1 SET status = 'active' WHERE id = 1";
        AntlrMySQLUpdateRecognizer recognizer = new AntlrMySQLUpdateRecognizer(sql);

        List<String> columns = recognizer.getUpdateColumns();
        Assertions.assertEquals(1, columns.size());
        Assertions.assertEquals("status", columns.get(0));

        List<Object> values = recognizer.getUpdateValues();
        Assertions.assertEquals(1, values.size());
    }

    /**
     * Test UPDATE statement with alias
     */
    @Test
    public void testUpdateWithAlias() {
        String sql = "UPDATE t1 AS t SET t.name = 'John' WHERE t.id = 1";
        AntlrMySQLUpdateRecognizer recognizer = new AntlrMySQLUpdateRecognizer(sql);

        Assertions.assertEquals("t1", recognizer.getTableName());
        Assertions.assertEquals("t", recognizer.getTableAlias());
        Assertions.assertNotNull(recognizer.getWhereCondition());
        Assertions.assertEquals(SQLType.UPDATE, recognizer.getSQLType());
    }

    /**
     * Test UPDATE statement with placeholder
     */
    @Test
    public void testUpdateWithPlaceholder() {
        String sql = "UPDATE t1 SET name = ? WHERE id = ?";
        AntlrMySQLUpdateRecognizer recognizer = new AntlrMySQLUpdateRecognizer(sql);

        Assertions.assertEquals("t1", recognizer.getTableName());
        List<String> columns = recognizer.getUpdateColumns();
        Assertions.assertEquals(1, columns.size());
        Assertions.assertEquals("name", columns.get(0));
    }

    /**
     * Test UPDATE statement with IN condition
     */
    @Test
    public void testUpdateWithInCondition() {
        String sql = "UPDATE t1 SET status = 'inactive' WHERE id IN (1, 2, 3)";
        AntlrMySQLUpdateRecognizer recognizer = new AntlrMySQLUpdateRecognizer(sql);

        Assertions.assertEquals("t1", recognizer.getTableName());
        Assertions.assertNotNull(recognizer.getWhereCondition());
        Assertions.assertTrue(recognizer.getWhereCondition().toLowerCase().contains("in"));
    }

    /**
     * Test UPDATE statement with BETWEEN condition
     */
    @Test
    public void testUpdateWithBetweenCondition() {
        String sql = "UPDATE t1 SET status = 'processed' WHERE id BETWEEN 1 AND 10";
        AntlrMySQLUpdateRecognizer recognizer = new AntlrMySQLUpdateRecognizer(sql);

        Assertions.assertEquals("t1", recognizer.getTableName());
        Assertions.assertNotNull(recognizer.getWhereCondition());
        Assertions.assertTrue(recognizer.getWhereCondition().toLowerCase().contains("between"));
    }

    /**
     * Test multiple columns update
     */
    @Test
    public void testMultipleColumnsUpdate() {
        String sql = "UPDATE t1 SET name = 'John', age = 30, city = 'NYC', status = 'active' WHERE id = 1";
        AntlrMySQLUpdateRecognizer recognizer = new AntlrMySQLUpdateRecognizer(sql);

        List<String> columns = recognizer.getUpdateColumns();
        Assertions.assertEquals(4, columns.size());
        Assertions.assertTrue(columns.contains("name"));
        Assertions.assertTrue(columns.contains("age"));
        Assertions.assertTrue(columns.contains("city"));
        Assertions.assertTrue(columns.contains("status"));
    }

    /**
     * Test UPDATE with multiple conditions
     */
    @Test
    public void testUpdateWithMultipleConditions() {
        String sql = "UPDATE t1 SET status = 'active' WHERE id > 10 AND age < 50";
        AntlrMySQLUpdateRecognizer recognizer = new AntlrMySQLUpdateRecognizer(sql);

        Assertions.assertEquals("t1", recognizer.getTableName());
        Assertions.assertNotNull(recognizer.getWhereCondition());
        Assertions.assertTrue(recognizer.getWhereCondition().contains("AND"));
    }

    /**
     * Test getLimitCondition method (should return null)
     */
    @Test
    public void testGetLimitCondition() {
        String sql = "UPDATE t1 SET name = 'John' WHERE id = 1";
        AntlrMySQLUpdateRecognizer recognizer = new AntlrMySQLUpdateRecognizer(sql);

        Assertions.assertNull(recognizer.getLimitCondition());
        Assertions.assertNull(recognizer.getLimitCondition(null, null));
    }

    /**
     * Test getOrderByCondition method (should return null)
     */
    @Test
    public void testGetOrderByCondition() {
        String sql = "UPDATE t1 SET name = 'John' WHERE id = 1";
        AntlrMySQLUpdateRecognizer recognizer = new AntlrMySQLUpdateRecognizer(sql);

        Assertions.assertNull(recognizer.getOrderByCondition());
        Assertions.assertNull(recognizer.getOrderByCondition(null, null));
    }

    /**
     * Test UPDATE with backtick table name
     */
    @Test
    public void testUpdateWithBacktickTableName() {
        String sql = "UPDATE `user_table` SET name = 'John' WHERE id = 1";
        AntlrMySQLUpdateRecognizer recognizer = new AntlrMySQLUpdateRecognizer(sql);

        Assertions.assertNotNull(recognizer.getTableName());
        Assertions.assertEquals(SQLType.UPDATE, recognizer.getSQLType());
    }

    /**
     * Test UPDATE with table prefix in column names
     */
    @Test
    public void testUpdateWithTablePrefix() {
        String sql = "UPDATE t1 SET t1.name = 'John' WHERE t1.id = 1";
        AntlrMySQLUpdateRecognizer recognizer = new AntlrMySQLUpdateRecognizer(sql);

        Assertions.assertEquals("t1", recognizer.getTableName());
        List<String> columns = recognizer.getUpdateColumns();
        Assertions.assertEquals(1, columns.size());
    }

    /**
     * Test UPDATE with NULL value
     */
    @Test
    public void testUpdateWithNullValue() {
        String sql = "UPDATE t1 SET name = NULL WHERE id = 1";
        AntlrMySQLUpdateRecognizer recognizer = new AntlrMySQLUpdateRecognizer(sql);

        List<String> columns = recognizer.getUpdateColumns();
        Assertions.assertEquals(1, columns.size());
        Assertions.assertEquals("name", columns.get(0));
    }

    /**
     * Test UPDATE with expression value
     */
    @Test
    public void testUpdateWithExpression() {
        String sql = "UPDATE t1 SET age = age + 1 WHERE id = 1";
        AntlrMySQLUpdateRecognizer recognizer = new AntlrMySQLUpdateRecognizer(sql);

        List<String> columns = recognizer.getUpdateColumns();
        Assertions.assertEquals(1, columns.size());
        Assertions.assertEquals("age", columns.get(0));
    }

    /**
     * Test getWhereCondition overloaded methods
     */
    @Test
    public void testGetWhereConditionOverloads() {
        String sql = "UPDATE t1 SET name = 'John' WHERE id = 1";
        AntlrMySQLUpdateRecognizer recognizer = new AntlrMySQLUpdateRecognizer(sql);

        String whereCondition1 = recognizer.getWhereCondition();
        String whereCondition2 = recognizer.getWhereCondition(null, null);

        Assertions.assertEquals(whereCondition1, whereCondition2);
        Assertions.assertEquals("id = 1", whereCondition1);
    }

    /**
     * Test complex UPDATE statement
     */
    @Test
    public void testComplexUpdate() {
        String sql =
                "UPDATE users AS u SET u.status = 'active', u.last_login = NOW() WHERE u.id IN (1, 2, 3) AND u.deleted = 0";
        AntlrMySQLUpdateRecognizer recognizer = new AntlrMySQLUpdateRecognizer(sql);

        Assertions.assertEquals("users", recognizer.getTableName());
        Assertions.assertEquals("u", recognizer.getTableAlias());
        Assertions.assertEquals(SQLType.UPDATE, recognizer.getSQLType());

        List<String> columns = recognizer.getUpdateColumns();
        Assertions.assertNotNull(columns);
        Assertions.assertTrue(columns.size() >= 1);
    }
}
