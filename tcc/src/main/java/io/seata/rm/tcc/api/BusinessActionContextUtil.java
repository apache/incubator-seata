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
package io.seata.rm.tcc.api;

import java.util.Collections;
import java.util.Map;

import io.seata.common.Constants;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.CollectionUtils;
import io.seata.rm.tcc.context.store.ContextStoreManager;
import io.seata.rm.tcc.interceptor.ActionContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * the api of sharing business action context to tcc phase 2
 *
 * @author tanzj
 */
public final class BusinessActionContextUtil {

    private BusinessActionContextUtil() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(BusinessActionContextUtil.class);

    private static final ThreadLocal<BusinessActionContext> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * add business action context and share it to tcc phase2
     *
     * @param key   the key of new context
     * @param value new context
     * @return branch report succeed
     */
    public static boolean addContext(String key, Object value) {
        if (value == null) {
            return false;
        }

        Map<String, Object> newContext = Collections.singletonMap(key, value);
        return addContext(newContext);
    }

    /**
     * batch share new context to tcc phase 2
     *
     * @param context the new context
     * @return branch report succeed
     */
    @SuppressWarnings("deprecation")
    public static boolean addContext(Map<String, Object> context) {
        if (CollectionUtils.isEmpty(context)) {
            return false;
        }

        // put action context
        BusinessActionContext actionContext = BusinessActionContextUtil.getContext();
        if (!ActionContextUtil.putActionContext(actionContext.getActionContext(), context)) {
            // the action context is not changed, do not report
            return false;
        }
        // set updated
        actionContext.setUpdated(true);

        // if delay report, params will be finally reported after phase 1 execution
        if (Boolean.TRUE.equals(actionContext.getDelayReport())) {
            return false;
        }

        // do branch report
        return reportContext();
    }

    /**
     * to do branch report sharing actionContext
     *
     * @return branch report succeed
     */
    public static boolean reportContext() {
        BusinessActionContext context = getContext();
        ContextStoreManager contextStoreManager = EnhancedServiceLoader.load(ContextStoreManager.class
                , context.getActionContext(Constants.TCC_ACTION_CONTEXT_STORE_TYPE, String.class));
        return contextStoreManager.storeContext(context);
    }

    public static BusinessActionContext getContext() {
        return CONTEXT_HOLDER.get();
    }

    public static void setContext(BusinessActionContext context) {
        CONTEXT_HOLDER.set(context);
    }

    public static void clear() {
        CONTEXT_HOLDER.remove();
    }
}
