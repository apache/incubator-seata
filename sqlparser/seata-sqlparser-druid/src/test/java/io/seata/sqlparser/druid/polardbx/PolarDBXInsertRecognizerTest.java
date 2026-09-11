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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLInSubQueryExpr;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.expr.MySqlOrderingExpr;
import io.seata.sqlparser.SQLParsingException;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.struct.NotPlaceholderExpr;
import io.seata.sqlparser.struct.Null;
import io.seata.sqlparser.struct.SqlMethodExpr;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test cases for delete recognizer of PolarDB-X
 *
 * @author hsien999
 */
public class PolarDBXInsertRecognizerTest extends AbstractPolarDBXRecognizerTest {
    private final int pkIndex = 0;

    @Test
    public void testGetSqlType() {
        String sql = "INSERT INTO t (id) VALUES (?)";
        SQLStatement ast = getSQLStatement(sql);

        PolarDBXInsertRecognizer recognizer = new PolarDBXInsertRecognizer(sql, ast);
        Assertions.assertEquals(recognizer.getSQLType(), SQLType.INSERT);
    }

    @Test
    public void testGetTableNameAlias() {
        String sql = "INSERT INTO t (id) VALUES (1)";
        SQLStatement ast = getSQLStatement(sql);

        PolarDBXInsertRecognizer recognizer = new PolarDBXInsertRecognizer(sql, ast);
        Assertions.assertNull(recognizer.getTableAlias());

        sql = "INSERT INTO t t1 (id) VALUES (1)";
        ast = getSQLStatement(sql);

        recognizer = new PolarDBXInsertRecognizer(sql, ast);
        Assertions.assertEquals("t", recognizer.getTableName());
        Assertions.assertEquals("t1", recognizer.getTableAlias());
    }

    @Test
    public void testValuesWithConstant() {
        String sql = "INSERT INTO t (id, name) VALUES (1, 'test')";
        SQLStatement statement = getSQLStatement(sql);

        PolarDBXInsertRecognizer recognizer = new PolarDBXInsertRecognizer(sql, statement);

        Assertions.assertEquals(sql, recognizer.getOriginalSQL());
        Assertions.assertFalse(recognizer.insertColumnsIsEmpty());

        Assertions.assertEquals(Arrays.asList("id", "name"), recognizer.getInsertColumns());

        List<List<Object>> insertRows = recognizer.getInsertRows(Collections.singletonList(pkIndex));
        Assertions.assertEquals(Collections.singletonList(Arrays.asList(1, "test")), insertRows);
    }

    @Test
    public void testValuesWithPlaceholder() {
        String sql = "INSERT INTO t (id, name) VALUES (?, ?)";
        SQLStatement statement = getSQLStatement(sql);

        PolarDBXInsertRecognizer recognizer = new PolarDBXInsertRecognizer(sql, statement);

        Assertions.assertEquals(sql, recognizer.getOriginalSQL());
        Assertions.assertFalse(recognizer.insertColumnsIsEmpty());

        Assertions.assertEquals(Arrays.asList("id", "name"), recognizer.getInsertColumns());

        List<List<Object>> insertRows = recognizer.getInsertRows(Collections.singletonList(pkIndex));
        Assertions.assertEquals(Collections.singletonList(Arrays.asList("?", "?")), insertRows);
    }

    @Test
    public void testGetInsertColumns() {
        // case1: empty
        String sql = "INSERT INTO t VALUES (?)";
        SQLStatement ast = getSQLStatement(sql);

        PolarDBXInsertRecognizer recognizer = new PolarDBXInsertRecognizer(sql, ast);
        List<String> insertColumns = recognizer.getInsertColumns();
        Assertions.assertNull(insertColumns);

        // case2: multi columns
        sql = "INSERT INTO t (id, name) VALUES (1, 'test')";
        ast = getSQLStatement(sql);

        recognizer = new PolarDBXInsertRecognizer(sql, ast);
        insertColumns = recognizer.getInsertColumns();
        Assertions.assertEquals(Arrays.asList("id", "name"), insertColumns);

        // case3: unrecognized expression of columns
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String sql2 = "INSERT INTO t(a) VALUES (?)";
            SQLStatement sqlStatement = getSQLStatement(sql2);
            SQLInsertStatement ast2 = (SQLInsertStatement) sqlStatement;
            ast2.getColumns().add(new SQLInSubQueryExpr());

            PolarDBXInsertRecognizer recognizer2 = new PolarDBXInsertRecognizer(sql2, ast2);
            recognizer2.getInsertColumns();
        });
    }

    @Test
    public void testGetInsertRows() {
        // case1: test expressions of value
        // VALUES(variant ref(placeholder etc.), value, null, method, default)
        String sql = "INSERT INTO t(no, name, age, time, school) " +
                "VALUES (?, 'test', null, now(), default)";
        SQLStatement ast = getSQLStatement(sql);

        PolarDBXInsertRecognizer recognizer = new PolarDBXInsertRecognizer(sql, ast);
        List<List<Object>> insertRows = recognizer.getInsertRows(Collections.singletonList(pkIndex));
        Assertions.assertEquals(1, insertRows.size());
        List<Object> insertRow = insertRows.get(0);
        Assertions.assertEquals(Arrays.asList("?", "test", Null.get(), SqlMethodExpr.get(), NotPlaceholderExpr.get()),
                insertRow);

        // case2: unrecognized expression of value
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String sql2 = "insert into t(a) values (?)";
            SQLInsertStatement ast2 = (SQLInsertStatement) getSQLStatement(sql2);
            ast2.getValuesList().get(0).getValues().set(pkIndex, new MySqlOrderingExpr());

            PolarDBXInsertRecognizer recognizer2 = new PolarDBXInsertRecognizer(sql2, ast2);
            recognizer2.getInsertRows(Collections.singletonList(pkIndex));
        });
    }

    @Test
    public void testGetInsertParamsValue() {
        String sql = "INSERT INTO t (id) VALUES (?)";
        SQLStatement ast = getSQLStatement(sql);
        PolarDBXInsertRecognizer recognizer = new PolarDBXInsertRecognizer(sql, ast);
        Assertions.assertEquals(Collections.singletonList("?"), recognizer.getInsertParamsValue());
    }

    @Test
    public void testGetDuplicateKeyUpdate() {
        String sql = "INSERT INTO t (id) VALUES (?)";
        SQLStatement ast = getSQLStatement(sql);
        PolarDBXInsertRecognizer recognizer = new PolarDBXInsertRecognizer(sql, ast);
        Assertions.assertNull(recognizer.getDuplicateKeyUpdate());
    }
}
