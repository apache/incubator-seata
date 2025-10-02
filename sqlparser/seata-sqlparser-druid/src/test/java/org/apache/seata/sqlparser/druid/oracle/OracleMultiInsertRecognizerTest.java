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
import org.apache.seata.common.exception.NotSupportYetException;
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
        // Test the case without alias
        String sql = "INSERT ALL INTO users (id) VALUES (1) INTO users (id) VALUES (2) SELECT 1 FROM DUAL";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        OracleMultiInsertRecognizer recognizer = new OracleMultiInsertRecognizer(sql, asts.get(0));
        Assertions.assertNull(recognizer.getTableAlias());
    }

    @Test
    public void testGetInsertColumns() {
        String sql =
                "INSERT ALL INTO users (id, name, age) VALUES (1, 'Tom', 20) INTO users (id, name, age) VALUES (2, 'Jerry', 25) SELECT 1 FROM DUAL";
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
        // Test the case with columns
        String sqlWithColumns =
                "INSERT ALL INTO users (id, name) VALUES (1, 'Tom') INTO users (id, name) VALUES (2, 'Jerry') SELECT 1 FROM DUAL";
        List<SQLStatement> asts = SQLUtils.parseStatements(sqlWithColumns, DB_TYPE);
        OracleMultiInsertRecognizer recognizer = new OracleMultiInsertRecognizer(sqlWithColumns, asts.get(0));
        Assertions.assertFalse(recognizer.insertColumnsIsEmpty());
    }

    @Test
    public void testGetInsertRows() {
        String sql =
                "INSERT ALL INTO users (id, name, age) VALUES (1, 'Tom', 20) INTO users (id, name, age) VALUES (2, 'Jerry', 25) SELECT 1 FROM DUAL";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        OracleMultiInsertRecognizer recognizer = new OracleMultiInsertRecognizer(sql, asts.get(0));
        List<List<Object>> rows = recognizer.getInsertRows(Collections.emptySet());

        Assertions.assertNotNull(rows);
        Assertions.assertEquals(2, rows.size());

        // Verify the first row of data
        List<Object> firstRow = rows.get(0);
        Assertions.assertEquals(3, firstRow.size());
        Assertions.assertEquals(1, firstRow.get(0));
        Assertions.assertEquals("Tom", firstRow.get(1));
        Assertions.assertEquals(20, firstRow.get(2));

        // Verify the second row of data
        List<Object> secondRow = rows.get(1);
        Assertions.assertEquals(3, secondRow.size());
        Assertions.assertEquals(2, secondRow.get(0));
        Assertions.assertEquals("Jerry", secondRow.get(1));
        Assertions.assertEquals(25, secondRow.get(2));
    }

    @Test
    public void testGetInsertRowsWithNullValues() {
        String sql =
                "INSERT ALL INTO users (id, name, age) VALUES (1, 'Tom', NULL) INTO users (id, name, age) VALUES (2, NULL, 25) SELECT 1 FROM DUAL";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        OracleMultiInsertRecognizer recognizer = new OracleMultiInsertRecognizer(sql, asts.get(0));
        List<List<Object>> rows = recognizer.getInsertRows(Collections.emptySet());

        Assertions.assertNotNull(rows);
        Assertions.assertEquals(2, rows.size());

        // Verify the first row of data (age is NULL)
        List<Object> firstRow = rows.get(0);
        Assertions.assertEquals(3, firstRow.size());
        Assertions.assertEquals(1, firstRow.get(0));
        Assertions.assertEquals("Tom", firstRow.get(1));
        Assertions.assertEquals(Null.get(), firstRow.get(2));

        // Verify the second row of data (name is NULL)
        List<Object> secondRow = rows.get(1);
        Assertions.assertEquals(3, secondRow.size());
        Assertions.assertEquals(2, secondRow.get(0));
        Assertions.assertEquals(Null.get(), secondRow.get(1));
        Assertions.assertEquals(25, secondRow.get(2));
    }

    @Test
    public void testGetInsertRowsWithParameters() {
        String sql =
                "INSERT ALL INTO users (id, name) VALUES (?, ?) INTO users (id, name) VALUES (?, ?) SELECT 1 FROM DUAL";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        OracleMultiInsertRecognizer recognizer = new OracleMultiInsertRecognizer(sql, asts.get(0));
        List<List<Object>> rows = recognizer.getInsertRows(Collections.emptySet());

        Assertions.assertNotNull(rows);
        Assertions.assertEquals(2, rows.size());

        // Validation parameter placeholders
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
        String sql =
                "INSERT ALL INTO users (id, name) VALUES (1, 'Tom') INTO users (id, name) VALUES (2, 'Jerry') SELECT 1 FROM DUAL";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        OracleMultiInsertRecognizer recognizer = new OracleMultiInsertRecognizer(sql, asts.get(0));
        List<String> paramsValue = recognizer.getInsertParamsValue();

        Assertions.assertNull(paramsValue);
    }

    @Test
    public void testGetDuplicateKeyUpdate() {
        String sql =
                "INSERT ALL INTO users (id, name) VALUES (1, 'Tom') INTO users (id, name) VALUES (2, 'Jerry') SELECT 1 FROM DUAL";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        OracleMultiInsertRecognizer recognizer = new OracleMultiInsertRecognizer(sql, asts.get(0));
        List<String> duplicateKeyUpdate = recognizer.getDuplicateKeyUpdate();

        Assertions.assertNull(duplicateKeyUpdate);
    }

    @Test
    public void testIsSqlSyntaxSupports() {
        String sql =
                "INSERT ALL INTO users (id, name) VALUES (1, 'Tom') INTO users (id, name) VALUES (2, 'Jerry') SELECT 1 FROM DUAL";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        OracleMultiInsertRecognizer recognizer = new OracleMultiInsertRecognizer(sql, asts.get(0));
        Assertions.assertTrue(recognizer.isSqlSyntaxSupports());
    }

    @Test
    public void testMultipleTablesInsert() {
        // Now this should throw an exception since different tables are involved
        String sql =
                "INSERT ALL INTO users (id, name) VALUES (1, 'Tom') INTO orders (id, user_id) VALUES (101, 1) SELECT 1 FROM DUAL";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        // should throw NotSupportYetException
        NotSupportYetException exception = Assertions.assertThrows(
                NotSupportYetException.class, () -> new OracleMultiInsertRecognizer(sql, asts.get(0)));

        Assertions.assertTrue(
                exception.getMessage().contains("Oracle Multi Insert with different tables is not supported yet"));
        Assertions.assertTrue(exception.getMessage().contains("users"));
        Assertions.assertTrue(exception.getMessage().contains("orders"));
    }

    @Test
    public void testMultipleTablesWithDifferentColumns() {
        // Testing the same table but different column definitions
        String sql =
                "INSERT ALL INTO users (id, name) VALUES (1, 'Tom') INTO users (id, name, age) VALUES (2, 'Jerry', 25) SELECT 1 FROM DUAL";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        // should throw NotSupportYetException
        NotSupportYetException exception = Assertions.assertThrows(
                NotSupportYetException.class, () -> new OracleMultiInsertRecognizer(sql, asts.get(0)));

        Assertions.assertTrue(exception
                .getMessage()
                .contains("Oracle Multi Insert with different column definitions is not supported yet"));
        Assertions.assertTrue(exception.getMessage().contains("users"));
    }

    @Test
    public void testSameTableMultipleInserts() {
        // Test the normal situation of the same table and column
        String sql =
                "INSERT ALL INTO users (id, name) VALUES (1, 'Tom') INTO users (id, name) VALUES (2, 'Jerry') SELECT 1 FROM DUAL";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        // success
        OracleMultiInsertRecognizer recognizer = new OracleMultiInsertRecognizer(sql, asts.get(0));

        Assertions.assertEquals("users", recognizer.getTableName());
        Assertions.assertEquals(2, recognizer.getInsertColumns().size());
        Assertions.assertEquals("id", recognizer.getInsertColumns().get(0));
        Assertions.assertEquals("name", recognizer.getInsertColumns().get(1));

        List<List<Object>> rows = recognizer.getInsertRows(Collections.emptySet());
        Assertions.assertEquals(2, rows.size());
    }

    @Test
    public void testEmptyColumnsConsistency() {
        // Tests the consistency of empty column definitions
        String sql = "INSERT ALL INTO users VALUES (1, 'Tom') INTO users VALUES (2, 'Jerry') SELECT 1 FROM DUAL";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        // This should succeed since neither entry specifies a column.
        OracleMultiInsertRecognizer recognizer = new OracleMultiInsertRecognizer(sql, asts.get(0));

        Assertions.assertEquals("users", recognizer.getTableName());
        Assertions.assertTrue(recognizer.insertColumnsIsEmpty());
    }

    @Test
    public void testMixedColumnDefinitions() {
        // Test one case with column definition and one case without column definition
        String sql =
                "INSERT ALL INTO users (id, name) VALUES (1, 'Tom') INTO users VALUES (2, 'Jerry') SELECT 1 FROM DUAL";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        // An exception should be thrown because the column definitions are inconsistent
        NotSupportYetException exception = Assertions.assertThrows(
                NotSupportYetException.class, () -> new OracleMultiInsertRecognizer(sql, asts.get(0)));

        Assertions.assertTrue(exception
                .getMessage()
                .contains("Oracle Multi Insert with different column definitions is not supported yet"));
    }

    @Test
    public void testCaseInsensitiveTableNames() {
        // Test table name case sensitivity
        String sql =
                "INSERT ALL INTO users (id, name) VALUES (1, 'Tom') INTO USERS (id, name) VALUES (2, 'Jerry') SELECT 1 FROM DUAL";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        // This should succeed because equalsIgnoreCase is used
        OracleMultiInsertRecognizer recognizer = new OracleMultiInsertRecognizer(sql, asts.get(0));

        Assertions.assertEquals("users", recognizer.getTableName());
        Assertions.assertEquals(2, recognizer.getInsertColumns().size());
    }

    @Test
    public void testTableWithSchema() {
        // Test table name with Schema
        String sql =
                "INSERT ALL INTO schema1.users (id, name) VALUES (1, 'Tom') INTO schema1.users (id, name) VALUES (2, 'Jerry') SELECT 1 FROM DUAL";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        // success
        OracleMultiInsertRecognizer recognizer = new OracleMultiInsertRecognizer(sql, asts.get(0));

        Assertions.assertEquals("schema1.users", recognizer.getTableName());
        Assertions.assertEquals(2, recognizer.getInsertColumns().size());
    }

    @Test
    public void testDifferentSchemas() {
        // Testing tables with the same name in different schemas
        String sql =
                "INSERT ALL INTO schema1.users (id, name) VALUES (1, 'Tom') INTO schema2.users (id, name) VALUES (2, 'Jerry') SELECT 1 FROM DUAL";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        // An exception should be thrown because they are different tables
        NotSupportYetException exception = Assertions.assertThrows(
                NotSupportYetException.class, () -> new OracleMultiInsertRecognizer(sql, asts.get(0)));

        Assertions.assertTrue(
                exception.getMessage().contains("Oracle Multi Insert with different tables is not supported yet"));
        Assertions.assertTrue(exception.getMessage().contains("schema1.users"));
        Assertions.assertTrue(exception.getMessage().contains("schema2.users"));
    }

    @Test
    public void testComplexInsertAllStatement() {
        // Testing a more complex INSERT ALL statement
        String sql = "INSERT ALL "
                + "INTO sales (prod_id, cust_id, time_id, amount) VALUES (product_id, customer_id, weekly_start_date, sales_sun) "
                + "INTO sales (prod_id, cust_id, time_id, amount) VALUES (product_id, customer_id, weekly_start_date+1, sales_mon) "
                + "INTO sales (prod_id, cust_id, time_id, amount) VALUES (product_id, customer_id, weekly_start_date+2, sales_tue) "
                + "SELECT product_id, customer_id, weekly_start_date, sales_sun, sales_mon, sales_tue FROM sales_input_table";

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
        // This test is mainly to ensure that no exceptions occur in boundary conditions

        String sql = "INSERT ALL INTO users (id, name) VALUES (1, 'Tom') SELECT 1 FROM DUAL";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        OracleMultiInsertRecognizer recognizer = new OracleMultiInsertRecognizer(sql, asts.get(0));

        Assertions.assertEquals(SQLType.INSERT, recognizer.getSQLType());
        Assertions.assertEquals("users", recognizer.getTableName());
        Assertions.assertTrue(recognizer.isSqlSyntaxSupports());
    }

    @Test
    public void testGetInsertRowsWithPrimaryKeyIndex() {
        String sql =
                "INSERT ALL INTO users (id, name, age) VALUES (1, 'Tom', 20) INTO users (id, name, age) VALUES (2, 'Jerry', 25) SELECT 1 FROM DUAL";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        OracleMultiInsertRecognizer recognizer = new OracleMultiInsertRecognizer(sql, asts.get(0));

        // Test behavior when primary key index is specified
        List<List<Object>> rows = recognizer.getInsertRows(Arrays.asList(0));

        Assertions.assertNotNull(rows);
        Assertions.assertEquals(2, rows.size());

        // Validate the value of the primary key field
        for (List<Object> row : rows) {
            Assertions.assertEquals(3, row.size());
            Assertions.assertTrue(row.get(0) instanceof Integer);
        }
    }

    @Test
    public void testConditionalInsertNotSupported() {
        // Testing conditional insert statements
        String sql = "INSERT ALL " + "INTO users (id, name) VALUES (1, 'Tom') "
                + "WHEN salary > 1000 THEN INTO employees (id, name) VALUES (2, 'Jerry') "
                + "SELECT 1 FROM DUAL";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        // Should throw NotSupportYetException
        NotSupportYetException exception = Assertions.assertThrows(
                NotSupportYetException.class, () -> new OracleMultiInsertRecognizer(sql, asts.get(0)));

        Assertions.assertTrue(exception.getMessage().contains("conditional clauses"));
        Assertions.assertTrue(exception.getMessage().contains("WHEN...THEN"));
    }
}
