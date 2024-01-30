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
package org.apache.seata.integration.tx.api.util;

import org.apache.seata.integration.tx.api.interceptor.handler.DefaultInvocationHandler;
import org.apache.seata.integration.tx.api.interceptor.handler.ProxyInvocationHandler;
import org.apache.seata.integration.tx.api.interceptor.parser.DefaultInterfaceParser;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;

import java.util.HashMap;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;


public class ProxyUtil {

    private static final Map<Object, Object> PROXYED_SET = new HashMap<>();

    public static <T> T createProxy(T target) {
        return createProxy(target, target.getClass().getName());
    }

    /**
     * The API for generating proxy for target. It can be used by spring aop, or
     * provide user to generate proxy manually.
     * <p>
     * At TM side, It can be used for the target bean with @GlobalTransactional or @GlobalLock to generate proxy which start global transaction.
     * At RM side, if you use TCC mode, It can be for target bean with @TwoPhaseBusinessAction to generate proxy which register branch source
     *      and branch transaction. If you want to use this API to generate proxy manual like dubbo, you must make sure the target bean is the
     *      business bean with @GlobalTransactional annotation. If you pass the ServiceBean(com.alibaba.dubbo.config.spring.ServiceBean) or
     *      ReferenceBean(com.alibaba.dubbo.config.spring.ReferenceBean), it will don't work.
     *
     * @param target    the business bean
     * @param beanName  the business bean name
     * @return          the proxy bean
     * @param <T>       the generics class
     */
    public static <T> T createProxy(T target, String beanName) {
        try {
            synchronized (PROXYED_SET) {
                if (PROXYED_SET.containsKey(target)) {
                    return (T) PROXYED_SET.get(target);
                }
                ProxyInvocationHandler proxyInvocationHandler = DefaultInterfaceParser.get().parserInterfaceToProxy(target, beanName);
                if (proxyInvocationHandler == null) {
                    return target;
                }
                T proxy = (T) new ByteBuddy().subclass(target.getClass())
                        .method(isDeclaredBy(target.getClass()))
                        .intercept(InvocationHandlerAdapter.of(new DefaultInvocationHandler(proxyInvocationHandler, target)))
                        .make()
                        .load(target.getClass().getClassLoader())
                        .getLoaded()
                        .getDeclaredConstructor()
                        .newInstance();
                PROXYED_SET.put(target, proxy);
                return proxy;
            }
        } catch (Throwable t) {
            throw new RuntimeException("error occurs when create seata proxy", t);
        }
    }

}
