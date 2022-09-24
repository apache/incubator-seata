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
package io.seata.commonapi.autoproxy;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.CollectionUtils;
import io.seata.commonapi.remoting.RemotingDesc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * the default transaction auto proxy
 *
 * @author ruishansun
 */
public class DefaultTransactionAutoProxy {

    /**
     * all the transaction auto proxy
     */
    protected static final List<TransactionAutoProxy> ALL_TRANSACTION_AUTO_PROXIES = new ArrayList<>();
    /**
     * method interceptor map, beanName -> IsTransactionProxyResult
     */
    private static final Map<String, IsTransactionProxyResult> METHOD_INTERCEPTOR_MAP = new ConcurrentHashMap<>();

    private static class SingletonHolder {
        private static final DefaultTransactionAutoProxy INSTANCE = new DefaultTransactionAutoProxy();
    }

    /**
     * Get the default transaction auto proxy
     *
     * @return the default transaction auto proxy
     */
    public static DefaultTransactionAutoProxy get() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Instantiates a new default transaction auto proxy
     */
    protected DefaultTransactionAutoProxy() {
        initTransactionAutoProxy();
    }

    /**
     * init transaction auto proxy
     */
    private void initTransactionAutoProxy() {
        List<TransactionAutoProxy> proxies = EnhancedServiceLoader.loadAll(TransactionAutoProxy.class);
        if (CollectionUtils.isNotEmpty(proxies)) {
            ALL_TRANSACTION_AUTO_PROXIES.addAll(proxies);
        }
    }

    /**
     * whether is transaction auto proxy
     *
     * @param beanName     the beanName
     * @param remotingDesc the remotingDesc
     * @return true or false
     */
    public boolean isTransactionAutoProxy(String beanName, RemotingDesc remotingDesc) {
        for (TransactionAutoProxy proxy : ALL_TRANSACTION_AUTO_PROXIES) {
            IsTransactionProxyResult result = proxy.isTransactionProxyTargetBean(remotingDesc);
            if (result.isProxyTargetBean()) {
                METHOD_INTERCEPTOR_MAP.put(beanName, result);
                return true;
            }
        }
        return false;
    }

    /**
     * get the IsTransactionProxyResult
     *
     * @param beanName the beanName
     * @return the IsTransactionProxyResult
     */
    public IsTransactionProxyResult getIsProxyTargetBeanResult(String beanName) {
        IsTransactionProxyResult result = METHOD_INTERCEPTOR_MAP.get(beanName);
        return result != null ? result : new IsTransactionProxyResult();
    }
}