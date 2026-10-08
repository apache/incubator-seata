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
package org.apache.seata.rm.datasource.exec;

import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.rm.datasource.ConnectionProxy;
import org.apache.seata.rm.datasource.StatementProxy;
import org.apache.seata.rm.datasource.exec.mariadb.*;
import org.apache.seata.rm.datasource.exec.mysql.*;
import org.apache.seata.rm.datasource.exec.polardbx.*;
import org.apache.seata.rm.datasource.exec.sqlserver.*;
import org.apache.seata.rm.datasource.sql.SQLVisitorFactory;
import org.apache.seata.sqlparser.SQLRecognizer;
import org.apache.seata.sqlparser.SQLType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ExecuteTemplateBehaviorTest {
    static Stream<Arguments> routes() {
        return Stream.of(
                Arguments.of("mysql", SQLType.UPDATE, UpdateExecutor.class),
                Arguments.of("sqlserver", SQLType.UPDATE, SqlServerUpdateExecutor.class),
                Arguments.of("mysql", SQLType.DELETE, DeleteExecutor.class),
                Arguments.of("sqlserver", SQLType.DELETE, SqlServerDeleteExecutor.class),
                Arguments.of("mysql", SQLType.SELECT_FOR_UPDATE, SelectForUpdateExecutor.class),
                Arguments.of("sqlserver", SQLType.SELECT_FOR_UPDATE, SqlServerSelectForUpdateExecutor.class),
                Arguments.of("mysql", SQLType.INSERT_ON_DUPLICATE_UPDATE, MySQLInsertOnDuplicateUpdateExecutor.class),
                Arguments.of(
                        "mariadb", SQLType.INSERT_ON_DUPLICATE_UPDATE, MariadbInsertOnDuplicateUpdateExecutor.class),
                Arguments.of(
                        "polardb-x", SQLType.INSERT_ON_DUPLICATE_UPDATE, PolarDBXInsertOnDuplicateUpdateExecutor.class),
                Arguments.of("mysql", SQLType.UPDATE_JOIN, MySQLUpdateJoinExecutor.class),
                Arguments.of("mariadb", SQLType.UPDATE_JOIN, MariadbUpdateJoinExecutor.class),
                Arguments.of("polardb-x", SQLType.UPDATE_JOIN, PolarDBXUpdateJoinExecutor.class),
                Arguments.of("mysql", SQLType.SELECT, PlainExecutor.class));
    }

    @ParameterizedTest
    @MethodSource("routes")
    void routesRecognizedSqlToDialectExecutor(String db, SQLType type, Class<? extends Executor> executorClass)
            throws Throwable {
        StatementProxy statement = mock(StatementProxy.class);
        ConnectionProxy connection = mock(ConnectionProxy.class);
        when(statement.getConnectionProxy()).thenReturn(connection);
        when(connection.getDbType()).thenReturn(db);
        StatementCallback callback = mock(StatementCallback.class);
        SQLRecognizer recognizer = mock(SQLRecognizer.class);
        when(recognizer.getSQLType()).thenReturn(type);
        try (MockedStatic<RootContext> context = mockStatic(RootContext.class);
                MockedConstruction<? extends Executor> executors =
                        mockConstruction(executorClass, (executor, construction) -> {
                            assertSame(statement, construction.arguments().get(0));
                            assertSame(callback, construction.arguments().get(1));
                            try {
                                when(executor.execute("parameter")).thenReturn(42);
                            } catch (Throwable e) {
                                throw new AssertionError(e);
                            }
                        })) {
            context.when(RootContext::getBranchType).thenReturn(BranchType.AT);
            Object result =
                    ExecuteTemplate.execute(Collections.singletonList(recognizer), statement, callback, "parameter");
            assertEquals(Integer.valueOf(42), result);
            assertEquals(1, executors.constructed().size());
            verify(executors.constructed().get(0)).execute("parameter");
            verifyNoInteractions(callback);
        }
    }

    @Test
    void unrecognizedSqlUsesCallbackAndWrapsRuntimeFailures() throws Exception {
        Statement target = mock(Statement.class);
        StatementProxy<Statement> statement = mock(StatementProxy.class);
        ConnectionProxy connection = mock(ConnectionProxy.class);
        when(statement.getTargetStatement()).thenReturn(target);
        when(statement.getConnectionProxy()).thenReturn(connection);
        when(statement.getTargetSQL()).thenReturn("select 1");
        when(connection.getDbType()).thenReturn("mysql");
        try (MockedStatic<RootContext> context = mockStatic(RootContext.class);
                MockedStatic<SQLVisitorFactory> visitors = mockStatic(SQLVisitorFactory.class)) {
            context.when(RootContext::requireGlobalLock).thenReturn(true);
            visitors.when(() -> SQLVisitorFactory.get("select 1", "mysql")).thenReturn(Collections.emptyList());
            assertEquals(
                    Integer.valueOf(7),
                    ExecuteTemplate.execute(
                            statement,
                            (s, args) -> {
                                assertSame(target, s);
                                assertArrayEquals(new Object[] {"arg"}, args);
                                return 7;
                            },
                            "arg"));
            RuntimeException failure = new IllegalStateException("callback");
            assertSame(
                    failure,
                    assertThrows(
                                    SQLException.class,
                                    () -> ExecuteTemplate.execute(statement, (s, args) -> {
                                        throw failure;
                                    }))
                            .getCause());
            SQLException sql = new SQLException("driver");
            assertSame(
                    sql,
                    assertThrows(
                            SQLException.class,
                            () -> ExecuteTemplate.execute(statement, (s, args) -> {
                                throw sql;
                            })));
        }
    }

    @Test
    void multipleStatementsUseMultiExecutor() throws Throwable {
        StatementProxy statement = mock(StatementProxy.class);
        ConnectionProxy connection = mock(ConnectionProxy.class);
        when(statement.getConnectionProxy()).thenReturn(connection);
        when(connection.getDbType()).thenReturn("mysql");
        try (MockedStatic<RootContext> context = mockStatic(RootContext.class);
                MockedConstruction<MultiExecutor> executors =
                        mockConstruction(MultiExecutor.class, (executor, construction) -> {
                            try {
                                when(executor.execute()).thenReturn(2);
                            } catch (Throwable e) {
                                throw new AssertionError(e);
                            }
                        })) {
            context.when(RootContext::getBranchType).thenReturn(BranchType.AT);
            Object result = ExecuteTemplate.execute(
                    Arrays.asList(mock(SQLRecognizer.class), mock(SQLRecognizer.class)),
                    statement,
                    mock(StatementCallback.class));
            assertEquals(Integer.valueOf(2), result);
            assertEquals(1, executors.constructed().size());
        }
    }

    @Test
    void unsupportedDialectOperationsFailBeforeCallback() {
        StatementProxy statement = mock(StatementProxy.class);
        ConnectionProxy connection = mock(ConnectionProxy.class);
        when(statement.getConnectionProxy()).thenReturn(connection);
        when(connection.getDbType()).thenReturn("oracle");
        StatementCallback callback = mock(StatementCallback.class);
        try (MockedStatic<RootContext> context = mockStatic(RootContext.class)) {
            context.when(RootContext::getBranchType).thenReturn(BranchType.AT);
            for (SQLType type : Arrays.asList(SQLType.UPDATE_JOIN, SQLType.INSERT_ON_DUPLICATE_UPDATE)) {
                SQLRecognizer recognizer = mock(SQLRecognizer.class);
                when(recognizer.getSQLType()).thenReturn(type);
                assertThrows(
                        NotSupportYetException.class,
                        () -> ExecuteTemplate.execute(Collections.singletonList(recognizer), statement, callback));
            }
            verifyNoInteractions(callback);
        }
    }

    @Test
    void nonTransactionalCallDoesNotParseSql() throws Exception {
        StatementProxy<Statement> statement = mock(StatementProxy.class);
        Statement target = mock(Statement.class);
        when(statement.getTargetStatement()).thenReturn(target);
        try (MockedStatic<RootContext> context = mockStatic(RootContext.class)) {
            assertEquals(Integer.valueOf(9), ExecuteTemplate.execute(statement, (s, args) -> {
                assertSame(target, s);
                return 9;
            }));
            verify(statement, never()).getConnectionProxy();
        }
    }
}
