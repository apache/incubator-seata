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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.seata.integration.tx.api.interceptor.handler.ProxyInvocationHandler;
import org.apache.seata.integration.tx.api.interceptor.parser.DefaultInterfaceParser;
import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.apache.seata.spring.tcc.NormalTccAction;
import org.apache.seata.spring.tcc.NormalTccActionImpl;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @date 2023/11/29
 */
class AdapterSpringSeataInterceptorTest {

    private static NormalTccAction normalTccAction;

    private static AdapterSpringSeataInterceptor adapterSpringSeataInterceptor;

    @BeforeAll
    static void init() throws Throwable {
        //given
        normalTccAction = new NormalTccActionImpl();
        ProxyInvocationHandler proxyInvocationHandler = DefaultInterfaceParser.get().parserInterfaceToProxy(normalTccAction, "proxyTccAction");
        adapterSpringSeataInterceptor = new AdapterSpringSeataInterceptor(proxyInvocationHandler);
    }

    @Test
    void should_throw_raw_exception_when_call_prepareWithException() throws Throwable {
        MyMockMethodInvocation myMockMethodInvocation = new MyMockMethodInvocation(NormalTccAction.class.getMethod("prepareWithException", BusinessActionContext.class), () -> normalTccAction.prepareWithException(null));

        //when then
        Assertions.assertThrows(IllegalArgumentException.class, () -> adapterSpringSeataInterceptor.invoke(myMockMethodInvocation));
    }

    @Test
    void should_success_when_call_prepare_with_ProxyInvocationHandler() throws Throwable {
        MyMockMethodInvocation myMockMethodInvocation = new MyMockMethodInvocation(NormalTccAction.class.getMethod("prepare", BusinessActionContext.class), () -> normalTccAction.prepare(null));

        //when then
        Assertions.assertTrue((Boolean) adapterSpringSeataInterceptor.invoke(myMockMethodInvocation));
    }

    static class MyMockMethodInvocation implements MethodInvocation {

        private Callable callable;
        private Method method;

        public MyMockMethodInvocation(Method method, Callable callable) {
            this.method = method;
            this.callable = callable;
        }

        @Nullable
        @Override
        public Object proceed() throws Throwable {
            return callable.call();
        }

        @Nonnull
        @Override
        public Method getMethod() {
            return method;
        }

        @Nonnull
        @Override
        public Object[] getArguments() {
            return new Object[0];
        }

        @Nullable
        @Override
        public Object getThis() {
            return null;
        }

        @Nonnull
        @Override
        public AccessibleObject getStaticPart() {
            return null;
        }
    }
}
