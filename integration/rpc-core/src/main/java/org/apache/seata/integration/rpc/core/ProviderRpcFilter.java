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
import org.apache.seata.core.model.BranchType;


public interface ProviderRpcFilter<T> extends BaseRpcFilter<T> {

    String[] TRX_CONTEXT_KEYS = new String[] {RootContext.KEY_XID, RootContext.KEY_XID.toLowerCase(),
        RootContext.KEY_BRANCH_TYPE};

    String LOW_KEY_XID = "tx_xid";

    /**
     * get contexts from RpcRequest
     *
     * @param rpcRequest
     * @return
     */
    default Map<String, String> getRpcContexts(T rpcRequest) {
        Map<String, String> contextMap = new HashMap<>();
        for (int i = 0; i < TRX_CONTEXT_KEYS.length; i++) {
            String contextValue = getRpcContext(rpcRequest, TRX_CONTEXT_KEYS[i]);
            if (StringUtils.isNotBlank(contextValue)) {
                contextMap.put(TRX_CONTEXT_KEYS[i], contextValue);
            }
        }
        return contextMap;
    }

    String getRpcContext(T rpcContext, String key);

    default String getXidFromContexts(Map<String, String> rpcContextMap) {
        String xid = getValueFromMap(rpcContextMap, RootContext.KEY_XID);
        if (StringUtils.isBlank(xid)) {
            return getValueFromMap(rpcContextMap, RootContext.KEY_XID.toLowerCase());
        }
        return xid;
    }

    default void bindRequestToContexts(Map<String, String> contextMap) {
        for (int i = 0; i < TRX_CONTEXT_KEYS.length; i++) {
            String contextValue = contextMap.get(TRX_CONTEXT_KEYS[i]);
            if (StringUtils.isNotBlank(contextValue)) {
                switch (TRX_CONTEXT_KEYS[i]) {
                    case RootContext.KEY_XID:
                    case LOW_KEY_XID:
                        RootContext.bind(contextValue);
                        break;
                    case RootContext.KEY_BRANCH_TYPE:
                        if (BranchType.TCC.name().equalsIgnoreCase(contextValue)) {
                            RootContext.bindBranchType(BranchType.TCC);
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("wrong context:" + TRX_CONTEXT_KEYS[i]);
                }

            }
        }
    }

    default Map<String, String> cleanRootContexts() {
        Map<String, String> contextMap = new HashMap<>();
        for (int i = 0; i < TRX_CONTEXT_KEYS.length; i++) {
            switch (TRX_CONTEXT_KEYS[i]) {
                case RootContext.KEY_XID:
                    String xid = RootContext.unbind();
                    contextMap.put(RootContext.KEY_XID, xid);
                    break;
                case LOW_KEY_XID:
                    break;
                case RootContext.KEY_BRANCH_TYPE:
                    BranchType contextValue = RootContext.getBranchType();
                    if (BranchType.TCC == contextValue) {
                        RootContext.unbindBranchType();
                    }
                    if (null != contextValue) {
                        contextMap.put(RootContext.KEY_BRANCH_TYPE, contextValue.name());
                    }
                    break;
                default:
                    throw new IllegalArgumentException("wrong context:" + TRX_CONTEXT_KEYS[i]);
            }

        }
        return contextMap;
    }

    default void resetRootContexts(Map<String, String> contextMap) {
        bindRequestToContexts(contextMap);
    }

}
