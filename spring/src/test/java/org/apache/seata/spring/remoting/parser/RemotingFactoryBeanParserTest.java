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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

public class RemotingFactoryBeanParserTest {

    interface Dummy {
    }

    @Test
    public void testGetRemotingFactoryBean_nonProxyReturnsNull() throws Exception {
        ApplicationContext ctx = Mockito.mock(ApplicationContext.class);
        RemotingFactoryBeanParser parser = new RemotingFactoryBeanParser(ctx);
        Method m = RemotingFactoryBeanParser.class.getDeclaredMethod("getRemotingFactoryBean", Object.class,
                String.class);
        m.setAccessible(true);
        Object result = m.invoke(parser, new Object(), "foo");
        Assertions.assertNull(result);
    }

    @Test
    public void testGetRemotingFactoryBean_proxyLooksUpFactoryBean() throws Exception {
        // create a jdk dynamic proxy to make SpringProxyUtils.isProxy return true
        Dummy proxy = (Dummy) Proxy.newProxyInstance(
                this.getClass().getClassLoader(),
                new Class[] { Dummy.class },
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        return null;
                    }
                });

        ApplicationContext ctx = Mockito.mock(ApplicationContext.class);
        Object factoryBean = new Object();
        Mockito.when(ctx.containsBean("&myBean")).thenReturn(true);
        Mockito.when(ctx.getBean("&myBean")).thenReturn(factoryBean);

        RemotingFactoryBeanParser parser = new RemotingFactoryBeanParser(ctx);
        Method m = RemotingFactoryBeanParser.class.getDeclaredMethod("getRemotingFactoryBean", Object.class,
                String.class);
        m.setAccessible(true);
        Object result = m.invoke(parser, proxy, "myBean");
        Assertions.assertSame(factoryBean, result);
    }
}
