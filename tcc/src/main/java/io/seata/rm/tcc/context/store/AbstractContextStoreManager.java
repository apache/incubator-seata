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
package io.seata.rm.tcc.context.store;

import io.seata.common.exception.FrameworkException;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.StringUtils;
import io.seata.rm.tcc.api.BusinessActionContext;

/**
 * abstract context store manager
 *
 * @author yangwenpeng
 */
public abstract class AbstractContextStoreManager implements ContextStoreManager {

    @Override
    public boolean storeContext(BusinessActionContext context) {
        // check is updated
        if (!Boolean.TRUE.equals(context.getUpdated())) {
            return false;
        }

        // if not supported, call the next store
        if (!isSupport(context)) {
            String nextStore = getNextStore();
            if (StringUtils.isBlank(nextStore)) {
                throw new FrameworkException("action context is not supported!");
            }
            ContextStoreManager nextStoreManager = EnhancedServiceLoader.load(ContextStoreManager.class, nextStore);
            if (nextStoreManager == null) {
                throw new FrameworkException("action context is not supported!");
            }
            return nextStoreManager.storeContext(context);
        }

        // do store
        if (doStore(context)) {
            // reset to un_updated
            context.setUpdated(null);
            return true;
        }
        return false;
    }

    /**
     * the next ContextStoreManager load level
     *
     * @return the load level
     */
    protected String getNextStore() {
        return "tc";
    }

    /**
     * check if it is supported
     *
     * @param context the BusinessActionContext
     * @return the boolean
     */
    protected boolean isSupport(BusinessActionContext context) {
        return true;
    }

    /**
     * do store context
     *
     * @param context the BusinessActionContext
     * @return the boolean
     */
    protected abstract boolean doStore(BusinessActionContext context);
}
