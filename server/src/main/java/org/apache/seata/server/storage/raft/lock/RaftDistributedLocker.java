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
package org.apache.seata.server.storage.raft.lock;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.server.cluster.raft.RaftServerManager;
import org.apache.seata.core.store.DistributedLockDO;
import org.apache.seata.core.store.DistributedLocker;
import org.apache.seata.server.storage.redis.lock.RedisDistributedLocker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.seata.common.DefaultValues.DEFAULT_SEATA_GROUP;

/**
 * @description raft distributed lock
 */
@LoadLevel(name = "raft")
public class RaftDistributedLocker implements DistributedLocker {

    protected static final Logger LOGGER = LoggerFactory.getLogger(
            RedisDistributedLocker.class);

    private final String group = ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.SERVER_RAFT_GROUP, DEFAULT_SEATA_GROUP);

    /**
     * Acquire the distributed lock
     *
     * @param distributedLockDO distributedLockDO
     * @return boolean
     */
    @Override
    public boolean acquireLock(DistributedLockDO distributedLockDO) {
        return RaftServerManager.isLeader(group);
    }

    /**
     * Release the distributed lock
     *
     * @param distributedLockDO distributedLockDO
     * @return boolean
     */
    @Override
    public boolean releaseLock(DistributedLockDO distributedLockDO) {
        return true;
    }
    
}
