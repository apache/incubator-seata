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
package io.seata.integration.tx.api.interceptor.parser;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.CollectionUtils;
import io.seata.integration.tx.api.interceptor.handler.ProxyInvocationHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * @author leezongjie
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

    @Override
    public ProxyInvocationHandler parserInterfaceToProxy(Object target) throws Exception {
        for (InterfaceParser interfaceParser : ALL_INTERFACE_PARSERS) {
            ProxyInvocationHandler proxyInvocationHandler = interfaceParser.parserInterfaceToProxy(target);
            if (proxyInvocationHandler != null) {
                return proxyInvocationHandler;
            }
        }
        return null;
    }

    @Override
    public IfNeedEnhanceBean parseIfNeedEnhanceBean(Object target) throws Exception {
        for (InterfaceParser interfaceParser : ALL_INTERFACE_PARSERS) {
            IfNeedEnhanceBean ifNeedEnhanceBean = interfaceParser.parseIfNeedEnhanceBean(target);
            if (ifNeedEnhanceBean.isIfNeed()) {
                return ifNeedEnhanceBean;
            }
        }
        return new IfNeedEnhanceBean();
    }

}