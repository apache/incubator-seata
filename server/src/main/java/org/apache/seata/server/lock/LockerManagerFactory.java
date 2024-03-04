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
package org.apache.seata.server.lock;

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.server.store.StoreConfig;
import org.apache.seata.server.store.StoreConfig.LockMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Lock manager factory.
 *
 */
public class LockerManagerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(LockerManagerFactory.class);
    private static final Configuration CONFIG = ConfigurationFactory.getInstance();

    /**
     * the lock manager
     */
    private static volatile LockManager LOCK_MANAGER;

    /**
     * Get lock manager.
     *
     * @return the lock manager
     */
    public static LockManager getLockManager() {
        init();
        return LOCK_MANAGER;
    }

    public static void init() {
        init(null);
    }

    public static void destroy() {
        LOCK_MANAGER = null;
    }

    public static void init(LockMode lockMode) {
        if (LOCK_MANAGER == null) {
            synchronized (LockerManagerFactory.class) {
                if (LOCK_MANAGER == null) {
                    if (null == lockMode) {
                        lockMode = StoreConfig.getLockMode();
                    }
                    LOGGER.info("use lock store mode: {}", lockMode.getName());
                    //if not exist the lock mode, throw exception
                    if (null != StoreConfig.StoreMode.get(lockMode.name())) {
                        LOCK_MANAGER = EnhancedServiceLoader.load(LockManager.class, lockMode.getName());
                    }
                }
            }
        }
    }

}
