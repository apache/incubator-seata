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

import io.grpc.ServerCall;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.model.BranchType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ServerListenerProxyTest {

    private ServerCall.Listener<String> target;

    @BeforeEach
    void setup() {
        target = mock(ServerCall.Listener.class);
        RootContext.unbind();
        RootContext.unbindBranchType();
    }

    @AfterEach
    void cleanup() {
        RootContext.unbind();
        RootContext.unbindBranchType();
    }

    @Test
    void testOnMessage_shouldDelegateToTarget() {
        ServerListenerProxy<String> proxy = new ServerListenerProxy<>(null, null, target);
        String message = "test-message";

        proxy.onMessage(message);

        verify(target, times(1)).onMessage(message);
    }

    @Test
    void testOnHalfClose_withNonEmptyXid_andTCCBranchType_shouldBindContext() {
        String xid = "test-xid";
        Map<String, String> context = new HashMap<>();
        context.put(RootContext.KEY_BRANCH_TYPE, BranchType.TCC.name());

        ServerListenerProxy<String> proxy = new ServerListenerProxy<>(xid, context, target);

        // Pre-bind some context to test cleanup
        RootContext.bind("old-xid");
        RootContext.bindBranchType(BranchType.AT);

        proxy.onHalfClose();

        // Verify RootContext binding updated
        Assertions.assertEquals(xid, RootContext.getXID());
        Assertions.assertEquals(BranchType.TCC, RootContext.getBranchType());

        verify(target, times(1)).onHalfClose();
    }

    @Test
    void testOnHalfClose_withEmptyXid_shouldOnlyCleanContext_andCallTarget() {
        ServerListenerProxy<String> proxy = new ServerListenerProxy<>(null, new HashMap<>(), target);

        // Pre-bind some context to test cleanup
        RootContext.bind("old-xid");
        RootContext.bindBranchType(BranchType.TCC);

        proxy.onHalfClose();

        // Context should be cleaned (unbind XID and branch type)
        Assertions.assertNull(RootContext.getXID());
        Assertions.assertNull(RootContext.getBranchType());

        verify(target, times(1)).onHalfClose();
    }

    @Test
    void testOnCancel_shouldDelegate() {
        ServerListenerProxy<String> proxy = new ServerListenerProxy<>(null, null, target);

        proxy.onCancel();

        verify(target, times(1)).onCancel();
    }

    @Test
    void testOnComplete_shouldDelegate() {
        ServerListenerProxy<String> proxy = new ServerListenerProxy<>(null, null, target);

        proxy.onComplete();

        verify(target, times(1)).onComplete();
    }

    @Test
    void testOnReady_shouldDelegate() {
        ServerListenerProxy<String> proxy = new ServerListenerProxy<>(null, null, target);

        proxy.onReady();

        verify(target, times(1)).onReady();
    }
}
