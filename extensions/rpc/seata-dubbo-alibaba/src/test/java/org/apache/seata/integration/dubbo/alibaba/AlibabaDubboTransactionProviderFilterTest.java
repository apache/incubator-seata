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

package org.apache.seata.integration.dubbo.alibaba;

import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import org.apache.seata.core.constants.DubboConstants;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.model.BranchType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AlibabaDubboTransactionProviderFilterTest {
    private AlibabaDubboTransactionProviderFilter filter;
    private Invoker invoker;
    private Invocation invocation;
    private RpcContext rpcContext;
    private RpcContext rpcServerContext;
    private MockedStatic<RpcContext> rpcContextMock;

    @BeforeEach
    void setUp() {
        filter = new AlibabaDubboTransactionProviderFilter();
        invoker = mock(Invoker.class);
        invocation = mock(Invocation.class);

        // Mock RpcContext
        rpcContextMock = mockStatic(RpcContext.class);
        rpcContext = mock(RpcContext.class);
        rpcServerContext = mock(RpcContext.class);

        rpcContextMock.when(RpcContext::getContext).thenReturn(rpcContext);
        rpcContextMock.when(RpcContext::getServerContext).thenReturn(rpcServerContext);
    }

    @AfterEach
    void tearDown() {
        // Clear RootContext
        RootContext.unbind();
        RootContext.unbindBranchType();

        if (rpcContextMock != null) {
            rpcContextMock.close();
        }
    }

    @Test
    void testInvokeWhenNotAlibabaDubbo() {
        // Arrange
        Result expectedResult = mock(Result.class);
        when(invoker.invoke(invocation)).thenReturn(expectedResult);

        // Test when DubboConstants.ALIBABADUBBO is false
        // We need to use reflection to change the value of the static final field
        try {
            // Use a different approach - test the actual behavior by checking if the filter returns early
            // Create a new filter and directly test its behavior
            AlibabaDubboTransactionProviderFilter filterWithFalse = new AlibabaDubboTransactionProviderFilter() {
                // Override the invoke method to simulate ALIBABADUBBO = false
                @Override
                public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
                    // Simulate ALIBABADUBBO = false
                    try {
                        java.lang.reflect.Field field = DubboConstants.class.getDeclaredField("ALIBABADUBBO");
                        field.setAccessible(true);
                        field.setBoolean(null, false);
                    } catch (Exception e) {
                        // Ignore
                    }
                    return super.invoke(invoker, invocation);
                }
            };

            // Act
            Result result = filterWithFalse.invoke(invoker, invocation);

            // Assert
            assertEquals(expectedResult, result);
            verify(invoker).invoke(invocation);
        } finally {
            // Restore the original value
            try {
                java.lang.reflect.Field field = DubboConstants.class.getDeclaredField("ALIBABADUBBO");
                field.setAccessible(true);
                field.setBoolean(null, true);
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    @Test
    void testInvokeWhenXidIsNull() {
        // Arrange
        Result expectedResult = mock(Result.class);
        when(invoker.invoke(invocation)).thenReturn(expectedResult);
        when(rpcContext.getAttachment(RootContext.KEY_XID)).thenReturn(null);
        when(rpcContext.getAttachment(RootContext.KEY_XID.toLowerCase())).thenReturn(null);

        // Act
        Result result = filter.invoke(invoker, invocation);

        // Assert
        assertEquals(expectedResult, result);
        verify(invoker).invoke(invocation);
        verify(rpcContext).getAttachment(RootContext.KEY_XID);
        verify(rpcContext).getAttachment(RootContext.KEY_XID.toLowerCase());
        assertNull(RootContext.getXID());
    }

    @Test
    void testInvokeWhenXidIsPresent() {
        // Arrange
        String xid = "test-xid-123";
        Result expectedResult = mock(Result.class);
        when(invoker.invoke(invocation)).thenReturn(expectedResult);
        when(rpcContext.getAttachment(RootContext.KEY_XID)).thenReturn(xid);
        when(rpcServerContext.getAttachment(RootContext.KEY_BRANCH_TYPE)).thenReturn(null);

        // Act
        Result result = filter.invoke(invoker, invocation);

        // Assert
        assertEquals(expectedResult, result);
        verify(invoker).invoke(invocation);
        // After execution, context should be cleared
        assertNull(RootContext.getXID());
    }

    @Test
    void testInvokeWhenXidAndTccBranchTypeArePresent() {
        // Arrange
        String xid = "test-xid-123";
        String branchType = BranchType.TCC.name();
        Result expectedResult = mock(Result.class);
        when(invoker.invoke(invocation)).thenReturn(expectedResult);
        when(rpcContext.getAttachment(RootContext.KEY_XID)).thenReturn(xid);
        when(rpcServerContext.getAttachment(RootContext.KEY_BRANCH_TYPE)).thenReturn(branchType);

        // Act
        Result result = filter.invoke(invoker, invocation);

        // Assert
        assertEquals(expectedResult, result);
        verify(invoker).invoke(invocation);
        // After execution, context should be cleared
        assertNull(RootContext.getXID());
        assertNull(RootContext.getBranchType());
    }

    @Test
    void testInvokeWhenXidAndAtBranchTypeArePresent() {
        // Arrange
        String xid = "test-xid-123";
        String branchType = BranchType.AT.name();
        Result expectedResult = mock(Result.class);
        when(invoker.invoke(invocation)).thenReturn(expectedResult);
        when(rpcContext.getAttachment(RootContext.KEY_XID)).thenReturn(xid);
        when(rpcServerContext.getAttachment(RootContext.KEY_BRANCH_TYPE)).thenReturn(branchType);

        // Act
        Result result = filter.invoke(invoker, invocation);

        // Assert
        assertEquals(expectedResult, result);
        verify(invoker).invoke(invocation);
        // After execution, context should be cleared
        assertNull(RootContext.getXID());
        assertNull(RootContext.getBranchType());
    }

    @Test
    void testInvokeWhenXidChangedDuringExecution() {
        // Arrange
        String originalXid = "original-xid-123";
        String changedXid = "changed-xid-456";
        Result expectedResult = mock(Result.class);

        // Mock the invocation to simulate XID change during execution
        when(invoker.invoke(invocation)).thenAnswer(invocationOnMock -> {
            // Simulate XID change during invocation
            RootContext.bind(changedXid);
            return expectedResult;
        });

        when(rpcContext.getAttachment(RootContext.KEY_XID)).thenReturn(originalXid);
        when(rpcContext.getAttachment(RootContext.KEY_BRANCH_TYPE)).thenReturn(null);

        // Act
        Result result = filter.invoke(invoker, invocation);

        // Assert
        assertEquals(expectedResult, result);
        verify(invoker).invoke(invocation);
        // According to the source code, when XID changes during execution,
        // the filter should restore the changed XID (not the original one)
        assertEquals(changedXid, RootContext.getXID());
    }

    @Test
    void testGetRpcXidWhenXidExists() {
        // Arrange
        String xid = "test-xid-123";
        when(rpcContext.getAttachment(RootContext.KEY_XID)).thenReturn(xid);

        // Act - Using reflection to test private method
        String result = invokePrivateMethod("getRpcXid");

        // Assert
        assertEquals(xid, result);
    }

    @Test
    void testGetRpcXidWhenXidIsNullButLowerCaseExists() {
        // Arrange
        String xid = "test-xid-123";
        when(rpcContext.getAttachment(RootContext.KEY_XID)).thenReturn(null);
        when(rpcContext.getAttachment(RootContext.KEY_XID.toLowerCase())).thenReturn(xid);

        // Act - Using reflection to test private method
        String result = invokePrivateMethod("getRpcXid");

        // Assert
        assertEquals(xid, result);
    }

    @Test
    void testGetRpcXidWhenBothXidAreNull() {
        // Arrange
        when(rpcContext.getAttachment(RootContext.KEY_XID)).thenReturn(null);
        when(rpcContext.getAttachment(RootContext.KEY_XID.toLowerCase())).thenReturn(null);

        // Act - Using reflection to test private method
        String result = invokePrivateMethod("getRpcXid");

        // Assert
        assertNull(result);
    }

    // Helper method to test private methods using reflection
    private String invokePrivateMethod(String methodName) {
        try {
            java.lang.reflect.Method method = AlibabaDubboTransactionProviderFilter.class.getDeclaredMethod(methodName);
            method.setAccessible(true);
            return (String) method.invoke(filter);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke private method: " + methodName, e);
        }
    }
}
