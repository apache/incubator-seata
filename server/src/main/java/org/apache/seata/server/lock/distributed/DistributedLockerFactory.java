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
package org.apache.seata.server.lock.distributed;

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.loader.EnhancedServiceNotFoundException;
import org.apache.seata.core.store.DefaultDistributedLocker;
import org.apache.seata.core.store.DistributedLocker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Distributed locker factory
 */
public class DistributedLockerFactory {

    /**
     * The constant LOGGER.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DistributedLockerFactory.class);

    private static volatile DistributedLocker DISTRIBUTED_LOCKER = null;

    /**
     * Get the distributed locker by lockerType
     *
     * @param lockerType the locker type
     * @return the distributed locker
     */
    public static DistributedLocker getDistributedLocker(String lockerType) {
        if (DISTRIBUTED_LOCKER == null) {
            synchronized (DistributedLocker.class) {
                if (DISTRIBUTED_LOCKER == null) {
                    DistributedLocker distributedLocker = null;
                    try {
                        if (!"file".equals(lockerType)) {
                            distributedLocker = EnhancedServiceLoader.load(DistributedLocker.class, lockerType);
                        }
                    } catch (EnhancedServiceNotFoundException ex) {
                        LOGGER.error("Get distributed locker failed: {}", ex.getMessage(), ex);
                    }
                    if (distributedLocker == null) {
                        distributedLocker = new DefaultDistributedLocker();
                    }
                    DISTRIBUTED_LOCKER = distributedLocker;
                }
            }
        }
        return DISTRIBUTED_LOCKER;
    }

    public static void cleanLocker() {
        DISTRIBUTED_LOCKER = null;
    }
}
