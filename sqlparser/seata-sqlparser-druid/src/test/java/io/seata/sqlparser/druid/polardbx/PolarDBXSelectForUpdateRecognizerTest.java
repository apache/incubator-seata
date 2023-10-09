/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.sqlparser.druid.polardbx;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.alibaba.druid.sql.ast.SQLStatement;
import io.seata.sqlparser.ParametersHolder;
import io.seata.sqlparser.SQLType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test cases for SelectForUpdate recognizer of PolarDB-X
 *
 * @author hsien999
 */
public class PolarDBXSelectForUpdateRecognizerTest extends AbstractPolarDBXRecognizerTest {
    @Test
    public void testGetSqlType() {
        String sql = "SELECT * FROM t FOR UPDATE";
        SQLStatement ast = getSQLStatement(sql);

        PolarDBXSelectForUpdateRecognizer recognizer = new PolarDBXSelectForUpdateRecognizer(sql, ast);
        Assertions.assertEquals(recognizer.getSQLType(), SQLType.SELECT_FOR_UPDATE);
    }

    @Test
    public void testGetTableNameAlias() {
        String sql = "SELECT * FROM t FOR UPDATE";
        SQLStatement ast = getSQLStatement(sql);

        PolarDBXSelectForUpdateRecognizer recognizer = new PolarDBXSelectForUpdateRecognizer(sql, ast);
        Assertions.assertNull(recognizer.getTableAlias());

        sql = "SELECT * FROM t t1 FOR UPDATE";
        ast = getSQLStatement(sql);

        recognizer = new PolarDBXSelectForUpdateRecognizer(sql, ast);
        Assertions.assertEquals("t", recognizer.getTableName());
        Assertions.assertEquals("t1", recognizer.getTableAlias());
    }

    @Test
    public void testWhereWithConstant() {
        String sql = "SELECT name FROM t FOR UPDATE";
        SQLStatement ast = getSQLStatement(sql);

        PolarDBXSelectForUpdateRecognizer recognizer = new PolarDBXSelectForUpdateRecognizer(sql, ast);

        Assertions.assertEquals(sql, recognizer.getOriginalSQL());
        Assertions.assertEquals("t", recognizer.getTableName());
        Assertions.assertEquals("", recognizer.getWhereCondition());
    }

    @Test
    public void testWhereWithPlaceholder() {
        String sql = "SELECT id, name FROM t WHERE id = ? FOR UPDATE";
        SQLStatement ast = getSQLStatement(sql);

        PolarDBXSelectForUpdateRecognizer recognizer = new PolarDBXSelectForUpdateRecognizer(sql, ast);

        Assertions.assertEquals(sql, recognizer.getOriginalSQL());
        Assertions.assertEquals("t", recognizer.getTableName());

        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        ParametersHolder parametersHolder = () -> Stream.of(
                        new AbstractMap.SimpleEntry<Integer, ArrayList<Object>>(1, new ArrayList<>(Collections.singletonList(1))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        String whereCondition = recognizer.getWhereCondition(parametersHolder, paramAppenderList);
        Assertions.assertEquals(Collections.singletonList(Collections.singletonList(1)), paramAppenderList);
        Assertions.assertEquals("id = ?", whereCondition);
    }

    @Test
    public void testWhereWithInList() {
        String sql = "SELECT id, name FROM t WHERE id in (?, ?) FOR UPDATE";
        SQLStatement ast = getSQLStatement(sql);

        PolarDBXSelectForUpdateRecognizer recognizer = new PolarDBXSelectForUpdateRecognizer(sql, ast);

        Assertions.assertEquals(sql, recognizer.getOriginalSQL());
        Assertions.assertEquals("t", recognizer.getTableName());

        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        ParametersHolder parametersHolder = () -> Stream.of(
                        new AbstractMap.SimpleEntry<Integer, ArrayList<Object>>(1, new ArrayList<>(Collections.singletonList(1))),
                        new AbstractMap.SimpleEntry<Integer, ArrayList<Object>>(2, new ArrayList<>(Collections.singletonList(2))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        String whereCondition = recognizer.getWhereCondition(parametersHolder, paramAppenderList);
        Assertions.assertEquals(Collections.singletonList(Arrays.asList(1, 2)), paramAppenderList);
        Assertions.assertEquals("id IN (?, ?)", whereCondition);
    }

    @Test
    public void testWhereWithBetween() {
        String sql = "SELECT id, name FROM t WHERE id BETWEEN ? AND ? FOR UPDATE";
        SQLStatement ast = getSQLStatement(sql);

        PolarDBXSelectForUpdateRecognizer recognizer = new PolarDBXSelectForUpdateRecognizer(sql, ast);

        Assertions.assertEquals(sql, recognizer.getOriginalSQL());
        Assertions.assertEquals("t", recognizer.getTableName());

        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        ParametersHolder parametersHolder = () -> Stream.of(
                        new AbstractMap.SimpleEntry<Integer, ArrayList<Object>>(1, new ArrayList<>(Collections.singletonList(1))),
                        new AbstractMap.SimpleEntry<Integer, ArrayList<Object>>(2, new ArrayList<>(Collections.singletonList(2))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        String whereCondition = recognizer.getWhereCondition(parametersHolder, paramAppenderList);
        Assertions.assertEquals(Collections.singletonList(Arrays.asList(1, 2)), paramAppenderList);
        Assertions.assertEquals("id BETWEEN ? AND ?", whereCondition);
    }

    @Test
    public void testWhereWithMixedExpression() {
        String sql = "SELECT id, name FROM t WHERE id in (?, ?) and name like ? FOR UPDATE";
        SQLStatement ast = getSQLStatement(sql);

        PolarDBXSelectForUpdateRecognizer recognizer = new PolarDBXSelectForUpdateRecognizer(sql, ast);

        Assertions.assertEquals(sql, recognizer.getOriginalSQL());
        Assertions.assertEquals("t", recognizer.getTableName());

        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        ParametersHolder parametersHolder = () -> Stream.of(
                        new AbstractMap.SimpleEntry<Integer, ArrayList<Object>>(1, new ArrayList<>(Collections.singletonList(1))),
                        new AbstractMap.SimpleEntry<Integer, ArrayList<Object>>(2, new ArrayList<>(Collections.singletonList(2))),
                        new AbstractMap.SimpleEntry<Integer, ArrayList<Object>>(3, new ArrayList<>(Collections.singletonList("%test%"))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        String whereCondition = recognizer.getWhereCondition(parametersHolder, paramAppenderList);
        Assertions.assertEquals(Collections.singletonList(Arrays.asList(1, 2, "%test%")), paramAppenderList);
        Assertions.assertEquals("id IN (?, ?)\n" +
                "\tAND name LIKE ?", whereCondition);
    }
}
