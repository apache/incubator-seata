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
package org.apache.seata.spring.annotation;

import org.aopalliance.intercept.MethodInterceptor;
import org.apache.seata.config.ConfigurationChangeEvent;
import org.apache.seata.core.constants.ConfigurationKeys;
import org.apache.seata.integration.tx.api.interceptor.InvocationWrapper;
import org.apache.seata.integration.tx.api.interceptor.SeataInterceptorPosition;
import org.apache.seata.integration.tx.api.interceptor.handler.ProxyInvocationHandler;
import org.apache.seata.integration.tx.api.interceptor.parser.DefaultInterfaceParser;
import org.apache.seata.integration.tx.api.interceptor.parser.IfNeedEnhanceBean;
import org.apache.seata.integration.tx.api.interceptor.parser.NeedEnhanceEnum;
import org.apache.seata.rm.RMClient;
import org.apache.seata.tm.TMClient;
import org.apache.seata.tm.api.FailureHandler;
import org.apache.seata.tm.api.FailureHandlerHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GlobalTransactionScannerBehaviorTest {
    private final Map<String, Set<Object>> savedSets = new HashMap<>();
    private ConfigurableListableBeanFactory previousFactory;
    private FailureHandler previousFailureHandler;
    private CountingScanner scanner;
    private DefaultListableBeanFactory factory;
    private DefaultInterfaceParser parser;
    private MockedStatic<DefaultInterfaceParser> parserScope;

    @BeforeEach
    void setUp() throws Exception {
        for (String name : new String[] {
            "PROXYED_SET", "EXCLUDE_BEAN_NAME_SET", "SCANNER_CHECKER_SET", "NEED_ENHANCE_BEAN_NAME_SET"
        }) {
            Set<Object> set = staticSet(name);
            savedSets.put(name, new LinkedHashSet<>(set));
            set.clear();
        }
        previousFactory = (ConfigurableListableBeanFactory) field("beanFactory").get(null);
        previousFailureHandler = FailureHandlerHolder.getFailureHandler();
        factory = new DefaultListableBeanFactory();
        GlobalTransactionScanner.setBeanFactory(factory);
        scanner = new CountingScanner();
        scanner.setBeanFactory((BeanFactory) factory);
        field("disableGlobalTransaction").set(scanner, false);
        parser = mock(DefaultInterfaceParser.class);
        parserScope = mockStatic(DefaultInterfaceParser.class);
        parserScope.when(DefaultInterfaceParser::get).thenReturn(parser);
    }

    @AfterEach
    void restoreState() throws Exception {
        parserScope.close();
        for (Map.Entry<String, Set<Object>> entry : savedSets.entrySet()) {
            Set<Object> set = staticSet(entry.getKey());
            set.clear();
            set.addAll(entry.getValue());
        }
        GlobalTransactionScanner.setBeanFactory(previousFactory);
        FailureHandlerHolder.setFailureHandler(previousFailureHandler);
    }

    @Test
    void wrapsBusinessBeanOnceAndDelegatesInvocation() throws Throwable {
        Service bean = new Service();
        AtomicInteger calls = new AtomicInteger();
        ProxyInvocationHandler handler = handler(SeataInterceptorPosition.Any, 10);
        when(handler.invoke(any())).thenAnswer(invocation -> {
            InvocationWrapper wrapper = invocation.getArgument(0);
            assertSame(bean, wrapper.getTarget());
            assertEquals("echo", wrapper.getMethod().getName());
            assertArrayEquals(new Object[] {"hello"}, wrapper.getArguments());
            assertNull(wrapper.getProxy());
            calls.incrementAndGet();
            return wrapper.proceed();
        });
        when(parser.parserInterfaceToProxy(bean, "service")).thenReturn(handler);
        staticSet("NEED_ENHANCE_BEAN_NAME_SET").add("service");
        Object proxy = scanner.wrapIfNecessary(bean, "service", Service.class);
        assertTrue(AopUtils.isAopProxy(proxy));
        assertEquals("hello", ((Service) proxy).echo("hello"));
        assertEquals(1, calls.get());
        assertSame(proxy, scanner.wrapIfNecessary(proxy, "service", Service.class));
        verify(parser, times(1)).parserInterfaceToProxy(bean, "service");
    }

    @ParameterizedTest
    @EnumSource(
            value = SeataInterceptorPosition.class,
            names = {"BeforeTransaction", "AfterTransaction"})
    void ordersSeataAdviceRelativeToSpringTransaction(SeataInterceptorPosition position) throws Exception {
        ProxyFactory proxyFactory = new ProxyFactory(new Service());
        DefaultPointcutAdvisor transaction = new DefaultPointcutAdvisor(new TransactionInterceptor());
        transaction.setOrder(10);
        proxyFactory.addAdvisor(transaction);
        Object bean = proxyFactory.getProxy();
        ProxyInvocationHandler handler = handler(position, 10);
        when(parser.parserInterfaceToProxy(bean, "service")).thenReturn(handler);
        staticSet("NEED_ENHANCE_BEAN_NAME_SET").add("service");
        assertSame(bean, scanner.wrapIfNecessary(bean, "service", Service.class));
        Advisor[] advisors = ((Advised) bean).getAdvisors();
        assertEquals(2, advisors.length);
        int seataIndex = position == SeataInterceptorPosition.BeforeTransaction ? 0 : 1;
        assertTrue(advisors[seataIndex].getAdvice() instanceof AdapterSpringSeataInterceptor);
        assertSame(transaction, advisors[1 - seataIndex]);
        verify(handler).setOrder(position == SeataInterceptorPosition.BeforeTransaction ? 9 : 11);
    }

    @ParameterizedTest
    @ValueSource(ints = {Ordered.HIGHEST_PRECEDENCE, 5, 20, Ordered.LOWEST_PRECEDENCE})
    void ordersAdviceWithoutSpringTransaction(int order) throws Exception {
        ProxyFactory proxyFactory = new ProxyFactory(new Service());
        DefaultPointcutAdvisor existing =
                new DefaultPointcutAdvisor((MethodInterceptor) invocation -> invocation.proceed());
        existing.setOrder(10);
        proxyFactory.addAdvisor(existing);
        Object bean = proxyFactory.getProxy();
        ProxyInvocationHandler handler = handler(SeataInterceptorPosition.Any, order);
        when(parser.parserInterfaceToProxy(bean, "service")).thenReturn(handler);
        staticSet("NEED_ENHANCE_BEAN_NAME_SET").add("service");
        scanner.wrapIfNecessary(bean, "service", Service.class);
        Advisor[] advisors = ((Advised) bean).getAdvisors();
        assertEquals(2, advisors.length);
        assertTrue(advisors[order <= 10 ? 0 : 1].getAdvice() instanceof AdapterSpringSeataInterceptor);
    }

    @Test
    void missingHandlerLeavesBeanUnchanged() throws Exception {
        Service bean = new Service();
        staticSet("NEED_ENHANCE_BEAN_NAME_SET").add("service");
        assertSame(bean, scanner.wrapIfNecessary(bean, "service", Service.class));
        assertFalse(staticSet("PROXYED_SET").contains("service"));
    }

    @Test
    void parserFailureRetainsCauseAndDoesNotMarkBeanProxied() throws Exception {
        Service bean = new Service();
        IllegalStateException cause = new IllegalStateException("invalid annotation combination");
        when(parser.parserInterfaceToProxy(bean, "service")).thenThrow(cause);
        staticSet("NEED_ENHANCE_BEAN_NAME_SET").add("service");
        RuntimeException failure =
                assertThrows(RuntimeException.class, () -> scanner.wrapIfNecessary(bean, "service", Service.class));
        assertSame(cause, failure.getCause());
        assertFalse(staticSet("PROXYED_SET").contains("service"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ref", "target", "absent", "null"})
    void discoversLocalServiceBehindRemoteDefinition(String property) throws Exception {
        RootBeanDefinition definition = new RootBeanDefinition(Service.class);
        if (!"absent".equals(property)) {
            definition
                    .getPropertyValues()
                    .add(
                            "ref".equals(property) ? "ref" : "target",
                            "null".equals(property) ? null : new RuntimeBeanReference("business"));
        }
        factory.registerBeanDefinition("remote", definition);
        when(parser.parseIfNeedEnhancement(Service.class)).thenReturn(enhancement(NeedEnhanceEnum.SERVICE_BEAN));
        context();
        scanner.afterPropertiesSet();
        Set<Object> expected = staticSet("NEED_ENHANCE_BEAN_NAME_SET");
        assertEquals(1, expected.size());
        assertTrue(expected.contains("ref".equals(property) || "target".equals(property) ? "business" : "remote"));
        scanner.afterPropertiesSet();
        assertEquals(1, scanner.initializations);
    }

    @Test
    void discoversTransactionalBeanAndSkipsInfrastructureAndUnknownClasses() throws Exception {
        factory.registerBeanDefinition("transactional", new RootBeanDefinition(Service.class));
        factory.registerBeanDefinition("plain", new RootBeanDefinition(Object.class));
        factory.registerBeanDefinition("empty", new RootBeanDefinition());
        factory.registerBeanDefinition("scanner", new RootBeanDefinition(GlobalTransactionScanner.class));
        RootBeanDefinition missing = new RootBeanDefinition();
        missing.setBeanClassName("example.DoesNotExist");
        factory.registerBeanDefinition("missing", missing);
        when(parser.parseIfNeedEnhancement(Service.class))
                .thenReturn(enhancement(NeedEnhanceEnum.GLOBAL_TRANSACTIONAL_BEAN));
        when(parser.parseIfNeedEnhancement(Object.class)).thenReturn(new IfNeedEnhanceBean());
        context();
        scanner.afterPropertiesSet();
        assertEquals(java.util.Collections.singleton("transactional"), staticSet("NEED_ENHANCE_BEAN_NAME_SET"));
        verify(parser, never()).parseIfNeedEnhancement(GlobalTransactionScanner.class);
    }

    @Test
    void scannerCheckerCanVetoDefinitionBeforeParsing() throws Exception {
        factory.registerBeanDefinition("service", new RootBeanDefinition(Service.class));
        GlobalTransactionScanner.addScannerCheckers((bean, name, beanFactory) -> false);
        context();
        scanner.afterPropertiesSet();
        verifyNoInteractions(parser);
        assertTrue(staticSet("NEED_ENHANCE_BEAN_NAME_SET").isEmpty());
    }

    @Test
    void configurationEnablesClientOnlyOnce() throws Exception {
        ConfigurationChangeEvent event = mock(ConfigurationChangeEvent.class);
        when(event.getDataId()).thenReturn(ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION);
        when(event.getNewValue()).thenReturn(" true ", " false ", "false");
        scanner.onChangeEvent(event);
        assertEquals(0, scanner.initializations);
        scanner.onChangeEvent(event);
        scanner.onChangeEvent(event);
        assertEquals(1, scanner.initializations);
    }

    @Test
    void initializesBothClientsBeforeRegisteringShutdownHook() {
        GlobalTransactionScanner real = spy(new GlobalTransactionScanner("app", "group"));
        try (MockedStatic<TMClient> tm = mockStatic(TMClient.class);
                MockedStatic<RMClient> rm = mockStatic(RMClient.class)) {
            doAnswer(invocation -> {
                        tm.verify(() -> TMClient.init(
                                "app",
                                "group",
                                GlobalTransactionScanner.getAccessKey(),
                                GlobalTransactionScanner.getSecretKey()));
                        rm.verify(() -> RMClient.init("app", "group"));
                        return null;
                    })
                    .when(real)
                    .registerSpringShutdownHook();
            real.initClient();
            verify(real).registerSpringShutdownHook();
        }
    }

    private void context() throws Exception {
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        when(context.getBeanFactory()).thenReturn(factory);
        when(context.getBeanDefinitionNames()).thenReturn(factory.getBeanDefinitionNames());
        field("applicationContext").set(scanner, context);
    }

    private static IfNeedEnhanceBean enhancement(NeedEnhanceEnum type) {
        IfNeedEnhanceBean value = new IfNeedEnhanceBean();
        value.setIfNeed(true);
        value.setNeedEnhanceEnum(type);
        return value;
    }

    private static ProxyInvocationHandler handler(SeataInterceptorPosition position, int initialOrder) {
        ProxyInvocationHandler handler = mock(ProxyInvocationHandler.class);
        AtomicInteger order = new AtomicInteger(initialOrder);
        when(handler.getPosition()).thenReturn(position);
        when(handler.getOrder()).thenAnswer(invocation -> order.get());
        doAnswer(invocation -> {
                    order.set(invocation.getArgument(0));
                    return null;
                })
                .when(handler)
                .setOrder(anyInt());
        return handler;
    }

    private static Field field(String name) throws Exception {
        Field field = GlobalTransactionScanner.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    @SuppressWarnings("unchecked")
    private static Set<Object> staticSet(String name) throws Exception {
        return (Set<Object>) field(name).get(null);
    }

    private static class CountingScanner extends GlobalTransactionScanner {
        int initializations;

        CountingScanner() {
            super("app", "group");
        }

        @Override
        protected void initClient() {
            initializations++;
        }
    }

    public static class Service {
        public String echo(String value) {
            return value;
        }
    }
}
