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
package org.apache.seata.rm.datasource;

import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.rm.datasource.sql.SQLVisitorFactory;
import org.apache.seata.rm.datasource.sql.struct.TableMetaCacheFactory;
import org.apache.seata.sqlparser.SQLRecognizer;
import org.apache.seata.sqlparser.SQLType;
import org.apache.seata.sqlparser.struct.TableMeta;
import org.apache.seata.sqlparser.struct.TableMetaCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

import java.lang.reflect.Method;
import java.sql.*;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JdbcConnectionProxyBehaviorTest {
    static Stream<Method> delegatedMethods() {
        return Arrays.stream(AbstractConnectionProxy.class.getDeclaredMethods())
                .filter(method -> !method.getName().startsWith("prepare")
                        && !method.getName().startsWith("createStatement")
                        && !Arrays.asList("getTargetConnection", "getDataSourceProxy", "getDbType")
                                .contains(method.getName()))
                .filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("delegatedMethods")
    void jdbcDelegatesArgumentsAndResults(Method method) throws Exception {
        Object[] arguments = Arrays.stream(method.getParameterTypes())
                .map(JdbcConnectionProxyBehaviorTest::value)
                .toArray();
        Object expected = method.getReturnType() == void.class ? null : value(method.getReturnType());
        Connection target = mock(Connection.class, invocation -> {
            assertEquals(method.getName(), invocation.getMethod().getName());
            assertArrayEquals(arguments, invocation.getArguments());
            return expected;
        });
        AbstractConnectionProxy proxy = mock(
                AbstractConnectionProxy.class,
                withSettings()
                        .useConstructor(mock(DataSourceProxy.class), target)
                        .defaultAnswer(CALLS_REAL_METHODS));
        assertEquals(expected, method.invoke(proxy, arguments));
        assertEquals(1, mockingDetails(target).getInvocations().size());
    }

    @Test
    void statementOverloadsWrapDriverStatements() throws Exception {
        Connection target = mock(Connection.class);
        Statement statement = mock(Statement.class);
        PreparedStatement prepared = mock(PreparedStatement.class);
        DataSourceProxy source = mock(DataSourceProxy.class);
        AbstractConnectionProxy proxy = new ConnectionProxy(source, target);
        for (Method method : AbstractConnectionProxy.class.getDeclaredMethods()) {
            if (!method.getName().equals("createStatement") && !method.getName().equals("prepareStatement")) {
                continue;
            }
            Object[] args = Arrays.stream(method.getParameterTypes())
                    .map(JdbcConnectionProxyBehaviorTest::value)
                    .toArray();
            Object driver = method.getName().equals("createStatement") ? statement : prepared;
            doReturn(driver).when(target);
            Connection.class
                    .getMethod(method.getName(), method.getParameterTypes())
                    .invoke(target, args);
            try (MockedStatic<RootContext> context = mockStatic(RootContext.class)) {
                StatementProxy<?> wrapped = (StatementProxy<?>) method.invoke(proxy, args);
                assertSame(driver, wrapped.getTargetStatement());
                assertSame(proxy, wrapped.getConnectionProxy());
            }
        }
    }

    @Test
    void insertRequestsPrimaryKeyColumnsFromMetadata() throws Exception {
        Connection target = mock(Connection.class);
        DataSourceProxy source = mock(DataSourceProxy.class);
        when(source.getDbType()).thenReturn("oracle");
        when(source.getResourceId()).thenReturn("resource");
        AbstractConnectionProxy proxy = new ConnectionProxy(source, target);
        SQLRecognizer recognizer = mock(SQLRecognizer.class);
        when(recognizer.getSQLType()).thenReturn(SQLType.INSERT);
        when(recognizer.getTableName()).thenReturn("orders");
        TableMetaCache cache = mock(TableMetaCache.class);
        TableMeta meta = mock(TableMeta.class);
        when(meta.getPrimaryKeyOnlyName()).thenReturn(Collections.singletonList("ID"));
        when(cache.getTableMeta(target, "orders", "resource")).thenReturn(meta);
        PreparedStatement prepared = mock(PreparedStatement.class);
        when(target.prepareStatement(eq("insert"), eq(new String[] {"ID"}))).thenReturn(prepared);
        try (MockedStatic<RootContext> context = mockStatic(RootContext.class);
                MockedStatic<SQLVisitorFactory> visitors = mockStatic(SQLVisitorFactory.class);
                MockedStatic<TableMetaCacheFactory> caches = mockStatic(TableMetaCacheFactory.class)) {
            context.when(RootContext::getBranchType).thenReturn(BranchType.AT);
            visitors.when(() -> SQLVisitorFactory.get("insert", "oracle"))
                    .thenReturn(Collections.singletonList(recognizer));
            caches.when(() -> TableMetaCacheFactory.getTableMetaCache("oracle")).thenReturn(cache);
            assertSame(prepared, ((PreparedStatementProxy) proxy.prepareStatement("insert")).getTargetStatement());
        }
    }

    @Test
    void callableStatementsCheckGlobalTransactionBeforeDelegating() throws Exception {
        Connection target = mock(Connection.class);
        AbstractConnectionProxy proxy = mock(
                AbstractConnectionProxy.class,
                withSettings()
                        .useConstructor(mock(DataSourceProxy.class), target)
                        .defaultAnswer(CALLS_REAL_METHODS));
        for (Method method : AbstractConnectionProxy.class.getDeclaredMethods()) {
            if (!method.getName().equals("prepareCall")) {
                continue;
            }
            Object[] args = Arrays.stream(method.getParameterTypes())
                    .map(JdbcConnectionProxyBehaviorTest::value)
                    .toArray();
            CallableStatement call = mock(CallableStatement.class);
            doReturn(call).when(target);
            Connection.class
                    .getMethod(method.getName(), method.getParameterTypes())
                    .invoke(target, args);
            try (MockedStatic<RootContext> context = mockStatic(RootContext.class)) {
                assertSame(call, method.invoke(proxy, args));
                context.verify(RootContext::assertNotInGlobalTransaction);
            }
        }
    }

    private static Object value(Class<?> type) {
        if (type == int.class) {
            return 7;
        }
        if (type == boolean.class) {
            return true;
        }
        if (type == String.class) {
            return "value";
        }
        if (type == Class.class) {
            return Connection.class;
        }
        if (type == int[].class) {
            return new int[] {1, 2};
        }
        if (type == String[].class) {
            return new String[] {"ID"};
        }
        if (type == Object[].class) {
            return new Object[] {"value"};
        }
        if (type == Properties.class) {
            Properties p = new Properties();
            p.setProperty("key", "value");
            return p;
        }
        if (type == Map.class) {
            return Collections.singletonMap("type", String.class);
        }
        if (type == SQLWarning.class) {
            return new SQLWarning("warning");
        }
        if (type == Object.class) {
            return new Object();
        }
        return mock(type);
    }
}
