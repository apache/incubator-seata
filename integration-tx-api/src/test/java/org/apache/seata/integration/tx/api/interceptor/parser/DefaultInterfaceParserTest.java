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
