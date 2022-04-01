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
        if (doStore(context)) {
            // reset to un_updated
            context.setUpdated(null);
            return true;
        }
        return false;
    }

    /**
     * do store context
     *
     * @param context the BusinessActionContext
     * @return the boolean
     */
    protected abstract boolean doStore(BusinessActionContext context);
}
