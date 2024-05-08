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
package org.apache.seata.integration.tx.api.interceptor.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.integration.tx.api.interceptor.handler.ProxyInvocationHandler;

/**
 */
public class DefaultInterfaceParser implements InterfaceParser {

    protected static final List<InterfaceParser> ALL_INTERFACE_PARSERS = new ArrayList<>();


    private static class SingletonHolder {
        private static final DefaultInterfaceParser INSTANCE = new DefaultInterfaceParser();
    }

    public static DefaultInterfaceParser get() {
        return DefaultInterfaceParser.SingletonHolder.INSTANCE;
    }

    protected DefaultInterfaceParser() {
        initInterfaceParser();
    }

    /**
     * init parsers
     */
    protected void initInterfaceParser() {
        List<InterfaceParser> interfaceParsers = EnhancedServiceLoader.loadAll(InterfaceParser.class);
        if (CollectionUtils.isNotEmpty(interfaceParsers)) {
            ALL_INTERFACE_PARSERS.addAll(interfaceParsers);
        }
    }

    /**
     * Create an interceptor chain that supports adding multiple interceptors.Create an interceptor chain that supports adding multiple interceptors.
     * The entry order of the facets can be specified through {@link ProxyInvocationHandler # order()}.
     * It is not allowed to load multiple interceptors of the same type, such as two-stage annotations for TCC and Saga that cannot exist simultaneously. The type can be specified through {@link ProxyInvocationHandler # type()}.It is not allowed to load multiple interceptors of the same type, such as two-stage annotations for TCC and Saga that cannot exist simultaneously. The type can be specified through {@link ProxyInvocationHandler # type()}.
     *
     * @param target
     * @param objectName
     * @return
     * @throws Exception
     */
    @Override
    public ProxyInvocationHandler parserInterfaceToProxy(Object target, String objectName) throws Exception {
        List<ProxyInvocationHandler> invocationHandlerList = new ArrayList<>();
        Set<String> invocationHandlerRepeatCheck = new HashSet<>();

        for (InterfaceParser interfaceParser : ALL_INTERFACE_PARSERS) {
            ProxyInvocationHandler proxyInvocationHandler = interfaceParser.parserInterfaceToProxy(target, objectName);
            if (proxyInvocationHandler != null) {
                if (!invocationHandlerRepeatCheck.add(proxyInvocationHandler.type())) {
                    throw new RuntimeException("there is already an annotation of type " + proxyInvocationHandler.type() + " for class: " + target.getClass().getName());
                }
                invocationHandlerList.add(proxyInvocationHandler);
            }
        }

        Collections.sort(invocationHandlerList, Comparator.comparingInt(ProxyInvocationHandler::order));

        ProxyInvocationHandler first = null;
        ProxyInvocationHandler last = null;
        for (ProxyInvocationHandler proxyInvocationHandler : invocationHandlerList) {
            if (first == null) {
                first = proxyInvocationHandler;
            }
            if (last != null) {
                last.setNextProxyInvocationHandler(proxyInvocationHandler);
            }
            last = proxyInvocationHandler;
        }

        return first;
    }

    @Override
    public IfNeedEnhanceBean parseIfNeedEnhancement(Class<?> beanClass) {
        for (InterfaceParser interfaceParser : ALL_INTERFACE_PARSERS) {
            IfNeedEnhanceBean ifNeedEnhanceBean = interfaceParser.parseIfNeedEnhancement(beanClass);
            if (ifNeedEnhanceBean.isIfNeed()) {
                return ifNeedEnhanceBean;
            }
        }
        return new IfNeedEnhanceBean();
    }

}
