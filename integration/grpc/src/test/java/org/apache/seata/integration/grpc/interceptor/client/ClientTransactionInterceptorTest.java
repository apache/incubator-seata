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
package org.apache.seata.integration.grpc.interceptor.client;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.integration.grpc.interceptor.GrpcHeaderKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.apache.seata.core.model.BranchType.AT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ClientTransactionInterceptorTest {

    @Mock
    private Channel channel;

    @Mock
    private CallOptions callOptions;

    @Mock
    private MethodDescriptor<String, String> method;

    @Mock
    private ClientCall.Listener<String> listener;

    @Mock
    private ClientCall<String, String> delegateCall;

    private ClientTransactionInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new ClientTransactionInterceptor();
    }

    @Test
    void testInterceptCall_withXid_shouldInjectHeaders() {
        // Ready
        String xid = "123456";
        BranchType branchType = AT;

        // Bind transaction context
        RootContext.bind(xid);
        RootContext.bindBranchType(branchType);

        // Mock channel.newCall(...) to return our delegate call
        Mockito.<ClientCall<String, String>>when(channel.newCall(any(), any())).thenReturn(delegateCall);

        // Metadata that will be passed into the call
        Metadata requestHeaders = new Metadata();

        // Create the interceptor and call interceptCall
        ClientCall<String, String> interceptedCall = interceptor.interceptCall(method, callOptions, channel);

        // Act
        interceptedCall.start(listener, requestHeaders);

        // Capture actual listener and metadata passed into delegateCall.start(...)
        ArgumentCaptor<Metadata> headersCaptor = ArgumentCaptor.forClass(Metadata.class);
        ArgumentCaptor<ClientCall.Listener<String>> listenerCaptor = ArgumentCaptor.forClass(ClientCall.Listener.class);

        verify(delegateCall).start(listenerCaptor.capture(), headersCaptor.capture());

        Metadata actualHeaders = headersCaptor.getValue();

        // Assert headers contain the expected XID and branch type
        Assertions.assertEquals(xid, actualHeaders.get(GrpcHeaderKey.XID_HEADER_KEY));
        Assertions.assertEquals(branchType.name(), actualHeaders.get(GrpcHeaderKey.BRANCH_HEADER_KEY));

        // Cleanup context
        RootContext.unbind();
        RootContext.unbindBranchType();
    }

    @Test
    void testInterceptCall_withoutXid_shouldNotInjectHeaders() {
        // ready
        Mockito.<ClientCall<String, String>>when(channel.newCall(any(), any())).thenReturn(delegateCall);
        Metadata metadata = new Metadata();

        // act
        ClientCall<String, String> interceptedCall = interceptor.interceptCall(method, callOptions, channel);
        interceptedCall.start(listener, metadata);

        // assert
        Assertions.assertNull(metadata.get(GrpcHeaderKey.XID_HEADER_KEY));
        Assertions.assertNull(metadata.get(GrpcHeaderKey.BRANCH_HEADER_KEY));
    }

    @Test
    void testOnHeaders_shouldDelegateToOriginalListener() {
        // ready
        Mockito.<ClientCall<String, String>>when(channel.newCall(any(), any())).thenReturn(delegateCall);

        Metadata requestHeaders = new Metadata();
        Metadata responseHeaders = new Metadata();
        responseHeaders.put(Metadata.Key.of("test-key", Metadata.ASCII_STRING_MARSHALLER), "test-value");

        ArgumentCaptor<ClientCall.Listener<String>> listenerCaptor = ArgumentCaptor.forClass(ClientCall.Listener.class);

        ClientCall<String, String> interceptedCall = interceptor.interceptCall(method, callOptions, channel);
        interceptedCall.start(listener, requestHeaders);

        // Verify and capture the listener argument passed to delegateCall.start()
        verify(delegateCall).start(listenerCaptor.capture(), any());

        // Act
        ClientCall.Listener<String> interceptedListener = listenerCaptor.getValue();
        interceptedListener.onHeaders(responseHeaders);

        // Assert
        verify(listener).onHeaders(responseHeaders);
    }
}
