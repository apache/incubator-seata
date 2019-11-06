/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.rm.datasource.exec;

import io.seata.rm.datasource.ConnectionContext;
import io.seata.rm.datasource.ConnectionProxy;
import io.seata.rm.datasource.PreparedStatementProxy;
import io.seata.rm.datasource.sql.SQLInsertRecognizer;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;

/**
 * AbstractDMLBaseExecutor test
 *
 * @author ggndnn
 */
public class AbstractDMLBaseExecutorTest {
    private ConnectionProxy connectionProxy;

    private AbstractDMLBaseExecutor executor;

    private Field branchRollbackFlagField;

    @BeforeEach
    public void initBeforeEach() throws Exception {
        branchRollbackFlagField = ConnectionProxy.LockRetryPolicy.class.getDeclaredField("LOCK_RETRY_POLICY_BRANCH_ROLLBACK_ON_CONFLICT");
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(branchRollbackFlagField, branchRollbackFlagField.getModifiers() & ~Modifier.FINAL);
        branchRollbackFlagField.setAccessible(true);
        boolean branchRollbackFlag = (boolean) branchRollbackFlagField.get(null);
        Assertions.assertTrue(branchRollbackFlag);

        Connection targetConnection = Mockito.mock(Connection.class);
        connectionProxy = Mockito.mock(ConnectionProxy.class);
        Mockito.doThrow(new LockConflictException())
                .when(connectionProxy).commit();
        Mockito.when(connectionProxy.getAutoCommit())
                .thenReturn(Boolean.TRUE);
        Mockito.when(connectionProxy.getTargetConnection())
                .thenReturn(targetConnection);
        Mockito.when(connectionProxy.getContext())
                .thenReturn(new ConnectionContext());
        PreparedStatementProxy statementProxy = Mockito.mock(PreparedStatementProxy.class);
        Mockito.when(statementProxy.getConnectionProxy())
                .thenReturn(connectionProxy);
        StatementCallback statementCallback = Mockito.mock(StatementCallback.class);
        SQLInsertRecognizer sqlInsertRecognizer = Mockito.mock(SQLInsertRecognizer.class);
        TableMeta tableMeta = Mockito.mock(TableMeta.class);
        executor = Mockito.spy(new InsertExecutor(statementProxy, statementCallback, sqlInsertRecognizer));
        Mockito.doReturn(tableMeta)
                .when(executor).getTableMeta();
        TableRecords tableRecords = new TableRecords();
        Mockito.doReturn(tableRecords)
                .when(executor).beforeImage();
        Mockito.doReturn(tableRecords)
                .when(executor).afterImage(tableRecords);
    }

    @Test
    public void testLockRetryPolicyRollbackOnConflict() throws Exception {
        boolean oldBranchRollbackFlag = (boolean) branchRollbackFlagField.get(null);
        branchRollbackFlagField.set(null, true);
        Assertions.assertThrows(LockWaitTimeoutException.class, executor::execute);
        Mockito.verify(connectionProxy.getTargetConnection(), Mockito.atLeastOnce())
                .rollback();
        Mockito.verify(connectionProxy, Mockito.never()).rollback();
        branchRollbackFlagField.set(null, oldBranchRollbackFlag);
    }

    @Test
    public void testLockRetryPolicyNotRollbackOnConflict() throws Throwable {
        boolean oldBranchRollbackFlag = (boolean) branchRollbackFlagField.get(null);
        branchRollbackFlagField.set(null, false);
        Assertions.assertThrows(LockConflictException.class, executor::execute);
        Mockito.verify(connectionProxy.getTargetConnection(), Mockito.times(1)).rollback();
        Mockito.verify(connectionProxy, Mockito.never()).rollback();
        branchRollbackFlagField.set(null, oldBranchRollbackFlag);
    }
}
