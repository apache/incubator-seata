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
package org.apache.seata.spring.util;

import org.junit.jupiter.api.Test;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;

import java.lang.reflect.Proxy;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class SpringProxyUtilsTest {

    interface TestService {
        void doSomething();
    }

    static class TestServiceImpl implements TestService {
        @Override
        public void doSomething() {
            // no-op
        }
    }

    // Helper: create a “pure” JDK dynamic proxy for TestService
    private TestService createJdkProxy() {
        return (TestService) Proxy.newProxyInstance(
                TestService.class.getClassLoader(), new Class<?>[] {TestService.class}, (proxy, method, args) -> null);
    }

    // Tests for findTargetClass

    @Test
    public void testFindTargetClass_WithNull() throws Exception {
        assertNull(SpringProxyUtils.findTargetClass(null));
    }

    @Test
    public void testFindTargetClass_WithPlainObject() throws Exception {
        TestServiceImpl plain = new TestServiceImpl();
        Class<?> result = SpringProxyUtils.findTargetClass(plain);
        assertEquals(TestServiceImpl.class, result);
    }

    @Test
    public void testFindTargetClass_WithJdkProxy() throws Exception {
        TestService jdkProxy = createJdkProxy();
        Class<?> result = SpringProxyUtils.findTargetClass(jdkProxy);
        assertEquals(jdkProxy.getClass(), result);
    }

    @Test
    public void testFindTargetClass_WithSpringAopProxy() throws Exception {
        ProxyFactory factory = new ProxyFactory(new TestServiceImpl());
        factory.addInterface(TestService.class);
        TestService springJdkProxy = (TestService) factory.getProxy();

        assertTrue(AopUtils.isAopProxy(springJdkProxy) && AopUtils.isJdkDynamicProxy(springJdkProxy));

        Class<?> result = SpringProxyUtils.findTargetClass(springJdkProxy);
        assertEquals(TestServiceImpl.class, result);
    }

    // Tests for findInterfaces

    @Test
    public void testFindInterfaces_WithJdkProxy() throws Exception {
        ProxyFactory factory = new ProxyFactory(new TestServiceImpl());
        factory.addInterface(TestService.class);
        TestService springJdkProxy = (TestService) factory.getProxy();

        assertTrue(AopUtils.isAopProxy(springJdkProxy) && AopUtils.isJdkDynamicProxy(springJdkProxy));

        Class<?>[] ifaces = SpringProxyUtils.findInterfaces(springJdkProxy);
        assertEquals(1, ifaces.length);
        assertEquals(TestService.class, ifaces[0]);
    }

    @Test
    public void testFindInterfaces_WithPlainObject() throws Exception {
        TestServiceImpl plain = new TestServiceImpl();
        Class<?>[] ifaces = SpringProxyUtils.findInterfaces(plain);
        assertNotNull(ifaces);
        assertEquals(0, ifaces.length);
    }

    // Tests for getAdvisedSupport

    @Test
    public void testGetAdvisedSupport_WithSpringJdkProxy() throws Exception {
        TestServiceImpl target = new TestServiceImpl();
        ProxyFactory factory = new ProxyFactory(target);
        factory.addInterface(TestService.class);
        TestService springProxy = (TestService) factory.getProxy();

        assertTrue(AopUtils.isAopProxy(springProxy));
        assertTrue(springProxy instanceof Advised);

        AdvisedSupport advisedSupport = SpringProxyUtils.getAdvisedSupport(springProxy);
        assertNotNull(advisedSupport);
        Object actualTarget = advisedSupport.getTargetSource().getTarget();
        assertTrue(actualTarget instanceof TestServiceImpl);
    }

    @Test
    public void testGetAdvisedSupport_WithCglibProxy() throws Exception {
        // Define a concrete class with no interfaces to force CGLIB proxy
        class NoInterfaceTarget {
            public void sayHello() {}
        }

        NoInterfaceTarget target = new NoInterfaceTarget();
        ProxyFactory factory = new ProxyFactory(target);
        factory.setProxyTargetClass(true); // force CGLIB instead of JDK dynamic proxy
        Object cglibProxy = factory.getProxy();

        assertTrue(AopUtils.isAopProxy(cglibProxy));
        assertFalse(AopUtils.isJdkDynamicProxy(cglibProxy)); // must be false to enter CGLIB branch

        AdvisedSupport advisedSupport = SpringProxyUtils.getAdvisedSupport(cglibProxy);
        assertNotNull(advisedSupport);

        Object realTarget = advisedSupport.getTargetSource().getTarget();
        assertTrue(realTarget instanceof NoInterfaceTarget);
    }

    // Tests for isProxy(Object)

    @Test
    public void testIsProxy_WithNull() {
        assertFalse(SpringProxyUtils.isProxy(null));
    }

    @Test
    public void testIsProxy_WithPlainObject() {
        TestServiceImpl plain = new TestServiceImpl();
        assertFalse(SpringProxyUtils.isProxy(plain));
    }

    @Test
    public void testIsProxy_WithJdkProxy() {
        TestService jdkProxy = createJdkProxy();
        assertTrue(SpringProxyUtils.isProxy(jdkProxy));
    }

    @Test
    public void testIsProxy_WithSpringAopProxy() {
        ProxyFactory factory = new ProxyFactory(new TestServiceImpl());
        factory.addInterface(TestService.class);
        TestService springProxy = (TestService) factory.getProxy();
        assertTrue(SpringProxyUtils.isProxy(springProxy));
    }

    // Tests for getTargetInterface

    @Test
    public void testGetTargetInterface_WithNull() {
        assertThrows(IllegalArgumentException.class, () -> SpringProxyUtils.getTargetInterface(null));
    }

    @Test
    public void testGetTargetInterface_WithJdkProxy() throws Exception {
        TestService jdkProxy = createJdkProxy();
        Class<?> iface = SpringProxyUtils.getTargetInterface(jdkProxy);
        assertEquals(TestService.class, iface);
    }

    @Test
    public void testGetTargetInterface_WithPlainObject() throws Exception {
        TestServiceImpl plain = new TestServiceImpl();
        Class<?> clazz = SpringProxyUtils.getTargetInterface(plain);
        assertEquals(TestServiceImpl.class, clazz);
    }

    // Tests for getAllInterfaces
    @Test
    public void testGetAllInterfaces_WithNull() {
        Class<?>[] result = SpringProxyUtils.getAllInterfaces(null);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testGetAllInterfaces_WithSingleInterface() {
        TestServiceImpl single = new TestServiceImpl();
        Class<?>[] ifaces = SpringProxyUtils.getAllInterfaces(single);
        assertNotNull(ifaces);
        assertEquals(1, ifaces.length);
        assertEquals(TestService.class, ifaces[0]);
    }

    @Test
    public void testGetAllInterfaces_WithMultipleInterfaces() {
        class MultiInterfaceImpl implements TestService, Runnable {
            @Override
            public void doSomething() {}

            @Override
            public void run() {}
        }
        MultiInterfaceImpl multi = new MultiInterfaceImpl();
        Class<?>[] ifaces = SpringProxyUtils.getAllInterfaces(multi);
        assertNotNull(ifaces);
        assertEquals(2, ifaces.length);
        assertTrue(Arrays.asList(ifaces).contains(TestService.class));
        assertTrue(Arrays.asList(ifaces).contains(Runnable.class));
    }

    // Tests for getTargetClass
    @Test
    public void testGetTargetClass_WithNull() throws Exception {
        assertNull(SpringProxyUtils.findTargetClass(null));
    }

    @Test
    public void testGetTargetClass_WithPlainObject() throws Exception {
        TestServiceImpl plain = new TestServiceImpl();
        Class<?> result = SpringProxyUtils.findTargetClass(plain);
        assertEquals(TestServiceImpl.class, result);
    }

    @Test
    public void testGetTargetClass_WithJdkProxy() throws Exception {
        TestService jdkProxy = createJdkProxy();
        Class<?> result = SpringProxyUtils.findTargetClass(jdkProxy);
        assertEquals(jdkProxy.getClass(), result);
    }

    @Test
    public void testGetTargetClass_WithSpringAopProxy() throws Exception {
        ProxyFactory factory = new ProxyFactory(new TestServiceImpl());
        factory.addInterface(TestService.class);
        TestService springJdkProxy = (TestService) factory.getProxy();

        assertTrue(AopUtils.isAopProxy(springJdkProxy) && AopUtils.isJdkDynamicProxy(springJdkProxy));

        Class<?> result = SpringProxyUtils.findTargetClass(springJdkProxy);
        assertEquals(TestServiceImpl.class, result);
    }

    @Test
    public void testGetTargetClass_WithNullTargetSource() throws Exception {
        class NullTargetSource implements TargetSource {
            @Override
            public Class<?> getTargetClass() {
                return TestService.class;
            }

            @Override
            public boolean isStatic() {
                return true;
            }

            @Override
            public Object getTarget() {
                return null;
            }

            @Override
            public void releaseTarget(Object target) {
                // no-op
            }
        }

        ProxyFactory factory = new ProxyFactory();
        factory.addInterface(TestService.class);
        factory.setTargetSource(new NullTargetSource());
        TestService proxyWithNullTarget = (TestService) factory.getProxy();

        assertTrue(AopUtils.isAopProxy(proxyWithNullTarget));

        Class<?> result = SpringProxyUtils.findTargetClass(proxyWithNullTarget);
        assertNull(result);
    }
}
