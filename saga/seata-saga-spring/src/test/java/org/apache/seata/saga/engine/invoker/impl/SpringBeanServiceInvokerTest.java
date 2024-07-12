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
package org.apache.seata.saga.engine.invoker.impl;

import java.util.Collections;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.seata.saga.statelang.domain.impl.AbstractTaskState;
import org.apache.seata.saga.statelang.domain.impl.ServiceTaskStateImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

/**
 * SpringBeanServiceInvokerTest
 */
public class SpringBeanServiceInvokerTest {
    @Test
    public void testInvokeByClassParam() throws Throwable {
        SpringBeanServiceInvoker springBeanServiceInvoker = new SpringBeanServiceInvoker();
        Object[] input = new Object[]{"param"};
        ServiceTaskStateImpl serviceTaskState = new ServiceTaskStateImpl();
        serviceTaskState.setServiceName("mockService");
        serviceTaskState.setServiceMethod("mockInvoke");
        serviceTaskState.setParameterTypes(Collections.singletonList("java.lang.String"));
        MockService mockService = new MockService();
        ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);
        when(applicationContext.getBean(anyString())).thenReturn(mockService);
        springBeanServiceInvoker.setThreadPoolExecutor(new ThreadPoolExecutor(1,
                1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>()));
        springBeanServiceInvoker.setApplicationContext(applicationContext);

        String output = (String) springBeanServiceInvoker.invoke(serviceTaskState, input);
        Assertions.assertEquals(output, "param");
    }

    @Test
    public void testInvokeByPrimitiveParam() throws Throwable {
        SpringBeanServiceInvoker springBeanServiceInvoker = new SpringBeanServiceInvoker();
        Object[] input = new Object[]{false};
        ServiceTaskStateImpl serviceTaskState = new ServiceTaskStateImpl();
        serviceTaskState.setServiceName("mockService");
        serviceTaskState.setServiceMethod("mockInvoke");
        serviceTaskState.setParameterTypes(Collections.singletonList("boolean"));
        MockService mockService = new MockService();
        serviceTaskState.setMethod(mockService.getClass().getMethod("mockInvoke", boolean.class));
        ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);
        when(applicationContext.getBean(anyString())).thenReturn(mockService);
        springBeanServiceInvoker.setThreadPoolExecutor(new ThreadPoolExecutor(1,
                1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>()));
        springBeanServiceInvoker.setApplicationContext(applicationContext);

        boolean output = (boolean) springBeanServiceInvoker.invoke(serviceTaskState, input);
        Assertions.assertEquals(output, false);
    }

    @Test
    public void testInvokeRetryFailed() throws Throwable {
        SpringBeanServiceInvoker springBeanServiceInvoker = new SpringBeanServiceInvoker();
        Object[] input = new Object[]{"param"};
        ServiceTaskStateImpl serviceTaskState = new ServiceTaskStateImpl();
        serviceTaskState.setServiceName("mockService");
        serviceTaskState.setServiceMethod("mockInvokeRetry");
        serviceTaskState.setParameterTypes(Collections.singletonList("java.lang.String"));
        AbstractTaskState.RetryImpl retry = new AbstractTaskState.RetryImpl();
        retry.setMaxAttempts(3);
        retry.setExceptions(Collections.singletonList("java.lang.NullPoint"));
        serviceTaskState.setRetry(Collections.singletonList(retry));
        MockService mockService = new MockService();
        ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);
        when(applicationContext.getBean(anyString())).thenReturn(mockService);
        springBeanServiceInvoker.setThreadPoolExecutor(new ThreadPoolExecutor(1,
                1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>()));
        springBeanServiceInvoker.setApplicationContext(applicationContext);

        Assertions.assertThrows(java.lang.RuntimeException.class, () -> springBeanServiceInvoker.invoke(serviceTaskState, input));
    }

    @Test
    public void testInvokeRetrySuccess() throws Throwable {
        SpringBeanServiceInvoker springBeanServiceInvoker = new SpringBeanServiceInvoker();
        Object[] input = new Object[]{"param"};
        ServiceTaskStateImpl serviceTaskState = new ServiceTaskStateImpl();
        serviceTaskState.setServiceName("mockService");
        serviceTaskState.setServiceMethod("mockInvokeRetry");
        serviceTaskState.setParameterTypes(Collections.singletonList("java.lang.String"));
        AbstractTaskState.RetryImpl retry = new AbstractTaskState.RetryImpl();
        retry.setMaxAttempts(3);
        retry.setExceptions(Collections.singletonList("java.lang.RuntimeException"));
        serviceTaskState.setRetry(Collections.singletonList(retry));
        MockService mockService = new MockService();
        ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);
        when(applicationContext.getBean(anyString())).thenReturn(mockService);
        springBeanServiceInvoker.setThreadPoolExecutor(new ThreadPoolExecutor(1,
                1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>()));
        springBeanServiceInvoker.setApplicationContext(applicationContext);

        String output = (String) springBeanServiceInvoker.invoke(serviceTaskState, input);
        Assertions.assertEquals(output, "param");
    }

    @Test
    public void testInvokeAsync() throws Throwable {
        SpringBeanServiceInvoker springBeanServiceInvoker = new SpringBeanServiceInvoker();
        Object[] input = new Object[]{"param"};
        ServiceTaskStateImpl serviceTaskState = new ServiceTaskStateImpl();
        serviceTaskState.setServiceName("mockService");
        serviceTaskState.setServiceMethod("mockInvoke");
        serviceTaskState.setParameterTypes(Collections.singletonList("java.lang.String"));
        serviceTaskState.setAsync(true);
        MockService mockService = new MockService();
        ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);
        when(applicationContext.getBean(anyString())).thenReturn(mockService);
        springBeanServiceInvoker.setThreadPoolExecutor(new ThreadPoolExecutor(1,
                1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>()));
        springBeanServiceInvoker.setApplicationContext(applicationContext);

        String output = (String) springBeanServiceInvoker.invoke(serviceTaskState, input);
        Assertions.assertEquals(output, null);
    }

    @Test
    public void testInvokeAsyncButSync() throws Throwable {
        SpringBeanServiceInvoker springBeanServiceInvoker = new SpringBeanServiceInvoker();
        Object[] input = new Object[]{"param"};
        ServiceTaskStateImpl serviceTaskState = new ServiceTaskStateImpl();
        serviceTaskState.setServiceName("mockService");
        serviceTaskState.setServiceMethod("mockInvoke");
        serviceTaskState.setParameterTypes(Collections.singletonList("java.lang.String"));
        serviceTaskState.setAsync(true);
        MockService mockService = new MockService();
        ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);
        when(applicationContext.getBean(anyString())).thenReturn(mockService);
        springBeanServiceInvoker.setApplicationContext(applicationContext);

        String output = (String) springBeanServiceInvoker.invoke(serviceTaskState, input);
        Assertions.assertEquals(output, "param");
    }
}