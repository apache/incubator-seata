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
package org.apache.seata.sqlparser.druid.mysql;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLIntegerExpr;
import com.alibaba.druid.sql.ast.statement.SQLJoinTableSource;
import com.alibaba.druid.sql.ast.statement.SQLSubqueryTableSource;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;
import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.sqlparser.SQLParsingException;
import org.apache.seata.sqlparser.SQLType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MySQLJoinBehaviorTest {
    @Test
    void nestedJoinsExposeAllTablesAndAliases() {
        String sql = "update orders o join items i on o.id = i.id join payments p on p.id = o.id set o.amount = 1";
        MySqlUpdateStatement ast =
                (MySqlUpdateStatement) SQLUtils.parseStatements(sql, "mysql").get(0);
        MySQLUpdateRecognizer update = new MySQLUpdateRecognizer(sql, ast);
        assertEquals(SQLType.UPDATE_JOIN, update.getSQLType());
        assertTrue(update.getTableName().endsWith("#orders#items#payments"));
        assertEquals("o", update.getTableAlias("orders"));
        assertEquals("i", update.getTableAlias("items"));
        assertEquals("p", update.getTableAlias("payments"));
        assertEquals("p.id = o.id", update.getJoinCondition(Collections::emptyMap, new ArrayList<>()));
        SQLJoinTableSource original = (SQLJoinTableSource) ast.getTableSource();
        SQLJoinTableSource rightNested = new SQLJoinTableSource(
                original.getRight(),
                SQLJoinTableSource.JoinType.INNER_JOIN,
                original.getLeft(),
                original.getCondition());
        ast.setTableSource(rightNested);
        assertTrue(update.getTableName().endsWith("#payments#orders#items"));
    }

    @Test
    void simpleUpdateHasNoJoinAndUnknownTargetFails() {
        String sql = "update orders set amount = 1";
        MySqlUpdateStatement ast =
                (MySqlUpdateStatement) SQLUtils.parseStatements(sql, "mysql").get(0);
        MySQLUpdateRecognizer update = new MySQLUpdateRecognizer(sql, ast);
        assertEquals("", update.getJoinCondition(Collections::emptyMap, new ArrayList<>()));
        assertEquals("", update.getLimitCondition());
        assertEquals("", update.getLimitCondition(Collections::emptyMap, new ArrayList<>()));
        assertEquals("", update.getOrderByCondition());
        assertEquals("", update.getOrderByCondition(Collections::emptyMap, new ArrayList<>()));
        ast.setTableSource(new SQLSubqueryTableSource());
        assertThrows(NotSupportYetException.class, update::getSQLType);
        assertThrows(NotSupportYetException.class, update::getTableName);
    }

    @Test
    void duplicateKeyUpdateExtractsColumnsAndRejectsUnexpectedTargets() {
        String sql = "insert into orders (id, amount) values (1, 2) on duplicate key update amount = 3";
        MySqlInsertStatement ast =
                (MySqlInsertStatement) SQLUtils.parseStatements(sql, "mysql").get(0);
        MySQLInsertRecognizer insert = new MySQLInsertRecognizer(sql, ast);
        assertEquals(SQLType.INSERT_ON_DUPLICATE_UPDATE, insert.getSQLType());
        assertEquals(Collections.singletonList("amount"), insert.getDuplicateKeyUpdate());
        SQLBinaryOpExpr update = (SQLBinaryOpExpr) ast.getDuplicateKeyUpdate().get(0);
        update.setLeft(new SQLIntegerExpr(1));
        assertThrows(SQLParsingException.class, insert::getDuplicateKeyUpdate);
    }
}
