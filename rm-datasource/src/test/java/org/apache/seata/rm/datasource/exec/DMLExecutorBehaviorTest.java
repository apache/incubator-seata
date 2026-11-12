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

import org.apache.seata.rm.datasource.ConnectionContext;
import org.apache.seata.rm.datasource.ConnectionProxy;
import org.apache.seata.rm.datasource.DataSourceProxy;
import org.apache.seata.rm.datasource.StatementProxy;
import org.apache.seata.rm.datasource.exception.TableMetaException;
import org.apache.seata.rm.datasource.sql.struct.TableRecords;
import org.apache.seata.sqlparser.SQLRecognizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DMLExecutorBehaviorTest {
    private ConnectionProxy connection;
    private Statement statement;
    private StatementCallback<Integer, Statement> callback;
    private AbstractDMLBaseExecutor<Integer, Statement> executor;
    private TableRecords before;
    private TableRecords after;
    private ConnectionContext context;

    @BeforeEach
    void setUp() throws Exception {
        connection = mock(ConnectionProxy.class);
        context = spy(new ConnectionContext());
        when(connection.getContext()).thenReturn(context);
        when(connection.getTargetConnection()).thenReturn(mock(Connection.class));
        statement = mock(Statement.class);
        StatementProxy<Statement> proxy = new StatementProxy<>(connection, statement);
        callback = mock(StatementCallback.class);
        executor = mock(
                AbstractDMLBaseExecutor.class,
                withSettings()
                        .useConstructor(proxy, callback, mock(SQLRecognizer.class))
                        .defaultAnswer(CALLS_REAL_METHODS));
        before = mock(TableRecords.class);
        after = mock(TableRecords.class);
        doReturn(before).when(executor).beforeImage();
        doReturn(after).when(executor).afterImage(before);
        doNothing().when(executor).prepareUndoLog(before, after);
        when(callback.execute(statement, "arg")).thenReturn(5);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void capturesImagesAroundDmlAndCommitsOnlyOwnedTransaction(boolean autoCommit) throws Throwable {
        when(connection.getAutoCommit()).thenReturn(autoCommit);
        assertEquals(5, executor.doExecute("arg"));
        org.mockito.InOrder order = inOrder(executor, callback, connection);
        if (autoCommit) {
            order.verify(connection).changeAutoCommit();
        }
        order.verify(executor).beforeImage();
        order.verify(callback).execute(statement, "arg");
        order.verify(executor).afterImage(before);
        order.verify(executor).prepareUndoLog(before, after);
        if (autoCommit) {
            order.verify(connection).commit();
            order.verify(connection).setAutoCommit(true);
        } else {
            verify(connection, never()).commit();
        }
    }

    @Test
    void dmlFailureRestoresAutoCommitAndResetsContext() throws Exception {
        when(connection.getAutoCommit()).thenReturn(true);
        SQLException failure = new SQLException("DML failed");
        when(callback.execute(statement, "arg")).thenThrow(failure);
        assertSame(failure, assertThrows(SQLException.class, () -> executor.doExecute("arg")));
        verify(connection.getTargetConnection()).rollback();
        verify(connection).setAutoCommit(true);
        verify(connection, never()).commit();
        verify(context).reset();
    }

    @Test
    void staleMetadataTriggersRefreshAndPreservesError() throws Exception {
        DataSourceProxy source = mock(DataSourceProxy.class);
        when(connection.getDataSourceProxy()).thenReturn(source);
        TableMetaException failure = new TableMetaException("orders", "amount");
        doThrow(failure).when(executor).beforeImage();
        assertSame(failure, assertThrows(TableMetaException.class, () -> executor.doExecute("arg")));
        verify(source).tableMetaRefreshEvent();
        verifyNoInteractions(callback);
    }
}
