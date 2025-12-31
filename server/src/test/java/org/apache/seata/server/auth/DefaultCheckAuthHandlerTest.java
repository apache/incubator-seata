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
package org.apache.seata.server.auth;

import org.apache.seata.core.protocol.RegisterRMRequest;
import org.apache.seata.core.protocol.RegisterTMRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DefaultCheckAuthHandler Test - Direct testing without mocks
 */
@DisplayName("DefaultCheckAuthHandler Test")
class DefaultCheckAuthHandlerTest {

    @Test
    @DisplayName("test TM registration always allowed - direct call")
    void testTMRegistrationAlwaysAllowed() {
        DefaultCheckAuthHandler handler = new DefaultCheckAuthHandler();
        RegisterTMRequest request = new RegisterTMRequest();
        request.setApplicationId("test-app");
        request.setTransactionServiceGroup("default");

        boolean result = handler.doRegTransactionManagerCheck(request);

        assertTrue(result, "TM registration should always be allowed");
    }

    @Test
    @DisplayName("test RM registration always allowed - direct call")
    void testRMRegistrationAlwaysAllowed() {
        DefaultCheckAuthHandler handler = new DefaultCheckAuthHandler();
        RegisterRMRequest request = new RegisterRMRequest();
        request.setApplicationId("test-app");
        request.setResourceIds("resource1");

        boolean result = handler.doRegResourceManagerCheck(request);

        assertTrue(result, "RM registration should always be allowed");
    }

    @Test
    @DisplayName("test TM auth check delegates to implementation - direct")
    void testTMAuthCheckDelegatesToImplementation() {
        DefaultCheckAuthHandler handler = new DefaultCheckAuthHandler();
        RegisterTMRequest request = new RegisterTMRequest();
        request.setApplicationId("test-tm");
        request.setTransactionServiceGroup("payment");

        // Public method should allow registration through default implementation
        boolean result = handler.regTransactionManagerCheckAuth(request);

        assertTrue(result, "Public auth check should return true via default handler");
    }

    @Test
    @DisplayName("test RM auth check delegates to implementation - direct")
    void testRMAuthCheckDelegatesToImplementation() {
        DefaultCheckAuthHandler handler = new DefaultCheckAuthHandler();
        RegisterRMRequest request = new RegisterRMRequest();
        request.setApplicationId("test-rm");
        request.setResourceIds("datasource1");

        // Public method should allow registration through default implementation
        boolean result = handler.regResourceManagerCheckAuth(request);

        assertTrue(result, "Public auth check should return true via default handler");
    }

    @Test
    @DisplayName("test handler with null request - allows pass through")
    void testHandlerWithNullRequest() {
        DefaultCheckAuthHandler handler = new DefaultCheckAuthHandler();

        // Should handle null gracefully without exception
        boolean tmResult = handler.doRegTransactionManagerCheck(null);
        boolean rmResult = handler.doRegResourceManagerCheck(null);

        assertTrue(tmResult, "Null TM request should be allowed");
        assertTrue(rmResult, "Null RM request should be allowed");
    }

    @Test
    @DisplayName("test handler is stateless and reusable")
    void testHandlerIsStatelessAndReusable() {
        DefaultCheckAuthHandler handler1 = new DefaultCheckAuthHandler();
        DefaultCheckAuthHandler handler2 = new DefaultCheckAuthHandler();

        RegisterTMRequest request1 = new RegisterTMRequest();
        request1.setApplicationId("app1");

        RegisterTMRequest request2 = new RegisterTMRequest();
        request2.setApplicationId("app2");

        // Multiple instances should behave consistently
        boolean result1 = handler1.doRegTransactionManagerCheck(request1);
        boolean result2 = handler2.doRegTransactionManagerCheck(request2);

        assertTrue(result1, "First handler instance should allow TM");
        assertTrue(result2, "Second handler instance should allow TM");
    }

    @Test
    @DisplayName("test handler implements AbstractCheckAuthHandler")
    void testHandlerImplementsAbstractCheckAuthHandler() {
        DefaultCheckAuthHandler handler = new DefaultCheckAuthHandler();

        assertNotNull(handler, "Handler should not be null");
        assertTrue(
                handler instanceof AbstractCheckAuthHandler,
                "DefaultCheckAuthHandler should extend AbstractCheckAuthHandler");
    }

    @Test
    @DisplayName("test multiple sequential calls maintain consistency")
    void testMultipleSequentialCallsMaintainConsistency() {
        DefaultCheckAuthHandler handler = new DefaultCheckAuthHandler();

        for (int i = 0; i < 5; i++) {
            RegisterTMRequest tmRequest = new RegisterTMRequest();
            tmRequest.setApplicationId("app" + i);

            RegisterRMRequest rmRequest = new RegisterRMRequest();
            rmRequest.setApplicationId("rm" + i);

            boolean tmResult = handler.doRegTransactionManagerCheck(tmRequest);
            boolean rmResult = handler.doRegResourceManagerCheck(rmRequest);

            assertTrue(tmResult, "Iteration " + i + ": TM check should pass");
            assertTrue(rmResult, "Iteration " + i + ": RM check should pass");
        }
    }

    @Test
    @DisplayName("test handler is thread-safe by being stateless")
    void testHandlerIsThreadSafe() throws InterruptedException {
        final DefaultCheckAuthHandler handler = new DefaultCheckAuthHandler();
        final boolean[] results = new boolean[2];

        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                RegisterTMRequest request = new RegisterTMRequest();
                request.setApplicationId("thread1");
                results[0] = handler.doRegTransactionManagerCheck(request);
            }
        });

        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                RegisterRMRequest request = new RegisterRMRequest();
                request.setApplicationId("thread2");
                results[1] = handler.doRegResourceManagerCheck(request);
            }
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        assertTrue(results[0], "Thread 1 TM check should pass");
        assertTrue(results[1], "Thread 2 RM check should pass");
    }
}
