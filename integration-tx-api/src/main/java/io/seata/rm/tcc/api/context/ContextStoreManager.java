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
package io.seata.rm.tcc.api.context;

import io.seata.rm.tcc.api.BusinessActionContext;

/**
 * manage the actionContext store
 * @author pengten
 */
public interface ContextStoreManager {

    /**
     * store context
     * @param context the BusinessActionContext
     * @return the boolean
     */
    boolean storeContext(BusinessActionContext context);

    /**
     * search context
     * @param context the BusinessActionContext in TC
     * @return the boolean
     */
    BusinessActionContext searchContext(BusinessActionContext context);
}