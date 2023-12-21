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
package io.seata.integration.tx.api.util;

import io.seata.integration.tx.api.interceptor.handler.DefaultInvocationHandler;
import io.seata.integration.tx.api.interceptor.handler.ProxyInvocationHandler;
import io.seata.integration.tx.api.interceptor.parser.DefaultInterfaceParser;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;

import java.util.HashMap;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;

/**
 */
public class ProxyUtil {

    private static final Map<Object, Object> PROXYED_SET = new HashMap<>();

    public static <T> T createProxy(T target) {
        return createProxy(target, target.getClass().getName());
    }

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
