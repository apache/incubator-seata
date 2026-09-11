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
import com.alibaba.druid.sql.ast.expr.SQLCharExpr;
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.alibaba.druid.sql.dialect.oracle.ast.expr.OracleDatetimeExpr;
import io.seata.sqlparser.ParametersHolder;
import io.seata.sqlparser.SQLParsingException;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.druid.BaseRecognizer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test cases for update recognizer of PolarDB-X
 *
 * @author hsien999
 */
public class PolarDBXUpdateRecognizerTest extends AbstractPolarDBXRecognizerTest {
    @Test
    public void testGetSqlType() {
        String sql = "UPDATE t SET name = 'test' WHERE id = 1";
        SQLStatement ast = getSQLStatement(sql);

        PolarDBXUpdateRecognizer recognizer = new PolarDBXUpdateRecognizer(sql, ast);
        Assertions.assertEquals(recognizer.getSQLType(), SQLType.UPDATE);
    }

    @Test
    public void testGetTableNameAlias() {
        String sql = "UPDATE t SET name = 'test' WHERE id = 1";
        SQLStatement ast = getSQLStatement(sql);

        PolarDBXUpdateRecognizer recognizer = new PolarDBXUpdateRecognizer(sql, ast);
        Assertions.assertNull(recognizer.getTableAlias());

        sql = "UPDATE t t1 SET t1.name = 'test' WHERE t1.id = 1";
        ast = getSQLStatement(sql);

        recognizer = new PolarDBXUpdateRecognizer(sql, ast);
        Assertions.assertEquals("t", recognizer.getTableName());
        Assertions.assertEquals("t1", recognizer.getTableAlias());
    }

    @Test
    public void testGetUpdateColumns() {
        // case1: common update sql
        String sql = "UPDATE t SET name = 'test', age = 18 WHERE id = 1";
        SQLStatement ast = getSQLStatement(sql);

        PolarDBXUpdateRecognizer recognizer = new PolarDBXUpdateRecognizer(sql, ast);
        Assertions.assertEquals(sql, recognizer.getOriginalSQL());
        Assertions.assertEquals("t", recognizer.getTableName());
        Assertions.assertEquals("id = 1", recognizer.getWhereCondition());

        Assertions.assertEquals(Arrays.asList("name", "age"), recognizer.getUpdateColumns());
        Assertions.assertEquals(Arrays.asList("test", 18), recognizer.getUpdateValues());

        // case2: table source with alias
        sql = "UPDATE t t1 SET t1.name = 'test', t1.age = 18 WHERE id = 1";
        ast = getSQLStatement(sql);
        recognizer = new PolarDBXUpdateRecognizer(sql, ast);
        Assertions.assertEquals(2, recognizer.getUpdateColumns().size());

        sql = "UPDATE d.t SET d.t.name = 'test', d.t.age = 18 WHERE id = 1";
        ast = getSQLStatement(sql);
        recognizer = new PolarDBXUpdateRecognizer(sql, ast);
        Assertions.assertEquals(2, recognizer.getUpdateColumns().size());

        // case3: test with error
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String sql2 = "UPDATE t SET id = 1";
            SQLUpdateStatement ast2 = (SQLUpdateStatement) getSQLStatement(sql2);
            List<SQLUpdateSetItem> updateSetItems = ast2.getItems();
            for (SQLUpdateSetItem updateSetItem : updateSetItems) {
                updateSetItem.setColumn(new SQLCharExpr());
            }
            PolarDBXUpdateRecognizer recognizer2 = new PolarDBXUpdateRecognizer(sql2, ast2);
            recognizer2.getUpdateColumns();
        });
    }

    @Test
    public void testGetUpdateValues() {
        // case1: test expressions of value
        // VALUES(value, variant ref(placeholder etc.), null, default, not placeholder)
        String sql = "UPDATE t\n" +
                "SET\n" +
                "\tid = 1,\n" +
                "\tno = ?,\n" +
                "\tage = 'test'";
        SQLStatement ast = getSQLStatement(sql);

        PolarDBXUpdateRecognizer recognizer = new PolarDBXUpdateRecognizer(sql, ast);
        List<Object> updateValues = recognizer.getUpdateValues();

        Assertions.assertEquals(3, updateValues.size());
        Assertions.assertEquals(1, updateValues.get(0));
        Assertions.assertEquals(BaseRecognizer.VMarker.class, updateValues.get(1).getClass());
        Assertions.assertEquals("test", updateValues.get(2));

        // case2: unrecognized expression of value
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String sql2 = "UPDATE t SET id = ?";
            SQLUpdateStatement ast2 = (SQLUpdateStatement) getSQLStatement(sql2);
            List<SQLUpdateSetItem> updateSetItems = ast2.getItems();
            updateSetItems.get(0).setValue(new OracleDatetimeExpr());
            PolarDBXUpdateRecognizer recognizer2 = new PolarDBXUpdateRecognizer(sql2, ast2);
            recognizer2.getUpdateValues();
        });
    }

    @Test
    public void testWhereWithPlaceholder() {
        String sql = "UPDATE t SET name = ? WHERE id = ?";

        SQLStatement ast = getSQLStatement(sql);
        PolarDBXUpdateRecognizer recognizer = new PolarDBXUpdateRecognizer(sql, ast);
        Assertions.assertEquals(sql, recognizer.getOriginalSQL());

        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        ParametersHolder parametersHolder = () -> Stream.of(
                        new AbstractMap.SimpleEntry<Integer, ArrayList<Object>>(1, new ArrayList<>(Collections.singletonList("test"))),
                        new AbstractMap.SimpleEntry<Integer, ArrayList<Object>>(2, new ArrayList<>(Collections.singletonList(1))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        String whereCondition = recognizer.getWhereCondition(parametersHolder, paramAppenderList);
        Assertions.assertEquals(Collections.singletonList(Collections.singletonList(1)), paramAppenderList);
        Assertions.assertEquals("id = ?", whereCondition);
    }

    @Test
    public void testWhereWithInList() {
        String sql = "UPDATE t SET name1 = 'test' WHERE id in (?, ?)";

        SQLStatement ast = getSQLStatement(sql);
        PolarDBXUpdateRecognizer recognizer = new PolarDBXUpdateRecognizer(sql, ast);
        Assertions.assertEquals(sql, recognizer.getOriginalSQL());

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
        String sql = "UPDATE t SET name = ? WHERE id BETWEEN ? AND ?";

        SQLStatement ast = getSQLStatement(sql);
        PolarDBXUpdateRecognizer recognizer = new PolarDBXUpdateRecognizer(sql, ast);
        Assertions.assertEquals(sql, recognizer.getOriginalSQL());

        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        ParametersHolder parametersHolder = () -> Stream.of(
                        new AbstractMap.SimpleEntry<Integer, ArrayList<Object>>(1, new ArrayList<>(Collections.singletonList("test"))),
                        new AbstractMap.SimpleEntry<Integer, ArrayList<Object>>(2, new ArrayList<>(Collections.singletonList(1))),
                        new AbstractMap.SimpleEntry<Integer, ArrayList<Object>>(3, new ArrayList<>(Collections.singletonList(2))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        String whereCondition = recognizer.getWhereCondition(parametersHolder, paramAppenderList);
        Assertions.assertEquals(Collections.singletonList(Arrays.asList(1, 2)), paramAppenderList);
        Assertions.assertEquals("id BETWEEN ? AND ?", whereCondition);
    }
}
