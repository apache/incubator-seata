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
package org.apache.seata.integration.grpc.interceptor.server;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.integration.grpc.interceptor.GrpcHeaderKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServerTransactionInterceptorTest {

    private ServerTransactionInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new ServerTransactionInterceptor();
        RootContext.unbind();
        RootContext.unbindBranchType();
    }

    @Test
    void testInterceptCall_shouldExtractXidAndBranchTypeAndWrapListener() {
        // Ready
        Metadata metadata = new Metadata();
        metadata.put(GrpcHeaderKey.XID_HEADER_KEY, "test-xid");
        metadata.put(GrpcHeaderKey.BRANCH_HEADER_KEY, "TCC");

        // Mocks
        ServerCall<String, String> serverCall = mock(ServerCall.class);
        ServerCallHandler<String, String> serverCallHandler = mock(ServerCallHandler.class);
        Listener<String> originalListener = mock(Listener.class);

        when(serverCallHandler.startCall(serverCall, metadata)).thenReturn(originalListener);

        // Call interceptor
        ServerCall.Listener<String> listener = interceptor.interceptCall(serverCall, metadata, serverCallHandler);

        assertNotNull(listener);
        assertInstanceOf(ServerListenerProxy.class, listener);
    }

    @Test
    void testGetRpcXid_shouldSupportUppercaseAndLowercaseKeys() throws Exception {
        Metadata metadata = new Metadata();
        metadata.put(GrpcHeaderKey.XID_HEADER_KEY, "upper-xid");

        Method getRpcXidMethod = interceptor.getClass().getDeclaredMethod("getRpcXid", Metadata.class);
        getRpcXidMethod.setAccessible(true);
        assertEquals("upper-xid", getRpcXidMethod.invoke(interceptor, metadata));

        Metadata metadataLower = new Metadata();
        metadataLower.put(GrpcHeaderKey.XID_HEADER_KEY_LOWERCASE, "lower-xid");
        assertEquals("lower-xid", getRpcXidMethod.invoke(interceptor, metadataLower));
    }

    @Test
    void testGetBranchName_shouldReturnCorrectValue() throws Exception {
        Metadata metadata = new Metadata();
        metadata.put(GrpcHeaderKey.BRANCH_HEADER_KEY, "branch-type");

        Method getBranchNameMethod = interceptor.getClass().getDeclaredMethod("getBranchName", Metadata.class);
        getBranchNameMethod.setAccessible(true);

        String branchName = (String) getBranchNameMethod.invoke(interceptor, metadata);

        assertEquals("branch-type", branchName);
    }
}
