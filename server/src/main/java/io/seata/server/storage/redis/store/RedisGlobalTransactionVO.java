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
package io.seata.server.storage.redis.store;

import io.seata.core.model.GlobalStatus;
import io.seata.core.store.GlobalTransactionDO;

/**
 * redis Global Transaction data object
 *
 * @author Bughue
 */
public class RedisGlobalTransactionVO {

    private GlobalTransactionDO globalTransactionDO;

    private GlobalStatus expectedStatus;

    /**
     * Gets globalTransactionDO.
     *
     * @return the globalTransactionDO.
     */
    public GlobalTransactionDO getGlobalTransactionDO() {
        return globalTransactionDO;
    }

    /**
     * Sets globalTransactionDO.
     *
     * @param globalTransactionDO the globalTransactionDO
     */
    public void setGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
        this.globalTransactionDO = globalTransactionDO;
    }

    /**
     * Gets expected status.
     *
     * @return the expected status.
     */
    public GlobalStatus getExpectedStatus() {
        return expectedStatus;
    }

    /**
     * Sets expected status.
     *
     * @param expectedStatus the expected status
     */
    public void setExpectedStatus(GlobalStatus expectedStatus) {
        this.expectedStatus = expectedStatus;
    }
}
