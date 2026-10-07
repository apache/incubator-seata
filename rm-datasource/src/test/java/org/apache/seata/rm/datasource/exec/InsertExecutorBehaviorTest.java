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
import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.rm.datasource.ConnectionProxy;
import org.apache.seata.rm.datasource.StatementProxy;
import org.apache.seata.sqlparser.SQLInsertRecognizer;
import org.apache.seata.sqlparser.struct.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InsertExecutorBehaviorTest {
    private BaseInsertExecutor executor;
    private StatementProxy statement;

    @BeforeEach
    void setUp() {
        statement = mock(StatementProxy.class);
        ConnectionProxy connection = mock(ConnectionProxy.class);
        when(statement.getConnectionProxy()).thenReturn(connection);
        when(connection.getDbType()).thenReturn("mysql");
        executor = mock(
                BaseInsertExecutor.class,
                withSettings()
                        .extraInterfaces(Sequenceable.class)
                        .useConstructor(statement, mock(StatementCallback.class), mock(SQLInsertRecognizer.class))
                        .defaultAnswer(CALLS_REAL_METHODS));
    }

    static Stream<Arguments> primaryKeyValues() {
        return Stream.of(
                Arguments.of(mock(Null.class), false, 1, true),
                Arguments.of(mock(Null.class), false, 2, false),
                Arguments.of(mock(Null.class), true, 2, true),
                Arguments.of(1, false, 2, true),
                Arguments.of(mock(SqlMethodExpr.class), false, 1, false),
                Arguments.of(mock(SqlMethodExpr.class), true, 2, true),
                Arguments.of(mock(SqlSequenceExpr.class), false, 1, true),
                Arguments.of(mock(SqlSequenceExpr.class), false, 2, false),
                Arguments.of(mock(SqlSequenceExpr.class), true, 2, true),
                Arguments.of(mock(SqlDefaultExpr.class), false, 1, true),
                Arguments.of(mock(SqlDefaultExpr.class), false, 2, false),
                Arguments.of(mock(SqlDefaultExpr.class), true, 2, true));
    }

    @ParameterizedTest
    @MethodSource("primaryKeyValues")
    void validatesGenerationStrategiesForPlainAndPreparedStatements(
            Object value, boolean prepared, int count, boolean allowed) {
        assertEquals(allowed, executor.checkPkValuesForSinglePk(Collections.nCopies(count, value), prepared));
        assertEquals(
                value instanceof Integer,
                executor.checkPkValuesForSinglePk(Arrays.asList(value, new Object()), prepared));
    }

    @Test
    void compositeKeysAllowAtMostOneGeneratedValueAndNoFunctions() {
        Map<String, List<Object>> values = new LinkedHashMap<>();
        values.put("id", Collections.singletonList(mock(Null.class)));
        values.put("tenant", Collections.singletonList(1));
        assertTrue(executor.checkPkValuesForMultiPk(values));
        values.put("tenant", Collections.singletonList(mock(Null.class)));
        assertFalse(executor.checkPkValuesForMultiPk(values));
        values.put("tenant", Collections.singletonList(mock(SqlMethodExpr.class)));
        assertFalse(executor.checkPkValuesForMultiPk(values));
        assertThrows(ShouldNeverHappenException.class, () -> executor.checkPkValuesForMultiPk(Collections.emptyMap()));
    }

    @Test
    void generatedKeysSupportNamedColumnsAndForwardOnlyDrivers() throws Exception {
        ResultSet keys = mock(ResultSet.class);
        when(statement.getGeneratedKeys()).thenReturn(keys);
        when(keys.next()).thenReturn(true, true, false);
        when(keys.getObject("id")).thenReturn(10, 11);
        doThrow(new SQLException("forward only")).when(keys).beforeFirst();
        assertEquals(Arrays.asList(10, 11), executor.getGeneratedKeys("id"));
        when(keys.next()).thenReturn(true, false);
        when(keys.getObject(1)).thenReturn(12);
        assertEquals(Collections.singletonList(12), executor.getGeneratedKeys(""));
        when(keys.next()).thenReturn(true, false);
        assertEquals(Collections.singletonList(12), executor.getGeneratedKeys());
        when(keys.next()).thenReturn(false);
        assertThrows(NotSupportYetException.class, () -> executor.getGeneratedKeys("id"));
    }

    @Test
    void sequenceFallbackQueriesDriverWhenGeneratedKeysAreUnavailable() throws Exception {
        doThrow(new SQLException("unsupported keys")).when(executor).getGeneratedKeys();
        doThrow(new NotSupportYetException("unsupported keys")).when(executor).getGeneratedKeys("id");
        SqlSequenceExpr sequence = mock(SqlSequenceExpr.class);
        doReturn("SELECT sequence_value").when((Sequenceable) executor).getSequenceSql(sequence);
        Connection connection = mock(Connection.class);
        Statement query = mock(Statement.class);
        ResultSet values = mock(ResultSet.class);
        when(statement.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(query);
        when(query.executeQuery("SELECT sequence_value")).thenReturn(values);
        when(values.next()).thenReturn(true, false, true, false);
        when(values.getObject(1)).thenReturn(42);
        assertEquals(Collections.singletonList(42), executor.getPkValuesBySequence(sequence));
        assertEquals(Collections.singletonList(42), executor.getPkValuesBySequence(sequence, "id"));
        verify(query, times(2)).close();
        verify(values, times(2)).close();
        doReturn(Collections.singletonList(9)).when(executor).getGeneratedKeys("id");
        assertEquals(Collections.singletonList(9), executor.getPkValuesBySequence(sequence, "id"));
        verify(query, times(2)).executeQuery(anyString());
    }
}
