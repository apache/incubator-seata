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
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLIntegerExpr;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleMultiInsertStatement;
import org.apache.seata.sqlparser.SQLParsingException;
import org.apache.seata.sqlparser.struct.NotPlaceholderExpr;
import org.apache.seata.sqlparser.struct.SqlMethodExpr;
import org.apache.seata.sqlparser.struct.SqlSequenceExpr;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OracleMultiInsertBehaviorTest {
    @Test
    void emptyOrIncompleteInsertHasNoRowsColumnsOrAlias() {
        OracleMultiInsertStatement ast = new OracleMultiInsertStatement();
        OracleMultiInsertRecognizer insert = new OracleMultiInsertRecognizer("insert all", ast);
        assertNull(insert.getTableName());
        assertNull(insert.getTableAlias());
        assertNull(insert.getInsertColumns());
        assertNull(insert.getInsertColumnsUnEscape());
        assertTrue(insert.insertColumnsIsEmpty());
        assertTrue(insert.getInsertRows(Collections.emptyList()).isEmpty());
        OracleMultiInsertStatement.InsertIntoClause clause = new OracleMultiInsertStatement.InsertIntoClause();
        ast.getEntries().add(clause);
        insert = new OracleMultiInsertRecognizer("insert all", ast);
        assertNull(insert.getTableName());
        assertNull(insert.getTableAlias());
        assertTrue(insert.getInsertRows(Collections.emptyList()).isEmpty());
    }

    @Test
    void omittedColumnsAndAliasArePreserved() {
        String sql = "insert all into orders values (1) into orders values (2) select 1 from dual";
        OracleMultiInsertStatement ast = (OracleMultiInsertStatement)
                SQLUtils.parseStatements(sql, "oracle").get(0);
        ((OracleMultiInsertStatement.InsertIntoClause) ast.getEntries().get(0))
                .getTableSource()
                .setAlias("o");
        OracleMultiInsertRecognizer insert = new OracleMultiInsertRecognizer(sql, ast);
        assertEquals("o", insert.getTableAlias());
        assertEquals("orders", insert.getTableName());
        assertTrue(insert.insertColumnsIsEmpty());
        assertEquals(Collections.emptyList(), insert.getInsertColumns());
        assertEquals(
                Arrays.asList(Collections.singletonList(1), Collections.singletonList(2)),
                insert.getInsertRows(Collections.emptyList()));
        assertNull(insert.getInsertParamsValue());
        assertNull(insert.getDuplicateKeyUpdate());
        assertTrue(insert.isSqlSyntaxSupports());
    }

    @Test
    void invalidColumnAndPrimaryKeyExpressionAreRejected() {
        String sql = "insert all into orders (id) values (1) select 1 from dual";
        OracleMultiInsertStatement ast = (OracleMultiInsertStatement)
                SQLUtils.parseStatements(sql, "oracle").get(0);
        OracleMultiInsertStatement.InsertIntoClause clause =
                (OracleMultiInsertStatement.InsertIntoClause) ast.getEntries().get(0);
        clause.getColumns().set(0, new SQLIntegerExpr(1));
        assertThrows(SQLParsingException.class, () -> new OracleMultiInsertRecognizer(sql, ast));
        clause.getColumns().set(0, new SQLIdentifierExpr("id"));
        clause.getValuesList()
                .get(0)
                .getValues()
                .set(0, new SQLBinaryOpExpr(new SQLIntegerExpr(1), SQLBinaryOperator.Add, new SQLIntegerExpr(2)));
        OracleMultiInsertRecognizer insert = new OracleMultiInsertRecognizer(sql, ast);
        assertThrows(SQLParsingException.class, () -> insert.getInsertRows(Collections.singletonList(0)));
        assertSame(
                NotPlaceholderExpr.get(),
                insert.getInsertRows(Collections.emptyList()).get(0).get(0));
    }

    @Test
    void sequenceAndFunctionValuesRemainTypedMarkers() {
        String sql = "insert all into orders (id, value) values (seq.nextval, abs(1)) select 1 from dual";
        OracleMultiInsertRecognizer insert = new OracleMultiInsertRecognizer(
                sql, SQLUtils.parseStatements(sql, "oracle").get(0));
        List<Object> row = insert.getInsertRows(Collections.singletonList(0)).get(0);
        assertTrue(row.get(0) instanceof SqlSequenceExpr);
        assertSame(SqlMethodExpr.get(), row.get(1));
    }
}
