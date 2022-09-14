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
package io.seata.server.storage.db.lock;


import io.seata.common.ConfigurationKeys;
import io.seata.common.loader.LoadLevel;
import io.seata.common.loader.Scope;
import io.seata.config.ConfigurationFactory;
import io.seata.core.store.DistributedLockDO;
import io.seata.core.store.DistributedLocker;
import io.seata.server.storage.db.DataBaseStoreFactory;
import io.seata.server.storage.db.DataBaseStoreType;

/**
 * @author chd
 */
@LoadLevel(name = "db", scope = Scope.SINGLETON)
public class DataBaseDistributedLocker implements DistributedLocker {

    DistributedLocker distributedLocker;

    /**
     * Instantiates a new Log store data base dao.
     */
    public DataBaseDistributedLocker() {
        this.distributedLocker = DataBaseStoreFactory.getDistributedLocker(ConfigurationFactory.getInstance()
            .getConfig(ConfigurationKeys.STORE_DB_STORE_TYPE, DataBaseStoreType.jdbc.name()));
    }

    @Override
    public boolean acquireLock(DistributedLockDO distributedLockDO) {
        return this.distributedLocker.acquireLock(distributedLockDO);
    }

    @Override
    public boolean releaseLock(DistributedLockDO distributedLockDO) {
        return this.distributedLocker.releaseLock(distributedLockDO);
    }

}
