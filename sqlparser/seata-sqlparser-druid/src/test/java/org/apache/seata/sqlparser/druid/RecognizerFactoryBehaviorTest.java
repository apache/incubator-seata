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

import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.sqlparser.EscapeHandler;
import org.apache.seata.sqlparser.EscapeHandlerFactory;
import org.apache.seata.sqlparser.SQLInsertRecognizer;
import org.apache.seata.sqlparser.SQLRecognizer;
import org.apache.seata.sqlparser.SQLUpdateRecognizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecognizerFactoryBehaviorTest {
    @ParameterizedTest
    @ValueSource(strings = {"mysql", "postgresql", "oracle", "dm", "kingbase", "oscar", "oceanbase", "sqlserver"})
    void updateColumnUnescapingUsesDialectHandler(String dialect) throws Exception {
        String sql = "update orders set amount = 1, price = 2";
        SQLUpdateRecognizer update = (SQLUpdateRecognizer) DialectMutationBehaviorTest.recognizer(
                dialect, "Update", sql, DialectMutationBehaviorTest.parse(dialect, sql));
        EscapeHandler handler = mock(EscapeHandler.class);
        when(handler.delColNameEscape("amount")).thenReturn("unescaped_amount");
        when(handler.delColNameEscape("price")).thenReturn("unescaped_price");
        try (MockedStatic<EscapeHandlerFactory> factory = mockStatic(EscapeHandlerFactory.class)) {
            factory.when(() -> EscapeHandlerFactory.getEscapeHandler(dialect)).thenReturn(handler);
            assertEquals(Arrays.asList("unescaped_amount", "unescaped_price"), update.getUpdateColumnsUnEscape());
            verify(handler).delColNameEscape("amount");
            verify(handler).delColNameEscape("price");
        }
    }

    @Test
    void factoryRejectsEmptyMixedAndMalformedStatements() {
        DruidSQLRecognizerFactoryImpl factory = new DruidSQLRecognizerFactoryImpl();
        assertThrows(UnsupportedOperationException.class, () -> factory.create("", "mysql"));
        assertThrows(
                UnsupportedOperationException.class,
                () -> factory.create("update orders set amount = 1; delete from orders", "mysql"));
        assertThrows(
                UnsupportedOperationException.class,
                () -> factory.create("insert into orders values (1); insert into orders values (2)", "mysql"));
        assertThrows(NotSupportYetException.class, () -> factory.create("update set where", "mysql"));
        assertNull(factory.create("create table orders (id int)", "mysql"));
    }

    @Test
    void factoryPreservesHomogeneousStatementOrder() {
        DruidSQLRecognizerFactoryImpl factory = new DruidSQLRecognizerFactoryImpl();
        for (String operation : Arrays.asList("update", "delete")) {
            String sql = "update".equals(operation)
                    ? "update orders set amount = 1; update items set amount = 2"
                    : "delete from orders; delete from items";
            List<SQLRecognizer> recognizers = factory.create(sql, "mysql");
            assertEquals(2, recognizers.size());
            assertEquals("orders", recognizers.get(0).getTableName());
            assertEquals("items", recognizers.get(1).getTableName());
        }
        String sql = "insert all into orders (id) values (1) into orders (id) values (2) select 1 from dual";
        SQLInsertRecognizer insert =
                (SQLInsertRecognizer) factory.create(sql, "oracle").get(0);
        assertEquals(
                Arrays.asList(Collections.singletonList(1), Collections.singletonList(2)),
                insert.getInsertRows(Collections.singletonList(0)));
    }
}
