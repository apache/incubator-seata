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
package org.apache.seata.sqlparser.druid.sqlserver;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLDateExpr;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.expr.MySqlOrderingExpr;
import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.sqlparser.SQLParsingException;
import org.apache.seata.sqlparser.SQLType;
import org.apache.seata.sqlparser.druid.AbstractRecognizerTest;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type SqlServer insert recognizer test.
 *
 */
public class SqlServerInsertRecognizerTest extends AbstractRecognizerTest {

    private final int pkIndex = 0;

    @Override
    public String getDbType() {
        return JdbcConstants.SQLSERVER;
    }

    @Test
    public void testGetSqlType() {
        String sql = "INSERT INTO t (id) values (?)";
        SQLStatement ast = getSQLStatement(sql);

        SqlServerInsertRecognizer recognizer = new SqlServerInsertRecognizer(sql, ast);
        Assertions.assertEquals(recognizer.getSQLType(), SQLType.INSERT);
    }

    @Test
    public void testGetTableAlias() {
        String sql = "INSERT INTO t (id) values (?)";
        SQLStatement ast = getSQLStatement(sql);

        SqlServerInsertRecognizer recognizer = new SqlServerInsertRecognizer(sql, ast);
        Assertions.assertNull(recognizer.getTableAlias());

        sql = "INSERT INTO t t1 (id) values (?)";
        ast = getSQLStatement(sql);

        recognizer = new SqlServerInsertRecognizer(sql, ast);
        Assertions.assertEquals("t1", recognizer.getTableAlias());
    }

    /**
     * Insert recognizer test 0.(test with constant)
     */
    @Test
    public void insertRecognizerTest_0() {

        String sql = "INSERT INTO t (name) VALUES ('name1')";

        SQLStatement statement = getSQLStatement(sql);

        SqlServerInsertRecognizer sqlServerInsertRecognizer = new SqlServerInsertRecognizer(sql, statement);

        Assertions.assertEquals(sql, sqlServerInsertRecognizer.getOriginalSQL());
        Assertions.assertEquals("t", sqlServerInsertRecognizer.getTableName());
        Assertions.assertFalse(sqlServerInsertRecognizer.insertColumnsIsEmpty());
        Assertions.assertEquals(Collections.singletonList("name"), sqlServerInsertRecognizer.getInsertColumns());
        Assertions.assertEquals(1, sqlServerInsertRecognizer.getInsertRows(Collections.singletonList(pkIndex)).size());
        Assertions.assertEquals(Collections.singletonList("name1"), sqlServerInsertRecognizer.getInsertRows(Collections.singletonList(pkIndex)).get(0));
    }

    /**
     * Insert recognizer test 1.(test with multi constant)
     */
    @Test
    public void insertRecognizerTest_1() {

        String sql = "INSERT INTO t (name, age) VALUES ('name1', '18')";

        SQLStatement statement = getSQLStatement(sql);

        SqlServerInsertRecognizer sqlServerInsertRecognizer = new SqlServerInsertRecognizer(sql, statement);

        Assertions.assertEquals(sql, sqlServerInsertRecognizer.getOriginalSQL());
        Assertions.assertEquals("t", sqlServerInsertRecognizer.getTableName());
        Assertions.assertFalse(sqlServerInsertRecognizer.insertColumnsIsEmpty());
        Assertions.assertEquals(Arrays.asList("name", "age"), sqlServerInsertRecognizer.getInsertColumns());
        Assertions.assertEquals(1, sqlServerInsertRecognizer.getInsertRows(Collections.singletonList(pkIndex)).size());
        Assertions.assertEquals(Arrays.asList("name1", "18"), sqlServerInsertRecognizer.getInsertRows(Collections.singletonList(pkIndex)).get(0));
    }

    /**
     * Insert recognizer test 2.(test with multi values)
     */
    @Test
    public void insertRecognizerTest_2() {

        String sql = "INSERT INTO t (name, age) VALUES ('name1', '18'), ('name2', '19'), ('name3', '20')";

        SQLStatement statement = getSQLStatement(sql);

        SqlServerInsertRecognizer sqlServerInsertRecognizer = new SqlServerInsertRecognizer(sql, statement);

        Assertions.assertEquals(sql, sqlServerInsertRecognizer.getOriginalSQL());
        Assertions.assertEquals("t", sqlServerInsertRecognizer.getTableName());
        Assertions.assertFalse(sqlServerInsertRecognizer.insertColumnsIsEmpty());
        Assertions.assertEquals(Arrays.asList("name", "age"), sqlServerInsertRecognizer.getInsertColumns());
        Assertions.assertEquals(3, sqlServerInsertRecognizer.getInsertRows(Collections.singletonList(pkIndex)).size());
        Assertions.assertEquals(Arrays.asList(Arrays.asList("name1", "18"), Arrays.asList("name2", "19"), Arrays.asList("name3", "20")),
                sqlServerInsertRecognizer.getInsertRows(Collections.singletonList(pkIndex)));
    }

    /**
     * Insert recognizer test 3.(test with placeholder)
     */
    @Test
    public void insertRecognizerTest_3() {

        String sql = "INSERT INTO t (name) VALUES (?)";

        SQLStatement statement = getSQLStatement(sql);

        SqlServerInsertRecognizer sqlServerInsertRecognizer = new SqlServerInsertRecognizer(sql, statement);

        Assertions.assertEquals(sql, sqlServerInsertRecognizer.getOriginalSQL());
        Assertions.assertEquals("t", sqlServerInsertRecognizer.getTableName());
        Assertions.assertFalse(sqlServerInsertRecognizer.insertColumnsIsEmpty());
        Assertions.assertEquals(Collections.singletonList("name"), sqlServerInsertRecognizer.getInsertColumns());
        Assertions.assertEquals(1, sqlServerInsertRecognizer.getInsertRows(Collections.singletonList(pkIndex)).size());
        Assertions.assertEquals(Collections.singletonList("?"), sqlServerInsertRecognizer.getInsertRows(Collections.singletonList(pkIndex)).get(0));
    }


    @Test
    public void testGetInsertColumns() {

        //test for no column
        String sql = "insert into t values (?)";
        SQLStatement ast = getSQLStatement(sql);

        SqlServerInsertRecognizer recognizer = new SqlServerInsertRecognizer(sql, ast);
        List<String> insertColumns = recognizer.getInsertColumns();
        Assertions.assertNull(insertColumns);

        //test for normal
        sql = "insert into t(a) values (?)";
        ast = getSQLStatement(sql);

        recognizer = new SqlServerInsertRecognizer(sql, ast);
        insertColumns = recognizer.getInsertColumns();
        Assertions.assertEquals(1, insertColumns.size());
        Assertions.assertEquals(Collections.singletonList("a"), insertColumns);

        //test for exception
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String s = "insert into t(a) values (?)";
            SQLStatement sqlStatement = getSQLStatement(s);
            SQLInsertStatement sqlInsertStatement = (SQLInsertStatement) sqlStatement;
            sqlInsertStatement.getColumns().add(new SQLDateExpr());

            SqlServerInsertRecognizer sqlServerInsertRecognizer = new SqlServerInsertRecognizer(s, sqlInsertStatement);
            sqlServerInsertRecognizer.getInsertColumns();
        });
    }

    @Test
    public void testGetInsertRows() {
        //test for null value
        String sql = "insert into t(id, no, name, age, time) values (default, null, 'a', ?, now())";
        SQLStatement ast = getSQLStatement(sql);

        SqlServerInsertRecognizer recognizer = new SqlServerInsertRecognizer(sql, ast);
        List<List<Object>> insertRows = recognizer.getInsertRows(Collections.singletonList(pkIndex));
        Assertions.assertEquals(1, insertRows.size());

        //test for sequence
        sql = "insert into t(id) values(next value for t1.id)";
        ast = getSQLStatement(sql);
        recognizer = new SqlServerInsertRecognizer(sql, ast);
        insertRows = recognizer.getInsertRows(Collections.singletonList(pkIndex));
        Assertions.assertEquals(1, insertRows.size());

        //test for top
        Assertions.assertThrows(NotSupportYetException.class, () -> {
            String s = "insert top(1) into t(id) values(id1)";
            SQLStatement sqlStatement = getSQLStatement(s);
            SqlServerInsertRecognizer sqlServerInsertRecognizer = new SqlServerInsertRecognizer(s, sqlStatement);
            sqlServerInsertRecognizer.getInsertRows(Collections.singletonList(pkIndex));
        });

        //test for exception
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String s = "insert into t(a) values (?)";
            SQLStatement sqlStatement = getSQLStatement(s);
            SQLInsertStatement sqlInsertStatement = (SQLInsertStatement) sqlStatement;
            sqlInsertStatement.getValuesList().get(0).getValues().set(pkIndex, new MySqlOrderingExpr());

            SqlServerInsertRecognizer sqlServerInsertRecognizer = new SqlServerInsertRecognizer(s, sqlInsertStatement);
            sqlServerInsertRecognizer.getInsertRows(Collections.singletonList(pkIndex));
        });
    }

    @Test
    public void testGetSQLType() {
        String sql = "insert into t(a) values (?)";
        SQLStatement ast = getSQLStatement(sql);

        SqlServerInsertRecognizer recognizer = new SqlServerInsertRecognizer(sql, ast);
        Assertions.assertEquals(SQLType.INSERT, recognizer.getSQLType());
    }

    @Test
    public void testGetInsertParamsValue() {
        String sql = "INSERT INTO t(a) VALUES (?)";
        SQLStatement ast = getSQLStatement(sql);
        SqlServerInsertRecognizer recognizer = new SqlServerInsertRecognizer(sql, ast);
        Assertions.assertEquals("?", recognizer.getInsertParamsValue().get(0));

        String sql_2 = "INSERT INTO t(a) VALUES ()";
        SQLStatement ast_2 = getSQLStatement(sql_2);
        SqlServerInsertRecognizer recognizer_2 = new SqlServerInsertRecognizer(sql_2, ast_2);
        Assertions.assertEquals("", recognizer_2.getInsertParamsValue().get(0));

        String sql_3 = "INSERT INTO T1 DEFAULT VALUES";
        SQLStatement ast_3 = getSQLStatement(sql_3);
        SqlServerInsertRecognizer recognizer_3 = new SqlServerInsertRecognizer(sql_3, ast_3);
        Assertions.assertTrue(recognizer_3.getInsertParamsValue().isEmpty());
    }

    @Test
    public void testGetDuplicateKeyUpdate() {
        String sql = "insert into t(a) values (?)";
        SQLStatement ast = getSQLStatement(sql);

        SqlServerInsertRecognizer recognizer = new SqlServerInsertRecognizer(sql, ast);
        Assertions.assertNull(recognizer.getDuplicateKeyUpdate());
    }
}
