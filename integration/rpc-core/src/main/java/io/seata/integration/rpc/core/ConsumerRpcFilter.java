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

package io.seata.integration.rpc.core;

import java.util.HashMap;
import java.util.Map;

import io.seata.common.util.StringUtils;
import io.seata.core.context.RootContext;

public interface ConsumerRpcFilter<T> extends BaseRpcFilter<T> {

    /**
     * get contexts from RootContext
     *
     * @return
     */
    default Map<String, String> getRootContexts() {
        Map<String, String> contextMap = new HashMap<>();
        if (RootContext.inGlobalTransaction()) {
            assertNotNull(RootContext.getXID(), "xid is null");
            contextMap.put(RootContext.KEY_XID, RootContext.getXID());
            contextMap.put(RootContext.KEY_BRANCH_TYPE, RootContext.getBranchType().name());
        }
        return contextMap;
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
        for (int i = 0; i < trxContextKeys.length; i++) {
            String contextValue = contextMap.get(trxContextKeys[i]);
            if (StringUtils.isNotBlank(contextValue)) {
                bindContextToRequest(rpcRequest, trxContextKeys[i], contextValue);
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
        for (int i = 0; i < trxContextKeys.length; i++) {
            String contextValue = contextMap.get(trxContextKeys[i]);
            if (StringUtils.isNotBlank(contextValue)) {
                cleanRequestContext(rpcRequest, trxContextKeys[i]);
            }
        }
    }

    void cleanRequestContext(T rpcRequest, String key);

}
