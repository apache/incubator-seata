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
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;

import java.lang.reflect.Proxy;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class SpringProxyUtilsTest {

    // Test interface and implementation
    interface TestService {
        void doSomething();
    }

    static class TestServiceImpl implements TestService {
        @Override
        public void doSomething() {
            // no-op
        }
    }

    // testFindTargetClass
    @Test
    public void testFindTargetClass() throws Exception {
        assertNull(SpringProxyUtils.findTargetClass(null));
        TestServiceImpl plain = new TestServiceImpl();
        assertEquals(TestServiceImpl.class, SpringProxyUtils.findTargetClass(plain));

        TestService jdkProxy = (TestService) Proxy.newProxyInstance(
                TestService.class.getClassLoader(),
                new Class<?>[]{TestService.class},
                (proxy, method, args) -> null
        );
        assertEquals(jdkProxy.getClass(), SpringProxyUtils.findTargetClass(jdkProxy));

        ProxyFactory factory = new ProxyFactory(new TestServiceImpl());
        factory.addInterface(TestService.class);
        TestService springJdkProxy = (TestService) factory.getProxy();
        assertTrue(AopUtils.isAopProxy(springJdkProxy) && AopUtils.isJdkDynamicProxy(springJdkProxy));
        assertEquals(TestServiceImpl.class, SpringProxyUtils.findTargetClass(springJdkProxy));
    }

    // testFindInterfaces
    @Test
    public void testFindInterfaces() throws Exception {
        ProxyFactory factory = new ProxyFactory(new TestServiceImpl());
        factory.addInterface(TestService.class);
        TestService springJdkProxy = (TestService) factory.getProxy();

        assertTrue(AopUtils.isAopProxy(springJdkProxy));
        assertTrue(AopUtils.isJdkDynamicProxy(springJdkProxy));

        Class<?>[] springIfaces = SpringProxyUtils.findInterfaces(springJdkProxy);
        assertEquals(1, springIfaces.length);
        assertEquals(TestService.class, springIfaces[0]);

        TestServiceImpl plain = new TestServiceImpl();
        assertEquals(0, SpringProxyUtils.findInterfaces(plain).length);
    }

    // testGetAdvisedSupport
    @Test
    public void testGetAdvisedSupport() throws Exception {
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
    public void testGetAdvisedSupport_ForCglibProxy() throws Exception {
        // Define a concrete class with no interfaces to force CGLIB proxy
        class NoInterfaceTarget {
            public void sayHello() {
            }
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


    // testIsProxy
    @Test
    public void testIsProxy() {
        assertFalse(SpringProxyUtils.isProxy(null));

        TestServiceImpl plain = new TestServiceImpl();
        assertFalse(SpringProxyUtils.isProxy(plain));

        TestService jdkProxy = (TestService) Proxy.newProxyInstance(
                TestService.class.getClassLoader(),
                new Class<?>[]{TestService.class},
                (proxy, method, args) -> null
        );
        assertTrue(SpringProxyUtils.isProxy(jdkProxy));

        ProxyFactory factory = new ProxyFactory(new TestServiceImpl());
        factory.addInterface(TestService.class);
        TestService springProxy = (TestService) factory.getProxy();
        assertTrue(SpringProxyUtils.isProxy(springProxy));
    }

    // testGetTargetInterface
    @Test
    public void testGetTargetInterface() throws Exception {
        assertThrows(IllegalArgumentException.class, () ->
                SpringProxyUtils.getTargetInterface(null));

        TestService jdkProxy = (TestService) Proxy.newProxyInstance(
                TestService.class.getClassLoader(),
                new Class<?>[]{TestService.class},
                (proxy, method, args) -> null
        );
        assertEquals(TestService.class, SpringProxyUtils.getTargetInterface(jdkProxy));

        TestServiceImpl plain = new TestServiceImpl();
        assertEquals(TestServiceImpl.class, SpringProxyUtils.getTargetInterface(plain));

        ProxyFactory factory = new ProxyFactory(new TestServiceImpl());
        factory.addInterface(TestService.class);
        TestService springProxy = (TestService) factory.getProxy();
        assertEquals(TestService.class, SpringProxyUtils.getTargetInterface(springProxy));
    }



    // testGetAllInterfaces()
    @Test
    public void testGetAllInterfaces() {
        assertEquals(0, SpringProxyUtils.getAllInterfaces(null).length);

        TestServiceImpl single = new TestServiceImpl();
        Class<?>[] ifacesSingle = SpringProxyUtils.getAllInterfaces(single);
        assertEquals(1, ifacesSingle.length);
        assertEquals(TestService.class, ifacesSingle[0]);

        class MultiInterfaceImpl implements TestService, Runnable {
            @Override
            public void doSomething() {
            }

            @Override
            public void run() {
            }
        }
        MultiInterfaceImpl multi = new MultiInterfaceImpl();
        Class<?>[] ifacesMulti = SpringProxyUtils.getAllInterfaces(multi);
        assertEquals(2, ifacesMulti.length);
        assertTrue(Arrays.asList(ifacesMulti).contains(TestService.class));
        assertTrue(Arrays.asList(ifacesMulti).contains(Runnable.class));
    }
}