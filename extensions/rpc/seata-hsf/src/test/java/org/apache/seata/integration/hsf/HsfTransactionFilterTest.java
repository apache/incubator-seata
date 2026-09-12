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
package org.apache.seata.integration.hsf;

import com.taobao.hsf.context.RPCContext;
import com.taobao.hsf.invocation.Invocation;
import com.taobao.hsf.invocation.InvocationHandler;
import com.taobao.hsf.invocation.RPCResult;
import com.taobao.hsf.util.concurrent.ListenableFuture;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.model.BranchType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class HsfTransactionFilterTest {

    private static final String DEFAULT_XID = "127.0.0.1:8091:12345678";

    @AfterEach
    void cleanup() {
        RootContext.unbind();
        RootContext.unbindBranchType();
    }

    @Test
    void testConsumerFilter_propagatesXid() throws Throwable {
        RootContext.bind(DEFAULT_XID);
        RootContext.bindBranchType(BranchType.TCC);

        Map<String, Object> clientAttachments = new HashMap<>();
        RPCContext mockClientContext = mock(RPCContext.class);
        when(mockClientContext.putAttachment(
                        org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(inv -> {
                    clientAttachments.put(inv.getArgument(0), inv.getArgument(1));
                    return mockClientContext;
                });
        when(mockClientContext.removeAttachment(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(inv -> {
                    clientAttachments.remove((String) inv.getArgument(0));
                    return mockClientContext;
                });

        InvocationHandler mockHandler = mock(InvocationHandler.class);
        @SuppressWarnings("unchecked")
        ListenableFuture<RPCResult> mockFuture = mock(ListenableFuture.class);
        when(mockHandler.invoke(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> {
            assertThat(clientAttachments).containsEntry(RootContext.KEY_XID, DEFAULT_XID);
            assertThat(clientAttachments).containsEntry(RootContext.KEY_BRANCH_TYPE, BranchType.TCC.name());
            return mockFuture;
        });

        HsfTransactionConsumerFilter filter = new HsfTransactionConsumerFilter();
        try (MockedStatic<RPCContext> rpcContextMock = mockStatic(RPCContext.class)) {
            rpcContextMock.when(RPCContext::getClientContext).thenReturn(mockClientContext);
            filter.invoke(mockHandler, new Invocation());
        }

        assertThat(clientAttachments).isEmpty();
    }

    @Test
    void testConsumerFilter_noXid() throws Throwable {
        Map<String, Object> clientAttachments = new HashMap<>();
        RPCContext mockClientContext = mock(RPCContext.class);
        when(mockClientContext.putAttachment(
                        org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(inv -> {
                    clientAttachments.put(inv.getArgument(0), inv.getArgument(1));
                    return mockClientContext;
                });

        InvocationHandler mockHandler = mock(InvocationHandler.class);
        @SuppressWarnings("unchecked")
        ListenableFuture<RPCResult> mockFuture = mock(ListenableFuture.class);
        when(mockHandler.invoke(org.mockito.ArgumentMatchers.any())).thenReturn(mockFuture);

        HsfTransactionConsumerFilter filter = new HsfTransactionConsumerFilter();
        try (MockedStatic<RPCContext> rpcContextMock = mockStatic(RPCContext.class)) {
            rpcContextMock.when(RPCContext::getClientContext).thenReturn(mockClientContext);
            filter.invoke(mockHandler, new Invocation());
        }

        assertThat(clientAttachments).isEmpty();
    }

    @Test
    void testProviderFilter_bindsXid() throws Throwable {
        Map<String, Object> serverAttachments = new HashMap<>();
        serverAttachments.put(RootContext.KEY_XID, DEFAULT_XID);
        serverAttachments.put(RootContext.KEY_BRANCH_TYPE, BranchType.TCC.name());

        RPCContext mockServerContext = mock(RPCContext.class);
        when(mockServerContext.getAttachment(RootContext.KEY_XID)).thenReturn(DEFAULT_XID);
        when(mockServerContext.getAttachment(RootContext.KEY_BRANCH_TYPE)).thenReturn(BranchType.TCC.name());
        when(mockServerContext.removeAttachment(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(mockServerContext);

        InvocationHandler mockHandler = mock(InvocationHandler.class);
        @SuppressWarnings("unchecked")
        ListenableFuture<RPCResult> mockFuture = mock(ListenableFuture.class);
        when(mockHandler.invoke(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> {
            assertThat(RootContext.getXID()).isEqualTo(DEFAULT_XID);
            assertThat(RootContext.getBranchType()).isEqualTo(BranchType.TCC);
            return mockFuture;
        });

        HsfTransactionProviderFilter filter = new HsfTransactionProviderFilter();
        try (MockedStatic<RPCContext> rpcContextMock = mockStatic(RPCContext.class)) {
            rpcContextMock.when(RPCContext::getServerContext).thenReturn(mockServerContext);
            filter.invoke(mockHandler, new Invocation());
        }

        assertThat(RootContext.getXID()).isNull();
    }

    @Test
    void testProviderFilter_noXid() throws Throwable {
        RPCContext mockServerContext = mock(RPCContext.class);
        when(mockServerContext.getAttachment(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(null);
        when(mockServerContext.removeAttachment(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(mockServerContext);

        InvocationHandler mockHandler = mock(InvocationHandler.class);
        @SuppressWarnings("unchecked")
        ListenableFuture<RPCResult> mockFuture = mock(ListenableFuture.class);
        when(mockHandler.invoke(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> {
            assertThat(RootContext.getXID()).isNull();
            return mockFuture;
        });

        HsfTransactionProviderFilter filter = new HsfTransactionProviderFilter();
        try (MockedStatic<RPCContext> rpcContextMock = mockStatic(RPCContext.class)) {
            rpcContextMock.when(RPCContext::getServerContext).thenReturn(mockServerContext);
            filter.invoke(mockHandler, new Invocation());
        }

        assertThat(RootContext.getXID()).isNull();
    }

    @Test
    void testProviderFilter_xidFromLowercaseKey() throws Throwable {
        RPCContext mockServerContext = mock(RPCContext.class);
        when(mockServerContext.getAttachment(RootContext.KEY_XID)).thenReturn(null);
        when(mockServerContext.getAttachment(RootContext.KEY_XID.toLowerCase())).thenReturn(DEFAULT_XID);
        when(mockServerContext.getAttachment(RootContext.KEY_BRANCH_TYPE)).thenReturn(null);
        when(mockServerContext.removeAttachment(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(mockServerContext);

        InvocationHandler mockHandler = mock(InvocationHandler.class);
        @SuppressWarnings("unchecked")
        ListenableFuture<RPCResult> mockFuture = mock(ListenableFuture.class);
        when(mockHandler.invoke(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> {
            assertThat(RootContext.getXID()).isEqualTo(DEFAULT_XID);
            return mockFuture;
        });

        HsfTransactionProviderFilter filter = new HsfTransactionProviderFilter();
        try (MockedStatic<RPCContext> rpcContextMock = mockStatic(RPCContext.class)) {
            rpcContextMock.when(RPCContext::getServerContext).thenReturn(mockServerContext);
            filter.invoke(mockHandler, new Invocation());
        }

        assertThat(RootContext.getXID()).isNull();
    }
}
