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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AbstractCheckAuthHandler Test
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AbstractCheckAuthHandler Test")
class AbstractCheckAuthHandlerTest {

    private TestCheckAuthHandler handler;

    @Mock
    private RegisterTMRequest mockTMRequest;

    @Mock
    private RegisterRMRequest mockRMRequest;

    private static class TestCheckAuthHandler extends AbstractCheckAuthHandler {
        @Override
        public boolean doRegTransactionManagerCheck(RegisterTMRequest request) {
            return true;
        }

        @Override
        public boolean doRegResourceManagerCheck(RegisterRMRequest request) {
            return true;
        }
    }

    @BeforeEach
    void setUp() {
        handler = new TestCheckAuthHandler();
    }

    @Test
    @DisplayName("test regTransactionManagerCheckAuth returns true")
    void testRegTransactionManagerCheckAuthReturnsTrue() {
        boolean result = handler.regTransactionManagerCheckAuth(mockTMRequest);
        assertTrue(result);
    }

    @Test
    @DisplayName("test regResourceManagerCheckAuth returns true")
    void testRegResourceManagerCheckAuthReturnsTrue() {
        boolean result = handler.regResourceManagerCheckAuth(mockRMRequest);
        assertTrue(result);
    }

    @Test
    @DisplayName("test regTransactionManagerCheckAuth with null request")
    void testRegTransactionManagerCheckAuthWithNullRequest() {
        boolean result = handler.regTransactionManagerCheckAuth(null);
        assertTrue(result);
    }

    @Test
    @DisplayName("test regResourceManagerCheckAuth with null request")
    void testRegResourceManagerCheckAuthWithNullRequest() {
        boolean result = handler.regResourceManagerCheckAuth(null);
        assertTrue(result);
    }

    @Test
    @DisplayName("test auth check implementation")
    void testAuthCheckImplementation() {
        AbstractCheckAuthHandler customHandler = new AbstractCheckAuthHandler() {
            @Override
            public boolean doRegTransactionManagerCheck(RegisterTMRequest request) {
                return request != null;
            }

            @Override
            public boolean doRegResourceManagerCheck(RegisterRMRequest request) {
                return request != null;
            }
        };

        boolean tmResult = customHandler.regTransactionManagerCheckAuth(mockTMRequest);
        boolean rmResult = customHandler.regResourceManagerCheckAuth(mockRMRequest);

        assertTrue(tmResult);
        assertTrue(rmResult);
    }
}
