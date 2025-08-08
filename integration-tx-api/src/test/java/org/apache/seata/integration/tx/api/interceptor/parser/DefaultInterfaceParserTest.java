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
import org.apache.seata.integration.tx.api.interceptor.handler.ProxyInvocationHandler;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    public void testSetOrderAndSorting() {
        // Create handlers with different order values
        TestHandler handler1 = new TestHandler("handler1", 1);
        TestHandler handler2 = new TestHandler("handler2", 2);
        TestHandler handler3 = new TestHandler("handler3", 3);
        TestHandler handler4 = new TestHandler("handler4", 4);

        // Set order values using setOrder method
        handler1.setOrder(4);
        handler2.setOrder(3);
        handler3.setOrder(2);
        handler4.setOrder(1);

        // Add handlers to list
        List<ProxyInvocationHandler> handlers = new ArrayList<>();
        handlers.add(handler1);
        handlers.add(handler2);
        handlers.add(handler3);
        handlers.add(handler4);

        // Verify order values before sorting
        assertEquals(4, handlers.get(0).order());
        assertEquals(3, handlers.get(1).order());
        assertEquals(2, handlers.get(2).order());
        assertEquals(1, handlers.get(3).order());

        // Sort handlers by order() method
        Collections.sort(handlers, Comparator.comparingInt(ProxyInvocationHandler::order));

        // Verify sorted order
        assertEquals("handler4", handlers.get(0).type()); // order: 1
        assertEquals("handler3", handlers.get(1).type()); // order: 2
        assertEquals("handler2", handlers.get(2).type()); // order: 3
        assertEquals("handler1", handlers.get(3).type()); // order: 4

        // Verify order values after sorting
        assertEquals(1, handlers.get(0).order());
        assertEquals(2, handlers.get(1).order());
        assertEquals(3, handlers.get(2).order());
        assertEquals(4, handlers.get(3).order());
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
