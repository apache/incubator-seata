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
package org.apache.seata.integration.rpc.core;

import java.util.HashMap;
import java.util.Map;

import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.context.RootContext;

public interface ConsumerRpcFilter<T> extends BaseRpcFilter<T> {

    /**
     * get contexts from RootContext
     *
     * @return
     */
    default Map<String, String> getRootContexts() {
        Map<String, String> contextMap = new HashMap<>();
        if (RootContext.inGlobalTransaction()) {
            for (int i = 0; i < TRX_CONTEXT_KEYS.length; i++) {
                switch (TRX_CONTEXT_KEYS[i]) {
                    case RootContext.KEY_XID:
                        assertNotNull(RootContext.getXID(), "xid is null");
                        contextMap.put(RootContext.KEY_XID, RootContext.getXID());
                        break;
                    case RootContext.KEY_BRANCH_TYPE:
                        contextMap.put(RootContext.KEY_BRANCH_TYPE, RootContext.getBranchType().name());
                        break;
                    default:
                        throw new IllegalArgumentException("wrong context: " + TRX_CONTEXT_KEYS[i]);
                }
            }
        } return contextMap;
    }

    default String getXidFromRootContexts(Map<String, String> rootContextMap) {
        return getValueFromMap(rootContextMap, RootContext.KEY_XID);
    }

    /**
     * bind contexts to RpcRequest
     *
     * @param rpcRequest
     * @param contextMap
     */
    default void bindContextsToRequest(T rpcRequest, Map<String, String> contextMap) {
        for (int i = 0; i < TRX_CONTEXT_KEYS.length; i++) {
            String contextValue = contextMap.get(TRX_CONTEXT_KEYS[i]);
            if (StringUtils.isNotBlank(contextValue)) {
                bindContextToRequest(rpcRequest, TRX_CONTEXT_KEYS[i], contextValue);
            }
        }
    }

    void bindContextToRequest(T rpcRequest, String key, String value);

    /**
     * clean contexts to RpcRequest
     *
     * @param rpcRequest
     * @param contextMap
     */
    default void cleanRequestContexts(T rpcRequest, Map<String, String> contextMap) {
        for (int i = 0; i < TRX_CONTEXT_KEYS.length; i++) {
            String contextValue = contextMap.get(TRX_CONTEXT_KEYS[i]);
            if (StringUtils.isNotBlank(contextValue)) {
                cleanRequestContext(rpcRequest, TRX_CONTEXT_KEYS[i]);
            }
        }
    }

    void cleanRequestContext(T rpcRequest, String key);

}
