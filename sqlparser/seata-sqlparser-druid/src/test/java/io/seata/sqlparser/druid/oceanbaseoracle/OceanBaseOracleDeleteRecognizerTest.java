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
package io.seata.sqlparser.druid.oceanbaseoracle;

import com.alibaba.druid.sql.ast.SQLStatement;
import io.seata.sqlparser.ParametersHolder;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.druid.AbstractRecognizerTest;
import io.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Test cases for delete recognizer of OceanBaseOracle
 *
 * @author hsien999
 */
public class OceanBaseOracleDeleteRecognizerTest extends AbstractRecognizerTest {

    @Override
    public String getDbType() {
        return JdbcConstants.OCEANBASE_ORACLE;
    }

    @Test
    public void testGetSqlType() {
        String sql = "DELETE FROM t WHERE id = ?";
        SQLStatement ast = getSQLStatement(sql);

        OceanBaseOracleDeleteRecognizer recognizer = new OceanBaseOracleDeleteRecognizer(sql, ast);
        Assertions.assertEquals(recognizer.getSQLType(), SQLType.DELETE);
    }

    @Test
    public void testGetTableNameAlias() {
        String sql = "DELETE FROM t WHERE id = ?";
        SQLStatement ast = getSQLStatement(sql);

        OceanBaseOracleDeleteRecognizer recognizer = new OceanBaseOracleDeleteRecognizer(sql, ast);
        Assertions.assertNull(recognizer.getTableAlias());

        sql = "DELETE FROM t t1 WHERE t1.id = ?";
        ast = getSQLStatement(sql);

        recognizer = new OceanBaseOracleDeleteRecognizer(sql, ast);
        Assertions.assertEquals("t", recognizer.getTableName());
        Assertions.assertEquals("t1", recognizer.getTableAlias());
    }

    @Test
    public void testWhereWithConstant() {
        String sql = "DELETE FROM t WHERE id = 1";

        SQLStatement statement = getSQLStatement(sql);
        OceanBaseOracleDeleteRecognizer recognizer = new OceanBaseOracleDeleteRecognizer(sql, statement);

        Assertions.assertEquals(sql, recognizer.getOriginalSQL());
        Assertions.assertEquals("id = 1", recognizer.getWhereCondition());
    }

    @Test
    public void testWhereWithPlaceholder() {
        String sql = "DELETE FROM t WHERE id in (?, ?)";

        SQLStatement statement = getSQLStatement(sql);
        OceanBaseOracleDeleteRecognizer recognizer = new OceanBaseOracleDeleteRecognizer(sql, statement);

        ParametersHolder parametersHolder = () -> Stream.of(
                new AbstractMap.SimpleEntry<Integer, ArrayList<Object>>(1, new ArrayList<>(Collections.singletonList(1))),
                new AbstractMap.SimpleEntry<Integer, ArrayList<Object>>(2, new ArrayList<>(Collections.singletonList(2))))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();

        Assertions.assertEquals(sql, recognizer.getOriginalSQL());
        Assertions.assertEquals("id IN (?, ?)", recognizer.getWhereCondition());

        String whereCondition = recognizer.getWhereCondition(parametersHolder, paramAppenderList);
        Assertions.assertEquals("id IN (?, ?)", whereCondition);
        Assertions.assertEquals(Collections.singletonList(Arrays.asList(1, 2)), paramAppenderList);
    }

    @Test
    public void testWhereWithBetween() {
        String sql = "DELETE FROM t WHERE id BETWEEN ? AND ?";

        SQLStatement statement = getSQLStatement(sql);
        OceanBaseOracleDeleteRecognizer recognizer = new OceanBaseOracleDeleteRecognizer(sql, statement);

        ParametersHolder parametersHolder = () -> Stream.of(
                new AbstractMap.SimpleEntry<Integer, ArrayList<Object>>(1, new ArrayList<>(Collections.singletonList(1))),
                new AbstractMap.SimpleEntry<Integer, ArrayList<Object>>(2, new ArrayList<>(Collections.singletonList(2))))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();

        Assertions.assertEquals(sql, recognizer.getOriginalSQL());
        Assertions.assertEquals("id BETWEEN ? AND ?", recognizer.getWhereCondition());

        String whereCondition = recognizer.getWhereCondition(parametersHolder, paramAppenderList);
        Assertions.assertEquals("id BETWEEN ? AND ?", whereCondition);
        Assertions.assertEquals(Collections.singletonList(Arrays.asList(1, 2)), paramAppenderList);
    }

    @Test
    public void testWhereWithExists() {
        String sql = "DELETE FROM t1 WHERE EXISTS (SELECT * FROM t2)";

        SQLStatement statement = getSQLStatement(sql);
        OceanBaseOracleDeleteRecognizer recognizer = new OceanBaseOracleDeleteRecognizer(sql, statement);

        Assertions.assertEquals(sql, recognizer.getOriginalSQL());
        Assertions.assertEquals("t1", recognizer.getTableName());
        Assertions.assertEquals("EXISTS (\n" +
            "\tSELECT *\n" +
            "\tFROM t2\n" +
            ")", recognizer.getWhereCondition());
    }

    @Test
    public void testWhereWithSubQuery() {
        String sql = "DELETE FROM t WHERE id in (SELECT id FROM t)";

        SQLStatement statement = getSQLStatement(sql);
        OceanBaseOracleDeleteRecognizer recognizer = new OceanBaseOracleDeleteRecognizer(sql, statement);

        Assertions.assertEquals(sql, recognizer.getOriginalSQL());
        Assertions.assertThrows(IllegalArgumentException.class, recognizer::getWhereCondition);
    }
}
