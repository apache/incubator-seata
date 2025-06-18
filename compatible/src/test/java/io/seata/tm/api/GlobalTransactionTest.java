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
package io.seata.tm.api;

import io.seata.core.exception.TransactionException;
import io.seata.core.model.GlobalStatus;
import io.seata.tm.api.transaction.SuspendedResourcesHolder;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test cases for GlobalTransaction interface compatibility.
 */
public class GlobalTransactionTest {

    @Test
    public void testDeprecatedAnnotation() {
        // Test that GlobalTransaction is marked as @Deprecated
        assertTrue(GlobalTransaction.class.isAnnotationPresent(Deprecated.class),
                "GlobalTransaction should be marked as @Deprecated");
    }

    @Test
    public void testIsInterface() {
        // Test that GlobalTransaction is an interface
        assertTrue(GlobalTransaction.class.isInterface(),
                "GlobalTransaction should be an interface");
    }

    @Test
    public void testExtendsBaseTransaction() {
        // Test that GlobalTransaction extends BaseTransaction
        assertTrue(org.apache.seata.tm.api.BaseTransaction.class.isAssignableFrom(GlobalTransaction.class),
                "GlobalTransaction should extend BaseTransaction");
    }

    @Test
    public void testBeginMethods() throws TransactionException {
        // Create a mock implementation
        GlobalTransaction mockTransaction = Mockito.mock(GlobalTransaction.class);
        
        // Test begin() method
        doNothing().when(mockTransaction).begin();
        assertDoesNotThrow(() -> mockTransaction.begin(),
                "begin() should not throw exception");
        verify(mockTransaction).begin();
        
        // Test begin(int timeout) method
        doNothing().when(mockTransaction).begin(anyInt());
        assertDoesNotThrow(() -> mockTransaction.begin(30000),
                "begin(timeout) should not throw exception");
        verify(mockTransaction).begin(30000);
        
        // Test begin(int timeout, String name) method
        doNothing().when(mockTransaction).begin(anyInt(), anyString());
        assertDoesNotThrow(() -> mockTransaction.begin(30000, "test-transaction"),
                "begin(timeout, name) should not throw exception");
        verify(mockTransaction).begin(30000, "test-transaction");
    }

    @Test
    public void testCommitMethod() throws TransactionException {
        // Create a mock implementation
        GlobalTransaction mockTransaction = Mockito.mock(GlobalTransaction.class);
        
        // Test commit() method
        doNothing().when(mockTransaction).commit();
        assertDoesNotThrow(() -> mockTransaction.commit(),
                "commit() should not throw exception");
        verify(mockTransaction).commit();
    }

    @Test
    public void testRollbackMethod() throws TransactionException {
        // Create a mock implementation
        GlobalTransaction mockTransaction = Mockito.mock(GlobalTransaction.class);
        
        // Test rollback() method
        doNothing().when(mockTransaction).rollback();
        assertDoesNotThrow(() -> mockTransaction.rollback(),
                "rollback() should not throw exception");
        verify(mockTransaction).rollback();
    }

    @Test
    public void testSuspendMethods() throws TransactionException {
        // Create a mock implementation
        GlobalTransaction mockTransaction = Mockito.mock(GlobalTransaction.class);
        SuspendedResourcesHolder mockHolder = Mockito.mock(SuspendedResourcesHolder.class);
        
        // Test suspend() method
        when(mockTransaction.suspend()).thenReturn(mockHolder);
        SuspendedResourcesHolder result = mockTransaction.suspend();
        assertSame(mockHolder, result, "suspend() should return the mock holder");
        verify(mockTransaction).suspend();
        
        // Test suspend(boolean clean) method
        when(mockTransaction.suspend(anyBoolean())).thenReturn(mockHolder);
        SuspendedResourcesHolder result2 = mockTransaction.suspend(true);
        assertSame(mockHolder, result2, "suspend(clean) should return the mock holder");
        verify(mockTransaction).suspend(true);
    }

    @Test
    public void testResumeMethod() throws TransactionException {
        // Create a mock implementation
        GlobalTransaction mockTransaction = Mockito.mock(GlobalTransaction.class);
        SuspendedResourcesHolder mockHolder = Mockito.mock(SuspendedResourcesHolder.class);
        
        // Test resume() method
        doNothing().when(mockTransaction).resume(any(SuspendedResourcesHolder.class));
        assertDoesNotThrow(() -> mockTransaction.resume(mockHolder),
                "resume() should not throw exception");
        verify(mockTransaction).resume(mockHolder);
    }

    @Test
    public void testGetStatusMethod() throws TransactionException {
        // Create a mock implementation
        GlobalTransaction mockTransaction = Mockito.mock(GlobalTransaction.class);
        
        // Test getStatus() method
        when(mockTransaction.getStatus()).thenReturn(GlobalStatus.Begin);
        GlobalStatus status = mockTransaction.getStatus();
        assertEquals(GlobalStatus.Begin, status, "getStatus() should return Begin status");
        verify(mockTransaction).getStatus();
    }

    @Test
    public void testGetXidMethod() {
        // Create a mock implementation
        GlobalTransaction mockTransaction = Mockito.mock(GlobalTransaction.class);
        
        // Test getXid() method
        String testXid = "test-xid-123";
        when(mockTransaction.getXid()).thenReturn(testXid);
        String xid = mockTransaction.getXid();
        assertEquals(testXid, xid, "getXid() should return the test XID");
        verify(mockTransaction).getXid();
    }

    @Test
    public void testGlobalReportMethod() throws TransactionException {
        // Create a mock implementation
        GlobalTransaction mockTransaction = Mockito.mock(GlobalTransaction.class);
        
        // Test globalReport() method
        doNothing().when(mockTransaction).globalReport(any(GlobalStatus.class));
        assertDoesNotThrow(() -> mockTransaction.globalReport(GlobalStatus.Committed),
                "globalReport() should not throw exception");
        verify(mockTransaction).globalReport(GlobalStatus.Committed);
    }

    @Test
    public void testGetLocalStatusMethod() {
        // Create a mock implementation
        GlobalTransaction mockTransaction = Mockito.mock(GlobalTransaction.class);
        
        // Test getLocalStatus() method
        when(mockTransaction.getLocalStatus()).thenReturn(GlobalStatus.Begin);
        GlobalStatus localStatus = mockTransaction.getLocalStatus();
        assertEquals(GlobalStatus.Begin, localStatus, "getLocalStatus() should return Begin status");
        verify(mockTransaction).getLocalStatus();
    }

    @Test
    public void testGetGlobalTransactionRoleMethod() {
        // Create a mock implementation
        GlobalTransaction mockTransaction = Mockito.mock(GlobalTransaction.class);
        
        // Test getGlobalTransactionRole() method
        when(mockTransaction.getGlobalTransactionRole()).thenReturn(GlobalTransactionRole.Launcher);
        GlobalTransactionRole role = mockTransaction.getGlobalTransactionRole();
        assertEquals(GlobalTransactionRole.Launcher, role, "getGlobalTransactionRole() should return Launcher role");
        verify(mockTransaction).getGlobalTransactionRole();
    }

    @Test
    public void testGetCreateTimeMethod() {
        // Create a mock implementation
        GlobalTransaction mockTransaction = Mockito.mock(GlobalTransaction.class);
        
        // Test getCreateTime() method
        long createTime = System.currentTimeMillis();
        when(mockTransaction.getCreateTime()).thenReturn(createTime);
        long time = mockTransaction.getCreateTime();
        assertEquals(createTime, time, "getCreateTime() should return the create time");
        verify(mockTransaction).getCreateTime();
    }

    @Test
    public void testTransactionExceptionHandling() throws TransactionException {
        // Create a mock implementation
        GlobalTransaction mockTransaction = Mockito.mock(GlobalTransaction.class);
        
        // Test that methods can throw TransactionException
        TransactionException testException = new TransactionException("Test exception");
        
        doThrow(testException).when(mockTransaction).begin();
        assertThrows(TransactionException.class, () -> mockTransaction.begin(),
                "begin() should be able to throw TransactionException");
        
        doThrow(testException).when(mockTransaction).commit();
        assertThrows(TransactionException.class, () -> mockTransaction.commit(),
                "commit() should be able to throw TransactionException");
        
        doThrow(testException).when(mockTransaction).rollback();
        assertThrows(TransactionException.class, () -> mockTransaction.rollback(),
                "rollback() should be able to throw TransactionException");
        
        doThrow(testException).when(mockTransaction).suspend();
        assertThrows(TransactionException.class, () -> mockTransaction.suspend(),
                "suspend() should be able to throw TransactionException");
        
        doThrow(testException).when(mockTransaction).resume(any());
        assertThrows(TransactionException.class, () -> mockTransaction.resume(null),
                "resume() should be able to throw TransactionException");
        
        when(mockTransaction.getStatus()).thenThrow(testException);
        assertThrows(TransactionException.class, () -> mockTransaction.getStatus(),
                "getStatus() should be able to throw TransactionException");
        
        doThrow(testException).when(mockTransaction).globalReport(any());
        assertThrows(TransactionException.class, () -> mockTransaction.globalReport(GlobalStatus.Committed),
                "globalReport() should be able to throw TransactionException");
    }

    @Test
    public void testInterfaceMethods() {
        // Test that all required methods are present in the interface
        try {
            // Test method signatures exist
            GlobalTransaction.class.getMethod("begin");
            GlobalTransaction.class.getMethod("begin", int.class);
            GlobalTransaction.class.getMethod("begin", int.class, String.class);
            GlobalTransaction.class.getMethod("commit");
            GlobalTransaction.class.getMethod("rollback");
            GlobalTransaction.class.getMethod("suspend");
            GlobalTransaction.class.getMethod("suspend", boolean.class);
            GlobalTransaction.class.getMethod("resume", SuspendedResourcesHolder.class);
            GlobalTransaction.class.getMethod("getStatus");
            GlobalTransaction.class.getMethod("getXid");
            GlobalTransaction.class.getMethod("globalReport", GlobalStatus.class);
            GlobalTransaction.class.getMethod("getLocalStatus");
            GlobalTransaction.class.getMethod("getGlobalTransactionRole");
            GlobalTransaction.class.getMethod("getCreateTime");
            
        } catch (NoSuchMethodException e) {
            fail("Required method not found in GlobalTransaction interface: " + e.getMessage());
        }
    }

    @Test
    public void testMethodReturnTypes() {
        // Test that methods have correct return types
        try {
            assertEquals(void.class, GlobalTransaction.class.getMethod("begin").getReturnType());
            assertEquals(void.class, GlobalTransaction.class.getMethod("begin", int.class).getReturnType());
            assertEquals(void.class, GlobalTransaction.class.getMethod("begin", int.class, String.class).getReturnType());
            assertEquals(void.class, GlobalTransaction.class.getMethod("commit").getReturnType());
            assertEquals(void.class, GlobalTransaction.class.getMethod("rollback").getReturnType());
            assertEquals(SuspendedResourcesHolder.class, GlobalTransaction.class.getMethod("suspend").getReturnType());
            assertEquals(SuspendedResourcesHolder.class, GlobalTransaction.class.getMethod("suspend", boolean.class).getReturnType());
            assertEquals(void.class, GlobalTransaction.class.getMethod("resume", SuspendedResourcesHolder.class).getReturnType());
            assertEquals(GlobalStatus.class, GlobalTransaction.class.getMethod("getStatus").getReturnType());
            assertEquals(String.class, GlobalTransaction.class.getMethod("getXid").getReturnType());
            assertEquals(void.class, GlobalTransaction.class.getMethod("globalReport", GlobalStatus.class).getReturnType());
            assertEquals(GlobalStatus.class, GlobalTransaction.class.getMethod("getLocalStatus").getReturnType());
            assertEquals(GlobalTransactionRole.class, GlobalTransaction.class.getMethod("getGlobalTransactionRole").getReturnType());
            assertEquals(long.class, GlobalTransaction.class.getMethod("getCreateTime").getReturnType());
            
        } catch (NoSuchMethodException e) {
            fail("Method not found when testing return types: " + e.getMessage());
        }
    }
} 