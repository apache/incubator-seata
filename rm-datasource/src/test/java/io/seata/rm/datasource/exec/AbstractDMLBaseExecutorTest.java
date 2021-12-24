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

import io.seata.common.exception.NotSupportYetException;
import io.seata.rm.datasource.ConnectionContext;
import io.seata.rm.datasource.ConnectionProxy;
import io.seata.rm.datasource.PreparedStatementProxy;
import io.seata.rm.datasource.exec.mysql.MySQLInsertExecutor;
import io.seata.rm.datasource.exec.oracle.OracleInsertExecutor;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.sqlparser.SQLInsertRecognizer;
import io.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.mockito.Mockito;

import java.sql.Connection;
import java.util.Arrays;

/**
 * AbstractDMLBaseExecutor test
 *
 * @author ggndnn
 */
@DisabledOnJre(JRE.JAVA_17) // `cglib:cglib:3.1~3.3.0` does not supported java17
public class AbstractDMLBaseExecutorTest {
    private ConnectionProxy connectionProxy;

    private AbstractDMLBaseExecutor executor;

    @BeforeEach
    public void initBeforeEach() throws Exception {
        Assertions.assertTrue(ConnectionProxy.LockRetryPolicy.isLockRetryPolicyBranchRollbackOnConflict());

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
        executor = Mockito.spy(new MySQLInsertExecutor(statementProxy, statementCallback, sqlInsertRecognizer));
        Mockito.doReturn(tableMeta)
                .when(executor).getTableMeta();
        TableRecords tableRecords = new TableRecords();
        Mockito.doReturn(tableRecords)
                .when(executor).beforeImage();
        Mockito.doReturn(tableRecords)
                .when(executor).afterImage(tableRecords);
    }

    @AfterEach
    public void cleanAfterEach() {
        ConnectionProxy.LockRetryPolicy.removeLockRetryPolicyBranchRollbackOnConflict();
    }

    @Test
    public void testLockRetryPolicyRollbackOnConflict() throws Exception {
        ConnectionProxy.LockRetryPolicy.setLockRetryPolicyBranchRollbackOnConflict(true);

        Assertions.assertThrows(LockWaitTimeoutException.class, executor::execute);
        Mockito.verify(connectionProxy.getTargetConnection(), Mockito.atLeastOnce())
                .rollback();
        Mockito.verify(connectionProxy, Mockito.never()).rollback();
    }

    @Test
    public void testLockRetryPolicyNotRollbackOnConflict() throws Throwable {
        ConnectionProxy.LockRetryPolicy.setLockRetryPolicyBranchRollbackOnConflict(false);

        Assertions.assertThrows(LockConflictException.class, executor::execute);
        Mockito.verify(connectionProxy.getTargetConnection(), Mockito.times(1)).rollback();
        Mockito.verify(connectionProxy, Mockito.never()).rollback();
    }

    @Test
    public void testOnlySupportMysqlWhenUseMultiPk(){
        Mockito.when(connectionProxy.getContext())
                .thenReturn(new ConnectionContext());
        PreparedStatementProxy statementProxy = Mockito.mock(PreparedStatementProxy.class);
        Mockito.when(statementProxy.getConnectionProxy())
                .thenReturn(connectionProxy);
        StatementCallback statementCallback = Mockito.mock(StatementCallback.class);
        SQLInsertRecognizer sqlInsertRecognizer = Mockito.mock(SQLInsertRecognizer.class);
        TableMeta tableMeta = Mockito.mock(TableMeta.class);
        executor = Mockito.spy(new OracleInsertExecutor(statementProxy, statementCallback, sqlInsertRecognizer));
        Mockito.when(executor.getDbType()).thenReturn(JdbcConstants.ORACLE);
        Mockito.doReturn(tableMeta).when(executor).getTableMeta();
        Mockito.when(tableMeta.getPrimaryKeyOnlyName()).thenReturn(Arrays.asList("id","userCode"));
        Assertions.assertThrows(NotSupportYetException.class,()-> executor.executeAutoCommitFalse(null));
    }
}
