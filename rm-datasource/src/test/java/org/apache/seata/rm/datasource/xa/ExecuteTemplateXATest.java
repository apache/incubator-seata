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
package org.apache.seata.rm.datasource.xa;

import org.apache.seata.rm.datasource.exec.StatementCallback;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Tests for ExecuteTemplateXA
 *
 */
public class ExecuteTemplateXATest {

    private AbstractConnectionProxyXA mockConnectionProxyXA;
    private Statement mockStatement;
    private StatementCallback<String, Statement> stringCallback;

    @BeforeEach
    public void setUp() throws SQLException {
        // Mock connection proxy with default autoCommit=true
        mockConnectionProxyXA = Mockito.mock(AbstractConnectionProxyXA.class);
        Mockito.when(mockConnectionProxyXA.getAutoCommit()).thenReturn(true);

        // Mock statements
        mockStatement = Mockito.mock(Statement.class);

        // Default callback that returns "success"
        stringCallback = (statement, args) -> "success";
    }

    @Test
    public void testExecuteSuccessWithAutoCommitTrue() throws SQLException {
        // Using default setup: autoCommit=true, stringCallback returns "success"

        // Execute
        String result = ExecuteTemplateXA.execute(mockConnectionProxyXA, stringCallback, mockStatement);

        // Verify
        Assertions.assertEquals("success", result);
        Mockito.verify(mockConnectionProxyXA).getAutoCommit();
        Mockito.verify(mockConnectionProxyXA).setAutoCommit(false);
        Mockito.verify(mockConnectionProxyXA).commit();
        Mockito.verify(mockConnectionProxyXA).setAutoCommit(true);
        Mockito.verify(mockStatement, Mockito.never()).close();
    }

    @Test
    public void testExecuteSuccessWithAutoCommitFalse() throws SQLException {
        // Override default autoCommit to false
        Mockito.when(mockConnectionProxyXA.getAutoCommit()).thenReturn(false);

        // Execute
        String result = ExecuteTemplateXA.execute(mockConnectionProxyXA, stringCallback, mockStatement);

        // Verify
        Assertions.assertEquals("success", result);
        Mockito.verify(mockConnectionProxyXA).getAutoCommit();
        Mockito.verify(mockConnectionProxyXA, Mockito.never()).setAutoCommit(Mockito.anyBoolean());
        Mockito.verify(mockConnectionProxyXA, Mockito.never()).commit();
        Mockito.verify(mockStatement, Mockito.never()).close();
    }

    @Test
    public void testExecuteWithSQLExceptionDuringExecution() throws SQLException {
        // Using default setup: autoCommit=true

        // Callback that throws SQLException
        StatementCallback<String, Statement> failingCallback = (statement, args) -> {
            throw new SQLException("execution failed");
        };

        // Execute and expect exception
        SQLException exception = Assertions.assertThrows(SQLException.class, () -> {
            ExecuteTemplateXA.execute(mockConnectionProxyXA, failingCallback, mockStatement);
        });

        // Verify
        Assertions.assertEquals("execution failed", exception.getMessage());
        Mockito.verify(mockConnectionProxyXA).getAutoCommit();
        Mockito.verify(mockConnectionProxyXA).setAutoCommit(false);
        Mockito.verify(mockConnectionProxyXA).rollback();
        Mockito.verify(mockConnectionProxyXA).setAutoCommit(true);
    }

    @Test
    public void testExecuteWithRuntimeExceptionDuringExecution() throws SQLException {
        StatementCallback<String, Statement> callback = (statement, args) -> {
            throw new RuntimeException("runtime exception");
        };

        // Execute and expect exception
        SQLException exception = Assertions.assertThrows(SQLException.class, () -> {
            ExecuteTemplateXA.execute(mockConnectionProxyXA, callback, mockStatement);
        });

        // Verify
        Assertions.assertEquals("java.lang.RuntimeException: runtime exception", exception.getMessage());
        Mockito.verify(mockConnectionProxyXA).getAutoCommit();
        Mockito.verify(mockConnectionProxyXA).setAutoCommit(false);
        Mockito.verify(mockConnectionProxyXA).rollback();
        Mockito.verify(mockConnectionProxyXA).setAutoCommit(true);
    }

    @Test
    public void testExecuteWithCommitFailure() throws SQLException {
        Mockito.doThrow(new SQLException("commit failed"))
                .when(mockConnectionProxyXA)
                .commit();

        // Execute and expect exception
        SQLException exception = Assertions.assertThrows(SQLException.class, () -> {
            ExecuteTemplateXA.execute(mockConnectionProxyXA, stringCallback, mockStatement);
        });

        // Verify
        Assertions.assertEquals("commit failed", exception.getMessage());
        Mockito.verify(mockConnectionProxyXA).getAutoCommit();
        Mockito.verify(mockConnectionProxyXA).setAutoCommit(false);
        Mockito.verify(mockConnectionProxyXA).commit();
        Mockito.verify(mockConnectionProxyXA).rollback();
        Mockito.verify(mockConnectionProxyXA).setAutoCommit(true);
    }

    @Test
    public void testExecuteWithCommitFailureAndXA_NOT_END() throws SQLException {

        // Create SQLException with XA_NOT_END SQLState
        SQLException xaNotEndException = new SQLException("XA not end", AbstractConnectionProxyXA.SQLSTATE_XA_NOT_END);
        Mockito.doThrow(xaNotEndException).when(mockConnectionProxyXA).commit();

        // Execute and expect exception
        SQLException exception = Assertions.assertThrows(SQLException.class, () -> {
            ExecuteTemplateXA.execute(mockConnectionProxyXA, stringCallback, mockStatement);
        });

        // Verify
        Assertions.assertEquals("XA not end", exception.getMessage());
        Mockito.verify(mockConnectionProxyXA).getAutoCommit();
        Mockito.verify(mockConnectionProxyXA).setAutoCommit(false);
        Mockito.verify(mockConnectionProxyXA).commit();
        // Should not rollback when XA_NOT_END
        Mockito.verify(mockConnectionProxyXA, Mockito.never()).rollback();
        Mockito.verify(mockConnectionProxyXA).setAutoCommit(true);
    }

    @Test
    public void testExecuteWithRollbackFailure() throws SQLException {

        Mockito.doThrow(new SQLException("rollback failed"))
                .when(mockConnectionProxyXA)
                .rollback();

        StatementCallback<String, Statement> callback = (statement, args) -> {
            throw new SQLException("execution failed");
        };

        // Execute and expect the original exception (not the rollback failure)
        SQLException exception = Assertions.assertThrows(SQLException.class, () -> {
            ExecuteTemplateXA.execute(mockConnectionProxyXA, callback, mockStatement);
        });

        // Verify
        Assertions.assertEquals("execution failed", exception.getMessage());
        Mockito.verify(mockConnectionProxyXA).getAutoCommit();
        Mockito.verify(mockConnectionProxyXA).setAutoCommit(false);
        Mockito.verify(mockConnectionProxyXA).rollback();
        Mockito.verify(mockConnectionProxyXA).setAutoCommit(true);
    }

    @Test
    public void testExecuteWithArguments() throws SQLException {
        Mockito.when(mockConnectionProxyXA.getAutoCommit()).thenReturn(false);

        // Mock statement and callback with arguments
        PreparedStatement mockStatement = Mockito.mock(PreparedStatement.class);
        StatementCallback<Integer, PreparedStatement> callback = (statement, args) -> {
            Assertions.assertEquals(2, args.length);
            Assertions.assertEquals("arg1", args[0]);
            Assertions.assertEquals(42, args[1]);
            return 1;
        };

        // Execute with arguments
        Integer result = ExecuteTemplateXA.execute(mockConnectionProxyXA, callback, mockStatement, "arg1", 42);

        // Verify
        Assertions.assertEquals(1, result);
        Mockito.verify(mockConnectionProxyXA).getAutoCommit();
        Mockito.verify(mockConnectionProxyXA, Mockito.never()).setAutoCommit(Mockito.anyBoolean());
    }
}
