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
package io.seata.rm.datasource;

import io.seata.core.exception.TransactionException;
import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.model.BranchType;
import io.seata.core.model.ResourceManager;
import io.seata.rm.DefaultResourceManager;
import io.seata.rm.datasource.exec.LockWaitTimeoutException;
import io.seata.rm.datasource.mock.MockConnection;
import io.seata.rm.datasource.mock.MockDriver;
import io.seata.rm.datasource.undo.SQLUndoLog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

/**
 * ConnectionProxy test
 *
 * @author ggndnn
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ConnectionProxyTest {
    private static DataSourceProxy dataSourceProxy;

    private final static String TEST_RESOURCE_ID = "testResourceId";

    private final static String TEST_XID = "testXid";

    private final static String lockKey = "order:123";

    @BeforeAll
    public void initBeforeAll() throws Exception {
        dataSourceProxy = Mockito.mock(DataSourceProxy.class);
        Mockito.when(dataSourceProxy.getResourceId())
                .thenReturn(TEST_RESOURCE_ID);
        ResourceManager rm = Mockito.mock(ResourceManager.class);
        Mockito.when(rm.branchRegister(BranchType.AT, dataSourceProxy.getResourceId(), null, TEST_XID, null, lockKey))
                .thenThrow(new TransactionException(TransactionExceptionCode.LockKeyConflict));
        DefaultResourceManager defaultResourceManager = DefaultResourceManager.get();
        Assertions.assertNotNull(defaultResourceManager);
        DefaultResourceManager.mockResourceManager(BranchType.AT, rm);
    }

    @BeforeEach
    public void initBeforeEach() {
        Assertions.assertTrue(ConnectionProxy.LockRetryPolicy.isLockRetryPolicyBranchRollbackOnConflict());
    }

    @AfterEach
    public void cleanAfterEach() {
        ConnectionProxy.LockRetryPolicy.removeLockRetryPolicyBranchRollbackOnConflict();
    }

    @Test
    public void testLockRetryPolicyRollbackOnConflict() {
        ConnectionProxy.LockRetryPolicy.setLockRetryPolicyBranchRollbackOnConflict(true);

        ConnectionProxy connectionProxy = new ConnectionProxy(dataSourceProxy, new MockConnection(new MockDriver(), "", null));
        connectionProxy.bind(TEST_XID);
        connectionProxy.appendUndoLog(new SQLUndoLog());
        connectionProxy.appendLockKey(lockKey);
        Assertions.assertThrows(LockWaitTimeoutException.class, connectionProxy::commit);
    }

    @Test
    public void testLockRetryPolicyNotRollbackOnConflict() {
        ConnectionProxy.LockRetryPolicy.setLockRetryPolicyBranchRollbackOnConflict(false);

        ConnectionProxy connectionProxy = new ConnectionProxy(dataSourceProxy, null);
        connectionProxy.bind(TEST_XID);
        connectionProxy.appendUndoLog(new SQLUndoLog());
        connectionProxy.appendLockKey(lockKey);
        Assertions.assertThrows(LockWaitTimeoutException.class, connectionProxy::commit);
    }
}
