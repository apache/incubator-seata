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

import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import org.apache.seata.sqlparser.ParametersHolder;
import org.apache.seata.sqlparser.SQLParsingException;
import org.apache.seata.sqlparser.SQLSelectRecognizer;
import org.apache.seata.sqlparser.SQLType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DialectSelectBehaviorTest {
    @ParameterizedTest
    @ValueSource(strings = {"mysql", "postgresql", "oracle", "dm", "kingbase", "oscar", "oceanbase", "sqlserver"})
    void selectConditionsAndBatchParameters(String dialect) throws Exception {
        String sql = "select * from orders o where id = ? order by id";
        SQLSelectStatement ast = (SQLSelectStatement) DialectMutationBehaviorTest.parse(dialect, sql);
        SQLSelectRecognizer select =
                (SQLSelectRecognizer) DialectMutationBehaviorTest.recognizer(dialect, "SelectForUpdate", sql, ast);
        assertEquals(SQLType.SELECT_FOR_UPDATE, select.getSQLType());
        assertEquals("orders", select.getTableName());
        assertEquals("o", select.getTableAlias());
        assertEquals("id = ?", select.getWhereCondition());
        ParametersHolder parameters = () -> Collections.singletonMap(1, new ArrayList<>(Collections.singletonList(42)));
        ArrayList<List<Object>> appended = new ArrayList<>();
        assertEquals("id = ?", select.getWhereCondition(parameters, appended));
        assertEquals(Collections.singletonList(Collections.singletonList(42)), appended);
        String expectedOrder = "dm".equals(dialect) || "sqlserver".equals(dialect) ? null : "ORDER BY id";
        assertEquals(expectedOrder, select.getOrderByCondition());
        assertEquals(expectedOrder, select.getOrderByCondition(parameters, new ArrayList<>()));
        if ("mysql".equals(dialect) || "postgresql".equals(dialect)) {
            assertEquals("", select.getLimitCondition());
            assertEquals("", select.getLimitCondition(parameters, new ArrayList<>()));
        } else {
            assertNull(select.getLimitCondition());
            assertNull(select.getLimitCondition(parameters, new ArrayList<>()));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"mysql", "postgresql", "oracle", "dm", "kingbase", "oscar", "oceanbase", "sqlserver"})
    void incompleteSelectAstIsRejected(String dialect) throws Exception {
        SQLSelectStatement ast = new SQLSelectStatement();
        SQLSelectRecognizer select =
                (SQLSelectRecognizer) DialectMutationBehaviorTest.recognizer(dialect, "SelectForUpdate", "select", ast);
        assertThrows(SQLParsingException.class, select::getWhereCondition);
        ast.setSelect(new SQLSelect());
        assertThrows(SQLParsingException.class, select::getWhereCondition);
    }
}
