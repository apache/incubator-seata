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
        // 1) null → should return null
        assertNull(SpringProxyUtils.findTargetClass(null));

        // 2) non-proxy object → return its own class
        TestServiceImpl plain = new TestServiceImpl();
        assertEquals(TestServiceImpl.class, SpringProxyUtils.findTargetClass(plain));

        // 3) “pure” JDK dynamic proxy (not Spring AOP) → findTargetClass(...) just returns proxy.getClass()
        TestService jdkProxy = (TestService) Proxy.newProxyInstance(
                TestService.class.getClassLoader(),
                new Class<?>[]{TestService.class},
                (proxy, method, args) -> null
        );
        assertEquals(jdkProxy.getClass(), SpringProxyUtils.findTargetClass(jdkProxy));

        // 4) Spring AOP proxy (JDK‐style, because we added an interface) → should unwrap and reveal TestServiceImpl.class
        ProxyFactory factory = new ProxyFactory(new TestServiceImpl());
        factory.addInterface(TestService.class);
        TestService springJdkProxy = (TestService) factory.getProxy();
        assertTrue(AopUtils.isAopProxy(springJdkProxy) && AopUtils.isJdkDynamicProxy(springJdkProxy));
        assertEquals(TestServiceImpl.class, SpringProxyUtils.findTargetClass(springJdkProxy));
    }

    // testFindInterfaces
    @Test
    public void testFindInterfaces() throws Exception {
        // 1) Spring AOP JDK‐style proxy: ProxyFactory + addInterface(...) → AopUtils.isJdkDynamicProxy(...) is true.
        ProxyFactory factory = new ProxyFactory(new TestServiceImpl());
        factory.addInterface(TestService.class);
        TestService springJdkProxy = (TestService) factory.getProxy();

        assertTrue(AopUtils.isAopProxy(springJdkProxy));
        assertTrue(AopUtils.isJdkDynamicProxy(springJdkProxy));

        Class<?>[] springIfaces = SpringProxyUtils.findInterfaces(springJdkProxy);
        // Should have exactly one interface: TestService.class
        assertEquals(1, springIfaces.length);
        assertEquals(TestService.class, springIfaces[0]);

        // 2) Plain object (no proxy) → findInterfaces(...) returns empty array
        TestServiceImpl plain = new TestServiceImpl();
        assertEquals(0, SpringProxyUtils.findInterfaces(plain).length);
    }

    // testGetAdvisedSupport
    @Test
    public void testGetAdvisedSupport() throws Exception {
        // Create a Spring AOP proxy that uses JDK dynamic‐proxy under the hood
        TestServiceImpl target = new TestServiceImpl();
        ProxyFactory factory = new ProxyFactory(target);
        factory.addInterface(TestService.class);
        TestService springProxy = (TestService) factory.getProxy();

        // Sanity check: it really is a Spring proxy
        assertTrue(AopUtils.isAopProxy(springProxy));
        assertTrue(springProxy instanceof Advised);

        // Now getAdvisedSupport(...) should succeed and not return null
        AdvisedSupport advisedSupport = SpringProxyUtils.getAdvisedSupport(springProxy);
        assertNotNull(advisedSupport);
        // The target source within advisedSupport should point to our TestServiceImpl
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

        // Create a Spring proxy using CGLIB (class-based proxying)
        ProxyFactory factory = new ProxyFactory(target);
        factory.setProxyTargetClass(true); // force CGLIB instead of JDK dynamic proxy
        Object cglibProxy = factory.getProxy();

        // Confirm it is a Spring AOP proxy and not a JDK dynamic proxy
        assertTrue(AopUtils.isAopProxy(cglibProxy));
        assertFalse(AopUtils.isJdkDynamicProxy(cglibProxy)); // must be false to enter CGLIB branch

        // Call getAdvisedSupport to exercise the CGLIB-specific code path
        AdvisedSupport advisedSupport = SpringProxyUtils.getAdvisedSupport(cglibProxy);
        assertNotNull(advisedSupport);

        // Verify that the target inside AdvisedSupport is the original object
        Object realTarget = advisedSupport.getTargetSource().getTarget();
        assertTrue(realTarget instanceof NoInterfaceTarget);
    }


    // testIsProxy
    @Test
    public void testIsProxy() {
        // 1) null → false
        assertFalse(SpringProxyUtils.isProxy(null));

        // 2) plain object → false
        TestServiceImpl plain = new TestServiceImpl();
        assertFalse(SpringProxyUtils.isProxy(plain));

        // 3) “pure” JDK dynamic proxy → true (Proxy.class.isAssignableFrom(...) or AopUtils.isAopProxy(...) check)
        TestService jdkProxy = (TestService) Proxy.newProxyInstance(
                TestService.class.getClassLoader(),
                new Class<?>[]{TestService.class},
                (proxy, method, args) -> null
        );
        assertTrue(SpringProxyUtils.isProxy(jdkProxy));

        // 4) Spring AOP proxy → true
        ProxyFactory factory = new ProxyFactory(new TestServiceImpl());
        factory.addInterface(TestService.class);
        TestService springProxy = (TestService) factory.getProxy();
        assertTrue(SpringProxyUtils.isProxy(springProxy));
    }

    // testGetTargetInterface
    @Test
    public void testGetTargetInterface() throws Exception {
        // 1) null → IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () ->
                SpringProxyUtils.getTargetInterface(null));

        // 2) “pure” JDK dynamic proxy → returns the first interface from proxy.getClass().getInterfaces()[0]
        TestService jdkProxy = (TestService) Proxy.newProxyInstance(
                TestService.class.getClassLoader(),
                new Class<?>[]{TestService.class},
                (proxy, method, args) -> null
        );
        assertEquals(TestService.class, SpringProxyUtils.getTargetInterface(jdkProxy));

        // 3) plain object (no proxy) → returns its own class
        TestServiceImpl plain = new TestServiceImpl();
        assertEquals(TestServiceImpl.class, SpringProxyUtils.getTargetInterface(plain));

        // 4) Spring AOP JDK‐style proxy → should return the proxied interface (TestService.class)
        ProxyFactory factory = new ProxyFactory(new TestServiceImpl());
        factory.addInterface(TestService.class);
        TestService springProxy = (TestService) factory.getProxy();
        assertEquals(TestService.class, SpringProxyUtils.getTargetInterface(springProxy));
    }



    // testGetAllInterfaces()
    @Test
    public void testGetAllInterfaces() {
        // 1) null → empty array
        assertEquals(0, SpringProxyUtils.getAllInterfaces(null).length);

        // 2) class that implements exactly one interface
        TestServiceImpl single = new TestServiceImpl();
        Class<?>[] ifacesSingle = SpringProxyUtils.getAllInterfaces(single);
        assertEquals(1, ifacesSingle.length);
        assertEquals(TestService.class, ifacesSingle[0]);

        // 3) class that implements two interfaces
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
        // The order is not guaranteed; just verify both are present
        assertEquals(2, ifacesMulti.length);
        assertTrue(Arrays.asList(ifacesMulti).contains(TestService.class));
        assertTrue(Arrays.asList(ifacesMulti).contains(Runnable.class));
    }
}