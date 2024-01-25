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
import org.apache.seata.sqlparser.ParametersHolder;
import org.apache.seata.sqlparser.SQLType;
import org.apache.seata.sqlparser.druid.AbstractRecognizerTest;
import org.apache.seata.sqlparser.druid.BaseRecognizer;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type SqlServer delete recognizer test.
 *
 */
public class SqlServerDeleteRecognizerTest extends AbstractRecognizerTest {
    @Test
    public void testVMarker() {
        Assertions.assertEquals("?", new BaseRecognizer.VMarker().toString());
    }

    /**
     * Delete recognizer test 0.(test with constant)
     */
    @Test
    public void deleteRecognizerTest_0() {
        String sql = "DELETE FROM t1 WHERE id = 'id1'";

        SQLStatement statement = getSQLStatement(sql);
        SqlServerDeleteRecognizer sqlServerDeleteRecognizer = new SqlServerDeleteRecognizer(sql, statement);

        Assertions.assertEquals(sql, sqlServerDeleteRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", sqlServerDeleteRecognizer.getTableName());
        Assertions.assertEquals("id = 'id1'", sqlServerDeleteRecognizer.getWhereCondition());
    }

    /**
     * Delete recognizer test 1.(test with placeholder)
     */
    @Test
    public void deleteRecognizerTest_1() {
        String sql = "DELETE FROM t1 WHERE id = ?";

        SQLStatement statement = getSQLStatement(sql);
        SqlServerDeleteRecognizer sqlServerDeleteRecognizer = new SqlServerDeleteRecognizer(sql, statement);

        ParametersHolder parametersHolder = () -> {
            ArrayList<Object> idParam = new ArrayList<>();
            idParam.add("id1");
            Map<Integer, ArrayList<Object>> result = new HashMap<>();
            result.put(1, idParam);
            return result;
        };
        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();

        Assertions.assertEquals(sql, sqlServerDeleteRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", sqlServerDeleteRecognizer.getTableName());
        Assertions.assertEquals("id = ?", sqlServerDeleteRecognizer.getWhereCondition());

        String whereCondition = sqlServerDeleteRecognizer.getWhereCondition(parametersHolder, paramAppenderList);
        Assertions.assertEquals("id = ?", whereCondition);
        Assertions.assertEquals(Collections.singletonList(Collections.singletonList("id1")), paramAppenderList);
    }

    /**
     * Delete recognizer test 2.(test with multi placeholder)
     */
    @Test
    public void deleteRecognizerTest_2() {
        String sql = "DELETE FROM t1 WHERE id in (?, ?)";

        SQLStatement statement = getSQLStatement(sql);
        SqlServerDeleteRecognizer sqlServerDeleteRecognizer = new SqlServerDeleteRecognizer(sql, statement);

        ParametersHolder parametersHolder = () -> {
            ArrayList<Object> id1Param = new ArrayList<>();
            id1Param.add("id1");
            ArrayList<Object> id2Param = new ArrayList<>();
            id2Param.add("id2");
            Map<Integer, ArrayList<Object>> result = new HashMap<>();
            result.put(1, id1Param);
            result.put(2, id2Param);
            return result;
        };
        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();

        Assertions.assertEquals(sql, sqlServerDeleteRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", sqlServerDeleteRecognizer.getTableName());
        Assertions.assertEquals("id IN (?, ?)", sqlServerDeleteRecognizer.getWhereCondition());

        String whereCondition = sqlServerDeleteRecognizer.getWhereCondition(parametersHolder, paramAppenderList);
        Assertions.assertEquals("id IN (?, ?)", whereCondition);
        Assertions.assertEquals(Collections.singletonList(Arrays.asList("id1", "id2")), paramAppenderList);
    }

    /**
     * Delete recognizer test 3.(test with between...and...)
     */
    @Test
    public void deleteRecognizerTest_3() {
        String sql = "DELETE FROM t1 WHERE id BETWEEN ? AND ?";

        SQLStatement statement = getSQLStatement(sql);
        SqlServerDeleteRecognizer sqlServerDeleteRecognizer = new SqlServerDeleteRecognizer(sql, statement);

        ParametersHolder parametersHolder = () -> {
            ArrayList<Object> id1Param = new ArrayList<>();
            id1Param.add("id1");
            ArrayList<Object> id2Param = new ArrayList<>();
            id2Param.add("id2");
            Map<Integer, ArrayList<Object>> result = new HashMap<>();
            result.put(1, id1Param);
            result.put(2, id2Param);
            return result;
        };
        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();

        Assertions.assertEquals(sql, sqlServerDeleteRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", sqlServerDeleteRecognizer.getTableName());
        Assertions.assertEquals("id BETWEEN ? AND ?", sqlServerDeleteRecognizer.getWhereCondition());

        String whereCondition = sqlServerDeleteRecognizer.getWhereCondition(parametersHolder, paramAppenderList);
        Assertions.assertEquals("id BETWEEN ? AND ?", whereCondition);
        Assertions.assertEquals(Collections.singletonList(Arrays.asList("id1", "id2")), paramAppenderList);
    }

    /**
     * Delete recognizer test 4.(test with exists)
     */
    @Test
    public void deleteRecognizerTest_4() {
        String sql = "DELETE FROM t1 WHERE EXISTS (SELECT * FROM t1)";

        SQLStatement statement = getSQLStatement(sql);
        SqlServerDeleteRecognizer sqlServerDeleteRecognizer = new SqlServerDeleteRecognizer(sql, statement);

        Assertions.assertEquals(sql, sqlServerDeleteRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", sqlServerDeleteRecognizer.getTableName());
        Assertions.assertEquals("EXISTS (\n" +
                "\tSELECT *\n" +
                "\tFROM t1\n" +
                ")", sqlServerDeleteRecognizer.getWhereCondition());
    }

    /**
     * Delete recognizer test 5.(test with SubQuery)
     */
    @Test
    public void deleteRecognizerTest_5() {
        String sql = "DELETE FROM t1 WHERE id in (SELECT id FROM t1)";

        SQLStatement statement = getSQLStatement(sql);
        SqlServerDeleteRecognizer sqlServerDeleteRecognizer = new SqlServerDeleteRecognizer(sql, statement);

        Assertions.assertEquals(sql, sqlServerDeleteRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", sqlServerDeleteRecognizer.getTableName());
        Assertions.assertThrows(IllegalArgumentException.class, sqlServerDeleteRecognizer::getWhereCondition);
    }

    @Test
    public void testGetSqlType() {
        String sql = "DELETE FROM t WHERE id = ?";
        SQLStatement ast = getSQLStatement(sql);

        SqlServerDeleteRecognizer recognizer = new SqlServerDeleteRecognizer(sql, ast);
        Assertions.assertEquals(recognizer.getSQLType(), SQLType.DELETE);
    }

    @Test
    public void testGetTableAlias() {
        String sql = "DELETE FROM t WHERE id = ?";
        SQLStatement ast = getSQLStatement(sql);

        SqlServerDeleteRecognizer recognizer = new SqlServerDeleteRecognizer(sql, ast);
        Assertions.assertNull(recognizer.getTableAlias());

        sql = "DELETE t1 FROM t t1 WHERE t1.id = ?";
        ast = getSQLStatement(sql);

        recognizer = new SqlServerDeleteRecognizer(sql, ast);
        Assertions.assertEquals("t1", recognizer.getTableAlias());
    }

    @Override
    public String getDbType() {
        return JdbcConstants.SQLSERVER;
    }
}
