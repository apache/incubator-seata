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
package io.seata.server.lock;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.server.store.StoreConfig;
import io.seata.server.store.StoreConfig.LockMode;

/**
 * The type Lock manager factory.
 *
 * @author sharajava
 */
public class LockerManagerFactory {

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
        if (LOCK_MANAGER == null) {
            init();
        }
        return LOCK_MANAGER;
    }

    public static void init() {
        init(null);
    }

    public static void init(LockMode lockMode) {
        if (LOCK_MANAGER == null) {
            synchronized (LockerManagerFactory.class) {
                if (LOCK_MANAGER == null) {
                    if (null == lockMode) {
                        lockMode = StoreConfig.getLockMode();
                    }
                    //if not exist the lock mode, throw exception
                    if (null != StoreConfig.StoreMode.get(lockMode.name())) {
                        LOCK_MANAGER = EnhancedServiceLoader.load(LockManager.class, lockMode.getName());
                    }
                }
            }
        }
    }

}
