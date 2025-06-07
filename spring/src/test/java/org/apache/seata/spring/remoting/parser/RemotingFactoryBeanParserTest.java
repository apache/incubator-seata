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
package org.apache.seata.spring.remoting.parser;

import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.integration.tx.api.remoting.RemotingDesc;
import org.apache.seata.integration.tx.api.remoting.parser.DefaultRemotingParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.ApplicationContext;

/**
 * Test for {@link RemotingFactoryBeanParser} class
 */
public class RemotingFactoryBeanParserTest {

    private ApplicationContext applicationContext;
    private RemotingFactoryBeanParser remotingFactoryBeanParser;
    private DefaultRemotingParser defaultRemotingParser;
    private MockedStatic<DefaultRemotingParser> mockedDefaultRemotingParser;

    private interface TestService {
        void doSomething();
    }

    private static class TestServiceImpl implements TestService {
        @Override
        public void doSomething() {
            // Empty implementation
        }
    }

    @BeforeEach
    public void setUp() {
        applicationContext = Mockito.mock(ApplicationContext.class);
        remotingFactoryBeanParser = new RemotingFactoryBeanParser(applicationContext);

        // Use Mockito.mockStatic to mock the static method of DefaultRemotingParser
        defaultRemotingParser = Mockito.mock(DefaultRemotingParser.class);
        mockedDefaultRemotingParser = Mockito.mockStatic(DefaultRemotingParser.class);
        mockedDefaultRemotingParser.when(DefaultRemotingParser::get).thenReturn(defaultRemotingParser);
    }

    @AfterEach
    public void tearDown() {
        if (mockedDefaultRemotingParser != null) {
            mockedDefaultRemotingParser.close();
        }
    }
    
    /**
     * Create a proxy test service
     *
     * @return the proxied test service
     */
    private TestService createProxyTestService() {
        TestService testService = new TestServiceImpl();
        ProxyFactory proxyFactory = new ProxyFactory(testService);
        return (TestService) proxyFactory.getProxy();
    }

    @Test
    public void testConstructorWithNullApplicationContext() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new RemotingFactoryBeanParser(null);
        });
    }
    
    @Test
    public void testGetRemotingFactoryBeanWithNonProxyBean() {
        TestService testService = new TestServiceImpl();
        Object result = remotingFactoryBeanParser.getRemotingFactoryBean(testService, "testService");
        Assertions.assertNull(result);
    }
    
    @Test
    public void testGetRemotingFactoryBeanWithProxyBeanButNoFactoryBean() {
        // Create proxy object
        TestService proxyTestService = createProxyTestService();
        
        // Mock applicationContext behavior
        Mockito.when(applicationContext.containsBean("&testService")).thenReturn(false);
        
        // Test
        Object result = remotingFactoryBeanParser.getRemotingFactoryBean(proxyTestService, "testService");
        Assertions.assertNull(result);
    }
    
    @Test
    public void testGetRemotingFactoryBeanWithProxyBeanAndFactoryBean() {
        // Create proxy object
        TestService proxyTestService = createProxyTestService();
        
        // Mock applicationContext behavior
        Object expectedFactoryBean = new Object();
        Mockito.when(applicationContext.containsBean("&testService")).thenReturn(true);
        Mockito.when(applicationContext.getBean("&testService")).thenReturn(expectedFactoryBean);
        
        // Test
        Object result = remotingFactoryBeanParser.getRemotingFactoryBean(proxyTestService, "testService");
        
        // Alternative to assertNotNull - check that result is the same as our expected object
        Assertions.assertSame(expectedFactoryBean, result);
    }
    
    @Test
    public void testIsReferenceWithNullFactoryBean() {
        TestService testService = new TestServiceImpl();
        boolean result = remotingFactoryBeanParser.isReference(testService, "testService");
        Assertions.assertFalse(result);
    }
    
    @Test
    public void testIsReferenceWithFactoryBean() {
        // Create proxy object
        TestService proxyTestService = createProxyTestService();
        
        Object factoryBean = new Object();
        
        // Mock applicationContext behavior
        Mockito.when(applicationContext.containsBean("&testService")).thenReturn(true);
        Mockito.when(applicationContext.getBean("&testService")).thenReturn(factoryBean);
        
        // Mock DefaultRemotingParser behavior
        Mockito.when(defaultRemotingParser.isReference(factoryBean, "&testService")).thenReturn(true);
        
        // Test
        boolean result = remotingFactoryBeanParser.isReference(proxyTestService, "testService");
        Assertions.assertTrue(result);
    }
    
    @Test
    public void testIsServiceWithNullFactoryBean() {
        TestService testService = new TestServiceImpl();
        boolean result = remotingFactoryBeanParser.isService(testService, "testService");
        Assertions.assertFalse(result);
    }
    
    @Test
    public void testIsServiceWithFactoryBean() {
        // Create proxy object
        TestService proxyTestService = createProxyTestService();
        
        Object factoryBean = new Object();
        
        // Mock applicationContext behavior
        Mockito.when(applicationContext.containsBean("&testService")).thenReturn(true);
        Mockito.when(applicationContext.getBean("&testService")).thenReturn(factoryBean);
        
        // Mock DefaultRemotingParser behavior
        Mockito.when(defaultRemotingParser.isService(factoryBean, "&testService")).thenReturn(true);
        
        // Test
        boolean result = remotingFactoryBeanParser.isService(proxyTestService, "testService");
        Assertions.assertTrue(result);
    }
    
    @Test
    public void testIsServiceWithClass() {
        boolean result = remotingFactoryBeanParser.isService(TestServiceImpl.class);
        Assertions.assertFalse(result);
    }
    
    @Test
    public void testGetServiceDescWithNullFactoryBean() {
        TestService testService = new TestServiceImpl();
        RemotingDesc result = remotingFactoryBeanParser.getServiceDesc(testService, "testService");
        Assertions.assertNull(result);
    }
    
    @Test
    public void testGetServiceDescWithFactoryBean() throws FrameworkException {
        // Create proxy object
        TestService proxyTestService = createProxyTestService();
        
        Object factoryBean = new Object();
        RemotingDesc expectedDesc = new RemotingDesc();
        
        // Mock applicationContext behavior
        Mockito.when(applicationContext.containsBean("&testService")).thenReturn(true);
        Mockito.when(applicationContext.getBean("&testService")).thenReturn(factoryBean);
        
        // Mock DefaultRemotingParser behavior
        Mockito.when(defaultRemotingParser.getServiceDesc(factoryBean, "&testService")).thenReturn(expectedDesc);
        
        // Test
        RemotingDesc result = remotingFactoryBeanParser.getServiceDesc(proxyTestService, "testService");
        Assertions.assertEquals(expectedDesc, result);
    }
    
    @Test
    public void testGetProtocol() {
        short result = remotingFactoryBeanParser.getProtocol();
        Assertions.assertEquals(0, result);
    }
} 