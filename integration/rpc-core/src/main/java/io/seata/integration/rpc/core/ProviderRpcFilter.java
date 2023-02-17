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
import io.seata.core.model.BranchType;

/**
 * @author slievrly
 */
public interface ProviderRpcFilter<T> extends BaseRpcFilter<T> {

    String[] trxContextKeys = new String[] {RootContext.KEY_XID, RootContext.KEY_XID.toLowerCase(),
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
        for (int i = 0; i < trxContextKeys.length; i++) {
            String contextValue = getRpcContext(rpcRequest, trxContextKeys[i]);
            if (StringUtils.isNotBlank(contextValue)) {
                contextMap.put(trxContextKeys[i], contextValue);
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
        for (int i = 0; i < trxContextKeys.length; i++) {
            String contextValue = contextMap.get(trxContextKeys[i]);
            if (StringUtils.isNotBlank(contextValue)) {
                switch (trxContextKeys[i]) {
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
                        throw new IllegalArgumentException("wrong context:" + trxContextKeys[i]);
                }

            }
        }
    }

    default Map<String, String> cleanRootContexts() {
        Map<String, String> contextMap = new HashMap<>();
        for (int i = 0; i < trxContextKeys.length; i++) {
            switch (trxContextKeys[i]) {
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
                    throw new IllegalArgumentException("wrong context:" + trxContextKeys[i]);
            }

        }
        return contextMap;
    }

    default void resetRootContexts(Map<String, String> contextMap) {
        bindRequestToContexts(contextMap);
    }

}
