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
package org.apache.seata.sqlparser.druid;

import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;
import com.alibaba.druid.sql.ast.expr.SQLIntegerExpr;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import org.apache.seata.sqlparser.SQLInsertRecognizer;
import org.apache.seata.sqlparser.SQLParsingException;
import org.apache.seata.sqlparser.SQLType;
import org.apache.seata.sqlparser.struct.NotPlaceholderExpr;
import org.apache.seata.sqlparser.struct.Null;
import org.apache.seata.sqlparser.struct.SqlMethodExpr;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialectInsertBehaviorTest {
    @ParameterizedTest
    @ValueSource(strings = {"mysql", "postgresql", "oracle", "dm", "kingbase", "oscar", "oceanbase", "sqlserver"})
    void omittedColumnsAndUnsupportedPrimaryKeyExpressions(String dialect) throws Exception {
        String sql = "insert into orders values (1)";
        SQLInsertStatement ast = (SQLInsertStatement) DialectMutationBehaviorTest.parse(dialect, sql);
        SQLInsertRecognizer insert =
                (SQLInsertRecognizer) DialectMutationBehaviorTest.recognizer(dialect, "Insert", sql, ast);
        assertTrue(insert.insertColumnsIsEmpty());
        assertNull(insert.getInsertColumns());
        assertNull(insert.getInsertColumnsUnEscape());
        assertNull(insert.getDuplicateKeyUpdate());
        assertTrue(insert.isSqlSyntaxSupports());
        ast.getColumns().add(new SQLIntegerExpr(1));
        assertThrows(SQLParsingException.class, insert::getInsertColumns);
        ast.getColumns().clear();
        ast.getValuesList()
                .get(0)
                .getValues()
                .set(0, new SQLBinaryOpExpr(new SQLIntegerExpr(1), SQLBinaryOperator.Add, new SQLIntegerExpr(2)));
        assertThrows(SQLParsingException.class, () -> insert.getInsertRows(Collections.singletonList(0)));
        assertSame(
                NotPlaceholderExpr.get(),
                insert.getInsertRows(Collections.emptyList()).get(0).get(0));
        ast.getTableSource().setAlias("o");
        assertEquals("o", insert.getTableAlias());
    }

    @ParameterizedTest
    @ValueSource(strings = {"mysql", "postgresql", "oracle", "dm", "kingbase", "oscar", "oceanbase", "sqlserver"})
    void insertedValuesPreserveNullLiteralPlaceholderAndFunction(String dialect) throws Exception {
        String sql = "insert into orders (a,b,c,d) values (null, 'paid', ?, abs(1))";
        SQLInsertStatement ast = (SQLInsertStatement) DialectMutationBehaviorTest.parse(dialect, sql);
        SQLInsertRecognizer insert =
                (SQLInsertRecognizer) DialectMutationBehaviorTest.recognizer(dialect, "Insert", sql, ast);
        assertEquals(SQLType.INSERT, insert.getSQLType());
        assertFalse(insert.insertColumnsIsEmpty());
        assertEquals(Arrays.asList("a", "b", "c", "d"), insert.getInsertColumns());
        assertEquals("orders", insert.getTableName());
        List<Object> row = insert.getInsertRows(Collections.singletonList(0)).get(0);
        assertSame(Null.get(), row.get(0));
        assertEquals("paid", row.get(1));
        assertEquals("?", row.get(2));
        assertTrue(row.get(3) instanceof SqlMethodExpr);
        if ("mysql".equals(dialect) || "sqlserver".equals(dialect)) {
            assertEquals(1, insert.getInsertParamsValue().size());
        } else {
            assertNull(insert.getInsertParamsValue());
        }
    }
}
