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

/**
 * Unit tests for AntlrMySQLDeleteRecognizer
 */
public class AntlrMySQLDeleteRecognizerTest {

    /**
     * Test simple DELETE statement
     */
    @Test
    public void testSimpleDelete() {
        String sql = "DELETE FROM t1 WHERE id = 1";
        AntlrMySQLDeleteRecognizer recognizer = new AntlrMySQLDeleteRecognizer(sql);

        Assertions.assertEquals("t1", recognizer.getTableName());
        Assertions.assertEquals("id = 1", recognizer.getWhereCondition());
        Assertions.assertNull(recognizer.getTableAlias());
        Assertions.assertEquals(SQLType.DELETE, recognizer.getSQLType());
        Assertions.assertEquals(sql, recognizer.getOriginalSQL());
    }

    /**
     * Test DELETE statement with alias
     */
    @Test
    public void testDeleteWithAlias() {
        String sql = "DELETE t FROM t1 AS t WHERE t.id = 1";
        AntlrMySQLDeleteRecognizer recognizer = new AntlrMySQLDeleteRecognizer(sql);

        Assertions.assertEquals("t1", recognizer.getTableName());
        Assertions.assertEquals("t", recognizer.getTableAlias());
        Assertions.assertEquals("t.id = 1", recognizer.getWhereCondition());
        Assertions.assertEquals(SQLType.DELETE, recognizer.getSQLType());
    }

    /**
     * Test DELETE statement with string condition
     */
    @Test
    public void testDeleteWithStringCondition() {
        String sql = "DELETE FROM users WHERE name = 'John'";
        AntlrMySQLDeleteRecognizer recognizer = new AntlrMySQLDeleteRecognizer(sql);

        Assertions.assertEquals("users", recognizer.getTableName());
        Assertions.assertEquals("name = 'John'", recognizer.getWhereCondition());
        Assertions.assertEquals(SQLType.DELETE, recognizer.getSQLType());
    }

    /**
     * Test DELETE statement with placeholder
     */
    @Test
    public void testDeleteWithPlaceholder() {
        String sql = "DELETE FROM t1 WHERE id = ?";
        AntlrMySQLDeleteRecognizer recognizer = new AntlrMySQLDeleteRecognizer(sql);

        Assertions.assertEquals("t1", recognizer.getTableName());
        Assertions.assertEquals("id = ?", recognizer.getWhereCondition());
        Assertions.assertEquals(SQLType.DELETE, recognizer.getSQLType());
    }

    /**
     * Test DELETE statement with IN condition
     */
    @Test
    public void testDeleteWithInCondition() {
        String sql = "DELETE FROM t1 WHERE id IN (1, 2, 3)";
        AntlrMySQLDeleteRecognizer recognizer = new AntlrMySQLDeleteRecognizer(sql);

        Assertions.assertEquals("t1", recognizer.getTableName());
        Assertions.assertNotNull(recognizer.getWhereCondition());
        Assertions.assertTrue(recognizer.getWhereCondition().contains("IN"));
        Assertions.assertEquals(SQLType.DELETE, recognizer.getSQLType());
    }

    /**
     * Test DELETE statement with BETWEEN condition
     */
    @Test
    public void testDeleteWithBetweenCondition() {
        String sql = "DELETE FROM t1 WHERE id BETWEEN 1 AND 10";
        AntlrMySQLDeleteRecognizer recognizer = new AntlrMySQLDeleteRecognizer(sql);

        Assertions.assertEquals("t1", recognizer.getTableName());
        Assertions.assertNotNull(recognizer.getWhereCondition());
        Assertions.assertTrue(recognizer.getWhereCondition().toLowerCase().contains("between"));
        Assertions.assertEquals(SQLType.DELETE, recognizer.getSQLType());
    }

    /**
     * Test DELETE statement with multiple conditions
     */
    @Test
    public void testDeleteWithMultipleConditions() {
        String sql = "DELETE FROM t1 WHERE id > 10 AND status = 'active'";
        AntlrMySQLDeleteRecognizer recognizer = new AntlrMySQLDeleteRecognizer(sql);

        Assertions.assertEquals("t1", recognizer.getTableName());
        Assertions.assertNotNull(recognizer.getWhereCondition());
        Assertions.assertTrue(recognizer.getWhereCondition().contains("AND"));
        Assertions.assertEquals(SQLType.DELETE, recognizer.getSQLType());
    }

    /**
     * Test getLimitCondition method (should return null)
     */
    @Test
    public void testGetLimitCondition() {
        String sql = "DELETE FROM t1 WHERE id = 1";
        AntlrMySQLDeleteRecognizer recognizer = new AntlrMySQLDeleteRecognizer(sql);

        Assertions.assertNull(recognizer.getLimitCondition());
        Assertions.assertNull(recognizer.getLimitCondition(null, null));
    }

    /**
     * Test getOrderByCondition method (should return null)
     */
    @Test
    public void testGetOrderByCondition() {
        String sql = "DELETE FROM t1 WHERE id = 1";
        AntlrMySQLDeleteRecognizer recognizer = new AntlrMySQLDeleteRecognizer(sql);

        Assertions.assertNull(recognizer.getOrderByCondition());
        Assertions.assertNull(recognizer.getOrderByCondition(null, null));
    }

    /**
     * Test DELETE with backtick table name
     */
    @Test
    public void testDeleteWithBacktickTableName() {
        String sql = "DELETE FROM `table_name` WHERE id = 1";
        AntlrMySQLDeleteRecognizer recognizer = new AntlrMySQLDeleteRecognizer(sql);

        Assertions.assertNotNull(recognizer.getTableName());
        Assertions.assertEquals("id = 1", recognizer.getWhereCondition());
        Assertions.assertEquals(SQLType.DELETE, recognizer.getSQLType());
    }

    /**
     * Test getWhereCondition overloaded methods
     */
    @Test
    public void testGetWhereConditionOverloads() {
        String sql = "DELETE FROM t1 WHERE id = 1";
        AntlrMySQLDeleteRecognizer recognizer = new AntlrMySQLDeleteRecognizer(sql);

        String whereCondition1 = recognizer.getWhereCondition();
        String whereCondition2 = recognizer.getWhereCondition(null, null);

        Assertions.assertEquals(whereCondition1, whereCondition2);
        Assertions.assertEquals("id = 1", whereCondition1);
    }
}
