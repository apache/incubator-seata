/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.integrationapi.util;

import io.seata.integrationapi.interceptor.handler.DefaultInvocationHandler;
import io.seata.integrationapi.interceptor.handler.ProxyInvocationHandler;
import io.seata.integrationapi.interceptor.parser.DefaultInterfaceParser;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.InvocationHandlerAdapter;

import java.util.HashMap;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;

/**
 * @author leezongjie
 * @date 2022/11/26
 */
public class ProxyUtil {

    private static final Map<Object, Object> PROXYED_SET = new HashMap<>();

    public static <T> T createProxy(T target) {
        try {
            synchronized (PROXYED_SET) {
                if (PROXYED_SET.containsKey(target)) {
                    return (T) PROXYED_SET.get(target);
                }
                ProxyInvocationHandler proxyInvocationHandler = DefaultInterfaceParser.get().parserInterfaceToProxy(target);
                if (proxyInvocationHandler == null) {
                    return target;
                }
                DynamicType.Builder.MethodDefinition.ImplementationDefinition<?> implementationDefinition = new ByteBuddy()
                            .subclass(target.getClass())
                            .method(isDeclaredBy(target.getClass()));
                T proxy = (T) implementationDefinition.intercept(InvocationHandlerAdapter.of(new DefaultInvocationHandler(proxyInvocationHandler, target)))
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
