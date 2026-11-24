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

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLIntegerExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLJoinTableSource;
import com.alibaba.druid.sql.ast.statement.SQLSubqueryTableSource;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.sqlparser.ParametersHolder;
import org.apache.seata.sqlparser.SQLDeleteRecognizer;
import org.apache.seata.sqlparser.SQLParsingException;
import org.apache.seata.sqlparser.SQLRecognizer;
import org.apache.seata.sqlparser.SQLType;
import org.apache.seata.sqlparser.SQLUpdateRecognizer;
import org.apache.seata.sqlparser.WhereRecognizer;
import org.apache.seata.sqlparser.struct.Null;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialectMutationBehaviorTest {
    static SQLStatement parse(String dialect, String sql) {
        return SQLUtils.parseStatements(sql, dialect).get(0);
    }

    static SQLRecognizer recognizer(String dialect, String operation, String sql, SQLStatement ast) throws Exception {
        String prefix;
        switch (dialect) {
            case "mysql":
                prefix = "MySQL";
                break;
            case "postgresql":
                prefix = "Postgresql";
                break;
            case "sqlserver":
                prefix = "SqlServer";
                break;
            case "oceanbase":
                prefix = "OceanBase";
                break;
            case "dm":
                prefix = "Dm";
                break;
            case "oscar":
                prefix = "Oscar";
                break;
            case "kingbase":
                prefix = "Kingbase";
                break;
            default:
                prefix = "Oracle";
        }
        return (SQLRecognizer)
                Class.forName("org.apache.seata.sqlparser.druid." + dialect + "." + prefix + operation + "Recognizer")
                        .getConstructor(String.class, SQLStatement.class)
                        .newInstance(sql, ast);
    }

    private ParametersHolder parameters() {
        return () -> {
            Map<Integer, ArrayList<Object>> values = new HashMap<>();
            values.put(1, new ArrayList<>(Arrays.asList("first", Null.get())));
            values.put(2, new ArrayList<>(Arrays.asList(10, 20)));
            return values;
        };
    }

    @ParameterizedTest
    @ValueSource(strings = {"mysql", "postgresql", "oracle", "dm", "kingbase", "oscar", "oceanbase", "sqlserver"})
    void qualifiedUpdateColumnsAndParameterMarkers(String dialect) throws Exception {
        String sql = "update orders set amount = ?, name = 'paid' where id = ?";
        SQLUpdateStatement ast = (SQLUpdateStatement) parse(dialect, sql);
        SQLUpdateRecognizer update = (SQLUpdateRecognizer) recognizer(dialect, "Update", sql, ast);
        assertEquals(SQLType.UPDATE, update.getSQLType());
        assertEquals(sql, update.getOriginalSQL());
        assertEquals("orders", update.getTableName());

        assertEquals("?", update.getUpdateValues().get(0).toString());
        assertEquals("paid", update.getUpdateValues().get(1));
        assertEquals(Arrays.asList("amount", "name"), update.getUpdateColumns());
        ast.getItems()
                .get(0)
                .setColumn(new SQLPropertyExpr(new SQLPropertyExpr(new SQLIdentifierExpr("shop"), "orders"), "amount"));
        ast.getItems().get(1).setColumn(new SQLPropertyExpr(new SQLIdentifierExpr("o"), "name"));
        assertEquals(Arrays.asList("shop.orders.amount", "o.name"), update.getUpdateColumns());
        ast.getTableSource().setAlias("o");
        assertEquals("o", update.getTableAlias());
        ast.getItems()
                .get(0)
                .setValue(new SQLBinaryOpExpr(new SQLIntegerExpr(1), SQLBinaryOperator.Add, new SQLIntegerExpr(2)));
        assertThrows(SQLParsingException.class, update::getUpdateValues);
        ast.getItems().get(0).setColumn(new SQLIntegerExpr(1));
        assertThrows(SQLParsingException.class, update::getUpdateColumns);
    }

    @ParameterizedTest
    @ValueSource(strings = {"mysql", "postgresql", "oracle", "dm", "kingbase", "oscar", "oceanbase", "sqlserver"})
    void whereBatchBindingsRetainOrderAndConvertSqlNull(String dialect) throws Exception {
        String sql = "update orders set amount = 1 where id = ? and amount = ?";
        SQLUpdateRecognizer update = (SQLUpdateRecognizer) recognizer(dialect, "Update", sql, parse(dialect, sql));
        ArrayList<List<Object>> appended = new ArrayList<>();
        assertEquals(
                "id = ? AND amount = ?",
                update.getWhereCondition(parameters(), appended).replaceAll("\\s+", " "));
        assertEquals(Arrays.asList(Arrays.asList("first", 10), Arrays.asList(null, 20)), appended);
        assertTrue(update.isSqlSyntaxSupports());
        String deleteSql = "delete from orders where id = ? and amount = ?";
        SQLDeleteRecognizer delete =
                (SQLDeleteRecognizer) recognizer(dialect, "Delete", deleteSql, parse(dialect, deleteSql));
        appended.clear();
        assertEquals(
                "id = ? AND amount = ?",
                delete.getWhereCondition(parameters(), appended).replaceAll("\\s+", " "));
        assertEquals(Arrays.asList(Arrays.asList("first", 10), Arrays.asList(null, 20)), appended);
        assertEquals(SQLType.DELETE, delete.getSQLType());
        assertTrue(delete.isSqlSyntaxSupports());
    }

    @ParameterizedTest
    @ValueSource(strings = {"postgresql", "oracle", "dm", "kingbase", "oscar", "oceanbase", "sqlserver"})
    void noWhereOrUnsupportedLimitAndOrderProduceEmptyConditions(String dialect) throws Exception {
        for (String operation : Arrays.asList("Update", "Delete")) {
            String sql = "Update".equals(operation) ? "update orders set amount = 1" : "delete from orders";
            WhereRecognizer mutation = (WhereRecognizer) recognizer(dialect, operation, sql, parse(dialect, sql));
            assertEquals("", mutation.getWhereCondition());
            assertEquals("", mutation.getWhereCondition(parameters(), new ArrayList<>()));
            assertNull(mutation.getLimitCondition());
            assertNull(mutation.getLimitCondition(parameters(), new ArrayList<>()));
            assertNull(mutation.getOrderByCondition());
            assertNull(mutation.getOrderByCondition(parameters(), new ArrayList<>()));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"postgresql", "oracle", "dm", "kingbase", "oscar", "oceanbase", "sqlserver"})
    void joinedAndUnknownMutationTargetsAreRejected(String dialect) throws Exception {
        String sql = "update orders set amount = 1";
        SQLUpdateStatement ast = (SQLUpdateStatement) parse(dialect, sql);
        SQLUpdateRecognizer update = (SQLUpdateRecognizer) recognizer(dialect, "Update", sql, ast);
        SQLJoinTableSource join = new SQLJoinTableSource(
                new SQLExprTableSource("orders"),
                SQLJoinTableSource.JoinType.INNER_JOIN,
                new SQLExprTableSource("items"),
                null);
        ast.setTableSource(join);
        assertThrows(NotSupportYetException.class, update::getTableName);
        ast.setTableSource(new SQLSubqueryTableSource());
        assertThrows(NotSupportYetException.class, update::getTableName);
        String deleteSql = "delete from orders";
        SQLDeleteStatement deleteAst = (SQLDeleteStatement) parse(dialect, deleteSql);
        SQLDeleteRecognizer delete = (SQLDeleteRecognizer) recognizer(dialect, "Delete", deleteSql, deleteAst);
        deleteAst.setTableSource(join);
        assertThrows(NotSupportYetException.class, delete::getTableName);
        deleteAst.setTableSource(new SQLSubqueryTableSource());
        assertThrows(NotSupportYetException.class, delete::getTableName);
    }

    @ParameterizedTest
    @ValueSource(strings = {"postgresql", "oracle", "dm", "kingbase", "oscar", "oceanbase", "mysql"})
    void unsupportedSubqueriesFailSyntaxValidation(String dialect) throws Exception {
        for (String sql : Arrays.asList(
                "update orders set amount = 1 where id in (select id from items)",
                "insert into orders(id) select id from items")) {
            String operation = sql.startsWith("update") ? "Update" : "Insert";
            SQLRecognizer mutation = recognizer(dialect, operation, sql, parse(dialect, sql));
            assertThrows(NotSupportYetException.class, mutation::isSqlSyntaxSupports);
        }
    }
}
