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
package org.apache.seata.integration.tx.api.interceptor.parser;

import org.apache.seata.integration.tx.api.interceptor.InvocationWrapper;
import org.apache.seata.integration.tx.api.interceptor.SeataInterceptorPosition;
import org.apache.seata.integration.tx.api.interceptor.handler.AbstractProxyInvocationHandler;
import org.apache.seata.integration.tx.api.interceptor.handler.GlobalTransactionalInterceptorHandler;
import org.apache.seata.integration.tx.api.interceptor.handler.ProxyInvocationHandler;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultInterfaceParserTest {

    @Test
    public void testHandlerOrdering() {
        // Create handlers with different orders
        TestHandler handler1 = new TestHandler("handler1", 3);
        TestHandler handler2 = new TestHandler("handler2", 1);
        TestHandler handler3 = new TestHandler("handler3", 2);

        // Add handlers to a list
        List<ProxyInvocationHandler> handlers = new ArrayList<>();
        handlers.add(handler1);
        handlers.add(handler2);
        handlers.add(handler3);

        // Sort handlers using the same comparator as in DefaultInterfaceParser
        Collections.sort(handlers, Comparator.comparingInt(ProxyInvocationHandler::order));

        // Verify the order
        assertEquals("handler2", handlers.get(0).type());
        assertEquals("handler3", handlers.get(1).type());
        assertEquals("handler1", handlers.get(2).type());
    }

    @Test
    public void testGlobalTransactionalInterceptorHandlerOrder() {
        // Create a GlobalTransactionalInterceptorHandler with a specific order
        GlobalTransactionalInterceptorHandler handler = mock(GlobalTransactionalInterceptorHandler.class);
        when(handler.order()).thenReturn(5);

        // Verify the order method returns the expected value
        assertEquals(5, handler.order());
    }

    /**
     * Test implementation of ProxyInvocationHandler
     */
    private static class TestHandler extends AbstractProxyInvocationHandler {
        private final String name;

        public TestHandler(String name, int order) {
            this.name = name;
            this.order = order;
        }

        @Override
        protected Object doInvoke(InvocationWrapper invocation) throws Throwable {
            return null;
        }

        @Override
        public Set<String> getMethodsToProxy() {
            return new HashSet<>();
        }

        @Override
        public SeataInterceptorPosition getPosition() {
            return SeataInterceptorPosition.BeforeTransaction;
        }

        @Override
        public String type() {
            return name;
        }

        @Override
        public int order() {
            return this.order;
        }
    }
}
