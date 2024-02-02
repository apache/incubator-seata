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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import org.apache.seata.sqlparser.SQLParsingException;
import org.apache.seata.sqlparser.SQLType;
import org.apache.seata.sqlparser.druid.AbstractRecognizerTest;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type SqlServer select for update recognizer test.
 *
 */
public class SqlServerSelectForUpdateRecognizerTest extends AbstractRecognizerTest {
    @Override
    public String getDbType() {
        return JdbcConstants.SQLSERVER;
    }

    /**
     * Select for update recognizer test 0.(test with constant)
     */
    @Test
    public void selectForUpdateRecognizerTest_0() {
        String sql = "SELECT name FROM t1 WITH (UPDLOCK) WHERE id = 'id1' ";

        SQLStatement statement = getSQLStatement(sql);
        SqlServerSelectForUpdateRecognizer sqlServerUpdateRecognizer = new SqlServerSelectForUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, sqlServerUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals(SQLType.SELECT_FOR_UPDATE, sqlServerUpdateRecognizer.getSQLType());
        Assertions.assertEquals("t1", sqlServerUpdateRecognizer.getTableName());
        Assertions.assertEquals("id = 'id1'", sqlServerUpdateRecognizer.getWhereCondition());
    }

    /**
     * Select for update recognizer test 1.(test with placeholder)
     */
    @Test
    public void selectForUpdateRecognizerTest_1() {
        String sql = "SELECT name FROM t1 WITH (UPDLOCK) WHERE id = ?";

        SQLStatement statement = getSQLStatement(sql);
        SqlServerSelectForUpdateRecognizer sqlServerUpdateRecognizer = new SqlServerSelectForUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, sqlServerUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", sqlServerUpdateRecognizer.getTableName());
        Assertions.assertEquals(SQLType.SELECT_FOR_UPDATE, sqlServerUpdateRecognizer.getSQLType());

        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = sqlServerUpdateRecognizer.getWhereCondition(() -> {
            ArrayList<Object> idParam = new ArrayList<>();
            idParam.add("id1");
            Map<Integer, ArrayList<Object>> result = new HashMap<>();
            result.put(1, idParam);
            return result;
        }, paramAppenderList);

        Assertions.assertEquals(Collections.singletonList(Collections.singletonList("id1")), paramAppenderList);
        Assertions.assertEquals("id = ?", whereCondition);
    }

    /**
     * Select for update recognizer test 2.(test with multi column)
     */
    @Test
    public void selectForUpdateRecognizerTest_2() {
        String sql = "SELECT name1, name2 FROM t1 WITH (UPDLOCK) WHERE id = ?";

        SQLStatement statement = getSQLStatement(sql);
        SqlServerSelectForUpdateRecognizer sqlServerUpdateRecognizer = new SqlServerSelectForUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, sqlServerUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", sqlServerUpdateRecognizer.getTableName());
        Assertions.assertEquals(SQLType.SELECT_FOR_UPDATE, sqlServerUpdateRecognizer.getSQLType());

        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = sqlServerUpdateRecognizer.getWhereCondition(() -> {
            ArrayList<Object> idParam = new ArrayList<>();
            idParam.add("id1");
            Map<Integer, ArrayList<Object>> result = new HashMap<>();
            result.put(1, idParam);
            return result;
        }, paramAppenderList);

        Assertions.assertEquals(Collections.singletonList(Collections.singletonList("id1")), paramAppenderList);
        Assertions.assertEquals("id = ?", whereCondition);
    }

    /**
     * Select for update recognizer test 3.(test with IN sql)
     */
    @Test
    public void selectForUpdateRecognizerTest_3() {

        String sql = "SELECT name1, name2 FROM t1 WITH (UPDLOCK) WHERE id IN (?,?)";

        SQLStatement statement = getSQLStatement(sql);
        SqlServerSelectForUpdateRecognizer sqlServerUpdateRecognizer = new SqlServerSelectForUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, sqlServerUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", sqlServerUpdateRecognizer.getTableName());

        // test overflow parameters
        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = sqlServerUpdateRecognizer.getWhereCondition(() -> {
            ArrayList<Object> id1Param = new ArrayList<>();
            id1Param.add("id1");
            ArrayList<Object> id2Param = new ArrayList<>();
            id2Param.add("id2");
            Map<Integer, ArrayList<Object>> result = new HashMap<>();
            result.put(1, id1Param);
            result.put(2, id2Param);
            return result;
        }, paramAppenderList);

        Assertions.assertEquals(Collections.singletonList(Arrays.asList("id1", "id2")), paramAppenderList);
        Assertions.assertEquals("id IN (?, ?)", whereCondition);
    }

    /**
     * Select for update recognizer test 4.(test with between...and... sql)
     */
    @Test
    public void selectForUpdateRecognizerTest_4() {

        String sql = "SELECT name1, name2 FROM t1 WITH (UPDLOCK) WHERE id between ? and ?";

        SQLStatement statement = getSQLStatement(sql);
        SqlServerSelectForUpdateRecognizer sqlServerUpdateRecognizer = new SqlServerSelectForUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, sqlServerUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", sqlServerUpdateRecognizer.getTableName());

        // test overflow parameters
        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = sqlServerUpdateRecognizer.getWhereCondition(() -> {
            ArrayList<Object> id1Param = new ArrayList<>();
            id1Param.add("id1");
            ArrayList<Object> id2Param = new ArrayList<>();
            id2Param.add("id2");
            Map<Integer, ArrayList<Object>> result = new HashMap<>();
            result.put(1, id1Param);
            result.put(2, id2Param);
            return result;
        }, paramAppenderList);

        Assertions.assertEquals(Collections.singletonList(Arrays.asList("id1", "id2")), paramAppenderList);
        Assertions.assertEquals("id BETWEEN ? AND ?", whereCondition);
    }

    @Test
    public void testGetWhereCondition_1() {
        String sql = "SELECT * FROM t WITH (UPDLOCK)";
        SQLStatement ast = getSQLStatement(sql);

        SqlServerSelectForUpdateRecognizer recognizer = new SqlServerSelectForUpdateRecognizer(sql, ast);
        String whereCondition = recognizer.getWhereCondition();

        Assertions.assertEquals("", whereCondition);

        //test for select was null
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String s = "select * from t WITH (UPDLOCK)";
            SQLStatement sqlStatement = getSQLStatement(s);
            SQLSelectStatement selectAst = (SQLSelectStatement) sqlStatement;
            selectAst.setSelect(null);
            new SqlServerSelectForUpdateRecognizer(s, selectAst).getWhereCondition();
        });

        //test for query was null
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String s = "select * from t";
            SQLStatement sqlStatement = getSQLStatement(s);
            SQLSelectStatement selectAst = (SQLSelectStatement) sqlStatement;
            selectAst.getSelect().setQuery(null);
            new SqlServerSelectForUpdateRecognizer(s, selectAst).getWhereCondition();
        });
    }

    @Test
    public void testGetSqlType() {
        String sql = "SELECT * FROM t WITH (UPDLOCK) WHERE id = ?";
        SQLStatement ast = getSQLStatement(sql);

        SqlServerSelectForUpdateRecognizer recognizer = new SqlServerSelectForUpdateRecognizer(sql, ast);
        Assertions.assertEquals(recognizer.getSQLType(), SQLType.SELECT_FOR_UPDATE);
    }

    @Test
    public void testGetTableAlias() {
        //test for no alias
        String sql = "SELECT * FROM t WITH (UPDLOCK) WHERE id = ?";
        SQLStatement ast = getSQLStatement(sql);

        SqlServerSelectForUpdateRecognizer recognizer = new SqlServerSelectForUpdateRecognizer(sql, ast);
        Assertions.assertNull(recognizer.getTableAlias());

        //test for alias
        sql = "SELECT * FROM t t1 WITH (UPDLOCK) WHERE id = ?";
        ast = getSQLStatement(sql);

        recognizer = new SqlServerSelectForUpdateRecognizer(sql, ast);
        Assertions.assertEquals("t1", recognizer.getTableAlias());
    }

    @Test
    public void testGetTableName() {
        String sql = "SELECT * FROM t WITH (UPDLOCK)";
        SQLStatement ast = getSQLStatement(sql);

        SqlServerSelectForUpdateRecognizer recognizer = new SqlServerSelectForUpdateRecognizer(sql, ast);
        Assertions.assertEquals("t", recognizer.getTableName());

        //test for alias
        sql = "SELECT * FROM t t1 WITH (UPDLOCK)";
        ast = getSQLStatement(sql);
        recognizer = new SqlServerSelectForUpdateRecognizer(sql, ast);
        Assertions.assertEquals("t", recognizer.getTableName());
    }
}
