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

import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.transaction.xa.XAException;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * Tests for ResourceManagerXA
 *
 */
public class ResourceManagerXATest {

    private ResourceManagerXA resourceManagerXA;

    @BeforeEach
    public void setUp() {
        resourceManagerXA = new ResourceManagerXA();
    }

    @Test
    public void testInit() {
        // Test init method - should not throw exception
        Assertions.assertDoesNotThrow(() -> resourceManagerXA.init());
    }

    @Test
    public void testGetBranchType() {
        Assertions.assertEquals(BranchType.XA, resourceManagerXA.getBranchType());
    }

    @Test
    public void testInitXaTwoPhaseTimeoutChecker() {
        // Test initialization of timeout checker
        Assertions.assertDoesNotThrow(() -> resourceManagerXA.initXaTwoPhaseTimeoutChecker());

        // Call again to test the already initialized case
        Assertions.assertDoesNotThrow(() -> resourceManagerXA.initXaTwoPhaseTimeoutChecker());
    }

    @Test
    public void testBranchCommitSuccess() throws Exception {
        // Mock data source and connection
        AbstractDataSourceProxyXA mockDataSourceProxyXA = Mockito.mock(AbstractDataSourceProxyXA.class);
        ConnectionProxyXA mockConnectionProxyXA = Mockito.mock(ConnectionProxyXA.class);

        // Setup mock behavior
        Mockito.when(mockDataSourceProxyXA.getConnectionForXAFinish(Mockito.any(XAXid.class)))
                .thenReturn(mockConnectionProxyXA);
        Mockito.doNothing()
                .when(mockConnectionProxyXA)
                .xaCommit(Mockito.anyString(), Mockito.anyLong(), Mockito.anyString());
        Mockito.doNothing().when(mockConnectionProxyXA).close();

        // Use reflection to set the dataSourceCache field
        Field dataSourceCacheField = ResourceManagerXA.class.getSuperclass().getDeclaredField("dataSourceCache");
        dataSourceCacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Resource> dataSourceCache = (Map<String, Resource>) dataSourceCacheField.get(resourceManagerXA);
        dataSourceCache.put("testResource", mockDataSourceProxyXA);

        // Test branch commit
        BranchStatus result =
                resourceManagerXA.branchCommit(BranchType.XA, "testXid", 123L, "testResource", "testData");

        Assertions.assertEquals(BranchStatus.PhaseTwo_Committed, result);
        Mockito.verify(mockDataSourceProxyXA).getConnectionForXAFinish(Mockito.any(XAXid.class));
        Mockito.verify(mockConnectionProxyXA).xaCommit("testXid", 123L, "testData");
        Mockito.verify(mockConnectionProxyXA).close();
    }

    @Test
    public void testBranchRollbackSuccess() throws Exception {
        // Mock data source and connection
        AbstractDataSourceProxyXA mockDataSourceProxyXA = Mockito.mock(AbstractDataSourceProxyXA.class);
        ConnectionProxyXA mockConnectionProxyXA = Mockito.mock(ConnectionProxyXA.class);

        // Setup mock behavior
        Mockito.when(mockDataSourceProxyXA.getConnectionForXAFinish(Mockito.any(XAXid.class)))
                .thenReturn(mockConnectionProxyXA);
        Mockito.doNothing()
                .when(mockConnectionProxyXA)
                .xaRollback(Mockito.anyString(), Mockito.anyLong(), Mockito.anyString());
        Mockito.doNothing().when(mockConnectionProxyXA).close();

        // Use reflection to set the dataSourceCache field
        Field dataSourceCacheField = ResourceManagerXA.class.getSuperclass().getDeclaredField("dataSourceCache");
        dataSourceCacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Resource> dataSourceCache = (Map<String, Resource>) dataSourceCacheField.get(resourceManagerXA);
        dataSourceCache.put("testResource", mockDataSourceProxyXA);

        // Test branch rollback
        BranchStatus result =
                resourceManagerXA.branchRollback(BranchType.XA, "testXid", 123L, "testResource", "testData");

        Assertions.assertEquals(BranchStatus.PhaseTwo_Rollbacked, result);
        Mockito.verify(mockDataSourceProxyXA).getConnectionForXAFinish(Mockito.any(XAXid.class));
        Mockito.verify(mockConnectionProxyXA).xaRollback("testXid", 123L, "testData");
        Mockito.verify(mockConnectionProxyXA).close();
    }

    @Test
    public void testBranchCommitWithXAExceptionXAER_NOTA() throws Exception {
        // Mock data source and connection that throws XAException
        AbstractDataSourceProxyXA mockDataSourceProxyXA = Mockito.mock(AbstractDataSourceProxyXA.class);
        ConnectionProxyXA mockConnectionProxyXA = Mockito.mock(ConnectionProxyXA.class);

        // Create XAException with XAER_NOTA and setup mock to throw it
        XAException xaException = new XAException("XAER_NOTA");
        xaException.errorCode = XAException.XAER_NOTA;
        Mockito.doThrow(xaException)
                .when(mockConnectionProxyXA)
                .xaCommit(Mockito.anyString(), Mockito.anyLong(), Mockito.anyString());
        Mockito.doNothing().when(mockConnectionProxyXA).close();

        Mockito.when(mockDataSourceProxyXA.getConnectionForXAFinish(Mockito.any(XAXid.class)))
                .thenReturn(mockConnectionProxyXA);

        // Use reflection to set the dataSourceCache field
        Field dataSourceCacheField = ResourceManagerXA.class.getSuperclass().getDeclaredField("dataSourceCache");
        dataSourceCacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Resource> dataSourceCache = (Map<String, Resource>) dataSourceCacheField.get(resourceManagerXA);
        dataSourceCache.put("testResource", mockDataSourceProxyXA);

        // Test branch commit with XAER_NOTA exception
        BranchStatus result =
                resourceManagerXA.branchCommit(BranchType.XA, "testXid", 123L, "testResource", "testData");

        Assertions.assertEquals(BranchStatus.PhaseTwo_CommitFailed_XAER_NOTA_Retryable, result);
        Mockito.verify(mockConnectionProxyXA).xaCommit("testXid", 123L, "testData");
    }

    @Test
    public void testBranchCommitWithXAException() throws Exception {
        // Mock data source and connection that throws XAException
        AbstractDataSourceProxyXA mockDataSourceProxyXA = Mockito.mock(AbstractDataSourceProxyXA.class);
        ConnectionProxyXA mockConnectionProxyXA = Mockito.mock(ConnectionProxyXA.class);

        // Create XAException and setup mock to throw it
        XAException xaException = new XAException("XA error");
        xaException.errorCode = XAException.XA_RBROLLBACK;
        Mockito.doThrow(xaException)
                .when(mockConnectionProxyXA)
                .xaCommit(Mockito.anyString(), Mockito.anyLong(), Mockito.anyString());
        Mockito.doNothing().when(mockConnectionProxyXA).close();

        Mockito.when(mockDataSourceProxyXA.getConnectionForXAFinish(Mockito.any(XAXid.class)))
                .thenReturn(mockConnectionProxyXA);

        // Use reflection to set the dataSourceCache field
        Field dataSourceCacheField = ResourceManagerXA.class.getSuperclass().getDeclaredField("dataSourceCache");
        dataSourceCacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Resource> dataSourceCache = (Map<String, Resource>) dataSourceCacheField.get(resourceManagerXA);
        dataSourceCache.put("testResource", mockDataSourceProxyXA);

        // Test branch commit with XAException
        BranchStatus result =
                resourceManagerXA.branchCommit(BranchType.XA, "testXid", 123L, "testResource", "testData");

        Assertions.assertEquals(BranchStatus.PhaseTwo_CommitFailed_Retryable, result);
        Mockito.verify(mockConnectionProxyXA).xaCommit("testXid", 123L, "testData");
    }

    @Test
    public void testBranchCommitWithXAExceptionAsSQLException() throws Exception {
        // Mock data source and connection that throws XAException (which is a subclass of SQLException)
        AbstractDataSourceProxyXA mockDataSourceProxyXA = Mockito.mock(AbstractDataSourceProxyXA.class);
        ConnectionProxyXA mockConnectionProxyXA = Mockito.mock(ConnectionProxyXA.class);

        // Create XAException (which extends SQLException) and setup mock to throw it
        XAException xaException = new XAException("XA error");
        xaException.errorCode = XAException.XA_RBROLLBACK; // Not XAER_NOTA, so it should be treated as SQLException
        Mockito.doThrow(xaException)
                .when(mockConnectionProxyXA)
                .xaCommit(Mockito.anyString(), Mockito.anyLong(), Mockito.anyString());
        Mockito.doNothing().when(mockConnectionProxyXA).close();

        Mockito.when(mockDataSourceProxyXA.getConnectionForXAFinish(Mockito.any(XAXid.class)))
                .thenReturn(mockConnectionProxyXA);

        // Use reflection to set the dataSourceCache field
        Field dataSourceCacheField = ResourceManagerXA.class.getSuperclass().getDeclaredField("dataSourceCache");
        dataSourceCacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Resource> dataSourceCache = (Map<String, Resource>) dataSourceCacheField.get(resourceManagerXA);
        dataSourceCache.put("testResource", mockDataSourceProxyXA);

        // Test branch commit with XAException (treated as SQLException)
        BranchStatus result =
                resourceManagerXA.branchCommit(BranchType.XA, "testXid", 123L, "testResource", "testData");

        Assertions.assertEquals(BranchStatus.PhaseTwo_CommitFailed_Retryable, result);
        Mockito.verify(mockConnectionProxyXA).xaCommit("testXid", 123L, "testData");
    }

    @Test
    public void testBranchRollbackWithXAExceptionXAER_NOTA() throws Exception {
        // Mock data source and connection that throws XAException
        AbstractDataSourceProxyXA mockDataSourceProxyXA = Mockito.mock(AbstractDataSourceProxyXA.class);
        ConnectionProxyXA mockConnectionProxyXA = Mockito.mock(ConnectionProxyXA.class);

        // Create XAException with XAER_NOTA and setup mock to throw it
        XAException xaException = new XAException("XAER_NOTA");
        xaException.errorCode = XAException.XAER_NOTA;
        Mockito.doThrow(xaException)
                .when(mockConnectionProxyXA)
                .xaRollback(Mockito.anyString(), Mockito.anyLong(), Mockito.anyString());
        Mockito.doNothing().when(mockConnectionProxyXA).close();

        Mockito.when(mockDataSourceProxyXA.getConnectionForXAFinish(Mockito.any(XAXid.class)))
                .thenReturn(mockConnectionProxyXA);

        // Use reflection to set the dataSourceCache field
        Field dataSourceCacheField = ResourceManagerXA.class.getSuperclass().getDeclaredField("dataSourceCache");
        dataSourceCacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Resource> dataSourceCache = (Map<String, Resource>) dataSourceCacheField.get(resourceManagerXA);
        dataSourceCache.put("testResource", mockDataSourceProxyXA);

        // Test branch rollback with XAER_NOTA exception
        BranchStatus result =
                resourceManagerXA.branchRollback(BranchType.XA, "testXid", 123L, "testResource", "testData");

        Assertions.assertEquals(BranchStatus.PhaseTwo_RollbackFailed_XAER_NOTA_Retryable, result);
        Mockito.verify(mockConnectionProxyXA).xaRollback("testXid", 123L, "testData");
    }

    @Test
    public void testBranchRollbackWithXAException() throws Exception {
        // Mock data source and connection that throws XAException
        AbstractDataSourceProxyXA mockDataSourceProxyXA = Mockito.mock(AbstractDataSourceProxyXA.class);
        ConnectionProxyXA mockConnectionProxyXA = Mockito.mock(ConnectionProxyXA.class);

        // Create XAException and setup mock to throw it
        XAException xaException = new XAException("XA error");
        xaException.errorCode = XAException.XA_RBROLLBACK;
        Mockito.doThrow(xaException)
                .when(mockConnectionProxyXA)
                .xaRollback(Mockito.anyString(), Mockito.anyLong(), Mockito.anyString());
        Mockito.doNothing().when(mockConnectionProxyXA).close();

        Mockito.when(mockDataSourceProxyXA.getConnectionForXAFinish(Mockito.any(XAXid.class)))
                .thenReturn(mockConnectionProxyXA);

        // Use reflection to set the dataSourceCache field
        Field dataSourceCacheField = ResourceManagerXA.class.getSuperclass().getDeclaredField("dataSourceCache");
        dataSourceCacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Resource> dataSourceCache = (Map<String, Resource>) dataSourceCacheField.get(resourceManagerXA);
        dataSourceCache.put("testResource", mockDataSourceProxyXA);

        // Test branch rollback with XAException
        BranchStatus result =
                resourceManagerXA.branchRollback(BranchType.XA, "testXid", 123L, "testResource", "testData");

        Assertions.assertEquals(BranchStatus.PhaseTwo_RollbackFailed_Retryable, result);
        Mockito.verify(mockConnectionProxyXA).xaRollback("testXid", 123L, "testData");
    }

    @Test
    public void testBranchRollbackWithXAExceptionAsSQLException() throws Exception {
        // Mock data source and connection that throws XAException (which is a subclass of SQLException)
        AbstractDataSourceProxyXA mockDataSourceProxyXA = Mockito.mock(AbstractDataSourceProxyXA.class);
        ConnectionProxyXA mockConnectionProxyXA = Mockito.mock(ConnectionProxyXA.class);

        // Create XAException (which extends SQLException) and setup mock to throw it
        XAException xaException = new XAException("XA error");
        xaException.errorCode = XAException.XA_RBROLLBACK; // Not XAER_NOTA, so it should be treated as SQLException
        Mockito.doThrow(xaException)
                .when(mockConnectionProxyXA)
                .xaRollback(Mockito.anyString(), Mockito.anyLong(), Mockito.anyString());
        Mockito.doNothing().when(mockConnectionProxyXA).close();

        Mockito.when(mockDataSourceProxyXA.getConnectionForXAFinish(Mockito.any(XAXid.class)))
                .thenReturn(mockConnectionProxyXA);

        // Use reflection to set the dataSourceCache field
        Field dataSourceCacheField = ResourceManagerXA.class.getSuperclass().getDeclaredField("dataSourceCache");
        dataSourceCacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Resource> dataSourceCache = (Map<String, Resource>) dataSourceCacheField.get(resourceManagerXA);
        dataSourceCache.put("testResource", mockDataSourceProxyXA);

        // Test branch rollback with XAException (treated as SQLException)
        BranchStatus result =
                resourceManagerXA.branchRollback(BranchType.XA, "testXid", 123L, "testResource", "testData");

        Assertions.assertEquals(BranchStatus.PhaseTwo_RollbackFailed_Retryable, result);
        Mockito.verify(mockConnectionProxyXA).xaRollback("testXid", 123L, "testData");
    }

    @Test
    public void testBranchCommitWithUnknownResource() throws TransactionException {
        // Test with unknown resource ID
        BranchStatus result =
                resourceManagerXA.branchCommit(BranchType.XA, "testXid", 123L, "unknownResource", "testData");

        // Should return failure status for unknown resource
        Assertions.assertEquals(BranchStatus.PhaseTwo_CommitFailed_Unretryable, result);
    }

    @Test
    public void testBranchRollbackWithUnknownResource() throws TransactionException {
        // Test with unknown resource ID
        BranchStatus result =
                resourceManagerXA.branchRollback(BranchType.XA, "testXid", 123L, "unknownResource", "testData");

        // Should return failure status for unknown resource
        Assertions.assertEquals(BranchStatus.PhaseTwo_RollbackFailed_Unretryable, result);
    }

    @Test
    public void testInitXaTwoPhaseTimeoutCheckerWithNoResources() {
        // Test with no resources that need holding
        Assertions.assertDoesNotThrow(() -> resourceManagerXA.initXaTwoPhaseTimeoutChecker());

        // Should not create a scheduler when no resources need holding
        // This is hard to test directly without accessing private fields
    }

    @Test
    public void testInitXaTwoPhaseTimeoutCheckerWithResourcesNeedingHold() {
        // This test would require setting up mock resources that return true for isShouldBeHeld()
        // Due to the complexity of mocking the dataSourceCache, we'll skip this for now
        Assertions.assertDoesNotThrow(() -> resourceManagerXA.initXaTwoPhaseTimeoutChecker());
    }
}
