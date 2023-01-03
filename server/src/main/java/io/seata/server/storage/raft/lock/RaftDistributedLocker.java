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
package io.seata.server.storage.raft.lock;

import io.seata.common.loader.LoadLevel;
import io.seata.server.cluster.raft.RaftServerFactory;
import io.seata.core.store.DistributedLockDO;
import io.seata.core.store.DistributedLocker;
import io.seata.server.storage.redis.lock.RedisDistributedLocker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @description raft distributed lock
 * @author  funkye
 */
@LoadLevel(name = "raft")
public class RaftDistributedLocker implements DistributedLocker {

    protected static final Logger LOGGER = LoggerFactory.getLogger(
            RedisDistributedLocker.class);

    /**
     * Acquire the distributed lock
     *
     * @param distributedLockDO
     * @return
     */
    @Override
    public boolean acquireLock(DistributedLockDO distributedLockDO) {
        return RaftServerFactory.getInstance().isLeader();
    }

    /**
     * Release the distributed lock
     *
     * @param distributedLockDO
     * @return
     */
    @Override
    public boolean releaseLock(DistributedLockDO distributedLockDO) {
        return true;
    }
    
}
