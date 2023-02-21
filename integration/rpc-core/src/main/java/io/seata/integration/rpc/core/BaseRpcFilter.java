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

import java.util.Map;

import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.core.context.RootContext;

/**
 * @author slievrly
 */
public interface BaseRpcFilter<T> {
    String[] TRX_CONTEXT_KEYS = new String[] {RootContext.KEY_XID, RootContext.KEY_BRANCH_TYPE};

    default String getValueFromMap(Map<String, String> rpcContextMap, String key) {
        return rpcContextMap.get(key);
    }
    default void assertNotNull(Object obj, String errMsg) {
        if (obj == null) {
            throw new IllegalStateException(errMsg);
        }
    }

    default String getJsonContext(Map<String, String> contextMap) {
        if (CollectionUtils.isEmpty(contextMap)) {
            return StringUtils.EMPTY;
        }
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < TRX_CONTEXT_KEYS.length; i++) {
            String contextValue = contextMap.get(TRX_CONTEXT_KEYS[i]);
            if (i > 0) {
                sb.append(",");
            }
            sb.append("\"").append(TRX_CONTEXT_KEYS[i]).append("\"");
            sb.append(":");
            if (null == contextValue) {
                sb.append("null");
            } else {
                sb.append("\"").append(contextValue).append("\"");
            }

        }
        sb.append("}");
        return sb.toString();
    }
}
