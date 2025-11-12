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
package org.apache.seata.apm.skywalking.plugin;

import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.protocol.transaction.GlobalCommitRequest;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.StringTag;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Method;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class RemotingProcessorProcessInterceptorTest {

    private RemotingProcessorProcessInterceptor interceptor;
    private EnhancedInstance enhancedInstance;
    private Method method;
    private MethodInterceptResult result;

    @BeforeEach
    public void setUp() throws Exception {
        interceptor = new RemotingProcessorProcessInterceptor();
        enhancedInstance = mock(EnhancedInstance.class);
        method = Object.class.getDeclaredMethod("toString");
        result = mock(MethodInterceptResult.class);
    }

    @Test
    public void testBeforeAndAfterMethod() throws Throwable {
        // Given
        RpcMessage rpcMessage = new RpcMessage();
        GlobalCommitRequest request = new GlobalCommitRequest();
        request.setXid("test-xid");
        rpcMessage.setBody(request);
        rpcMessage.setHeadMap(new java.util.HashMap<>());

        Object[] allArguments = new Object[] {null, rpcMessage};
        Class<?>[] argumentTypes = new Class[] {Object.class, RpcMessage.class};

        try (MockedStatic<ContextManager> contextManagerMockedStatic = Mockito.mockStatic(ContextManager.class)) {
            AbstractSpan span = mock(AbstractSpan.class);
            contextManagerMockedStatic
                    .when(() -> ContextManager.createEntrySpan(anyString(), any(ContextCarrier.class)))
                    .thenReturn(span);

            // When
            interceptor.beforeMethod(enhancedInstance, method, allArguments, argumentTypes, result);

            // THen
            verify(span, atLeastOnce()).setComponent(any());
            verify(span, atLeastOnce()).tag(any(StringTag.class), any(String.class));
        }

        // afterMethod should call ContextManager.stopSpan if body is AbstractMessage
        Object[] afterArgs = new Object[] {rpcMessage};
        try (MockedStatic<ContextManager> contextManagerMockedStatic = Mockito.mockStatic(ContextManager.class)) {
            contextManagerMockedStatic.when(ContextManager::stopSpan).then(invocation -> null);

            // When
            interceptor.afterMethod(enhancedInstance, method, afterArgs, argumentTypes, null);

            // Then
            contextManagerMockedStatic.verify(ContextManager::stopSpan, times(1));
        }
    }
}
